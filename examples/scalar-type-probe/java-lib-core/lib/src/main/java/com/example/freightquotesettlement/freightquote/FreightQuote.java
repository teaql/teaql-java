
package com.example.freightquotesettlement.freightquote;

import com.example.freightquotesettlement.platform.Platform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class FreightQuote extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "FreightQuote";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "661de8675c51355614d50183496c991171adfdb1f52cc15029141a8114080a1e";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("reference", 2), java.util.Map.entry("amount", 3), java.util.Map.entry("shipment_number", 4), java.util.Map.entry("package_count", 5), java.util.Map.entry("delivery_date", 6), java.util.Map.entry("pickup_time", 7), java.util.Map.entry("quoted_at", 8), java.util.Map.entry("instructions", 9), java.util.Map.entry("version", 10));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("reference", java.util.List.of("reference", "reference")), java.util.Map.entry("amount", java.util.List.of("amount", "amount")), java.util.Map.entry("shipment_number", java.util.List.of("shipmentNumber", "shipment_number")), java.util.Map.entry("package_count", java.util.List.of("packageCount", "package_count")), java.util.Map.entry("delivery_date", java.util.List.of("deliveryDate", "delivery_date")), java.util.Map.entry("pickup_time", java.util.List.of("pickupTime", "pickup_time")), java.util.Map.entry("quoted_at", java.util.List.of("quotedAt", "quoted_at")), java.util.Map.entry("instructions", java.util.List.of("instructions", "instructions")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            FreightQuote.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String REFERENCE_PROPERTY = "reference";
    public static final String AMOUNT_PROPERTY = "amount";
    public static final String SHIPMENT_NUMBER_PROPERTY = "shipment_number";
    public static final String PACKAGE_COUNT_PROPERTY = "package_count";
    public static final String DELIVERY_DATE_PROPERTY = "delivery_date";
    public static final String PICKUP_TIME_PROPERTY = "pickup_time";
    public static final String QUOTED_AT_PROPERTY = "quoted_at";
    public static final String INSTRUCTIONS_PROPERTY = "instructions";
    private Platform platform;

    private String reference;

    private BigDecimal amount;

    private Long shipmentNumber;

    private Integer packageCount;

    private LocalDate deliveryDate;

    private LocalTime pickupTime;

    private LocalDateTime quotedAt;

    private String instructions;


    public Platform getPlatform(){
        return this.platform;
    }

    public String getReference(){
        return this.reference;
    }

    public BigDecimal getAmount(){
        return this.amount;
    }

    public Long getShipmentNumber(){
        return this.shipmentNumber;
    }

    public Integer getPackageCount(){
        return this.packageCount;
    }

    public LocalDate getDeliveryDate(){
        return this.deliveryDate;
    }

    public LocalTime getPickupTime(){
        return this.pickupTime;
    }

    public LocalDateTime getQuotedAt(){
        return this.quotedAt;
    }

    public String getInstructions(){
        return this.instructions;
    }

    public FreightQuote updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public FreightQuote updateReference(String reference){
        reference = (reference == null ? null : reference.trim());
        if(isPropertyLoaded(REFERENCE_PROPERTY) && Objects.equals(this.reference, reference)){
            return this;
        }
        handleUpdate(REFERENCE_PROPERTY, getReference(), reference);
        this.reference = reference;
        return this;
    }

    public FreightQuote updateAmount(BigDecimal amount){
        if(isPropertyLoaded(AMOUNT_PROPERTY) && Objects.equals(this.amount, amount)){
            return this;
        }
        handleUpdate(AMOUNT_PROPERTY, getAmount(), amount);
        this.amount = amount;
        return this;
    }

    public FreightQuote updateShipmentNumber(Long shipmentNumber){
        if(isPropertyLoaded(SHIPMENT_NUMBER_PROPERTY) && Objects.equals(this.shipmentNumber, shipmentNumber)){
            return this;
        }
        handleUpdate(SHIPMENT_NUMBER_PROPERTY, getShipmentNumber(), shipmentNumber);
        this.shipmentNumber = shipmentNumber;
        return this;
    }

    public FreightQuote updatePackageCount(Integer packageCount){
        if(isPropertyLoaded(PACKAGE_COUNT_PROPERTY) && Objects.equals(this.packageCount, packageCount)){
            return this;
        }
        handleUpdate(PACKAGE_COUNT_PROPERTY, getPackageCount(), packageCount);
        this.packageCount = packageCount;
        return this;
    }

    public FreightQuote updateDeliveryDate(LocalDate deliveryDate){
        if(isPropertyLoaded(DELIVERY_DATE_PROPERTY) && Objects.equals(this.deliveryDate, deliveryDate)){
            return this;
        }
        handleUpdate(DELIVERY_DATE_PROPERTY, getDeliveryDate(), deliveryDate);
        this.deliveryDate = deliveryDate;
        return this;
    }

    public FreightQuote updatePickupTime(LocalTime pickupTime){
        if(isPropertyLoaded(PICKUP_TIME_PROPERTY) && Objects.equals(this.pickupTime, pickupTime)){
            return this;
        }
        handleUpdate(PICKUP_TIME_PROPERTY, getPickupTime(), pickupTime);
        this.pickupTime = pickupTime;
        return this;
    }

    public FreightQuote updateQuotedAt(LocalDateTime quotedAt){
        if(isPropertyLoaded(QUOTED_AT_PROPERTY) && Objects.equals(this.quotedAt, quotedAt)){
            return this;
        }
        handleUpdate(QUOTED_AT_PROPERTY, getQuotedAt(), quotedAt);
        this.quotedAt = quotedAt;
        return this;
    }

    public FreightQuote updateInstructions(String instructions){
        instructions = (instructions == null ? null : instructions.trim());
        if(isPropertyLoaded(INSTRUCTIONS_PROPERTY) && Objects.equals(this.instructions, instructions)){
            return this;
        }
        handleUpdate(INSTRUCTIONS_PROPERTY, getInstructions(), instructions);
        this.instructions = instructions;
        return this;
    }


    public static FreightQuote refer(Long id){
        FreightQuote refer = new FreightQuote();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public FreightQuote comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<FreightQuote> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "reference": this.reference = (value == null ? null : ((String)value).trim()); break;

            case "amount": this.amount = (BigDecimal) value; break;

            case "shipment_number": this.shipmentNumber = (Long) value; break;

            case "package_count": this.packageCount = (Integer) value; break;

            case "delivery_date": this.deliveryDate = (LocalDate) value; break;

            case "pickup_time": this.pickupTime = (LocalTime) value; break;

            case "quoted_at": this.quotedAt = (LocalDateTime) value; break;

            case "instructions": this.instructions = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "reference": return this.reference;
            case "amount": return this.amount;
            case "shipment_number": return this.shipmentNumber;
            case "package_count": return this.packageCount;
            case "delivery_date": return this.deliveryDate;
            case "pickup_time": return this.pickupTime;
            case "quoted_at": return this.quotedAt;
            case "instructions": return this.instructions;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "reference": return this.reference;
                    case "amount": return this.amount;
                    case "shipmentNumber": return this.shipmentNumber;
                    case "packageCount": return this.packageCount;
                    case "deliveryDate": return this.deliveryDate;
                    case "pickupTime": return this.pickupTime;
                    case "quotedAt": return this.quotedAt;
                    case "instructions": return this.instructions;
                    default: return super.__internalGet(property);
                }
        }
    }

}