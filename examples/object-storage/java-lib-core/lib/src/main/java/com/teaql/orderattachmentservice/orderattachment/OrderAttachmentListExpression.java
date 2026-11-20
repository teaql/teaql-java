
package com.teaql.orderattachmentservice.orderattachment;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class OrderAttachmentListExpression<T, E, U extends OrderAttachment> extends SmartListExpression<T, E, U> {
    public OrderAttachmentListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public OrderAttachmentListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public OrderAttachmentExpression<T, U, U> first() {
       return new OrderAttachmentExpression(super.first());
    }

    public OrderAttachmentExpression<T, U, U> get(int index) {
      return new OrderAttachmentExpression(super.get(index));
    }
}