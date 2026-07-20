package io.teaql.core.sqlite;

import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.meta.EntityDescriptor;

public class SqlitePortableSQLRepository extends PortableSQLRepository {
    public SqlitePortableSQLRepository(EntityDescriptor entityDescriptor,
                                       io.teaql.core.sql.portable.TeaQLDatabase database,
                                       PortableSQLRepositoryResolver resolver) {
        super(entityDescriptor, database, resolver);
        // Register Sqlite specific expression parsers here
    }
}
