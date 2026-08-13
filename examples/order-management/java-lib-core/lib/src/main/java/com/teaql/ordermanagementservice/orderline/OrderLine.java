
package com.teaql.ordermanagementservice.orderline;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.product.Product;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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
public class OrderLine extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderLine";

    public static final String CUSTOMER_ORDER_PROPERTY = "customerOrder";
    public static final String PRODUCT_PROPERTY = "product";
    public static final String PRODUCT_NAME_PROPERTY = "productName";
    public static final String SKU_PROPERTY = "sku";
    public static final String QUANTITY_PROPERTY = "quantity";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commercePlatform";
    public static final String CREATE_TIME_PROPERTY = "createTime";
    private CustomerOrder customerOrder;
    private Product product;
    private String productName;
    private String sku;
    private Integer quantity;
    private CommercePlatform commercePlatform;
    private LocalDateTime createTime;

    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }
    public Product getProduct(){
        return this.product;
    }
    public String getProductName(){
        return this.productName;
    }
    public String getSku(){
        return this.sku;
    }
    public Integer getQuantity(){
        return this.quantity;
    }
    public CommercePlatform getCommercePlatform(){
        return this.commercePlatform;
    }
    public LocalDateTime getCreateTime(){
        return this.createTime;
    }
    public OrderLine updateCustomerOrder(CustomerOrder customerOrder){
        if(Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }
    public OrderLine updateProduct(Product product){
        if(Objects.equals(this.product, product)){
            return this;
        }
        handleUpdate(PRODUCT_PROPERTY, getProduct(), product);
        this.product = product;
        return this;
    }
    public OrderLine updateProductName(String productName){
        productName = (productName == null ? null : productName.trim());
        if(Objects.equals(this.productName, productName)){
            return this;
        }
        handleUpdate(PRODUCT_NAME_PROPERTY, getProductName(), productName);
        this.productName = productName;
        return this;
    }
    public OrderLine updateSku(String sku){
        sku = (sku == null ? null : sku.trim());
        if(Objects.equals(this.sku, sku)){
            return this;
        }
        handleUpdate(SKU_PROPERTY, getSku(), sku);
        this.sku = sku;
        return this;
    }
    public OrderLine updateQuantity(Integer quantity){
        if(Objects.equals(this.quantity, quantity)){
            return this;
        }
        handleUpdate(QUANTITY_PROPERTY, getQuantity(), quantity);
        this.quantity = quantity;
        return this;
    }
    public OrderLine updateCommercePlatform(CommercePlatform commercePlatform){
        if(Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }
    public OrderLine updateCreateTime(LocalDateTime createTime){
        if(Objects.equals(this.createTime, createTime)){
            return this;
        }
        handleUpdate(CREATE_TIME_PROPERTY, getCreateTime(), createTime);
        this.createTime = createTime;
        return this;
    }

    public static OrderLine refer(Long id){
        OrderLine refer = new OrderLine();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderLine comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderLine> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        switch (property) {
            case "customerOrder": this.customerOrder = (CustomerOrder) value; break;

            case "product": this.product = (Product) value; break;

            case "productName": this.productName = (value == null ? null : ((String)value).trim()); break;

            case "sku": this.sku = (value == null ? null : ((String)value).trim()); break;

            case "quantity": this.quantity = (Integer) value; break;

            case "commercePlatform": this.commercePlatform = (CommercePlatform) value; break;

            case "createTime": this.createTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customerOrder": return this.customerOrder;
            case "product": return this.product;
            case "productName": return this.productName;
            case "sku": return this.sku;
            case "quantity": return this.quantity;
            case "commercePlatform": return this.commercePlatform;
            case "createTime": return this.createTime;
            default: return super.__internalGet(property);
        }
    }

}