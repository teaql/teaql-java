
package com.example.schoolmanagementservice.schooltype;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.school.School;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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
public class SchoolType extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "SchoolType";

    public static final String PLATFORM_PROPERTY = "platform";
    public static final String NAME_PROPERTY = "name";
    public static final String CODE_PROPERTY = "code";
    public static final String DISPLAY_ORDER_PROPERTY = "displayOrder";
    public static final String SCHOOL_LIST_PROPERTY = "schoolList";
    private Platform platform;
    private String name;
    private String code;
    private BigDecimal displayOrder;
    private SmartList<School> schoolList;

    public Platform getPlatform(){
        return this.platform;
    }
    public String getName(){
        return this.name;
    }
    public String getCode(){
        return this.code;
    }
    public BigDecimal getDisplayOrder(){
        return this.displayOrder;
    }
    public SmartList<School> getSchoolList(){
        return this.schoolList;
    }
    public SchoolType updatePlatform(Platform platform){
        if(Objects.equals(this.platform, platform)){
            return this;
        }
        handleUpdate(PLATFORM_PROPERTY, getPlatform(), platform);
        this.platform = platform;
        return this;
    }
    public SchoolType updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public SchoolType updateCode(String code){
        code = (code == null ? null : code.trim());
        if(Objects.equals(this.code, code)){
            return this;
        }
        handleUpdate(CODE_PROPERTY, getCode(), code);
        this.code = code;
        return this;
    }
    public SchoolType updateDisplayOrder(BigDecimal displayOrder){
        if(Objects.equals(this.displayOrder, displayOrder)){
            return this;
        }
        handleUpdate(DISPLAY_ORDER_PROPERTY, getDisplayOrder(), displayOrder);
        this.displayOrder = displayOrder;
        return this;
    }
    public SchoolType addSchool(School school){
        if (school == null){
            return this;
        }

        if(null == this.schoolList){
            this.schoolList = new SmartList<>();
        }

        this.schoolList.add(school);
        school.cacheRelation(School.SCHOOL_TYPE_PROPERTY, this);
        return this;
    }

    public static SchoolType refer(Long id){
        SchoolType refer = new SchoolType();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public SchoolType comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<SchoolType> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "platform": this.platform = (Platform) value; break;

            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "code": this.code = (value == null ? null : ((String)value).trim()); break;

            case "displayOrder": this.displayOrder = (BigDecimal) value; break;

            case "schoolList": this.schoolList = (SmartList<School>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "platform": return this.platform;
            case "name": return this.name;
            case "code": return this.code;
            case "displayOrder": return this.displayOrder;
            case "schoolList": return this.schoolList;
            default: return super.__internalGet(property);
        }
    }

}