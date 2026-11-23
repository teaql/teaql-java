
package com.teaql.ordereditreference.merchant;

import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderListExpression;
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

    public SalesOrderListExpression<T, U, SalesOrder> getSalesOrderList(){
        return new SalesOrderListExpression(loaded("sales_order_list", "getSalesOrderList", Merchant::getSalesOrderList));
    }
    public MerchantExpression<T, U, U> addSalesOrder(SalesOrder salesOrder){
       return new MerchantExpression(this, $it ->  ((Merchant)$it).addSalesOrder(salesOrder));
    }
}