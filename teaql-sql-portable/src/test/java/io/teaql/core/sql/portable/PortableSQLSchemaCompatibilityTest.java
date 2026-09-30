package io.teaql.core.sql.portable;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PortableSQLSchemaCompatibilityTest {

    @Test
    public void acceptsAliasesAndStorageThatCoversTheModelValueDomain() {
        MetadataDatabase database = compatibleDatabase();

        repository(database).ensurePhysicalSchema(context());

        assertTrue("compatible metadata must not cause DDL", database.executedSql.isEmpty());
    }

    @Test
    public void rejectsAnIncompatibleExistingColumnType() {
        MetadataDatabase database = compatibleDatabase();
        database.replaceColumn(column("name", "INTEGER", 1));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));

        assertTrue(failure.getMessage().contains("SchemaCompatibilityFixture"));
        assertTrue(failure.getMessage().contains("schema_compat_data"));
        assertTrue(failure.getMessage().contains("name"));
        assertTrue(failure.getMessage().contains("VARCHAR(100)"));
        assertTrue(failure.getMessage().contains("INTEGER"));
    }

    @Test
    public void rejectsTextAndDecimalStorageThatIsTooNarrow() {
        MetadataDatabase textDatabase = compatibleDatabase();
        textDatabase.replaceColumn(column("name", "VARCHAR", 1, "column_size", 32));

        IllegalStateException textFailure = assertThrows(
                IllegalStateException.class,
                () -> repository(textDatabase).ensurePhysicalSchema(context()));
        assertTrue(textFailure.getMessage().contains("required max length=100"));
        assertTrue(textFailure.getMessage().contains("max length=32"));

        MetadataDatabase decimalDatabase = compatibleDatabase();
        decimalDatabase.replaceColumn(column(
                "amount", "NUMERIC", 1,
                "numeric_precision", 18,
                "decimal_digits", 2));

        IllegalStateException decimalFailure = assertThrows(
                IllegalStateException.class,
                () -> repository(decimalDatabase).ensurePhysicalSchema(context()));
        assertTrue(decimalFailure.getMessage().contains("required precision=19, scale=7"));
        assertTrue(decimalFailure.getMessage().contains("precision=18, scale=2"));
    }

    @Test
    public void rejectsNullabilityThatDoesNotMatchTheModel() {
        MetadataDatabase database = compatibleDatabase();
        database.replaceColumn(column("required_code", "VARCHAR", 1, "column_size", 100));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));

        assertTrue(failure.getMessage().contains("required_code"));
        assertTrue(failure.getMessage().contains("expected nullable=false"));
        assertTrue(failure.getMessage().contains("nullable=true"));
    }

    @Test
    public void preservesCompatibilityWithProvidersThatOnlyReportColumnNames() {
        MetadataDatabase database = compatibleDatabase();
        database.columns.replaceAll((name, ignored) -> Map.of("COLUMN_NAME", name));

        repository(database).ensurePhysicalSchema(context());

        assertTrue("missing optional shape metadata must not cause DDL", database.executedSql.isEmpty());
    }

    private static PortableSQLRepository<PortableSQLDatabaseTest.Task> repository(
            MetadataDatabase database) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("SchemaCompatibilityFixture");
        descriptor.setTargetType(PortableSQLDatabaseTest.Task.class);
        descriptor.setEntitySupplier(PortableSQLDatabaseTest.Task::new);
        descriptor.setParent(new EntityDescriptor());

        GenericSQLProperty id = property(descriptor, "id", "BIGINT", Long.class);
        GenericSQLProperty version = property(descriptor, "version", "BIGINT", Long.class);
        GenericSQLProperty name = property(descriptor, "name", "VARCHAR(100)", String.class);
        GenericSQLProperty amount = property(
                descriptor, "amount", "NUMERIC(19,7)", BigDecimal.class);
        GenericSQLProperty requiredCode = property(
                descriptor, "required_code", "VARCHAR(100)", String.class);
        requiredCode.with("required", "true");
        descriptor.setProperties(List.of(id, version, name, amount, requiredCode));
        return new PortableSQLRepository<>(descriptor, database, null);
    }

    private static GenericSQLProperty property(
            EntityDescriptor owner, String name, String sqlType, Class<?> javaType) {
        GenericSQLProperty property = new GenericSQLProperty("schema_compat_data", name, sqlType);
        property.setName(name);
        property.setOwner(owner);
        property.setType(new SimplePropertyType(javaType));
        return property;
    }

    private static UserContext context() {
        return new DefaultUserContext(TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .build());
    }

    private static MetadataDatabase compatibleDatabase() {
        return new MetadataDatabase(List.of(
                column("id", "INT8", 0),
                column("version", "BIGINT", 1),
                column("name", "CHARACTER VARYING", 1, "column_size", 255),
                column(
                        "amount", "DECIMAL", 1,
                        "numeric_precision", 38,
                        "decimal_digits", 10),
                column("required_code", "VARCHAR", 0, "column_size", 100)));
    }

    private static Map<String, Object> column(
            String name, String type, int nullable, Object... shape) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("COLUMN_NAME", name);
        column.put("TYPE_NAME", type);
        column.put("NULLABLE", nullable);
        for (int index = 0; index < shape.length; index += 2) {
            column.put(String.valueOf(shape[index]), shape[index + 1]);
        }
        return column;
    }

    private static final class MetadataDatabase implements TeaQLDatabase {
        private final Map<String, Map<String, Object>> columns = new LinkedHashMap<>();
        private final List<String> executedSql = new ArrayList<>();

        private MetadataDatabase(List<Map<String, Object>> columns) {
            for (Map<String, Object> column : columns) {
                this.columns.put(String.valueOf(column.get("COLUMN_NAME")), column);
            }
        }

        private void replaceColumn(Map<String, Object> column) {
            columns.put(String.valueOf(column.get("COLUMN_NAME")), column);
        }

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            return List.of();
        }

        @Override
        public int executeUpdate(String sql, Object[] args) {
            return 0;
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
            return new int[0];
        }

        @Override
        public void execute(String sql) {
            executedSql.add(sql);
        }

        @Override
        public void executeInTransaction(Runnable action) {
            action.run();
        }

        @Override
        public List<Map<String, Object>> getTableColumns(String tableName) {
            if ("teaql_id_space".equalsIgnoreCase(tableName)) {
                return List.of(Map.of("column_name", "type_name"));
            }
            return new ArrayList<>(columns.values());
        }
    }
}
