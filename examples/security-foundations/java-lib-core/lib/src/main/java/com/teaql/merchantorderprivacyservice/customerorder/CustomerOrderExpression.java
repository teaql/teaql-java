
package com.teaql.merchantorderprivacyservice.customerorder;

import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileExpression;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CustomerOrderExpression<T, E, U extends CustomerOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerOrderExpression<T, U, U> save(UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CustomerOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public MerchantExpression<T, U, Merchant> getMerchant(){
       return new MerchantExpression(loaded("merchant", "getMerchant", CustomerOrder::getMerchant));
    }

    public CustomerOrderExpression<T, U, U> updateMerchant(Merchant merchant){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateMerchant(merchant));
    }

    public CustomerProfileExpression<T, U, CustomerProfile> getCustomerProfile(){
       return new CustomerProfileExpression(loaded("customer_profile", "getCustomerProfile", CustomerOrder::getCustomerProfile));
    }

    public CustomerOrderExpression<T, U, U> updateCustomerProfile(CustomerProfile customerProfile){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCustomerProfile(customerProfile));
    }

    public Expression<T, String> getOrderNumber(){
       return loaded("order_number", "getOrderNumber", CustomerOrder::getOrderNumber);
    }
    public CustomerOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderNumber(orderNumber));
    }

    public Expression<T, String> getFulfillmentStatus(){
       return loaded("fulfillment_status", "getFulfillmentStatus", CustomerOrder::getFulfillmentStatus);
    }
    public CustomerOrderExpression<T, U, U> updateFulfillmentStatus(String fulfillmentStatus){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateFulfillmentStatus(fulfillmentStatus));
    }

}