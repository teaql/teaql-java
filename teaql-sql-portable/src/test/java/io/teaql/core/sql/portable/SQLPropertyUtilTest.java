package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.SQLColumn;
import org.junit.Test;

public class SQLPropertyUtilTest {

    @Test
    public void resolvesVarcharMaxPlaceholderBeforeDialectDDLCompilation() {
        PropertyDescriptor property = stringProperty("description")
                .with("sqlType", "VARCHAR(<max>)")
                .with("max", "4096");

        SQLColumn column = SQLPropertyUtil.getColumns(property).get(0);

        assertEquals("VARCHAR(4096)", column.getType());
        assertFalse(column.getType().contains("<max>"));
    }

    @Test
    public void usesPortableDefaultWhenMaxMetadataIsAbsent() {
        PropertyDescriptor property = stringProperty("description")
                .with("sqlType", "VARCHAR(<max>)");

        SQLColumn column = SQLPropertyUtil.getColumns(property).get(0);

        assertEquals("VARCHAR(255)", column.getType());
        assertFalse(column.getType().contains("<max>"));
    }

    private static PropertyDescriptor stringProperty(String name) {
        EntityDescriptor owner = new EntityDescriptor();
        owner.setType("Document");
        PropertyDescriptor property = new PropertyDescriptor();
        property.setOwner(owner);
        property.setName(name);
        return property;
    }
}
