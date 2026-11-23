
package com.example.contextfixservice.entry;

import com.example.contextfixservice.catalog.Catalog;
import com.example.contextfixservice.catalog.CatalogExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class EntryExpression<T, E, U extends Entry> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public EntryExpression(Expression<T, U> expression){
        super(expression);
    }

    public EntryExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public EntryExpression<T, U, U> updateId(Long id){
        return new EntryExpression(this, $it -> {((Entry)$it).__internalSet("id", id); return this;});
     }

     public EntryExpression<T, U, U> save(UserContext userContext){
        return new EntryExpression(this, $it -> ((Entry)$it).auditAs("Saved by Expression").save(userContext));
     }

     public EntryExpression<T, U, U> save(String intent, UserContext userContext){
        return new EntryExpression(this, $it -> ((Entry)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CatalogExpression<T, U, Catalog> getCatalog(){
       return new CatalogExpression(loaded("catalog", "getCatalog", Entry::getCatalog));
    }

    public EntryExpression<T, U, U> updateCatalog(Catalog catalog){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateCatalog(catalog));
    }

    public Expression<T, String> getName(){
       return loaded("name", "getName", Entry::getName);
    }
    public EntryExpression<T, U, U> updateName(String name){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateName(name));
    }

    public Expression<T, LocalDateTime> getBornAt(){
       return loaded("born_at", "getBornAt", Entry::getBornAt);
    }
    public EntryExpression<T, U, U> updateBornAt(LocalDateTime bornAt){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateBornAt(bornAt));
    }

    public Expression<T, LocalDateTime> getTouchedAt(){
       return loaded("touched_at", "getTouchedAt", Entry::getTouchedAt);
    }
    public EntryExpression<T, U, U> updateTouchedAt(LocalDateTime touchedAt){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateTouchedAt(touchedAt));
    }

    public Expression<T, String> getSourceIp(){
       return loaded("source_ip", "getSourceIp", Entry::getSourceIp);
    }
    public EntryExpression<T, U, U> updateSourceIp(String sourceIp){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateSourceIp(sourceIp));
    }

    public Expression<T, String> getLastIp(){
       return loaded("last_ip", "getLastIp", Entry::getLastIp);
    }
    public EntryExpression<T, U, U> updateLastIp(String lastIp){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateLastIp(lastIp));
    }

    public Expression<T, String> getOpenedBy(){
       return loaded("opened_by", "getOpenedBy", Entry::getOpenedBy);
    }
    public EntryExpression<T, U, U> updateOpenedBy(String openedBy){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateOpenedBy(openedBy));
    }

    public Expression<T, String> getChangedBy(){
       return loaded("changed_by", "getChangedBy", Entry::getChangedBy);
    }
    public EntryExpression<T, U, U> updateChangedBy(String changedBy){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateChangedBy(changedBy));
    }

    public Expression<T, String> getGuardedToken(){
       return loaded("guarded_token", "getGuardedToken", Entry::getGuardedToken);
    }
    public EntryExpression<T, U, U> updateGuardedToken(String guardedToken){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateGuardedToken(guardedToken));
    }

}