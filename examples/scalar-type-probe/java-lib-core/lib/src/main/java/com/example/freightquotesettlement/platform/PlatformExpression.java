
package com.example.freightquotesettlement.platform;

import com.example.freightquotesettlement.freightquote.FreightQuote;
import com.example.freightquotesettlement.freightquote.FreightQuoteListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class PlatformExpression<T, E, U extends Platform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public PlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PlatformExpression<T, U, U> save(UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Platform::getName);
    }
    public PlatformExpression<T, U, U> updateName(String name){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateName(name));
    }

    public FreightQuoteListExpression<T, U, FreightQuote> getFreightQuoteList(){
        return new FreightQuoteListExpression(loaded("freight_quote_list", "getFreightQuoteList", Platform::getFreightQuoteList));
    }
    public PlatformExpression<T, U, U> addFreightQuote(FreightQuote freightQuote){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addFreightQuote(freightQuote));
    }
}