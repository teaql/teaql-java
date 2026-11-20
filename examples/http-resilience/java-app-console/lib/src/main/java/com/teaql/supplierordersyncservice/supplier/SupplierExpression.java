
package com.teaql.supplierordersyncservice.supplier;

import com.teaql.supplierordersyncservice.supplierorder.SupplierOrder;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrderListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class SupplierExpression<T, E, U extends Supplier> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SupplierExpression(Expression<T, U> expression){
        super(expression);
    }

    public SupplierExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SupplierExpression<T, U, U> updateId(Long id){
        return new SupplierExpression(this, $it -> {((Supplier)$it).__internalSet("id", id); return this;});
     }

     public SupplierExpression<T, U, U> save(UserContext userContext){
        return new SupplierExpression(this, $it -> ((Supplier)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SupplierExpression<T, U, U> save(String intent, UserContext userContext){
        return new SupplierExpression(this, $it -> ((Supplier)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Supplier::getName);
    }
    public SupplierExpression<T, U, U> updateName(String name){
       return new SupplierExpression(this, $it ->  ((Supplier)$it).updateName(name));
    }

    public SupplierOrderListExpression<T, U, SupplierOrder> getSupplierOrderList(){
        return new SupplierOrderListExpression(loaded("supplier_order_list", "getSupplierOrderList", Supplier::getSupplierOrderList));
    }
    public SupplierExpression<T, U, U> addSupplierOrder(SupplierOrder supplierOrder){
       return new SupplierExpression(this, $it ->  ((Supplier)$it).addSupplierOrder(supplierOrder));
    }
}