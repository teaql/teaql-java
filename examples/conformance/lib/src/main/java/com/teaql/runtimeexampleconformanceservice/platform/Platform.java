
package com.teaql.runtimeexampleconformanceservice.platform;

import com.teaql.runtimeexampleconformanceservice.workitem.WorkItem;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
import java.util.Objects;

/**
 * [TEAQL AI WARNING]
 * TeaQL was explicitly designed to PREVENT AI hallucinations and random guessing.
 * DO NOT GUESS METHOD NAMES!
 * The methods listed below are the ONLY valid ways to interact with this entity.
 * If you encounter compilation errors (e.g., method not found), DO NOT guess another method name.
 * Read the method signatures in this file before proceeding.
 */
public class Platform extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "Platform";

    public static final String NAME_PROPERTY = "name";
    public static final String WORK_ITEM_LIST_PROPERTY = "workItemList";
    private String name;
    private SmartList<WorkItem> workItemList;

    public String getName(){
        return this.name;
    }
    public SmartList<WorkItem> getWorkItemList(){
        return this.workItemList;
    }
    public Platform updateName(String name){
        name = (name == null ? null : name.trim());
        if(Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }
    public Platform addWorkItem(WorkItem workItem){
        if (workItem == null){
            return this;
        }

        if(null == this.workItemList){
            this.workItemList = new SmartList<>();
        }

        this.workItemList.add(workItem);
        workItem.cacheRelation(WorkItem.PLATFORM_PROPERTY, this);
        return this;
    }

    public static Platform refer(Long id){
        Platform refer = new Platform();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public Platform comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<Platform> auditAs(String action) {
        return super.auditAs(action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "workItemList": this.workItemList = (SmartList<WorkItem>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "workItemList": return this.workItemList;
            default: return super.__internalGet(property);
        }
    }

}