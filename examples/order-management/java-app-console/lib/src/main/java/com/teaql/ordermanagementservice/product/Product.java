
package com.teaql.ordermanagementservice.product;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class Product extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Product";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "53d713469d480508489d19bea0bdb683bf3d6b9b5c37b81ba4c247b0200f2d0d";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("sku", 2), java.util.Map.entry("image_url", 3), java.util.Map.entry("commerce_platform", 4), java.util.Map.entry("create_time", 5), java.util.Map.entry("update_time", 6), java.util.Map.entry("version", 7));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("sku", java.util.List.of("sku", "sku")), java.util.Map.entry("image_url", java.util.List.of("imageUrl", "image_url")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("update_time", java.util.List.of("updateTime", "update_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "order_line_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            Product.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String SKU_PROPERTY = "sku";
    public static final String IMAGE_URL_PROPERTY = "image_url";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String CREATE_TIME_PROPERTY = "create_time";
    public static final String UPDATE_TIME_PROPERTY = "update_time";
    public static final String ORDER_LINE_LIST_PROPERTY = "order_line_list";
    private String name;

    private String sku;

    private String imageUrl;

    private CommercePlatform commercePlatform;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private SmartList<OrderLine> orderLineList;

    public String getName(){
        return this.name;
    }

    public String getSku(){
        return this.sku;
    }

    public String getImageUrl(){
        return this.imageUrl;
    }

    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }

    public LocalDateTime getCreateTime(){
        return this.createTime;
    }

    public LocalDateTime getUpdateTime(){
        return this.updateTime;
    }

    public SmartList<OrderLine> getOrderLineList(){
        return this.orderLineList == null ? SmartList.unloaded() : this.orderLineList;
    }
    public Product updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public Product updateSku(String sku){
        sku = (sku == null ? null : sku.trim());
        if(isPropertyLoaded(SKU_PROPERTY) && Objects.equals(this.sku, sku)){
            return this;
        }
        handleUpdate(SKU_PROPERTY, getSku(), sku);
        this.sku = sku;
        return this;
    }

    public Product updateImageUrl(String imageUrl){
        imageUrl = (imageUrl == null ? null : imageUrl.trim());
        if(isPropertyLoaded(IMAGE_URL_PROPERTY) && Objects.equals(this.imageUrl, imageUrl)){
            return this;
        }
        handleUpdate(IMAGE_URL_PROPERTY, getImageUrl(), imageUrl);
        this.imageUrl = imageUrl;
        return this;
    }

    public Product updateCommercePlatform(CommercePlatform commercePlatform){
        if(isPropertyLoaded(COMMERCE_PLATFORM_PROPERTY) && Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }

    public Product updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public Product updateUpdateTime(LocalDateTime updateTime){
        if(isPropertyLoaded(UPDATE_TIME_PROPERTY) && Objects.equals(this.updateTime, updateTime)){
            return this;
        }
        handleUpdate(UPDATE_TIME_PROPERTY, getUpdateTime(), updateTime);
        this.updateTime = updateTime;
        return this;
    }

    public Product addOrderLine(OrderLine orderLine){
        if (orderLine == null){
            return this;
        }

        if(null == this.orderLineList){
            SmartList<OrderLine> teaqlUnloadedItems = SmartList.unloaded();
            this.orderLineList = teaqlUnloadedItems.map(value -> value);
        }

        this.orderLineList.add(orderLine);
        orderLine.updateProduct(this);
        return this;
    }

    public static Product refer(Long id){
        Product refer = new Product();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Product comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Product> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "sku": this.sku = (value == null ? null : ((String)value).trim()); break;

            case "image_url": this.imageUrl = (value == null ? null : ((String)value).trim()); break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            case "update_time": this.updateTime = (LocalDateTime) value; break;

            case "order_line_list": this.orderLineList = (SmartList<OrderLine>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "sku": return this.sku;
            case "image_url": return this.imageUrl;
            case "commerce_platform": return this.commercePlatform;
            case "create_time": return this.createTime;
            case "update_time": return this.updateTime;
            case "order_line_list": return this.orderLineList;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "name": return this.name;
                    case "sku": return this.sku;
                    case "imageUrl": return this.imageUrl;
                    case "commercePlatform": return this.commercePlatform;
                    case "createTime": return this.createTime;
                    case "updateTime": return this.updateTime;
                    default: return super.__internalGet(property);
                }
        }
    }

}