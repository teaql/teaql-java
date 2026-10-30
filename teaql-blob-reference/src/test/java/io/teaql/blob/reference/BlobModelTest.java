package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlobModelTest {
    @Test public void inheritedFieldRetainsCanonicalDeclaringIdentityAndChildShadowing() {
        var parent=new EntityDescriptor(); parent.setType("Parent");
        var spec=BlobFieldSpec.of("Parent","document",List.of("pdf"),true,"default"); parent.addSimpleProperty("document",BlobRef.class).withBlobFieldSpec(spec);
        var child=new EntityDescriptor(); child.setType("Child"); child.setParent(parent);
        var metadata=new SimpleEntityMetaFactory(); metadata.register(child);
        assertSame(spec,BlobModel.field(metadata,new BlobFieldTarget("Child",42L,"document")));
        child.addSimpleProperty("document",String.class);
        assertTrue(BlobModel.fields(child).isEmpty()); assertThrows(BlobException.class,() -> BlobModel.field(metadata,new BlobFieldTarget("Child",42L,"document")));
    }
    @Test public void unannotatedBlobTypeRejectsInsteadOfBeingTreatedAsAnOrdinaryField() {
        var descriptor=new EntityDescriptor(); descriptor.setType("Asset"); descriptor.addSimpleProperty("document",BlobRef.class);
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> BlobModel.fields(descriptor)).code());
    }
    @Test public void wrongDeclaringEntityRejectsEvenWhenTheFieldNameAndTypeMatch() {
        var descriptor=new EntityDescriptor(); descriptor.setType("Asset");
        descriptor.addSimpleProperty("document",BlobRef.class).withBlobFieldSpec(BlobFieldSpec.of("Different","document",List.of("pdf"),true,"default"));
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> BlobModel.fields(descriptor)).code());
    }
}
