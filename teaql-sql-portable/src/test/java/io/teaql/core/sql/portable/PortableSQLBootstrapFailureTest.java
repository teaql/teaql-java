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

public class PortableSQLBootstrapFailureTest {
    private static final String TABLE = "bootstrap_failure_data";

    @Test
    public void rootSelectFailureIsNotTreatedAsMissingRoot() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, false);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.failQuery = true;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertTrue(failure.getMessage().contains("inspect root"));
        Assert.assertTrue(failure.getMessage().contains("BootstrapFailure"));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertTrue(failure.getCause().getMessage().contains("simulated bootstrap SELECT"));
        Assert.assertEquals(0, database.insertCount);
    }

    @Test
    public void rootInsertFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, false);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.failInsert = true;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertTrue(failure.getMessage().contains("create root"));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertTrue(database.query("SELECT * FROM " + TABLE, new Object[0]).isEmpty());
        Assert.assertTrue(database.query("SELECT * FROM teaql_id_space", new Object[0]).isEmpty());
    }

    @Test
    public void rootRestoreFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, false);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.execute("INSERT INTO " + TABLE + " (id,version) VALUES (1,-1)");
        database.failUpdate = true;

        Assert.assertThrows(IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertEquals(-1L, ((Number) database.query(
                "SELECT version FROM " + TABLE + " WHERE id = 1", new Object[0])
                .get(0).get("version")).longValue());
    }

    @Test
    public void constantSelectFailureIsNotTreatedAsMissingConstant() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, true);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.failQuery = true;

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertTrue(failure.getMessage().contains("inspect constant"));
        Assert.assertTrue(failure.getMessage().contains(TABLE));
        Assert.assertEquals(0, database.insertCount);
    }

    @Test
    public void constantInsertFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, true);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        database.failInsert = true;

        Assert.assertThrows(IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertTrue(database.query("SELECT * FROM " + TABLE, new Object[0]).isEmpty());
        Assert.assertTrue(database.query("SELECT * FROM teaql_id_space", new Object[0]).isEmpty());
    }

    @Test
    public void constantReconcileFailureIsNotReportedAsSuccess() throws Exception {
        FailingDatabase database = new FailingDatabase();
        PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, true);
        UserContext context = context();
        repository.ensurePhysicalSchema(context);
        long constantId = Math.abs((long) "PRIMARY".hashCode());
        database.execute("INSERT INTO " + TABLE
                + " (id,version,code) VALUES (" + constantId + ",1,'STALE')");
        database.failUpdate = true;

        Assert.assertThrows(IllegalStateException.class, () -> repository.ensureInitData(context));
        Assert.assertEquals("STALE", database.query(
                "SELECT code FROM " + TABLE + " WHERE id = " + constantId, new Object[0])
                .get(0).get("code"));
    }

    @Test
    public void repeatedRootAndConstantBootstrapIsIdempotent() throws Exception {
        for (boolean constant : List.of(false, true)) {
            FailingDatabase database = new FailingDatabase();
            PortableSQLRepository<PortableSQLDatabaseTest.Task> repository = repository(database, constant);
            UserContext context = context();
            repository.ensurePhysicalSchema(context);

            repository.ensureInitData(context);
            repository.ensureInitData(context);

            List<Map<String, Object>> rows = database.query(
                    "SELECT * FROM " + TABLE, new Object[0]);
            Assert.assertEquals(1, rows.size());
            Assert.assertEquals(1L, ((Number) rows.get(0).get("version")).longValue());
            Assert.assertEquals(1, database.insertCount);
        }
    }

    private static PortableSQLRepository<PortableSQLDatabaseTest.Task> repository(
            FailingDatabase database, boolean constant) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("BootstrapFailure");
        descriptor.setTargetType(PortableSQLDatabaseTest.Task.class);
        descriptor.setEntitySupplier(PortableSQLDatabaseTest.Task::new);
        if (constant) {
            EntityDescriptor parent = new EntityDescriptor();
            parent.setType("BootstrapParent");
            descriptor.setParent(parent);
            descriptor.with("constant", "true");
        }
        List<PropertyDescriptor> properties = new ArrayList<>();
        for (String name : List.of("id", "version", "code")) {
            boolean numeric = !"code".equals(name);
            GenericSQLProperty property = new GenericSQLProperty(
                    TABLE, name, numeric ? "INTEGER" : "VARCHAR(100)");
            property.setName(name);
            property.setOwner(descriptor);
            property.setType(new SimplePropertyType(numeric ? Long.class : String.class));
            if (constant && "code".equals(name)) {
                property.with("identifier", "true");
                property.with("candidates", "PRIMARY");
            }
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

    private static final class FailingDatabase extends PortableSQLDatabaseTest.SQLiteTeaQLDatabase {
        private boolean failQuery;
        private boolean failInsert;
        private boolean failUpdate;
        private int insertCount;

        private FailingDatabase() throws Exception {}

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            if (failQuery && sql.startsWith("SELECT * FROM " + TABLE + " WHERE")) {
                throw new IllegalStateException("simulated bootstrap SELECT failure");
            }
            return super.query(sql, args);
        }

        @Override
        public void execute(String sql) {
            if (sql.startsWith("INSERT INTO " + TABLE)) {
                insertCount++;
                if (failInsert) {
                    throw new IllegalStateException("simulated bootstrap INSERT failure");
                }
            }
            if (failUpdate && sql.startsWith("UPDATE " + TABLE)) {
                throw new IllegalStateException("simulated bootstrap UPDATE failure");
            }
            super.execute(sql);
        }
    }
}
