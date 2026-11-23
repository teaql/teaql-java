package io.teaql.core.sql.portable;

import io.teaql.core.BaseEntity;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sql.SqlAstCompiler;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import static org.junit.Assert.*;

/** The compiler and inherited discriminator projection must share one scoped flag. */
public class InheritedSelectTableFlagTest {
    private SQLEntityDescriptor descriptor(String name) {
        var descriptor = new SQLEntityDescriptor(); descriptor.setType(name);
        descriptor.with("table_name", name.toLowerCase() + "_data");
        for (String nameOfField : new String[]{"id", "version"}) {
            var property = (GenericSQLProperty)descriptor.addSimpleProperty(nameOfField, Long.class);
            property.setColumnType("BIGINT");
        }
        return descriptor;
    }
    @Test public void repositoryAndCompilerUseTheSameMultiTableFlag() {
        assertEquals(SqlAstCompiler.MULTI_TABLE, PortableSQLRepository.MULTI_TABLE);
    }
    @Test public void middleTypeDiscriminatorUsesItsOwnQualifiedTableInAJoin() {
        var parent = descriptor("Parent"); var child = descriptor("Child"); var grandchild = descriptor("Grandchild");
        child.setParent(parent); grandchild.setParent(child);
        var repository = new PortableSQLRepository<BaseEntity>(child, null, null);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build());
        context.putAttribute(SqlAstCompiler.MULTI_TABLE, true);
        assertEquals("childData._child_type AS _type_", repository.getTypeSQL(context));
        assertEquals(Boolean.TRUE, context.getObj(SqlAstCompiler.MULTI_TABLE));
    }
    @Test public void singleTableAndLeafDiscriminatorsKeepTheirExistingShape() {
        var parent = descriptor("Parent"); var child = descriptor("Child"); child.setParent(parent);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build());
        assertEquals("_child_type AS _type_", new PortableSQLRepository<BaseEntity>(parent, null, null).getTypeSQL(context));
        assertNull(new PortableSQLRepository<BaseEntity>(child, null, null).getTypeSQL(context));
    }
}
