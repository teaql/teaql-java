package io.teaql.core.sql;

import io.teaql.core.SearchRequest;
import io.teaql.core.UserContext;
import io.teaql.core.meta.PropertyDescriptor;

public interface SqlCompilerDelegate extends SQLColumnResolver {
    PropertyDescriptor findProperty(String propertyName);
    SQLColumn getSqlColumn(PropertyDescriptor property);
    String prepareLimit(SearchRequest request);
    default String prepareLimit(SearchRequest request, java.util.Map<String, Object> parameters) {
        return prepareLimit(request);
    }
    String getTypeSQL(UserContext userContext);
    String getPartitionSQL();

    /** A selected statistical value may need its provider's numeric ordering domain. */
    default String statisticalOrderSql(UserContext context, io.teaql.core.Expression selected, String sql) {
        return sql;
    }

    default void validateStatisticalParameter(UserContext context, io.teaql.core.Expression selected, Object value) {}

    /** Providers must implement real GBK encoding; returning the same operand is not support. */
    default String gbkOrderSql(UserContext context, io.teaql.core.Expression selected, String sql) {
        throw new io.teaql.core.TeaQLRuntimeException("GBK ordering is unsupported by this SQL delegate");
    }
}
