package io.teaql.core.mysql;

import io.teaql.core.Entity;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;

public class MysqlPortableSQLRepository<T extends Entity> extends PortableSQLRepository<T> {

    public MysqlPortableSQLRepository(EntityDescriptor entityDescriptor, TeaQLDatabase database, PortableSQLRepositoryResolver resolver) {
        super(entityDescriptor, database, resolver);
        registerExpressionParser(new MysqlAggrExpressionParser());
        registerExpressionParser(new MysqlParameterParser());
        registerExpressionParser(new MysqlTwoOperatorExpressionParser());
    }
}
