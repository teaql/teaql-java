
package com.teaql.supplieronboardingservice.warehouse;

import com.teaql.supplieronboardingservice.supplier.Supplier;
import com.teaql.supplieronboardingservice.supplier.SupplierListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class WarehouseExpression<T, E, U extends Warehouse> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public WarehouseExpression(Expression<T, U> expression){
        super(expression);
    }

    public WarehouseExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public WarehouseExpression<T, U, U> updateId(Long id){
        return new WarehouseExpression(this, $it -> {((Warehouse)$it).__internalSet("id", id); return this;});
     }

     public WarehouseExpression<T, U, U> save(UserContext userContext){
        return new WarehouseExpression(this, $it -> ((Warehouse)$it).auditAs("Saved by Expression").save(userContext));
     }

     public WarehouseExpression<T, U, U> save(String intent, UserContext userContext){
        return new WarehouseExpression(this, $it -> ((Warehouse)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Warehouse::getName);
    }
    public WarehouseExpression<T, U, U> updateName(String name){
       return new WarehouseExpression(this, $it ->  ((Warehouse)$it).updateName(name));
    }

    public SupplierListExpression<T, U, Supplier> getSupplierList(){
        return new SupplierListExpression(loaded("supplier_list", "getSupplierList", Warehouse::getSupplierList));
    }
    public WarehouseExpression<T, U, U> addSupplier(Supplier supplier){
       return new WarehouseExpression(this, $it ->  ((Warehouse)$it).addSupplier(supplier));
    }
}