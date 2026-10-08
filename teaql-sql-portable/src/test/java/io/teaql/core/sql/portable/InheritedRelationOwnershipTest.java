package io.teaql.core.sql.portable;

import io.teaql.core.BaseEntity;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import org.junit.Test;
import static org.junit.Assert.*;

public class InheritedRelationOwnershipTest {
    private SQLEntityDescriptor descriptor(String name) {
        var descriptor = new SQLEntityDescriptor();
        descriptor.setType(name);
        descriptor.with("table_name", name.toLowerCase() + "_data");
        for (String field : new String[]{"id", "version"}) {
            var property = (GenericSQLProperty) descriptor.addSimpleProperty(field, Long.class);
            property.setColumnType("BIGINT");
        }
        return descriptor;
    }

    private Relation relation(EntityDescriptor owner, EntityDescriptor keeper) {
        var relation = new Relation();
        relation.setOwner(owner);
        relation.setRelationKeeper(keeper);
        return relation;
    }

    @Test
    public void inheritedForwardFkIsHandledButAncestorReverseAndUnrelatedRelationsAreNot() {
        var parent = descriptor("Parent");
        var child = descriptor("Child");
        child.setParent(parent);
        var other = descriptor("Other");
        var repository = new PortableSQLRepository<BaseEntity>(child, null, null);
        assertTrue(repository.shouldHandle(relation(child, child)));
        assertTrue(repository.shouldHandle(relation(parent, parent)));
        assertFalse(repository.shouldHandle(relation(parent, child)));
        assertFalse(repository.shouldHandle(relation(child, parent)));
        assertFalse(repository.shouldHandle(relation(other, other)));
    }
}
