package io.teaql.core.hana;

import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.core.sql.SQLEntityDescriptor;

public class HanaEntityDescriptor extends SQLEntityDescriptor {
    @Override
    protected GenericSQLProperty createPropertyDescriptor() {
        return new HanaProperty();
    }

    @Override
    protected GenericSQLRelation createRelation() {
        return new HanaRelation();
    }
}
