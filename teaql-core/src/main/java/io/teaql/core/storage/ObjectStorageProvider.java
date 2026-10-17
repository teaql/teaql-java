package io.teaql.core.storage;

import java.io.InputStream;
import java.util.Optional;
import io.teaql.core.storage.ObjectStorage.*;

/** Runtime-scoped SPI. Invocation can only be obtained through the context-owned facade. */
public interface ObjectStorageProvider {
    Metadata put(Invocation invocation, Key key, InputStream content, PutOptions options);
    Content get(Invocation invocation, Key key);
    Optional<Metadata> head(Invocation invocation, Key key);
    void delete(Invocation invocation, Key key);
    ReadUrl getReadUrl(Invocation invocation, Key key, ReadUrlOptions options);
}
