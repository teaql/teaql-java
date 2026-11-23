
package com.example.freightquotesettlement.freightquote;

import com.example.freightquotesettlement.platform.Platform;
import com.example.freightquotesettlement.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class FreightQuoteExpression<T, E, U extends FreightQuote> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public FreightQuoteExpression(Expression<T, U> expression){
        super(expression);
    }

    public FreightQuoteExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public FreightQuoteExpression<T, U, U> save(UserContext userContext){
        return new FreightQuoteExpression(this, $it -> ((FreightQuote)$it).auditAs("Saved by Expression").save(userContext));
     }

     public FreightQuoteExpression<T, U, U> save(String intent, UserContext userContext){
        return new FreightQuoteExpression(this, $it -> ((FreightQuote)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", FreightQuote::getPlatform));
    }

    public FreightQuoteExpression<T, U, U> updatePlatform(Platform platform){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updatePlatform(platform));
    }

    public Expression<T, String> getReference(){
       return loaded("reference", "getReference", FreightQuote::getReference);
    }
    public FreightQuoteExpression<T, U, U> updateReference(String reference){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateReference(reference));
    }

    public Expression<T, BigDecimal> getAmount(){
       return loaded("amount", "getAmount", FreightQuote::getAmount);
    }
    public FreightQuoteExpression<T, U, U> updateAmount(BigDecimal amount){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateAmount(amount));
    }

    public Expression<T, Long> getShipmentNumber(){
       return loaded("shipment_number", "getShipmentNumber", FreightQuote::getShipmentNumber);
    }
    public FreightQuoteExpression<T, U, U> updateShipmentNumber(Long shipmentNumber){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateShipmentNumber(shipmentNumber));
    }

    public Expression<T, Integer> getPackageCount(){
       return loaded("package_count", "getPackageCount", FreightQuote::getPackageCount);
    }
    public FreightQuoteExpression<T, U, U> updatePackageCount(Integer packageCount){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updatePackageCount(packageCount));
    }

    public Expression<T, LocalDate> getDeliveryDate(){
       return loaded("delivery_date", "getDeliveryDate", FreightQuote::getDeliveryDate);
    }
    public FreightQuoteExpression<T, U, U> updateDeliveryDate(LocalDate deliveryDate){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateDeliveryDate(deliveryDate));
    }

    public Expression<T, LocalTime> getPickupTime(){
       return loaded("pickup_time", "getPickupTime", FreightQuote::getPickupTime);
    }
    public FreightQuoteExpression<T, U, U> updatePickupTime(LocalTime pickupTime){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updatePickupTime(pickupTime));
    }

    public Expression<T, LocalDateTime> getQuotedAt(){
       return loaded("quoted_at", "getQuotedAt", FreightQuote::getQuotedAt);
    }
    public FreightQuoteExpression<T, U, U> updateQuotedAt(LocalDateTime quotedAt){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateQuotedAt(quotedAt));
    }

    public Expression<T, String> getInstructions(){
       return loaded("instructions", "getInstructions", FreightQuote::getInstructions);
    }
    public FreightQuoteExpression<T, U, U> updateInstructions(String instructions){
       return new FreightQuoteExpression(this, $it ->  ((FreightQuote)$it).updateInstructions(instructions));
    }

}