package io.teaql.examples.reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.teaql.core.UserContext;
import io.teaql.core.reference.ContextBoundDocumentService;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedDocumentSnapshot;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Framework-free, application-owned SQLite consumer of the protected tqd1 document snapshot. */
public final class SqliteOrderDocumentService implements ContextBoundDocumentService {
    public static final String PURPOSE = "edit-order-items";
    public static final String MODEL = "sqlite-order-document-v1";
    private static final String AGGREGATE = "Order";
    private static final String RELATION = "items";
    private static final Duration ACCEPT_LIFETIME = Duration.ofMinutes(15);
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private final String jdbcUrl;

    public SqliteOrderDocumentService(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
    }

    private record Order(long id, long version, long customerId, String number) {}
    private record Item(long id, long version, String product, String sku, long quantity) {}
    private record Change(Item item, long quantity) {}
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
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            authorize(connection, principal);
            Order order = order(connection, snapshot.businessId());
            owner(principal, order);
            if (order.id() != snapshot.aggregate().id()
                    || order.version() != snapshot.aggregate().version()) throw revision();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    @Override
    public ContextualDocument openDocument(
            UserContext context, TrustedReferencePrincipal principal, OpenRequest request) {
        currentPrincipal(context, principal);
        scope(request.aggregateType(), request.purpose());
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            connection.setAutoCommit(false);
            try {
                authorize(connection, principal);
                Order order = order(connection, request.businessId());
                owner(principal, order);
                ContextualDocument result = render(
                        context, order, items(connection, order.id()), request.lifetime());
                connection.commit();
                return result;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    @Override
    public AcceptedDocument acceptDocument(
            UserContext context, TrustedReferencePrincipal principal, AcceptRequest request) {
        currentPrincipal(context, principal);
        scope(request.aggregateType(), request.purpose());
        SubmittedDocument submitted = request.submitted();
        DocumentSnapshot snapshot = context.consumeDocumentSnapshot(
                submitted.documentToken(), AGGREGATE, PURPOSE).snapshot();
        if (!snapshot.modelFingerprint().equals(MODEL)
                || !snapshot.businessId().equals(submitted.businessId())) throw projection();
        byte[] digest = digest(submitted);
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            connection.setAutoCommit(false);
            try {
                authorize(connection, principal);
                Order order = order(connection, snapshot.businessId());
                owner(principal, order);
                Receipt earlier = receipt(connection, principal.subject(), submitted.commandId(), digest);
                if (earlier != null) {
                    connection.commit();
                    return new AcceptedDocument(
                            earlier.document(), earlier.previous(), earlier.next(), earlier.changed());
                }
                if (order.id() != snapshot.aggregate().id()
                        || order.version() != snapshot.aggregate().version()) throw revision();
                List<Item> current = items(connection, order.id());
                Mutation mutation = validate(snapshot, submitted.body(), order, current);
                List<InternalEntityIdentity> changed = new ArrayList<>();
                long next = order.version();
                if (!mutation.changes().isEmpty() || !mutation.removals().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE orders SET version=version+1 WHERE id=? AND version=?")) {
                        statement.setLong(1, order.id());
                        statement.setLong(2, order.version());
                        if (statement.executeUpdate() != 1) throw revision();
                    }
                    next++;
                    for (Change change : mutation.changes()) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "UPDATE order_items SET quantity=?, version=version+1 "
                                        + "WHERE id=? AND order_id=? AND version=?")) {
                            statement.setLong(1, change.quantity());
                            statement.setLong(2, change.item().id());
                            statement.setLong(3, order.id());
                            statement.setLong(4, change.item().version());
                            if (statement.executeUpdate() != 1) throw revision();
                        }
                        changed.add(new InternalEntityIdentity(
                                "OrderItem", change.item().id(), change.item().version() + 1));
                    }
                    for (Item item : mutation.removals()) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM order_items WHERE id=? AND order_id=? AND version=?")) {
                            statement.setLong(1, item.id());
                            statement.setLong(2, order.id());
                            statement.setLong(3, item.version());
                            if (statement.executeUpdate() != 1) throw revision();
                        }
                        changed.add(new InternalEntityIdentity("OrderItem", item.id(), 0));
                    }
                }
                Order updated = new Order(order.id(), next, order.customerId(), order.number());
                ContextualDocument document = render(
                        context, updated, items(connection, order.id()), ACCEPT_LIFETIME);
                saveReceipt(connection, principal.subject(), submitted.commandId(), digest,
                        order.version(), next, changed, document);
                connection.commit();
                return new AcceptedDocument(document, order.version(), next, changed);
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
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

    private static void authorize(Connection connection, TrustedReferencePrincipal principal)
            throws SQLException {
        if (!"Customer".equals(principal.domainRootType())) {
            throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT can_edit FROM document_permissions WHERE subject=? AND customer_id=?")) {
            statement.setString(1, principal.subject());
            statement.setLong(2, principal.domainRootId());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || rows.getInt(1) != 1) {
                    throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
                }
            }
        }
    }

    private static Order order(Connection connection, String businessId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,version,customer_id,order_number FROM orders WHERE order_number=?")) {
            statement.setString(1, businessId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw error(RoundTripReferenceErrorCode.DOCUMENT_NOT_FOUND);
                return new Order(rows.getLong(1), rows.getLong(2), rows.getLong(3), rows.getString(4));
            }
        }
    }

    private static void owner(TrustedReferencePrincipal principal, Order order) {
        if (principal.domainRootId() != order.customerId()) {
            throw error(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED);
        }
    }

    private static List<Item> items(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,version,product_name,sku,quantity FROM order_items "
                        + "WHERE order_id=? ORDER BY id LIMIT 101")) {
            statement.setLong(1, orderId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Item> result = new ArrayList<>();
                while (rows.next()) result.add(new Item(rows.getLong(1), rows.getLong(2),
                        rows.getString(3), rows.getString(4), rows.getLong(5)));
                if (result.size() > 100) throw projection();
                return result;
            }
        }
    }

    private static ContextualDocument render(
            UserContext context, Order order, List<Item> items, Duration lifetime) {
        List<DocumentSnapshot.Entity> entities = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<Map<String, Object>> bodyItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            String key = "item:" + index;
            keys.add(key);
            entities.add(new DocumentSnapshot.Entity(key,
                    new InternalEntityIdentity("OrderItem", item.id(), item.version()),
                    Set.of("product_name", "sku", "quantity"), Set.of("quantity"),
                    Set.of(DocumentSnapshot.MutationKind.UPDATE,
                            DocumentSnapshot.MutationKind.REMOVE_CHILD)));
            bodyItems.add(Map.of("ref", key, "productName", item.product(),
                    "sku", item.sku(), "quantity", item.quantity()));
        }
        ReferenceDocumentScope scope = new ReferenceDocumentScope(
                "order-" + order.id() + "-version-" + order.version(), PURPOSE,
                AGGREGATE, order.id(), order.version());
        DocumentSnapshot snapshot = new DocumentSnapshot(MODEL, order.number(), scope,
                new InternalEntityIdentity(AGGREGATE, order.id(), order.version()),
                entities, List.of(new DocumentSnapshot.Relation(RELATION, keys,
                        DocumentSnapshot.Completeness.COMPLETE, true,
                        Set.of(DocumentSnapshot.MutationKind.REMOVE_CHILD))));
        return new ContextualDocument(order.number(), context.issueDocumentSnapshot(snapshot, lifetime),
                Map.of("orderNumber", order.number(), "items", bodyItems));
    }

    private record Mutation(List<Change> changes, List<Item> removals) {}

    private static Mutation validate(
            DocumentSnapshot snapshot, Map<String, Object> body, Order order, List<Item> current) {
        if (!body.keySet().equals(Set.of("orderNumber", "items"))
                && !body.keySet().equals(Set.of("orderNumber", "items", "removedRefs"))) {
            throw projection();
        }
        if (!order.number().equals(body.get("orderNumber"))) throw projection();
        DocumentSnapshot.Relation relation = snapshot.relations().stream()
                .filter(value -> RELATION.equals(value.name())).findFirst().orElseThrow(
                        SqliteOrderDocumentService::projection);
        if (snapshot.relations().size() != 1
                || relation.completeness() != DocumentSnapshot.Completeness.COMPLETE
                || relation.rowKeys().size() != snapshot.entities().size()
                || relation.rowKeys().size() != current.size()) throw projection();
        Map<String, DocumentSnapshot.Entity> issued = new HashMap<>();
        Map<Long, Item> currentById = new HashMap<>();
        for (DocumentSnapshot.Entity entity : snapshot.entities()) issued.put(entity.rowKey(), entity);
        for (Item item : current) currentById.put(item.id(), item);
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
            if (!entity.loadedFields().containsAll(Set.of("product_name", "sku", "quantity"))) {
                throw projection();
            }
            Item item = checkedItem(entity, currentById);
            if (!item.product().equals(row.get("productName"))
                    || !item.sku().equals(row.get("sku"))) throw projection();
            if (!(row.get("quantity") instanceof Number number)
                    || !isIntegral(number) || number.longValue() <= 0) {
                throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
            }
            if (item.quantity() != number.longValue()) {
                if (!entity.writableFields().contains("quantity")
                        || !entity.allowedMutations().contains(DocumentSnapshot.MutationKind.UPDATE)) {
                    throw projection();
                }
                changes.add(new Change(item, number.longValue()));
            }
        }
        Object removedValue = body.getOrDefault("removedRefs", List.of());
        if (!(removedValue instanceof List<?> removedKeys)) throw projection();
        Set<String> removed = new HashSet<>();
        List<Item> removals = new ArrayList<>();
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
            removals.add(checkedItem(entity, currentById));
        }
        if (retained.size() + removed.size() != relationKeys.size()) throw projection();
        return new Mutation(changes, removals);
    }

    private static boolean isIntegral(Number number) {
        return number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long;
    }

    private static Item checkedItem(DocumentSnapshot.Entity entity, Map<Long, Item> current) {
        if (!"OrderItem".equals(entity.identity().entityType())) throw projection();
        Item item = current.get(entity.identity().id());
        if (item == null) throw error(RoundTripReferenceErrorCode.SCOPE_MISMATCH);
        if (item.version() != entity.identity().version()) throw revision();
        return item;
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

    private static Receipt receipt(
            Connection connection, String subject, String commandId, byte[] digest) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT digest,previous_version,new_version,changed_entities,result_token,result_body "
                        + "FROM document_commands WHERE subject=? AND command_id=?")) {
            statement.setString(1, subject);
            statement.setString(2, commandId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                if (!MessageDigest.isEqual(rows.getBytes(1), digest)) {
                    throw error(RoundTripReferenceErrorCode.DOCUMENT_COMMAND_CONFLICT);
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = JSON.readValue(rows.getString(6), Map.class);
                    InternalEntityIdentity[] changed = JSON.readValue(
                            rows.getString(4), InternalEntityIdentity[].class);
                    return new Receipt(rows.getLong(2), rows.getLong(3), Arrays.asList(changed),
                            new ContextualDocument((String) body.get("orderNumber"),
                                    rows.getString(5), body));
                } catch (IOException exception) {
                    throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
                }
            }
        }
    }

    private static void saveReceipt(
            Connection connection, String subject, String commandId, byte[] digest,
            long previous, long next, List<InternalEntityIdentity> changed,
            ContextualDocument document) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO document_commands(subject,command_id,digest,previous_version,"
                        + "new_version,changed_entities,result_token,result_body) VALUES(?,?,?,?,?,?,?,?)")) {
            statement.setString(1, subject);
            statement.setString(2, commandId);
            statement.setBytes(3, digest);
            statement.setLong(4, previous);
            statement.setLong(5, next);
            try {
                statement.setString(6, JSON.writeValueAsString(changed));
                statement.setString(8, JSON.writeValueAsString(document.body()));
            } catch (JsonProcessingException exception) {
                throw error(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED);
            }
            statement.setString(7, document.documentToken());
            statement.executeUpdate();
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
    private static RoundTripReferenceException databaseFailure(SQLException cause) {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED,
                "DOCUMENT_VALIDATION_FAILED", cause);
    }
}
