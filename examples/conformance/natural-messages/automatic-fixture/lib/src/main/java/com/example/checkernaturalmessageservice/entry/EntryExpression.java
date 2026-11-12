
package com.example.checkernaturalmessageservice.entry;

import com.example.checkernaturalmessageservice.catalog.Catalog;
import com.example.checkernaturalmessageservice.catalog.CatalogExpression;
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

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", Entry::getCreateTime);
    }
    public EntryExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", Entry::getUpdateTime);
    }
    public EntryExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new EntryExpression(this, $it ->  ((Entry)$it).updateUpdateTime(updateTime));
    }

}