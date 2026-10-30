package com.teaql.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineRequest;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.reference.ContextBoundDocumentService;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedDocumentSnapshot;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Application-owned document adapter backed by generated TeaQL Order entities and one JDBC owner. */
public final class GeneratedOrderDocumentService implements ContextBoundDocumentService {
    public static final String PURPOSE = "edit-generated-order-items";
    public static final String MODEL = "order-management-generated-java-v1";
    private static final String AGGREGATE = "CustomerOrder";
    private static final String RELATION = "orderLineList";
    private static final Duration ACCEPT_LIFETIME = Duration.ofMinutes(15);
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private final JdbcSqlExecutor jdbc;

    public GeneratedOrderDocumentService(JdbcSqlExecutor jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    /** Optional application tables share the same physical JDBC adapter as generated saves. */
    public void ensureDocumentTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS generated_document_permissions("
                + "subject TEXT NOT NULL, customer_id BIGINT NOT NULL, can_edit INTEGER NOT NULL, "
                + "PRIMARY KEY(subject,customer_id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS generated_document_commands("
                + "subject TEXT NOT NULL, command_id TEXT NOT NULL, digest BLOB NOT NULL, "
                + "previous_version BIGINT NOT NULL, new_version BIGINT NOT NULL, "
                + "changed_entities TEXT NOT NULL, result_token TEXT NOT NULL, result_body TEXT NOT NULL, "
                + "PRIMARY KEY(subject,command_id))");
    }

    private record Change(OrderLine line, int quantity) {}
    private record Mutation(List<Change> changes, List<OrderLine> removals) {}
    private record Receipt(long previous, long next, List<InternalEntityIdentity> changed,
                           ContextualDocument document) {}

    @Override public boolean supportsViewPreparation() { return true; }

    @Override public void validatePreparedDocument(
            UserContext context, TrustedReferencePrincipal principal, VerifiedDocumentSnapshot verified) {
        currentPrincipal(context, principal);
        DocumentSnapshot snapshot = verified.snapshot();
        if (!MODEL.equals(snapshot.modelFingerprint())
                || !PURPOSE.equals(snapshot.scope().purpose())
                || !AGGREGATE.equals(snapshot.scope().aggregateType())) throw projection();
        authorize(principal);
        CustomerOrder order = loadRoot(context, snapshot.businessId());
        owner(principal, order);
        if (!order.getId().equals(snapshot.aggregate().id())
                || !order.getVersion().equals(snapshot.aggregate().version())) throw revision();
    }

    @Override public ContextualDocument openDocument(
            UserContext context, TrustedReferencePrincipal principal, OpenRequest request) {
        currentPrincipal(context, principal);
        scope(request.aggregateType(), request.purpose());
        AtomicReference<ContextualDocument> result = new AtomicReference<>();
        jdbc.executeInTransaction(() -> {
            authorize(principal);
            CustomerOrder order = loadFull(context, request.businessId());
            owner(principal, order);
            result.set(render(context, order, request.lifetime()));
        });
        return result.get();
    }

