
package com.teaql.tracechainservice.paymentattempt;

import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class PaymentAttemptExpression<T, E, U extends PaymentAttempt> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PaymentAttemptExpression(Expression<T, U> expression){
        super(expression);
    }

    public PaymentAttemptExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PaymentAttemptExpression<T, U, U> updateId(Long id){
        return new PaymentAttemptExpression(this, $it -> {((PaymentAttempt)$it).__internalSet("id", id); return this;});
     }

     public PaymentAttemptExpression<T, U, U> save(UserContext userContext){
        return new PaymentAttemptExpression(this, $it -> ((PaymentAttempt)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PaymentAttemptExpression<T, U, U> save(String intent, UserContext userContext){
        return new PaymentAttemptExpression(this, $it -> ((PaymentAttempt)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PaymentExpression<T, U, Payment> getPayment(){
       return new PaymentExpression(loaded("payment", PaymentAttempt::getPayment));
    }

    public PaymentAttemptExpression<T, U, U> updatePayment(Payment payment){
       return new PaymentAttemptExpression(this, $it ->  ((PaymentAttempt)$it).updatePayment(payment));
    }

    public Expression<T, String> getReferenceCode(){
       return loaded("referenceCode", PaymentAttempt::getReferenceCode);
    }
    public PaymentAttemptExpression<T, U, U> updateReferenceCode(String referenceCode){
       return new PaymentAttemptExpression(this, $it ->  ((PaymentAttempt)$it).updateReferenceCode(referenceCode));
    }

}