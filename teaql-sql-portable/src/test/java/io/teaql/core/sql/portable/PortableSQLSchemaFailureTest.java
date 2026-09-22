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
            return super.getTableColumns(tableName);
        }
    }
}
