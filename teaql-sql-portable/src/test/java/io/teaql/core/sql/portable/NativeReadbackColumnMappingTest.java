package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual SELECT * maps physical metadata, not inferred member spelling. */
public class NativeReadbackColumnMappingTest {
    public static final class Row extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
                Row.class, "native-readback-v1", Map.of("id", 0, "version", 1,
                        "customer_reference", 2, "established_date", 3, "counter_value", 4),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "customer_reference", List.of("customerReference", "external_ref"),
                        "established_date", List.of("establishedDate", "STARTED_ON"),
                        "counter_value", List.of("counterValue", "counter_value")), Set.of()));
        String customerReference;
        LocalDate establishedDate;
        Integer counterValue;
        @Override public Object __internalGet(String name) {
            return switch (name) {
                case "customerReference" -> customerReference;
                case "establishedDate" -> establishedDate;
                case "counterValue" -> counterValue;
                default -> super.__internalGet(name);
            };
        }
        @Override public void __internalSet(String name, Object value) {
            markPropertyLoaded(name);
            switch (name) {
                case "customerReference" -> customerReference = (String) value;
                case "establishedDate" -> establishedDate = (LocalDate) value;
                case "counterValue" -> counterValue = (Integer) value;
                default -> super.__internalSet(name, value);
            }
        }
    }
    record Fixture(PortableSQLDatabaseTest.SQLiteTeaQLDatabase database,
                   PortableSQLRepository<Row> repository, DefaultUserContext context) {}
    private Fixture fixture() throws Exception {
        assertNotNull(Row.LAYOUT);
        var descriptor = new EntityDescriptor();
        descriptor.setType("Row"); descriptor.setTargetType(Row.class); descriptor.setEntitySupplier(Row::new);
        var definitions = List.of(
                new Object[]{"id", "id", Long.class, "INTEGER"},
                new Object[]{"version", "version", Long.class, "INTEGER"},
                new Object[]{"customerReference", "external_ref", String.class, "VARCHAR(100)"},
                new Object[]{"establishedDate", "STARTED_ON", LocalDate.class, "DATE"},
                new Object[]{"counterValue", "counter_value", Integer.class, "INTEGER"});
        List<PropertyDescriptor> properties = new ArrayList<>();
        for (Object[] definition : definitions) {
            var property = new GenericSQLProperty("native_readback_data", (String) definition[1], (String) definition[3]);
            property.setName((String) definition[0]); property.setOwner(descriptor);
            property.setType(new SimplePropertyType((Class<?>) definition[2])); properties.add(property);
        }
        descriptor.setProperties(properties);
        var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false).build());
        var database = new PortableSQLDatabaseTest.SQLiteTeaQLDatabase();
        var repository = new PortableSQLRepository<Row>(descriptor, database, type -> null, metadata);
        repository.ensurePhysicalSchema(context);
        return new Fixture(database, repository, context);
    }
    @Test public void physicalCustomColumnsHydrateValuesAndLoadedNull() throws Exception {
        var fixture = fixture();
        fixture.database.executeUpdate("INSERT INTO native_readback_data(id,version,external_ref,STARTED_ON,counter_value) VALUES(?,?,?,?,?)",
                new Object[]{1L, 1L, "CR-A", "2026-10-07", null});
        var row = fixture.repository.loadPersistedById(fixture.context, 1L);
        assertEquals("CR-A", row.customerReference); assertEquals(LocalDate.of(2026, 10, 7), row.establishedDate);
        assertNull(row.counterValue);
        for (String name : List.of("id", "version", "customer_reference", "established_date", "counter_value")) assertTrue(row.isPropertyLoaded(name));
        assertSame(Row.LAYOUT, row.__internalLoadState().layout());
        assertFalse(row.__internalHasMutationLedger()); assertTrue(row.getUpdatedProperties().isEmpty());
    }
    @Test public void invalidPhysicalDateCannotBeSkippedOrConvertedToLoadedNull() throws Exception {
        for (String invalid : List.of("not-a-date", "bad", "")) {
            var fixture = fixture();
            fixture.database.executeUpdate("INSERT INTO native_readback_data(id,version,external_ref,STARTED_ON,counter_value) VALUES(?,?,?,?,?)",
                    new Object[]{1L, 1L, "CR-A", invalid, null});
            assertThrows(DateTimeParseException.class, () -> fixture.repository.loadPersistedById(fixture.context, 1L));
        }
    }
}
