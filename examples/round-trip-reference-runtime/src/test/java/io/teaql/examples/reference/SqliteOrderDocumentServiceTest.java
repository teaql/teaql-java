package io.teaql.examples.reference;

import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ContextBoundDocumentService.AcceptRequest;
import io.teaql.core.reference.ContextBoundDocumentService.AcceptedDocument;
import io.teaql.core.reference.ContextBoundDocumentService.ContextualDocument;
import io.teaql.core.reference.ContextBoundDocumentService.OpenRequest;
import io.teaql.core.reference.ContextBoundDocumentService.SubmittedDocument;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.view.AcceptedDocumentView;
import io.teaql.core.view.DocumentView;
import io.teaql.core.view.PreparedView;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.core.view.ViewPreparationService;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.secure.document.reference.AeadRoundTripReferenceProvider;
import io.teaql.secure.document.reference.DeploymentProfile;
import io.teaql.secure.document.reference.RoundTripReferenceKey;
import io.teaql.secure.document.reference.SecureDocumentReferenceModule;
import io.teaql.secure.document.reference.StaticRoundTripReferenceKeyProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.After;
import org.junit.Test;

public class SqliteOrderDocumentServiceTest {
    private static final String NUMBER = "DOC-SQLITE-1001";
    private static final String PURPOSE = SqliteOrderDocumentService.PURPOSE;
    private String url;
    private Path database;
    private UserContext alice;

    private void setup() throws Exception {
        database = Files.createTempFile("teaql-java-document-", ".sqlite");
        url = "jdbc:sqlite:" + database;
        sql("CREATE TABLE orders(id INTEGER PRIMARY KEY, version INTEGER NOT NULL, "
                + "customer_id INTEGER NOT NULL, order_number TEXT NOT NULL UNIQUE)");
        sql("CREATE TABLE order_items(id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL, "
                + "version INTEGER NOT NULL, product_name TEXT NOT NULL, sku TEXT NOT NULL, "
                + "quantity INTEGER NOT NULL)");
        sql("CREATE TABLE document_permissions(subject TEXT NOT NULL, customer_id INTEGER NOT NULL, "
                + "can_edit INTEGER NOT NULL, PRIMARY KEY(subject, customer_id))");
        sql("CREATE TABLE document_commands(subject TEXT NOT NULL, command_id TEXT NOT NULL, "
                + "digest BLOB NOT NULL, previous_version INTEGER NOT NULL, new_version INTEGER NOT NULL, "
                + "changed_entities TEXT NOT NULL, result_token TEXT NOT NULL, result_body TEXT NOT NULL, "
                + "PRIMARY KEY(subject, command_id))");
        sql("INSERT INTO orders VALUES(10,1,7,'" + NUMBER + "')");
        sql("INSERT INTO order_items VALUES(101,10,1,'Blue cup','CUP-1',2)");
        sql("INSERT INTO order_items VALUES(102,10,1,'Red cup','CUP-2',3)");
        sql("INSERT INTO document_permissions VALUES('alice',7,1)");
        alice = context("alice");
    }

    @After
    public void cleanup() throws Exception {
        if (database != null) Files.deleteIfExists(database);
    }

