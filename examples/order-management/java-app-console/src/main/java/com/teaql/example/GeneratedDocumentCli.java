package com.teaql.example;

import com.teaql.ordermanagementservice.EntityMetaRegistry;
import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ContextBoundDocumentService.AcceptRequest;
import io.teaql.core.reference.ContextBoundDocumentService.OpenRequest;
import io.teaql.core.reference.ContextBoundDocumentService.SubmittedDocument;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.view.PreparedView;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationService;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.secure.document.reference.AeadRoundTripReferenceProvider;
import io.teaql.secure.document.reference.DeploymentProfile;
import io.teaql.secure.document.reference.RoundTripReferenceKey;
import io.teaql.secure.document.reference.SecureDocumentReferenceModule;
import io.teaql.secure.document.reference.StaticRoundTripReferenceKeyProvider;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.sqlite.SQLiteDataSource;

/** No Web container: generated entities, protected document and one physical SQLite transaction. */
public final class GeneratedDocumentCli {
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneratedDocumentCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("This example uses its own temporary database");
        Path database = Files.createTempFile("teaql-generated-document-cli-", ".sqlite");
        try {
            run(database);
        } finally {
            Files.deleteIfExists(database);
        }
    }

    private static void run(Path database) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + database);
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(source);
        SqliteDataServiceExecutor executor = new SqliteDataServiceExecutor("sqlite", jdbc, source);
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        new EntityMetaRegistry().assemble(metadata);
        AtomicLong ids = new AtomicLong(1000);
        InternalIdGenerationService idGeneration = (context, entity) -> ids.getAndIncrement();
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        AeadRoundTripReferenceProvider provider = AeadRoundTripReferenceProvider.governed(
                DeploymentProfile.DEVELOPMENT, "generated-order-cli", "ephemeral-sqlite",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("ephemeral", key)),
                (context, principal, scope, identity) -> {
                    List<Map<String, Object>> grants = jdbc.queryForList(
                            "SELECT can_edit FROM generated_document_permissions "
                                    + "WHERE subject=? AND customer_id=?",
                            new Object[] {principal.subject(), principal.domainRootId()});
                    if (!"Customer".equals(principal.domainRootType())
                            || !"CustomerOrder".equals(scope.aggregateType())
                            || grants.size() != 1
                            || ((Number) grants.get(0).get("can_edit")).intValue() != 1) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "AUTHORIZATION_REQUIRED");
                    }
                });
        try (TeaQLRuntime runtime = SecureDocumentReferenceModule.of(provider)
                .installInto(TeaQLRuntime.builder().metadata(metadata)
                        .dataService("default", executor).dataService("sqlite", executor)
                        .idGenerationService(idGeneration)
                        .viewAuthorization(new ViewAuthorization() {
                            @Override public Identity identity(UserContext context) {
                                TrustedReferencePrincipal principal = context.trustedReferencePrincipal();
                                return new Identity(principal.authenticationRealm(), principal.subject(),
                                        principal.domainRootType() + ":" + principal.domainRootId());
                            }
                            @Override public boolean canReadEntity(
                                    UserContext context, EntityTarget entity, String path) { return false; }
                            @Override public FieldDecision field(UserContext context, EntityTarget entity,
                                    String field, String path, ViewOptions.Mode mode) {
                                return FieldDecision.hide();
                            }
                        })).build()) {
            UserContext context = new DefaultUserContext(runtime);
            context.ensureSchema();
            GeneratedOrderDocumentService documents = new GeneratedOrderDocumentService(jdbc);
            documents.ensureDocumentTables();
            CommercePlatform platform = new CommercePlatform().updateName("Ephemeral CLI platform");
            platform.auditAs("seed generated document CLI platform").save(context);
            Customer customer = new Customer().updateName("Ephemeral CLI customer")
                    .updateEmail("cli@example.invalid").updateCommercePlatform(platform);
            customer.auditAs("seed generated document CLI customer").save(context);
            CustomerOrder order = new CustomerOrder().updateOrderNumber("CLI-DOC-1001")
                    .updateOrderDate(LocalDate.of(2026, 10, 26))
                    .updateTotalAmount(new BigDecimal("25.00"))
                    .updateStatusToPending().updateCustomer(customer).updateCommercePlatform(platform);
            order.auditAs("seed generated document CLI Order").save(context);
            new OrderLine().updateCustomerOrder(order).updateProductName("Blue cup")
                    .updateSku("CUP-1").updateQuantity(2).updateCommercePlatform(platform)
                    .auditAs("seed generated document CLI item").save(context);
            jdbc.update("INSERT INTO generated_document_permissions(subject,customer_id,can_edit) "
                    + "VALUES(?,?,1)", new Object[] {"alice", customer.getId()});
            context.withTrustedReferencePrincipal(new TrustedReferencePrincipal(
                    "oidc", "alice", "Customer", customer.getId()));
            context.withContextBoundDocumentService(documents);
            ViewPreparationService views = context.capability(ViewPreparationService.class);
            ViewOptions query = new ViewOptions(ViewOptions.Mode.GOVERNED,
                    ViewOptions.BusinessCompletion.QUERY, null, Duration.ofMinutes(15), false);
            PreparedView opened = views.prepare(context,
                    context.openDocumentForView(new OpenRequest("CustomerOrder", "CLI-DOC-1001",
                            GeneratedOrderDocumentService.PURPOSE, Duration.ofMinutes(15))), query);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) opened.value();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("items");
            List<Map<String, Object>> changed = new ArrayList<>();
            for (Map<String, Object> row : rows) changed.add(new LinkedHashMap<>(row));
            changed.get(0).put("quantity", 5);
            Map<String, Object> submittedBody = Map.of("orderNumber", "CLI-DOC-1001", "items", changed);
            Map<String, String> credentials = opened.documentCredentials();
            var accepted = context.acceptDocumentForView(new AcceptRequest("CustomerOrder",
                    GeneratedOrderDocumentService.PURPOSE,
                    new SubmittedDocument(credentials.get("businessId"),
                            credentials.get("documentToken"), "cli-generated-command", submittedBody)));
            ViewOptions committed = new ViewOptions(ViewOptions.Mode.GOVERNED,
                    ViewOptions.BusinessCompletion.COMMITTED, null, Duration.ofMinutes(15), false);
            PreparedView presented = views.prepare(context, accepted.document(), committed);
            var lines = Q.orderLines().orderByIdAscending();
            lines.top(2);
            var saved = Q.customerOrders().withOrderNumberIs("CLI-DOC-1001")
                    .selectOrderLineListWith(lines)
                    .comment("Verify generated document CLI persistence")
                    .purpose("Confirm the generated document round trip")
                    .executeForOne(context);
            long receipts = ((Number) jdbc.queryForList(
                    "SELECT count(*) AS count FROM generated_document_commands", new Object[0])
                    .get(0).get("count")).longValue();
            if (accepted.newRevision() <= accepted.previousRevision()
                    || presented.documentCredentials().get("documentToken") == null
                    || saved.getVersion() != accepted.newRevision()
                    || saved.getOrderLineList().size() != 1
                    || saved.getOrderLineList().get(0).getQuantity() != 5
                    || receipts != 1) {
                throw new IllegalStateException("Generated document round trip did not complete");
            }
            System.out.println("GENERATED_DOCUMENT_CLI_PASS order=CLI-DOC-1001 previousVersion="
                    + accepted.previousRevision() + " newVersion=" + accepted.newRevision()
                    + " quantity=5 commandReceipts=1");
        }
    }
}
