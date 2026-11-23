package com.teaql.merchantorderprivacyservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.SensitiveDiagnosticTextRuntimeLogSink;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.reference.AeadRoundTripReferenceProvider;
import io.teaql.runtime.reference.DeploymentProfile;
import io.teaql.runtime.reference.RoundTripReferenceKey;
import io.teaql.runtime.reference.StaticRoundTripReferenceKeyProvider;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class MerchantOrderPrivacyTest {
    private static CustomerOrder order(UserContext context, Merchant merchant, String reference, String email) {
        var profile = Q.customerProfiles().comment("Prepare a delivery profile")
                .purpose("Associate customer delivery data with the authenticated merchant").newEntity(context);
        profile.updateMerchant(merchant);
        profile.updateEmail(email);
        profile.auditAs("Record the customer's delivery profile").save(context);
        var order = Q.customerOrders().comment("Prepare a dispatch order")
                .purpose("Keep the merchant's dispatch and its delivery profile together").newEntity(context);
        order.updateMerchant(merchant);
        order.updateCustomerProfile(profile);
        order.updateOrderNumber(reference);
        order.updateFulfillmentStatus("ready");
        order.auditAs("Queue the merchant dispatch").save(context);
        return order;
    }

    private static CustomerOrder lookup(UserContext context, long merchant, long id, String reference, String email) {
        return Q.customerOrders().withIdIs(id).withOrderNumberIs(reference)
                .withMerchantMatching(Q.merchantsWithMinimalFields().withIdIs(merchant))
                .withCustomerProfileMatching(Q.customerProfilesWithMinimalFields().withEmailIs(email))
                .selectCustomerProfile().limit(1)
                .comment("Read the scoped dispatch and its delivery profile")
                .purpose("Show a dispatch belonging to the authenticated merchant")
                .executeForOne(context);
    }

    private static UserContext principal(TeaQLRuntime runtime, long merchant) {
        return new DefaultUserContext(runtime).withTrustedReferencePrincipal(
                new TrustedReferencePrincipal("local-fixture", "dispatcher", "Merchant", merchant));
    }

    @Test public void governedOrderAndItsPrivacyBoundaries() throws Exception {
        String round = System.getenv("TEAQL_SHOWCASE_ROUND");
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_ORDER_PRIVACY_DATABASE"));
        var jdbc = new JdbcSqlExecutor(source);
        var sqlite = new SqliteDataServiceExecutor("sqlite", jdbc, source);
        var ids = new IdSpaceIdGenerator(new WorkspaceTeaQLDatabase(jdbc, source));
        ids.ensureIdSpaceTable();

        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x27);
        var provider = AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST, "merchant-dispatch",
                "local-fixture", new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("fixture", key)),
                (caller, principal, scope, identity) -> {
                    if (!"dispatcher".equals(principal.subject()) || !"Merchant".equals(principal.domainRootType())
                            || principal.domainRootId() != 1 || !"view-dispatch".equals(scope.purpose())
                            || !"CustomerOrder".equals(scope.aggregateType()) || !"CustomerOrder".equals(identity.entityType())) {
                        throw new RoundTripReferenceException(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "The principal cannot use this dispatch reference");
                    }
                });
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", sqlite).dataService("sqlite", sqlite)
                .idGenerationService(ids).roundTripReferenceProvider(provider).build().install(GeneratedRuntimeModule.module());
        var context = principal(runtime, 1);
        context.ensureSchema();
        var merchant = Q.merchantsWithMinimalFields().withIdIs(1L).limit(1)
                .comment("Read the initialized merchant").purpose("Set the trusted dispatch scope").executeForOne(context);
        assertNotNull(merchant);
        var other = Q.merchants().comment("Prepare an independent merchant")
                .purpose("Prove that a second merchant's order remains outside the current scope").newEntity(context);
        other.updateName("Independent retailer " + round);
        other.auditAs("Register a second merchant").save(context);

        String email = "sealed-" + round + "@example.test";
        String foreignEmail = "foreign-" + round + "@example.test";
        String reference = "SHIP-PRIVATE-" + round;
        String foreignReference = "SHIP-OTHER-" + round;
        var own = order(context, merchant, reference, email);
        var foreign = order(context, other, foreignReference, foreignEmail);
        var scope = new ReferenceDocumentScope("dispatch-" + own.getId(), "view-dispatch", "CustomerOrder", own.getId(), own.getVersion());
        Object token = context.referenceFor(own, scope, Duration.ofMinutes(5));
        assertTrue(token.toString().startsWith("tqr1."));
        var wrongPurpose = new ReferenceDocumentScope(scope.documentId(), "edit-dispatch", scope.aggregateType(), scope.aggregateId(), scope.aggregateRevision());
        assertThrows(RoundTripReferenceException.class, () -> context.resolveReference(token, wrongPurpose, "CustomerOrder"));
        assertThrows(RoundTripReferenceException.class, () -> principal(runtime, other.getId()).resolveReference(token, scope, "CustomerOrder"));
        var claims = context.resolveReference(token, scope, "CustomerOrder");
        var row = lookup(context, merchant.getId(), claims.id(), reference, email);
        assertNotNull(row);
        assertEquals(claims.version(), row.getVersion().longValue());
        assertEquals(email, E.customerOrder(row).getCustomerProfile().eval().getEmail());
        assertNull(lookup(context, merchant.getId(), foreign.getId(), foreignReference, foreignEmail));

        try (var output = new PrintStream(Files.newOutputStream(Path.of(System.getenv("TEAQL_ORDER_PRIVACY_DEBUG_FILE"))),
                true, StandardCharsets.UTF_8)) {
            var isolatedDiagnostic = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", sqlite).dataService("sqlite", sqlite).idGenerationService(ids)
                    .logSink(new SensitiveDiagnosticTextRuntimeLogSink(output))
                    .build().install(GeneratedRuntimeModule.module());
            var checked = lookup(principal(isolatedDiagnostic, merchant.getId()), merchant.getId(), claims.id(), reference, email);
            assertNotNull(checked);
            assertEquals(row.getId(), checked.getId());
        }

        assertTrue(CustomerOrder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
                .endsWith("/java-lib-core/lib/target/classes/"));
        System.out.println("ORDER_PRIVACY=" + new ObjectMapper().writeValueAsString(Map.of(
                "round", round, "authorizedOrder", row.getId(), "foreignOrder", foreign.getId(),
                "merchant", merchant.getId(), "foreignMerchant", other.getId(),
                "purposeDenied", true, "foreignDenied", true, "version", row.getVersion())));
    }
}