    @Override public AcceptedDocument acceptDocument(
            UserContext context, TrustedReferencePrincipal principal, AcceptRequest request) {
        currentPrincipal(context, principal);
        scope(request.aggregateType(), request.purpose());
        SubmittedDocument submitted = request.submitted();
        DocumentSnapshot snapshot = context.consumeDocumentSnapshot(
                submitted.documentToken(), AGGREGATE, PURPOSE).snapshot();
        if (!MODEL.equals(snapshot.modelFingerprint())
                || !snapshot.businessId().equals(submitted.businessId())) throw projection();
        byte[] digest = digest(submitted);
        AtomicReference<AcceptedDocument> result = new AtomicReference<>();
        jdbc.executeInTransaction(() -> {
            authorize(principal);
            CustomerOrder order = loadFull(context, snapshot.businessId());
            owner(principal, order);
            Receipt earlier = receipt(principal.subject(), submitted.commandId(), digest);
            if (earlier != null) {
                result.set(new AcceptedDocument(
                        earlier.document(), earlier.previous(), earlier.next(), earlier.changed()));
                return;
            }
            if (!order.getId().equals(snapshot.aggregate().id())
                    || !order.getVersion().equals(snapshot.aggregate().version())) throw revision();
            Mutation mutation = validate(snapshot, submitted.body(), order);
            long previous = order.getVersion();
            List<InternalEntityIdentity> changed = new ArrayList<>();
            if (!mutation.changes().isEmpty() || !mutation.removals().isEmpty()) {
                LocalDateTime now = context.businessTime();
                if (order.getUpdateTime() != null && !now.isAfter(order.getUpdateTime())) {
                    now = order.getUpdateTime().plusNanos(1);
                }
                order.updateUpdateTime(now).auditAs("accept generated document items").save(context);
                for (Change change : mutation.changes()) {
                    OrderLine line = change.line();
                    line.updateQuantity(change.quantity())
                            .auditAs("update generated projected item").save(context);
                    changed.add(new InternalEntityIdentity(
                            "OrderLine", line.getId(), line.getVersion()));
                }
                for (OrderLine line : mutation.removals()) {
                    long id = line.getId();
                    line.markForDeletion();
                    line.auditAs("remove generated projected item").save(context);
                    changed.add(new InternalEntityIdentity("OrderLine", id, 0));
                }
            }
            CustomerOrder refreshed = loadFull(context, snapshot.businessId());
            ContextualDocument document = render(context, refreshed, ACCEPT_LIFETIME);
            long next = refreshed.getVersion();
            saveReceipt(principal.subject(), submitted.commandId(), digest,
                    previous, next, changed, document);
            result.set(new AcceptedDocument(document, previous, next, changed));
        });
        return result.get();
    }

    private static void scope(String aggregate, String purpose) {
        if (!AGGREGATE.equals(aggregate) || !PURPOSE.equals(purpose)) {
            throw error(RoundTripReferenceErrorCode.SCOPE_MISMATCH);
        }
    }

    private static void currentPrincipal(UserContext context, TrustedReferencePrincipal principal) {
        if (!context.trustedReferencePrincipal().equals(principal)) {
            throw error(RoundTripReferenceErrorCode.CONTEXT_MISMATCH);
        }
    }

    private void authorize(TrustedReferencePrincipal principal) {
        if (!"Customer".equals(principal.domainRootType())) {
            throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT can_edit FROM generated_document_permissions WHERE subject=? AND customer_id=?",
                new Object[] {principal.subject(), principal.domainRootId()});
        if (rows.size() != 1 || ((Number) rows.get(0).get("can_edit")).intValue() != 1) {
            throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
        }
    }

    private static void owner(TrustedReferencePrincipal principal, CustomerOrder order) {
        if (order.getCustomer() == null || order.getCustomer().getId() == null
                || !order.getCustomer().getId().equals(principal.domainRootId())) {
            throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
        }
    }

    private static CustomerOrder loadRoot(UserContext context, String businessId) {
        CustomerOrderRequest<CustomerOrder> request = Q.customerOrdersWithMinimalFields()
                .withOrderNumberIs(businessId).selectId().selectVersion()
                .selectOrderNumber().selectCustomerIdOnly();
        request.top(2);
        SmartList<CustomerOrder> orders = request.comment("Load current generated Order authority")
                .purpose("Validate a previously issued editable document").executeForList(context);
        if (orders.isEmpty()) throw error(RoundTripReferenceErrorCode.DOCUMENT_NOT_FOUND);
        if (orders.size() != 1) throw projection();
        return orders.get(0);
    }

    private static CustomerOrder loadFull(UserContext context, String businessId) {
        OrderLineRequest<OrderLine> lines = Q.orderLines().orderByIdAscending();
        lines.top(101);
        CustomerOrderRequest<CustomerOrder> request = Q.customerOrders()
                .withOrderNumberIs(businessId).selectOrderLineListWith(lines);
        request.top(2);
        SmartList<CustomerOrder> orders = request.comment("Load generated Order and complete item projection")
                .purpose("Open or accept context-bound Order document").executeForList(context);
        if (orders.isEmpty()) throw error(RoundTripReferenceErrorCode.DOCUMENT_NOT_FOUND);
        if (orders.size() != 1) throw projection();
        CustomerOrder order = orders.get(0);
        if (order.getOrderLineList() == null || order.getOrderLineList().size() > 100) throw projection();
        return order;
    }

