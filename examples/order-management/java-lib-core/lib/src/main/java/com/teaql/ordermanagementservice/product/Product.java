
package com.teaql.ordermanagementservice.product;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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

    public static final String NAME_PROPERTY = "name";
    public static final String SKU_PROPERTY = "sku";
    public static final String IMAGE_URL_PROPERTY = "imageUrl";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    public static final String UPDATE_TIME_PROPERTY = "updateTime";
    public static final String ORDER_LINE_LIST_PROPERTY = "orderLineList";
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
        return this.orderLineList;
    }
    public Product updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public Product updateSku(String sku){
        sku = (sku == null ? null : sku.trim());
        if(Objects.equals(this.sku, sku)){
            return this;
        }
        handleUpdate(SKU_PROPERTY, getSku(), sku);
        this.sku = sku;
        return this;
    }
    public Product updateImageUrl(String imageUrl){
        imageUrl = (imageUrl == null ? null : imageUrl.trim());
        if(Objects.equals(this.imageUrl, imageUrl)){
            return this;
        }
        handleUpdate(IMAGE_URL_PROPERTY, getImageUrl(), imageUrl);
        this.imageUrl = imageUrl;
        return this;
    }
    public Product updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public Product updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }
    public Product updateUpdateTime(LocalDateTime updateTime){
        if(Objects.equals(this.updateTime, updateTime)){
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
            this.orderLineList = new SmartList<>();
        }

        this.orderLineList.add(orderLine);
        orderLine.cacheRelation(OrderLine.PRODUCT_PROPERTY, this);
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
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "sku": this.sku = (value == null ? null : ((String)value).trim()); break;

            case "imageUrl": this.imageUrl = (value == null ? null : ((String)value).trim()); break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            case "updateTime": this.updateTime = (LocalDateTime) value; break;

            case "orderLineList": this.orderLineList = (SmartList<OrderLine>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "sku": return this.sku;
            case "imageUrl": return this.imageUrl;
            case "commercePlatform": return this.commercePlatform;
            case "createTime": return this.createTime;
            case "updateTime": return this.updateTime;
            case "orderLineList": return this.orderLineList;
            default: return super.__internalGet(property);
        }
    }

}