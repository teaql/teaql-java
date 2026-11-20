
package com.example.exactdecimalservice.decimalentry;

import com.example.exactdecimalservice.platform.Platform;
import com.example.exactdecimalservice.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.util.function.Function;

public class DecimalEntryExpression<T, E, U extends DecimalEntry> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public DecimalEntryExpression(Expression<T, U> expression){
        super(expression);
    }

    public DecimalEntryExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public DecimalEntryExpression<T, U, U> updateId(Long id){
        return new DecimalEntryExpression(this, $it -> {((DecimalEntry)$it).__internalSet("id", id); return this;});
     }

     public DecimalEntryExpression<T, U, U> save(UserContext userContext){
        return new DecimalEntryExpression(this, $it -> ((DecimalEntry)$it).auditAs("Saved by Expression").save(userContext));
     }

     public DecimalEntryExpression<T, U, U> save(String intent, UserContext userContext){
        return new DecimalEntryExpression(this, $it -> ((DecimalEntry)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", DecimalEntry::getPlatform));
    }

    public DecimalEntryExpression<T, U, U> updatePlatform(Platform platform){
       return new DecimalEntryExpression(this, $it ->  ((DecimalEntry)$it).updatePlatform(platform));
    }

    public Expression<T, String> getLabel(){
       return loaded("label", "getLabel", DecimalEntry::getLabel);
    }
    public DecimalEntryExpression<T, U, U> updateLabel(String label){
       return new DecimalEntryExpression(this, $it ->  ((DecimalEntry)$it).updateLabel(label));
    }

    public Expression<T, BigDecimal> getAmount(){
       return loaded("amount", "getAmount", DecimalEntry::getAmount);
    }
    public DecimalEntryExpression<T, U, U> updateAmount(BigDecimal amount){
       return new DecimalEntryExpression(this, $it ->  ((DecimalEntry)$it).updateAmount(amount));
    }

}