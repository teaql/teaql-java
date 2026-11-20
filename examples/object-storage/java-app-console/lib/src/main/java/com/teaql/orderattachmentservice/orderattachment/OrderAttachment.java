
package com.teaql.orderattachmentservice.orderattachment;

import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class OrderAttachment extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "OrderAttachment";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "1b7e0fc32cc094c60c46058779d5933a02527a724ffa64e3e722a9670f3883aa";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("customer_order", 1), java.util.Map.entry("file_name", 2), java.util.Map.entry("content_type", 3), java.util.Map.entry("content_length", 4), java.util.Map.entry("content_sha256", 5), java.util.Map.entry("object_key", 6), java.util.Map.entry("version", 7));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("customer_order", java.util.List.of("customerOrder", "customer_order")), java.util.Map.entry("file_name", java.util.List.of("fileName", "file_name")), java.util.Map.entry("content_type", java.util.List.of("contentType", "content_type")), java.util.Map.entry("content_length", java.util.List.of("contentLength", "content_length")), java.util.Map.entry("content_sha256", java.util.List.of("contentSha256", "content_sha256")), java.util.Map.entry("object_key", java.util.List.of("objectKey", "object_key")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
);
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            OrderAttachment.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String CUSTOMER_ORDER_PROPERTY = "customer_order";
    public static final String FILE_NAME_PROPERTY = "file_name";
    public static final String CONTENT_TYPE_PROPERTY = "content_type";
    public static final String CONTENT_LENGTH_PROPERTY = "content_length";
    public static final String CONTENT_SHA256_PROPERTY = "content_sha256";
    public static final String OBJECT_KEY_PROPERTY = "object_key";
    private CustomerOrder customerOrder;

    private String fileName;

    private String contentType;

    private Integer contentLength;

    private String contentSha256;

    private String objectKey;


    public CustomerOrder getCustomerOrder(){
        return this.customerOrder;
    }

    public String getFileName(){
        return this.fileName;
    }

    public String getContentType(){
        return this.contentType;
    }

    public Integer getContentLength(){
        return this.contentLength;
    }

    public String getContentSha256(){
        return this.contentSha256;
    }

    public String getObjectKey(){
        return this.objectKey;
    }

    public OrderAttachment updateCustomerOrder(CustomerOrder customerOrder){
        if(isPropertyLoaded(CUSTOMER_ORDER_PROPERTY) && Objects.equals(this.customerOrder, customerOrder)){
            return this;
        }
        handleUpdate(CUSTOMER_ORDER_PROPERTY, getCustomerOrder(), customerOrder);
        this.customerOrder = customerOrder;
        return this;
    }

    public OrderAttachment updateFileName(String fileName){
        fileName = (fileName == null ? null : fileName.trim());
        if(isPropertyLoaded(FILE_NAME_PROPERTY) && Objects.equals(this.fileName, fileName)){
            return this;
        }
        handleUpdate(FILE_NAME_PROPERTY, getFileName(), fileName);
        this.fileName = fileName;
        return this;
    }

    public OrderAttachment updateContentType(String contentType){
        contentType = (contentType == null ? null : contentType.trim());
        if(isPropertyLoaded(CONTENT_TYPE_PROPERTY) && Objects.equals(this.contentType, contentType)){
            return this;
        }
        handleUpdate(CONTENT_TYPE_PROPERTY, getContentType(), contentType);
        this.contentType = contentType;
        return this;
    }

    public OrderAttachment updateContentLength(Integer contentLength){
        if(isPropertyLoaded(CONTENT_LENGTH_PROPERTY) && Objects.equals(this.contentLength, contentLength)){
            return this;
        }
        handleUpdate(CONTENT_LENGTH_PROPERTY, getContentLength(), contentLength);
        this.contentLength = contentLength;
        return this;
    }

    public OrderAttachment updateContentSha256(String contentSha256){
        contentSha256 = (contentSha256 == null ? null : contentSha256.trim());
        if(isPropertyLoaded(CONTENT_SHA256_PROPERTY) && Objects.equals(this.contentSha256, contentSha256)){
            return this;
        }
        handleUpdate(CONTENT_SHA256_PROPERTY, getContentSha256(), contentSha256);
        this.contentSha256 = contentSha256;
        return this;
    }

    public OrderAttachment updateObjectKey(String objectKey){
        objectKey = (objectKey == null ? null : objectKey.trim());
        if(isPropertyLoaded(OBJECT_KEY_PROPERTY) && Objects.equals(this.objectKey, objectKey)){
            return this;
        }
        handleUpdate(OBJECT_KEY_PROPERTY, getObjectKey(), objectKey);
        this.objectKey = objectKey;
        return this;
    }


    public static OrderAttachment refer(Long id){
        OrderAttachment refer = new OrderAttachment();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public OrderAttachment comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<OrderAttachment> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "customer_order": this.customerOrder = (CustomerOrder) value; break;

            case "file_name": this.fileName = (value == null ? null : ((String)value).trim()); break;

            case "content_type": this.contentType = (value == null ? null : ((String)value).trim()); break;

            case "content_length": this.contentLength = (Integer) value; break;

            case "content_sha256": this.contentSha256 = (value == null ? null : ((String)value).trim()); break;

            case "object_key": this.objectKey = (value == null ? null : ((String)value).trim()); break;

            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "customer_order": return this.customerOrder;
            case "file_name": return this.fileName;
            case "content_type": return this.contentType;
            case "content_length": return this.contentLength;
            case "content_sha256": return this.contentSha256;
            case "object_key": return this.objectKey;
            default:
                // Presentation uses the installed Java member mapping; SQL and
                // hydration retain canonical names. These aliases only read.
                switch (property) {
                    case "customerOrder": return this.customerOrder;
                    case "fileName": return this.fileName;
                    case "contentType": return this.contentType;
                    case "contentLength": return this.contentLength;
                    case "contentSha256": return this.contentSha256;
                    case "objectKey": return this.objectKey;
                    default: return super.__internalGet(property);
                }
        }
    }

}