
package com.teaql.runtimeexampleconformanceservice.workitem;

import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class WorkItemExpression<T, E, U extends WorkItem> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public WorkItemExpression(Expression<T, U> expression){
        super(expression);
    }

    public WorkItemExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public WorkItemExpression<T, U, U> updateId(Long id){
        return new WorkItemExpression(this, $it -> {((WorkItem)$it).__internalSet("id", id); return this;});
     }

     public WorkItemExpression<T, U, U> save(UserContext userContext){
        return new WorkItemExpression(this, $it -> ((WorkItem)$it).auditAs("Saved by Expression").save(userContext));
     }

     public WorkItemExpression<T, U, U> save(String intent, UserContext userContext){
        return new WorkItemExpression(this, $it -> ((WorkItem)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getTitle(){
       return loaded("title", WorkItem::getTitle);
    }
    public WorkItemExpression<T, U, U> updateTitle(String title){
       return new WorkItemExpression(this, $it ->  ((WorkItem)$it).updateTitle(title));
    }

    public Expression<T, String> getDescription(){
       return loaded("description", WorkItem::getDescription);
    }
    public WorkItemExpression<T, U, U> updateDescription(String description){
       return new WorkItemExpression(this, $it ->  ((WorkItem)$it).updateDescription(description));
    }

    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", WorkItem::getPlatform));
    }

    public WorkItemExpression<T, U, U> updatePlatform(Platform platform){
       return new WorkItemExpression(this, $it ->  ((WorkItem)$it).updatePlatform(platform));
    }

}