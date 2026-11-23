
package com.example.contextfixservice.catalog;

import com.example.contextfixservice.entry.Entry;
import com.example.contextfixservice.entry.EntryListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CatalogExpression<T, E, U extends Catalog> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CatalogExpression(Expression<T, U> expression){
        super(expression);
    }

    public CatalogExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CatalogExpression<T, U, U> updateId(Long id){
        return new CatalogExpression(this, $it -> {((Catalog)$it).__internalSet("id", id); return this;});
     }

     public CatalogExpression<T, U, U> save(UserContext userContext){
        return new CatalogExpression(this, $it -> ((Catalog)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CatalogExpression<T, U, U> save(String intent, UserContext userContext){
        return new CatalogExpression(this, $it -> ((Catalog)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Catalog::getName);
    }
    public CatalogExpression<T, U, U> updateName(String name){
       return new CatalogExpression(this, $it ->  ((Catalog)$it).updateName(name));
    }

    public EntryListExpression<T, U, Entry> getEntryList(){
        return new EntryListExpression(loaded("entry_list", "getEntryList", Catalog::getEntryList));
    }
    public CatalogExpression<T, U, U> addEntry(Entry entry){
       return new CatalogExpression(this, $it ->  ((Catalog)$it).addEntry(entry));
    }
}