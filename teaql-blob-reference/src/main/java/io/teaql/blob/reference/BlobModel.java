package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import java.util.*;

/** Canonical model field resolution, including inherited fields and invalid Blob metadata. */
final class BlobModel {
    private BlobModel() {}
    static Map<String,BlobFieldSpec> fields(EntityDescriptor root) {
        Map<String,BlobFieldSpec> fields=new LinkedHashMap<>(); Set<String> seen=new HashSet<>();
        for (EntityDescriptor descriptor=root;descriptor!=null;descriptor=descriptor.getParent()) {
            for (PropertyDescriptor property:descriptor.getProperties()) {
                if (!seen.add(property.getName())) continue;
                BlobFieldSpec spec=property.blobFieldSpec(); boolean type=property.getType()!=null && property.getType().javaType()==BlobRef.class;
                if (spec==null && !type) continue;
                if (spec==null || !type || !spec.field().equals(property.getName()) || !spec.entity().equals(descriptor.getType())) {
                    throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
                }
                fields.put(property.getName(),spec);
            }
        }
        return fields;
    }
    static BlobFieldSpec field(EntityMetaFactory metadata,BlobFieldTarget target) {
        BlobFieldSpec field=fields(metadata.resolveEntityDescriptor(target.entity())).get(target.field());
        if (field==null) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
        return field;
    }
}
