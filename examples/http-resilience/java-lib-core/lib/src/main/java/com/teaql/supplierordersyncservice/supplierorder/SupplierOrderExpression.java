
package com.teaql.supplierordersyncservice.supplierorder;

import com.teaql.supplierordersyncservice.supplier.Supplier;
import com.teaql.supplierordersyncservice.supplier.SupplierExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class SupplierOrderExpression<T, E, U extends SupplierOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SupplierOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public SupplierOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SupplierOrderExpression<T, U, U> save(UserContext userContext){
        return new SupplierOrderExpression(this, $it -> ((SupplierOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SupplierOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new SupplierOrderExpression(this, $it -> ((SupplierOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public SupplierExpression<T, U, Supplier> getSupplier(){
       return new SupplierExpression(loaded("supplier", "getSupplier", SupplierOrder::getSupplier));
    }

    public SupplierOrderExpression<T, U, U> updateSupplier(Supplier supplier){
       return new SupplierOrderExpression(this, $it ->  ((SupplierOrder)$it).updateSupplier(supplier));
    }

    public Expression<T, String> getExternalReference(){
       return loaded("external_reference", "getExternalReference", SupplierOrder::getExternalReference);
    }
    public SupplierOrderExpression<T, U, U> updateExternalReference(String externalReference){
       return new SupplierOrderExpression(this, $it ->  ((SupplierOrder)$it).updateExternalReference(externalReference));
    }

    public Expression<T, String> getSyncStatus(){
       return loaded("sync_status", "getSyncStatus", SupplierOrder::getSyncStatus);
    }
    public SupplierOrderExpression<T, U, U> updateSyncStatus(String syncStatus){
       return new SupplierOrderExpression(this, $it ->  ((SupplierOrder)$it).updateSyncStatus(syncStatus));
    }

}