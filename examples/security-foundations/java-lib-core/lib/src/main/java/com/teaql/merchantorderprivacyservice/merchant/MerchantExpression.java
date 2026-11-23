
package com.teaql.merchantorderprivacyservice.merchant;

import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderListExpression;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class MerchantExpression<T, E, U extends Merchant> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public MerchantExpression(Expression<T, U> expression){
        super(expression);
    }

    public MerchantExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public MerchantExpression<T, U, U> save(UserContext userContext){
        return new MerchantExpression(this, $it -> ((Merchant)$it).auditAs("Saved by Expression").save(userContext));
     }

     public MerchantExpression<T, U, U> save(String intent, UserContext userContext){
        return new MerchantExpression(this, $it -> ((Merchant)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Merchant::getName);
    }
    public MerchantExpression<T, U, U> updateName(String name){
       return new MerchantExpression(this, $it ->  ((Merchant)$it).updateName(name));
    }

    public CustomerProfileListExpression<T, U, CustomerProfile> getCustomerProfileList(){
        return new CustomerProfileListExpression(loaded("customer_profile_list", "getCustomerProfileList", Merchant::getCustomerProfileList));
    }
    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customer_order_list", "getCustomerOrderList", Merchant::getCustomerOrderList));
    }
    public MerchantExpression<T, U, U> addCustomerProfile(CustomerProfile customerProfile){
       return new MerchantExpression(this, $it ->  ((Merchant)$it).addCustomerProfile(customerProfile));
    }
    public MerchantExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new MerchantExpression(this, $it ->  ((Merchant)$it).addCustomerOrder(customerOrder));
    }
}