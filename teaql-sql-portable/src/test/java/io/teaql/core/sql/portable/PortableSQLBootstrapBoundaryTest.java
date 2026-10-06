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
import org.junit.Test;
import static org.junit.Assert.*;

/** Native provider boundary; generated typed bootstrap is verified by the School example. */
public class PortableSQLBootstrapBoundaryTest {
    @Test
    public void portableRepositoryDoesNotExposeDataBootstrap() {
        assertThrows(NoSuchMethodException.class,
                () -> PortableSQLRepository.class.getMethod("ensureInitData", UserContext.class));
        assertThrows(NoSuchMethodException.class,
                () -> PortableSQLRepository.class.getMethod("ensureSchema", UserContext.class));
    }

    @Test
    public void portableServiceCannotReintroduceDataBootstrap() {
        assertThrows(NoSuchMethodException.class,
                () -> PortableSQLDataService.class.getMethod("ensureSchema", UserContext.class, String.class));
    }

    @Test
    public void physicalSchemaDoesNotInterpretRootOrConstantCandidates() throws Exception {
        for (boolean constant : List.of(false, true)) {
            var database = new PortableSQLDatabaseTest.SQLiteTeaQLDatabase();
            var descriptor = new EntityDescriptor();
            descriptor.setType("BootstrapBoundary");
            descriptor.setTargetType(PortableSQLDatabaseTest.Task.class);
            descriptor.setEntitySupplier(PortableSQLDatabaseTest.Task::new);
            if (constant) {
                descriptor.setParent(new EntityDescriptor());
                descriptor.with("constant", "true");
            }
            List<PropertyDescriptor> properties = new ArrayList<>();
            for (String name : List.of("id", "version", "code")) {
                boolean numeric = !"code".equals(name);
                var property = new GenericSQLProperty("bootstrap_boundary_data", name,
                        numeric ? "INTEGER" : "VARCHAR(100)");
                property.setName(name);
                property.setOwner(descriptor);
                property.setType(new SimplePropertyType(numeric ? Long.class : String.class));
                if ("id".equals(name)) property.with("candidates", "1001");
                if ("code".equals(name)) property.with("identifier", "true").with("candidates", "PRIMARY");
                properties.add(property);
            }
            descriptor.setProperties(properties);
            var context = new DefaultUserContext(TeaQLRuntime.builder()
                    .metadata(new SimpleEntityMetaFactory()).build());
            var repository = new PortableSQLRepository<>(descriptor, database, null);
            repository.ensurePhysicalSchema(context);
            repository.ensurePhysicalSchema(context);
            assertTrue(database.query("SELECT * FROM bootstrap_boundary_data", new Object[0]).isEmpty());
            // Repeated DDL must neither reconcile values nor revive tombstones.
            database.execute("INSERT INTO bootstrap_boundary_data VALUES (1001,-2,'KEEP')");
            repository.ensurePhysicalSchema(context);
            var row = database.query("SELECT * FROM bootstrap_boundary_data", new Object[0]).get(0);
            assertEquals(-2L, ((Number) row.get("version")).longValue());
            assertEquals("KEEP", row.get("code"));
        }
    }
}
