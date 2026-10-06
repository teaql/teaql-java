
package com.teaql.tracechainservice.payment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderExpression;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
import com.teaql.tracechainservice.paymentattempt.PaymentAttemptListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class PaymentExpression<T, E, U extends Payment> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PaymentExpression(Expression<T, U> expression){
        super(expression);
    }

    public PaymentExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PaymentExpression<T, U, U> updateId(Long id){
        return new PaymentExpression(this, $it -> {((Payment)$it).__internalSet("id", id); return this;});
     }

     public PaymentExpression<T, U, U> save(UserContext userContext){
        return new PaymentExpression(this, $it -> ((Payment)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PaymentExpression<T, U, U> save(String intent, UserContext userContext){
        return new PaymentExpression(this, $it -> ((Payment)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CustomerOrderExpression<T, U, CustomerOrder> getCustomerOrder(){
       return new CustomerOrderExpression(loaded("customerOrder", Payment::getCustomerOrder));
    }

    public PaymentExpression<T, U, U> updateCustomerOrder(CustomerOrder customerOrder){
       return new PaymentExpression(this, $it ->  ((Payment)$it).updateCustomerOrder(customerOrder));
    }

    public Expression<T, String> getReferenceCode(){
       return loaded("referenceCode", Payment::getReferenceCode);
    }
    public PaymentExpression<T, U, U> updateReferenceCode(String referenceCode){
       return new PaymentExpression(this, $it ->  ((Payment)$it).updateReferenceCode(referenceCode));
    }

    public PaymentAttemptListExpression<T, U, PaymentAttempt> getPaymentAttemptList(){
        return new PaymentAttemptListExpression(loaded("paymentAttemptList", Payment::getPaymentAttemptList));
    }
    public PaymentExpression<T, U, U> addPaymentAttempt(PaymentAttempt paymentAttempt){
       return new PaymentExpression(this, $it ->  ((Payment)$it).addPaymentAttempt(paymentAttempt));
    }
}