    private static ContextualDocument render(
            UserContext context, CustomerOrder order, Duration lifetime) {
        List<DocumentSnapshot.Entity> entities = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>();
        List<Map<String, Object>> bodyItems = new ArrayList<>();
        for (int index = 0; index < order.getOrderLineList().size(); index++) {
            OrderLine line = order.getOrderLineList().get(index);
            if (line.getId() == null || line.getVersion() == null || line.getProductName() == null
                    || line.getSku() == null || line.getQuantity() == null) throw projection();
            String rowKey = "item:" + index;
            rowKeys.add(rowKey);
            entities.add(new DocumentSnapshot.Entity(rowKey,
                    new InternalEntityIdentity("OrderLine", line.getId(), line.getVersion()),
                    Set.of("productName", "sku", "quantity"), Set.of("quantity"),
                    Set.of(DocumentSnapshot.MutationKind.UPDATE,
                            DocumentSnapshot.MutationKind.REMOVE_CHILD)));
            bodyItems.add(Map.of("ref", rowKey, "productName", line.getProductName(),
                    "sku", line.getSku(), "quantity", line.getQuantity()));
        }
        ReferenceDocumentScope scope = new ReferenceDocumentScope(
                "generated-order-" + order.getId() + "-version-" + order.getVersion(),
                PURPOSE, AGGREGATE, order.getId(), order.getVersion());
        DocumentSnapshot snapshot = new DocumentSnapshot(MODEL, order.getOrderNumber(), scope,
                new InternalEntityIdentity(AGGREGATE, order.getId(), order.getVersion()),
                entities, List.of(new DocumentSnapshot.Relation(RELATION, rowKeys,
                        DocumentSnapshot.Completeness.COMPLETE, true,
                        Set.of(DocumentSnapshot.MutationKind.REMOVE_CHILD))));
        return new ContextualDocument(order.getOrderNumber(),
                context.issueDocumentSnapshot(snapshot, lifetime),
                Map.of("orderNumber", order.getOrderNumber(), "items", bodyItems));
    }

    private static Mutation validate(
            DocumentSnapshot snapshot, Map<String, Object> body, CustomerOrder order) {
        if (!body.keySet().equals(Set.of("orderNumber", "items"))
                && !body.keySet().equals(Set.of("orderNumber", "items", "removedRefs"))) {
            throw projection();
        }
        if (!order.getOrderNumber().equals(body.get("orderNumber"))) throw projection();
        DocumentSnapshot.Relation relation = snapshot.relations().stream()
                .filter(value -> RELATION.equals(value.name())).findFirst().orElseThrow(
                        GeneratedOrderDocumentService::projection);
        SmartList<OrderLine> lines = order.getOrderLineList();
        if (snapshot.relations().size() != 1
                || relation.completeness() != DocumentSnapshot.Completeness.COMPLETE
                || relation.rowKeys().size() != snapshot.entities().size()
                || relation.rowKeys().size() != lines.size()) throw projection();
        Map<String, DocumentSnapshot.Entity> issued = new HashMap<>();
        Map<Long, OrderLine> currentById = new HashMap<>();
        for (DocumentSnapshot.Entity entity : snapshot.entities()) issued.put(entity.rowKey(), entity);
        for (OrderLine line : lines) currentById.put(line.getId(), line);
        Set<String> relationKeys = Set.copyOf(relation.rowKeys());
        if (!relationKeys.equals(issued.keySet())) throw projection();
        if (!(body.get("items") instanceof List<?> submittedRows)) throw projection();
        Set<String> retained = new HashSet<>();
        List<Change> changes = new ArrayList<>();
        for (Object value : submittedRows) {
            if (!(value instanceof Map<?, ?> row)
                    || !row.keySet().equals(Set.of("ref", "productName", "sku", "quantity"))
                    || !(row.get("ref") instanceof String key)
                    || !retained.add(key) || !relationKeys.contains(key)) throw projection();
            DocumentSnapshot.Entity entity = issued.get(key);
            if (!entity.loadedFields().containsAll(Set.of("productName", "sku", "quantity"))) {
                throw projection();
            }
            OrderLine line = checkedLine(entity, currentById);
            if (line.getProductName() == null || line.getSku() == null
                    || line.getQuantity() == null) throw projection();
            if (!line.getProductName().equals(row.get("productName"))
                    || !line.getSku().equals(row.get("sku"))) throw projection();
            if (!(row.get("quantity") instanceof Number quantity)
                    || !isIntegral(quantity) || quantity.longValue() <= 0
                    || quantity.longValue() > Integer.MAX_VALUE) {
                throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
            }
            if (line.getQuantity() != quantity.intValue()) {
                if (!entity.writableFields().contains("quantity")
                        || !entity.allowedMutations().contains(DocumentSnapshot.MutationKind.UPDATE)) {
                    throw projection();
                }
                changes.add(new Change(line, quantity.intValue()));
            }
        }
        Object removedValue = body.getOrDefault("removedRefs", List.of());
        if (!(removedValue instanceof List<?> removedKeys)) throw projection();
        Set<String> removed = new HashSet<>();
        List<OrderLine> removals = new ArrayList<>();
        for (Object value : removedKeys) {
            if (!(value instanceof String key) || !removed.add(key)
                    || retained.contains(key) || !relationKeys.contains(key)
                    || !relation.writable()
                    || !relation.allowedMutations().contains(DocumentSnapshot.MutationKind.REMOVE_CHILD)) {
                throw projection();
            }
            DocumentSnapshot.Entity entity = issued.get(key);
            if (!entity.allowedMutations().contains(DocumentSnapshot.MutationKind.REMOVE_CHILD)) {
                throw projection();
            }
            removals.add(checkedLine(entity, currentById));
        }
        if (retained.size() + removed.size() != relationKeys.size()) throw projection();
        return new Mutation(changes, removals);
    }

