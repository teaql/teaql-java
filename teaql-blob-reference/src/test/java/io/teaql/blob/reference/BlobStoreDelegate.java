package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobStore;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/** Test-only fault injection around the actual provider. */
class BlobStoreDelegate implements BlobStore {
    final BlobStore delegate;
    BlobStoreDelegate(BlobStore delegate) { this.delegate=delegate; }
    @Override public Capabilities capabilities() { return delegate.capabilities(); }
    @Override public UploadTarget createUpload(UserContext context,String id) { return delegate.createUpload(context,id); }
    @Override public StoredContent write(UserContext context,UploadTarget target,InputStream source,long maximum) { return delegate.write(context,target,source,maximum); }
    @Override public Optional<StoredContent> stat(UserContext context,String location) { return delegate.stat(context,location); }
    @Override public Optional<StoredContent> sealForDeletion(UserContext context,String location,String expectedVersion) { return delegate.sealForDeletion(context,location,expectedVersion); }
    @Override public RangeContent openRange(UserContext context,String location,String version,long offset,long length) { return delegate.openRange(context,location,version,offset,length); }
    @Override public List<SignedRead> signReadBatch(UserContext context,List<ReadRequest> requests) { return delegate.signReadBatch(context,requests); }
    @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) { return delegate.deleteIfVersion(context,location,version); }
}
