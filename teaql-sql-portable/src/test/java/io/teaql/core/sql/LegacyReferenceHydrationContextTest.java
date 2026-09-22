package io.teaql.core.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.Test;

/** The legacy ResultSet mappers must not borrow another runtime's entity factory. */
public class LegacyReferenceHydrationContextTest {

    public static class Reference extends BaseEntity {
        private final String source;

        public Reference(String source) {
            this.source = source;
        }

        @Override
        public String typeName() {
            return "Reference";
        }
    }

    public static class Slot extends BaseEntity {
        private Reference ref;

        @Override
        public Object __internalGet(String property) {
            if ("ref".equals(property)) return ref;
            return super.__internalGet(property);
        }

        @Override
        public void __internalSet(String property, Object value) {
            if ("ref".equals(property)) {
                ref = (Reference) value;
                return;
            }
            super.__internalSet(property, value);
        }
    }

    @Test
    public void propertyAndRelationHydrationUseInvokingContextWhenGlobalIsPoisoned()
            throws Exception {
        SimpleEntityMetaFactory first = metadata("first");
        SimpleEntityMetaFactory second = metadata("second");
        UserContext firstContext = new DefaultUserContext(
                TeaQLRuntime.builder().metadata(first).build());
        UserContext secondContext = new DefaultUserContext(
                TeaQLRuntime.builder().metadata(second).build());
        EntityMetaFactory previous = EntityMetaFactory.get();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement()) {
            EntityMetaFactory.registerGlobal(second);
            try (ResultSet rows = statement.executeQuery("SELECT 42 AS ref")) {
                rows.next();
                assertReference("first", firstContext, property(), rows);
                assertReference("second", secondContext, property(), rows);
                assertReference("first", firstContext, relation(), rows);
                assertReference("second", secondContext, relation(), rows);
            }

            EntityMetaFactory.registerGlobal(null);
            try (ResultSet rows = statement.executeQuery("SELECT 42 AS ref")) {
                rows.next();
                assertReference("first", firstContext, property(), rows);
                assertReference("second", secondContext, relation(), rows);
            }

            try (ResultSet rows = statement.executeQuery("SELECT NULL AS ref")) {
                rows.next();
                Slot slot = new Slot();
                property().setPropertyValue(firstContext, slot, rows);
                assertNull(slot.ref);
                relation().setPropertyValue(secondContext, slot, rows);
                assertNull(slot.ref);
            }
        } finally {
            EntityMetaFactory.registerGlobal(previous);
        }
    }

    private static void assertReference(
            String expectedSource, UserContext context, SQLProperty mapper, ResultSet rows) {
        Slot slot = new Slot();
        mapper.setPropertyValue(context, slot, rows);
        assertNotNull(slot.ref);
        assertEquals(expectedSource, slot.ref.source);
        assertEquals(Long.valueOf(42), slot.ref.getId());
        assertEquals(EntityStatus.REFER, slot.ref.get$status());
    }

    private static GenericSQLProperty property() {
        GenericSQLProperty property = new GenericSQLProperty();
        property.setName("ref");
        property.setType(new SimplePropertyType(Reference.class));
        return property;
    }

    private static GenericSQLRelation relation() {
        GenericSQLRelation relation = new GenericSQLRelation();
        relation.setName("ref");
        relation.setType(new SimplePropertyType(Reference.class));
        return relation;
    }

    private static SimpleEntityMetaFactory metadata(String source) {
        SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("Reference");
        descriptor.setTargetType(Reference.class);
        descriptor.setEntitySupplier(() -> new Reference(source));
        factory.register(descriptor);
        return factory;
    }
}
