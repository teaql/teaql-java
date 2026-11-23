
package com.example.scalartypeconformanceservice.typeplatform;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import io.teaql.core.Audited;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.RemoteInput;
import io.teaql.core.SmartList;
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
public class TypePlatform extends BaseEntity implements RemoteInput {
    public static String INTERNAL_TYPE = "TypePlatform";

    // Framework metadata: complete fixed layout, independent of SQL/projection order.
    public static final String __TEAQL_FIELD_LAYOUT_REVISION = "5afe0d829346ae9bcaf7f37458837cfbdcdee743f3e9fb8fab9c754126349de5";
    public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
        java.util.Map.ofEntries(java.util.Map.entry("id", 0), java.util.Map.entry("name", 1), java.util.Map.entry("version", 2));
    public static final java.util.Map<String, java.util.List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.of("id", "id")), java.util.Map.entry("name", java.util.List.of("name", "name")), java.util.Map.entry("version", java.util.List.of("version", "version")));
    public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of(
        "scalar_type_probe_list");
    public static final FieldLayout __TEAQL_FIELD_LAYOUT =
        FieldLayout.installGenerated(FieldLayout.generated(
            TypePlatform.class, __TEAQL_FIELD_LAYOUT_REVISION,
            __TEAQL_FIXED_FIELD_INDEXES, __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES,
            JsonFieldNamingProfile.fromModelValue("camelCase")));


    public static final String NAME_PROPERTY = "name";
    public static final String SCALAR_TYPE_PROBE_LIST_PROPERTY = "scalar_type_probe_list";
    private String name;

    private SmartList<ScalarTypeProbe> scalarTypeProbeList;

    public String getName(){
        return this.name;
    }

    public SmartList<ScalarTypeProbe> getScalarTypeProbeList(){
        return this.scalarTypeProbeList;
    }
    public TypePlatform updateName(String name){
        name = (name == null ? null : name.trim());
        if(isPropertyLoaded(NAME_PROPERTY) && Objects.equals(this.name, name)){
            return this;
        }
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;
        return this;
    }

    public TypePlatform addScalarTypeProbe(ScalarTypeProbe scalarTypeProbe){
        if (scalarTypeProbe == null){
            return this;
        }

        if(null == this.scalarTypeProbeList){
            this.scalarTypeProbeList = new SmartList<>();
        }

        this.scalarTypeProbeList.add(scalarTypeProbe);
        scalarTypeProbe.updateTypePlatform(this);
        return this;
    }

    public static TypePlatform refer(Long id){
        TypePlatform refer = new TypePlatform();
        refer.__internalSet("id", id);
        refer.set$status(EntityStatus.REFER);
        return refer;
    }
    @Override
    public String typeName(){
        return INTERNAL_TYPE;
    }

    public TypePlatform comment(String comment){
        this.setComment(comment);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Audited<TypePlatform> auditAs(String action) {
        return new Audited<>(this, action);
    }

    // ===== Framework Internal: generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void __internalSet(String property, Object value) {
        markPropertyLoaded(property);
        switch (property) {
            case "name": this.name = (value == null ? null : ((String)value).trim()); break;

            case "scalar_type_probe_list": this.scalarTypeProbeList = (SmartList<ScalarTypeProbe>) value; break;
            default: super.__internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object __internalGet(String property) {
        switch (property) {
            case "name": return this.name;
            case "scalar_type_probe_list": return this.scalarTypeProbeList;
            default: return super.__internalGet(property);
        }
    }

}