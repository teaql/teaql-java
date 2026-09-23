package io.teaql.core.sql.portable;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class PortableSQLSchemaFailureTest {
    private static final String TABLE = "schema_failure_data";

    @Test
    public void createTableFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        database.failExecutePrefix = "CREATE TABLE " + TABLE;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertTrue(database.getTableColumns(TABLE).isEmpty());
    }

    @Test
    public void addColumnFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        database.execute("CREATE TABLE " + TABLE + " (id INTEGER PRIMARY KEY, version INTEGER)");
        database.failExecutePrefix = "ALTER TABLE " + TABLE;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertEquals(2, database.getTableColumns(TABLE).size());
    }

    @Test
    public void idSpaceFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        database.failExecutePrefix = "CREATE TABLE teaql_id_space";

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));
        Assert.assertTrue(failure.getMessage().contains("teaql_id_space"));
        Assert.assertTrue(database.getTableColumns("teaql_id_space").isEmpty());
    }

    @Test
    public void metadataFailureIsNotTreatedAsMissingTable() throws Exception {
        FailingDatabase database = new FailingDatabase();
        database.failInspectionTable = TABLE;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class,
                () -> repository(database).ensurePhysicalSchema(context()));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertEquals(0, database.executeCount);
    }

    @Test
    public void uppercaseMetadataLabelsStillMakeSchemaRerunsIdempotent() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.uppercaseColumnLabels = true;
        int initialStatements = database.executeCount;

        repository.ensurePhysicalSchema(context);

        Assert.assertEquals(initialStatements, database.executeCount);
    }

    @Test
    public void missingMetadataColumnNameFailsBeforeIssuingDdl() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.omitColumnLabels = true;
        int initialStatements = database.executeCount;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class, () -> repository.ensurePhysicalSchema(context));

        Assert.assertTrue(failure.getMessage().contains("column_name"));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertEquals(initialStatements, database.executeCount);
    }

    private static PortableSQLRepository<PortableSQLDatabaseTest.Task> repository(
            FailingDatabase database) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("SchemaFailure");
        descriptor.setTargetType(PortableSQLDatabaseTest.Task.class);
        descriptor.setEntitySupplier(PortableSQLDatabaseTest.Task::new);
        List<PropertyDescriptor> properties = new ArrayList<>();
        for (String name : List.of("id", "version", "title")) {
            boolean numeric = !"title".equals(name);
            GenericSQLProperty property = new GenericSQLProperty(
                    TABLE, name, numeric ? "INTEGER" : "VARCHAR(100)");
            property.setName(name);
            property.setOwner(descriptor);
            property.setType(new SimplePropertyType(numeric ? Long.class : String.class));
            properties.add(property);
        }
        descriptor.setProperties(properties);
        return new PortableSQLRepository<>(descriptor, database, null);
    }

    private static UserContext context() {
        return new DefaultUserContext(TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .build());
    }

    private static final class FailingDatabase
            extends PortableSQLDatabaseTest.SQLiteTeaQLDatabase {
        private String failExecutePrefix;
        private String failInspectionTable;
        private boolean uppercaseColumnLabels;
        private boolean omitColumnLabels;
        private int executeCount;

        private FailingDatabase() throws Exception {}

        @Override
        public void execute(String sql) {
            executeCount++;
            if (failExecutePrefix != null && sql.startsWith(failExecutePrefix)) {
                throw new IllegalStateException("simulated DDL failure");
            }
            super.execute(sql);
        }

        @Override
        public List<Map<String, Object>> getTableColumns(String tableName) {
            if (tableName.equals(failInspectionTable)) {
                throw new IllegalStateException("simulated metadata failure");
            }
            List<Map<String, Object>> columns = super.getTableColumns(tableName);
            if (omitColumnLabels) {
                return columns.stream()
                        .map(column -> Map.<String, Object>of("UNRELATED", column.get("column_name")))
                        .toList();
            }
            if (!uppercaseColumnLabels) {
                return columns;
            }
            return columns.stream()
                    .map(column -> Map.<String, Object>of("COLUMN_NAME", column.get("column_name")))
                    .toList();
        }
    }
}