    private UserContext context(String subject) {
        byte[] secret = new byte[32];
        Arrays.fill(secret, (byte) 0x47);
        AeadRoundTripReferenceProvider provider = AeadRoundTripReferenceProvider.governed(
                DeploymentProfile.DEVELOPMENT, "generated-order-service", "sqlite-e2e",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1", secret)),
                (context, principal, scope, identity) -> {
                    if (!"Customer".equals(principal.domainRootType())
                            || principal.domainRootId() != 7
                            || !"Order".equals(scope.aggregateType())
                            || scope.aggregateId() != 10
                            || !"alice".equals(principal.subject())
                            || grant(principal.subject()) != 1) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "AUTHORIZATION_REQUIRED");
                    }
                });
        TeaQLRuntime runtime = SecureDocumentReferenceModule.of(provider)
                .installInto(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
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
                        }))
                .build();
        return new DefaultUserContext(runtime)
                .withTrustedReferencePrincipal(new TrustedReferencePrincipal("oidc", subject, "Customer", 7))
                .withContextBoundDocumentService(new SqliteOrderDocumentService(url));
    }

    private int grant(String subject) {
        try (Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT can_edit FROM document_permissions WHERE subject='" + subject + "'")) {
            return result.next() ? result.getInt(1) : 0;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void sql(String statement) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                Statement sql = connection.createStatement()) {
            sql.execute(statement);
        }
    }

    private long scalar(String query) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query)) {
            Assert.assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static OpenRequest openRequest() {
        return new OpenRequest("Order", NUMBER, PURPOSE, Duration.ofMinutes(15));
    }

    private static ViewOptions viewOptions(ViewOptions.BusinessCompletion completion) {
        return new ViewOptions(ViewOptions.Mode.GOVERNED, completion,
                null, Duration.ofMinutes(15), false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> edit(
            ContextualDocument document, long quantity, boolean removeSecond) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) document.body().get("items");
        List<Map<String, Object>> copy = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            if (index == 1 && removeSecond) continue;
            Map<String, Object> item = new LinkedHashMap<>(rows.get(index));
            if (index == 0) item.put("quantity", quantity);
            copy.add(item);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNumber", NUMBER);
        body.put("items", copy);
        if (removeSecond) body.put("removedRefs", List.of("item:1"));
        return body;
    }

    private static AcceptRequest accept(
            ContextualDocument document, String command, Map<String, Object> body) {
        return new AcceptRequest("Order", PURPOSE,
                new SubmittedDocument(NUMBER, document.documentToken(), command, body));
    }

    private static void fails(RoundTripReferenceErrorCode code, Runnable call) {
        RoundTripReferenceException thrown = Assert.assertThrows(
                RoundTripReferenceException.class, call::run);
        Assert.assertEquals(code, thrown.getCode());
    }

    @Test
    public void realSqliteEditRemovalAndIdempotentReplay() throws Exception {
        setup();
        ContextualDocument opened = alice.openDocument(openRequest());
        Assert.assertTrue(opened.documentToken().startsWith("tqd1.k1."));
        Assert.assertFalse(opened.toString().contains(opened.documentToken()));
        AcceptRequest request = accept(opened, "command-1", edit(opened, 5, true));
        AcceptedDocument accepted = alice.acceptDocument(request);
        Assert.assertEquals(1, accepted.previousRevision());
        Assert.assertEquals(2, accepted.newRevision());
        Assert.assertEquals(2, accepted.changedEntities().size());
        Assert.assertEquals(5, scalar("SELECT quantity FROM order_items WHERE id=101"));
        Assert.assertEquals(0, scalar("SELECT count(*) FROM order_items WHERE id=102"));
        Assert.assertEquals(2, scalar("SELECT version FROM orders WHERE id=10"));
        AcceptedDocument repeated = alice.acceptDocument(request);
        Assert.assertEquals(accepted.document().documentToken(), repeated.document().documentToken());
        Assert.assertEquals(2, scalar("SELECT version FROM orders WHERE id=10"));
        fails(RoundTripReferenceErrorCode.DOCUMENT_COMMAND_CONFLICT,
                () -> alice.acceptDocument(accept(opened, "command-1", edit(opened, 6, true))));
    }

    @Test
    public void invalidProjectionAndStaleVersionNeverWrite() throws Exception {
        setup();
        ContextualDocument opened = alice.openDocument(openRequest());
        Map<String, Object> bad = edit(opened, 5, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) bad.get("items");
        rows.get(0).put("productName", "forged");
        fails(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> alice.acceptDocument(accept(opened, "bad-readonly", bad)));
        Map<String, Object> missing = edit(opened, 5, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> missingRows = (List<Map<String, Object>>) missing.get("items");
        missingRows.remove(1);
        fails(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> alice.acceptDocument(accept(opened, "missing-row", missing)));
        Assert.assertEquals(1, scalar("SELECT version FROM orders WHERE id=10"));
        Assert.assertEquals(2, scalar("SELECT quantity FROM order_items WHERE id=101"));
        sql("UPDATE orders SET version=2 WHERE id=10");
        fails(RoundTripReferenceErrorCode.DOCUMENT_REVISION_CONFLICT,
                () -> alice.acceptDocument(accept(opened, "stale", edit(opened, 5, false))));
        Assert.assertEquals(2, scalar("SELECT quantity FROM order_items WHERE id=101"));
    }

    @Test
    public void revokedAndDifferentActorCannotUseSnapshot() throws Exception {
        setup();
        ContextualDocument opened = alice.openDocument(openRequest());
        UserContext bob = context("bob");
        fails(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> bob.acceptDocument(accept(opened, "bob-command", edit(opened, 5, false))));
        sql("UPDATE document_permissions SET can_edit=0 WHERE subject='alice'");
        fails(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                () -> alice.acceptDocument(accept(opened, "revoked", edit(opened, 5, false))));
        Assert.assertEquals(1, scalar("SELECT version FROM orders WHERE id=10"));
        Assert.assertEquals(0, scalar("SELECT count(*) FROM document_commands"));
    }

    @Test
    public void failedDatabaseWriteRollsBackRootRevisionAndCommand() throws Exception {
        setup();
        ContextualDocument opened = alice.openDocument(openRequest());
        sql("CREATE TRIGGER fail_item_update BEFORE UPDATE ON order_items "
                + "BEGIN SELECT RAISE(ABORT,'test rejected item update'); END");
        fails(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED,
                () -> alice.acceptDocument(accept(opened, "failed-write", edit(opened, 5, false))));
        Assert.assertEquals(1, scalar("SELECT version FROM orders WHERE id=10"));
        Assert.assertEquals(2, scalar("SELECT quantity FROM order_items WHERE id=101"));
        Assert.assertEquals(0, scalar("SELECT count(*) FROM document_commands"));
        sql("DROP TRIGGER fail_item_update");
        Assert.assertEquals(2, alice.acceptDocument(
                accept(opened, "failed-write", edit(opened, 5, false))).newRevision());
    }

    @Test
    public void submittedBodyIsFrozenBeforeDigestAndDirectPrincipalMustMatchContext() throws Exception {
        setup();
        ContextualDocument opened = alice.openDocument(openRequest());
        Map<String, Object> mutable = edit(opened, 5, false);
        SubmittedDocument submitted = new SubmittedDocument(
                NUMBER, opened.documentToken(), "frozen-command", mutable);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) mutable.get("items");
        rows.get(0).put("quantity", 99);
        Assert.assertEquals(2, alice.acceptDocument(
                new AcceptRequest("Order", PURPOSE, submitted)).newRevision());
        Assert.assertEquals(5, scalar("SELECT quantity FROM order_items WHERE id=101"));
        fails(RoundTripReferenceErrorCode.CONTEXT_MISMATCH,
                () -> alice.contextBoundDocumentService().openDocument(
                        alice, new TrustedReferencePrincipal("oidc", "bob", "Customer", 7),
                        openRequest()));
    }

    @Test
    public void adoptedOpenAndAcceptedDocumentUseOneTokenAndRecheckCurrentAuthority()
            throws Exception {
        setup();
        ViewPreparationService views = alice.capability(ViewPreparationService.class);
        DocumentView opened = alice.openDocumentForView(openRequest());
        ViewOptions query = viewOptions(ViewOptions.BusinessCompletion.QUERY);
        PreparedView prepared = views.prepare(alice, opened, query);
        String token = prepared.documentCredentials().get("documentToken");
        Assert.assertTrue(token.startsWith("tqd1.k1."));
        Assert.assertEquals(NUMBER, prepared.documentCredentials().get("businessId"));
        Assert.assertFalse(prepared.toString().contains(token));
        Assert.assertFalse(opened.toString().contains(token));
        Assert.assertSame(prepared, views.prepare(alice, prepared, query));
        Assert.assertEquals(token, views.prepare(alice, opened, query)
                .documentCredentials().get("documentToken"));
        UserContext anotherContext = context("alice");
        Assert.assertEquals(ViewPreparationException.Code.VIEW_PREPARED_MISMATCH,
                Assert.assertThrows(ViewPreparationException.class,
                        () -> anotherContext.capability(ViewPreparationService.class)
                                .prepare(anotherContext, opened, query)).code());
        var originalService = alice.contextBoundDocumentService();
        alice.withContextBoundDocumentService(new SqliteOrderDocumentService(url));
        Assert.assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                Assert.assertThrows(ViewPreparationException.class,
                        () -> views.prepare(alice, prepared, query)).code());
        alice.withContextBoundDocumentService(originalService);
        Assert.assertEquals(ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,
                Assert.assertThrows(ViewPreparationException.class,
                        () -> views.prepare(alice, Map.of("documentToken", token, "prepared", true),
                                query)).code());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) prepared.value();
        Map<String, Object> changed = edit(
                new ContextualDocument(NUMBER, token, body), 5, false);
        AcceptedDocumentView accepted = alice.acceptDocumentForView(accept(
                new ContextualDocument(NUMBER, token, body), "view-command", changed));
        Assert.assertEquals(2, accepted.newRevision());
        ViewOptions committed = viewOptions(ViewOptions.BusinessCompletion.COMMITTED);
        PreparedView afterSave = views.prepare(alice, accepted.document(), committed);
        Assert.assertEquals(5, scalar("SELECT quantity FROM order_items WHERE id=101"));
        Assert.assertNotEquals(token, afterSave.documentCredentials().get("documentToken"));
        Assert.assertTrue(afterSave.documentContext().isPresent());
        Assert.assertEquals(ViewPreparationException.Code.VIEW_PREPARED_MISMATCH,
                Assert.assertThrows(ViewPreparationException.class,
                        () -> views.prepare(alice, accepted.document(), query)).code());
        sql("UPDATE document_permissions SET can_edit=0 WHERE subject='alice'");
        Assert.assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                Assert.assertThrows(ViewPreparationException.class,
                        () -> views.prepare(alice, afterSave, committed)).code());
    }

    @Test
    public void postCommitViewFailureReportsCommittedStateWithoutReplayingMutation() throws Exception {
        setup();
        DocumentView opened = alice.openDocumentForView(openRequest());
        PreparedView prepared = alice.capability(ViewPreparationService.class)
                .prepare(alice, opened, viewOptions(ViewOptions.BusinessCompletion.QUERY));
        String token = prepared.documentCredentials().get("documentToken");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) prepared.value();
        sql("CREATE TRIGGER revoke_on_command AFTER INSERT ON document_commands "
                + "BEGIN UPDATE document_permissions SET can_edit=0 WHERE subject='alice'; END");
        ViewPreparationException failed = Assert.assertThrows(ViewPreparationException.class,
                () -> alice.acceptDocumentForView(accept(
                        new ContextualDocument(NUMBER, token, body), "committed-view-failure",
                        edit(new ContextualDocument(NUMBER, token, body), 5, false))));
        Assert.assertTrue(failed.businessCommitted());
        Assert.assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED, failed.code());
        Assert.assertEquals(2, scalar("SELECT version FROM orders WHERE id=10"));
        Assert.assertEquals(5, scalar("SELECT quantity FROM order_items WHERE id=101"));
        Assert.assertEquals(1, scalar("SELECT count(*) FROM document_commands"));
    }
}
