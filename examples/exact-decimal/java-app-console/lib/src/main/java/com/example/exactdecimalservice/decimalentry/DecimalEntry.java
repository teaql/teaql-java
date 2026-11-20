
package com.example.exactdecimalservice.decimalentry;

import com.example.exactdecimalservice.platform.Platform;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class DecimalEntry extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "DecimalEntry";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "46367c1f6d22180cb63ea4bcac6772b4e507f4ef69000bb272b73de3ab179c0f";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("platform", 1), java.util.Map.entry("label", 2), java.util.Map.entry("amount", 3), java.util.Map.entry("version", 4));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("platform", java.util.List.of("platform", "platform")), java.util.Map.entry("label", java.util.List.of("label", "label")), java.util.Map.entry("amount", java.util.List.of("amount", "amount")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            DecimalEntry.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String PLATFORM_PROPERTY = "platform";
    public static final String LABEL_PROPERTY = "label";
    public static final String AMOUNT_PROPERTY = "amount";
    private Platform platform;

    private String label;

    private BigDecimal amount;


    public Platform getPlatform(){
        return this.platform;
    }

    public String getLabel(){
        return this.label;
    }

    public BigDecimal getAmount(){
        return this.amount;
    }

    public DecimalEntry updatePlatform(Platform platform){
        if(isPropertyLoaded(PLATFORM_PROPERTY) && Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }

    public DecimalEntry updateLabel(String label){
        label = (label == null ? null : label.trim());
        if(isPropertyLoaded(LABEL_PROPERTY) && Objects.equals(this.label, label)){
            return this;
        }
        handleUpdate(LABEL_PROPERTY, getLabel(), label);
        this.label = label;
        return this;
    }

    public DecimalEntry updateAmount(BigDecimal amount){
        if(isPropertyLoaded(AMOUNT_PROPERTY) && Objects.equals(this.amount, amount)){
            return this;
        }
        handleUpdate(AMOUNT_PROPERTY, getAmount(), amount);
        this.amount = amount;
        return this;
    }


    public static DecimalEntry refer(Long id){
        DecimalEntry refer = new DecimalEntry();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public DecimalEntry comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<DecimalEntry> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "label": this.label = (value == null ? null : ((String)value).trim()); break;

            case "amount": this.amount = (BigDecimal) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "label": return this.label;
            case "amount": return this.amount;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "platform": return this.platform;
                    case "label": return this.label;
                    case "amount": return this.amount;
                    default: return super.__internalGet(property);
                }
        }
    }

}