
package com.teaql.merchantorderprivacyservice.customerprofile;

import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderListExpression;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CustomerProfileExpression<T, E, U extends CustomerProfile> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerProfileExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerProfileExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerProfileExpression<T, U, U> save(UserContext userContext){
        return new CustomerProfileExpression(this, $it -> ((CustomerProfile)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CustomerProfileExpression<T, U, U> save(String intent, UserContext userContext){
        return new CustomerProfileExpression(this, $it -> ((CustomerProfile)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public MerchantExpression<T, U, Merchant> getMerchant(){
       return new MerchantExpression(loaded("merchant", "getMerchant", CustomerProfile::getMerchant));
    }

    public CustomerProfileExpression<T, U, U> updateMerchant(Merchant merchant){
       return new CustomerProfileExpression(this, $it ->  ((CustomerProfile)$it).updateMerchant(merchant));
    }

    public Expression<T, String> getEmail(){
       return loaded("email", "getEmail", CustomerProfile::getEmail);
    }
    public CustomerProfileExpression<T, U, U> updateEmail(String email){
       return new CustomerProfileExpression(this, $it ->  ((CustomerProfile)$it).updateEmail(email));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customer_order_list", "getCustomerOrderList", CustomerProfile::getCustomerOrderList));
    }
    public CustomerProfileExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new CustomerProfileExpression(this, $it ->  ((CustomerProfile)$it).addCustomerOrder(customerOrder));
    }
}