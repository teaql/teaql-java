
package com.teaql.ordermanagementservice.orderline;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.product.Product;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
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
public class OrderLine extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderLine";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "561e1330c3f6785b4b09c99056d83367e88ffa0c3bc41bf7be5a42ba53982496";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("customer_order", 1), java.util.Map.entry("product", 2), java.util.Map.entry("product_name", 3), java.util.Map.entry("sku", 4), java.util.Map.entry("quantity", 5), java.util.Map.entry("commerce_platform", 6), java.util.Map.entry("create_time", 7), java.util.Map.entry("version", 8));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("customer_order", java.util.List.of("customerOrder", "customer_order")), java.util.Map.entry("product", java.util.List.of("product", "product")), java.util.Map.entry("product_name", java.util.List.of("productName", "product_name")), java.util.Map.entry("sku", java.util.List.of("sku", "sku")), java.util.Map.entry("quantity", java.util.List.of("quantity", "quantity")), java.util.Map.entry("commerce_platform", java.util.List.of("commercePlatform", "commerce_platform")), java.util.Map.entry("create_time", java.util.List.of("createTime", "create_time")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderLine.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CUSTOMER_ORDER_PROPERTY = "customer_order";
    public static final String PRODUCT_PROPERTY = "product";
    public static final String PRODUCT_NAME_PROPERTY = "product_name";
    public static final String SKU_PROPERTY = "sku";
    public static final String QUANTITY_PROPERTY = "quantity";
    public static final String COMMERCE_PLATFORM_PROPERTY = "commerce_platform";
    public static final String CREATE_TIME_PROPERTY = "create_time";
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
        if(isPropertyLoaded(CUSTOMER_ORDER_PROPERTY) && Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public OrderLine updateProduct(Product product){
        if(isPropertyLoaded(PRODUCT_PROPERTY) && Objects.equals(this.product, product)){
            return this;
        }
        handleUpdate(PRODUCT_PROPERTY, getProduct(), product);
        this.product = product;
        return this;
    }

    public OrderLine updateProductName(String productName){
        productName = (productName == null ? null : productName.trim());
        if(isPropertyLoaded(PRODUCT_NAME_PROPERTY) && Objects.equals(this.productName, productName)){
            return this;
        }
        handleUpdate(PRODUCT_NAME_PROPERTY, getProductName(), productName);
        this.productName = productName;
        return this;
    }

    public OrderLine updateSku(String sku){
        sku = (sku == null ? null : sku.trim());
        if(isPropertyLoaded(SKU_PROPERTY) && Objects.equals(this.sku, sku)){
            return this;
        }
        handleUpdate(SKU_PROPERTY, getSku(), sku);
        this.sku = sku;
        return this;
    }

    public OrderLine updateQuantity(Integer quantity){
        if(isPropertyLoaded(QUANTITY_PROPERTY) && Objects.equals(this.quantity, quantity)){
            return this;
        }
        handleUpdate(QUANTITY_PROPERTY, getQuantity(), quantity);
        this.quantity = quantity;
        return this;
    }

    public OrderLine updateCommercePlatform(CommercePlatform commercePlatform){
        if(isPropertyLoaded(COMMERCE_PLATFORM_PROPERTY) && Objects.equals(this.commercePlatform, commercePlatform)){
            return this;
        }
        handleUpdate(COMMERCE_PLATFORM_PROPERTY, getCommercePlatform(), commercePlatform);
        this.commercePlatform = commercePlatform;
        return this;
    }

    public OrderLine updateCreateTime(LocalDateTime createTime){
        if(isPropertyLoaded(CREATE_TIME_PROPERTY) && Objects.equals(this.createTime, createTime)){
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
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customer_order": this.customerOrder = (CustomerOrder) value; break;

            case "product": this.product = (Product) value; break;

            case "product_name": this.productName = (value == null ? null : ((String)value).trim()); break;

            case "sku": this.sku = (value == null ? null : ((String)value).trim()); break;

            case "quantity": this.quantity = (Integer) value; break;

            case "commerce_platform": this.commercePlatform = (CommercePlatform) value; break;

            case "create_time": this.createTime = (LocalDateTime) value; break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customer_order": return this.customerOrder;
            case "product": return this.product;
            case "product_name": return this.productName;
            case "sku": return this.sku;
            case "quantity": return this.quantity;
            case "commerce_platform": return this.commercePlatform;
            case "create_time": return this.createTime;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
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

}