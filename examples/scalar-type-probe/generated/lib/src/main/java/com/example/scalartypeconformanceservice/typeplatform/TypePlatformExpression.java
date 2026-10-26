
package com.example.scalartypeconformanceservice.typeplatform;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class TypePlatformExpression<T, E, U extends TypePlatform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public TypePlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public TypePlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public TypePlatformExpression<T, U, U> updateId(Long id){
        return new TypePlatformExpression(this, $it -> {((TypePlatform)$it).__internalSet("id", id); return this;});
     }

     public TypePlatformExpression<T, U, U> save(UserContext userContext){
        return new TypePlatformExpression(this, $it -> ((TypePlatform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public TypePlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new TypePlatformExpression(this, $it -> ((TypePlatform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", TypePlatform::getName);
    }
    public TypePlatformExpression<T, U, U> updateName(String name){
       return new TypePlatformExpression(this, $it ->  ((TypePlatform)$it).updateName(name));
    }

    public ScalarTypeProbeListExpression<T, U, ScalarTypeProbe> getScalarTypeProbeList(){
        return new ScalarTypeProbeListExpression(loaded("scalar_type_probe_list", "getScalarTypeProbeList", TypePlatform::getScalarTypeProbeList));
    }
    public TypePlatformExpression<T, U, U> addScalarTypeProbe(ScalarTypeProbe scalarTypeProbe){
       return new TypePlatformExpression(this, $it ->  ((TypePlatform)$it).addScalarTypeProbe(scalarTypeProbe));
    }
}