    private static OrderLine checkedLine(
            DocumentSnapshot.Entity entity, Map<Long, OrderLine> current) {
        if (!"OrderLine".equals(entity.identity().entityType())) throw projection();
        OrderLine line = current.get(entity.identity().id());
        if (line == null) throw error(RoundTripReferenceErrorCode.SCOPE_MISMATCH);
        if (!line.getVersion().equals(entity.identity().version())) throw revision();
        return line;
    }

    private static boolean isIntegral(Number number) {
        return number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long;
    }

    private static byte[] digest(SubmittedDocument submitted) {
        try {
            byte[] payload = JSON.writeValueAsBytes(List.of(
                    submitted.businessId(), submitted.documentToken(), submitted.body()));
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
        }
    }

    private Receipt receipt(String subject, String commandId, byte[] digest) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT digest,previous_version,new_version,changed_entities,result_token,result_body "
                        + "FROM generated_document_commands WHERE subject=? AND command_id=?",
                new Object[] {subject, commandId});
        if (rows.isEmpty()) return null;
        if (rows.size() != 1 || !MessageDigest.isEqual((byte[]) rows.get(0).get("digest"), digest)) {
            throw error(RoundTripReferenceErrorCode.DOCUMENT_COMMAND_CONFLICT);
        }
        Map<String, Object> row = rows.get(0);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue((String) row.get("result_body"), Map.class);
            InternalEntityIdentity[] changed = JSON.readValue(
                    (String) row.get("changed_entities"), InternalEntityIdentity[].class);
            return new Receipt(((Number) row.get("previous_version")).longValue(),
                    ((Number) row.get("new_version")).longValue(), Arrays.asList(changed),
                    new ContextualDocument((String) body.get("orderNumber"),
                            (String) row.get("result_token"), body));
        } catch (JsonProcessingException invalid) {
            throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
        }
    }

    private void saveReceipt(String subject, String commandId, byte[] digest,
            long previous, long next, List<InternalEntityIdentity> changed,
            ContextualDocument document) {
        try {
            jdbc.update("INSERT INTO generated_document_commands(subject,command_id,digest,"
                            + "previous_version,new_version,changed_entities,result_token,result_body) "
                            + "VALUES(?,?,?,?,?,?,?,?)",
                    new Object[] {subject, commandId, digest, previous, next,
                            JSON.writeValueAsString(changed), document.documentToken(),
                            JSON.writeValueAsString(document.body())});
        } catch (JsonProcessingException invalid) {
            throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
        }
    }

    private static RoundTripReferenceException projection() {
        return error(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION);
    }
    private static RoundTripReferenceException revision() {
        return error(RoundTripReferenceErrorCode.DOCUMENT_REVISION_CONFLICT);
    }
    private static RoundTripReferenceException error(RoundTripReferenceErrorCode code) {
        return new RoundTripReferenceException(code, code.name());
    }
}
