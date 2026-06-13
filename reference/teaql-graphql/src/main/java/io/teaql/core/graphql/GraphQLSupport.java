package io.teaql.core.graphql;

import io.teaql.core.utils.ClassUtil;

import io.teaql.core.BaseRequest;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;

public interface GraphQLSupport {

    // indicate to the search request for this object field(this field is object),
    // if getRequestProperty return not null, then the request will add parent level(source items)
    // criteria
    default BaseRequest buildQuery(GraphQLFetcherParam param) {
        GraphQLFieldQuery fieldQuery = findFieldQuery(param);
        return fieldQuery.buildQuery(param.userContext(), param.params());
    }

    // the request property linked to the parent(source) property,
    // then parent request will select this property and pass thought to build this field search
    // request, the property name should be the property from parent to this field
    default String getRequestProperty(GraphQLFetcherParam param) {
        UserContext userContext = param.userContext();
        String parentType = param.parentType();
        EntityDescriptor entityDescriptor = userContext.resolveEntityDescriptor(parentType);
        PropertyDescriptor property = entityDescriptor.findProperty(param.field());
        // simple fields
        if (property == null || ClassUtil.isSimpleValueType(property.getType().javaType())) {
            return param.field();
        }
        return findFieldQuery(param).getRequestProperty();
    }

    GraphQLFieldQuery findFieldQuery(GraphQLFetcherParam param);

    void register(GraphQLFieldQuery query);
}
