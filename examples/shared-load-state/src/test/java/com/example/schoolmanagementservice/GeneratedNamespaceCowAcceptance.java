package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.UserContext;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import static org.junit.jupiter.api.Assertions.*;

final class GeneratedNamespaceCowAcceptance {
    static School verify(UserContext context, Long id, String name, String sibling, String note,
            JdbcDynamicFieldsProvider provider, DynamicFieldContext definitionContext, ObjectMapper mapper) throws Exception {
        var def=new DynamicFieldDef();def.setScope(DynamicFieldScope.global());
        def.setOwnerType("School");def.setCode("name");def.setName("name");def.setDataType(DynamicDataType.STRING);
        var existing=provider.listFieldDefs(definitionContext,"School").stream()
                .filter(field -> "name".equals(field.getCode())).findFirst();
        if(existing.isEmpty()) provider.registerFieldDef(definitionContext,def);
        else assertEquals(DynamicDataType.STRING,existing.get().getDataType());
        var request=Q.schools().withNameIn(name,sibling).selectSelfFields();
        request.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(note));
        var rows=request.orderByIdAscending().limit(2).comment("what: load same-shape namespace controls")
                .purpose("why: separate fixed derived and persistent same-name members").executeForList(context);
        assertEquals(2,rows.size());
        var shared=rows.get(0).__internalLoadState();assertSame(shared,rows.get(1).__internalLoadState());
        var first=rows.get(0);assertEquals(id,first.getId());
        assertEquals(DynamicFieldValue.State.NOT_LOADED,first.dynamicFields().field("name").state());
        assertEquals(DynamicFieldValue.State.VALUE,first.dynamicFields().field(note).state());
        Object siblingNote=rows.get(1).dynamicFields().field(note).value();
        first.updateDynamicField(note,"value-only private control");
        assertSame(shared,first.__internalLoadState());
        assertSame(shared,rows.get(1).__internalLoadState());
        assertEquals("value-only private control",first.dynamicFields().field(note).value());
        assertEquals(siblingNote,rows.get(1).dynamicFields().field(note).value());
        assertTrue(rows.get(1).getUpdatedProperties().isEmpty());
        assertTrue(rows.get(1).__internalDynamicMutations().isEmpty());
        System.out.println("PASS generated Java value-only dynamic mutation retains shared snapshot and sibling payload");
        first.addDynamicProperty("name","readonly same name");
        first.updateDynamicField("name","persistent same name");
        assertNotSame(shared,first.__internalLoadState());assertSame(shared,rows.get(1).__internalLoadState());
        assertFalse(shared.isLoaded("#name"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED,rows.get(1).dynamicFields().field("name").state());
        assertTrue(rows.get(1).getUpdatedProperties().isEmpty());
        assertFalse(first.getUpdatedProperties().contains("name"));
        var json=mapper.readTree(mapper.writeValueAsString(first));
        assertEquals(name,json.get("name").asText());
        assertEquals("readonly same name",json.get("_name").asText());
        assertEquals("persistent same name",json.get("#name").asText());
        first.auditAs("persist only the namespaced extension").save(context);
        var reread=Q.schools().withIdIs(id).selectSelfFields();
        reread.selectDynamicFieldsWith(new DynamicFieldSelection().selectString("name"));
        var read=reread.limit(1).comment("what: read back the namespace controls")
                .purpose("why: prove readonly properties never persist").executeForOne(context);
        assertEquals(name,read.getName());assertNull(read.getProperty("_name"));
        assertEquals("persistent same name",read.dynamicFields().field("name").value());
        var before=read.__internalLoadState();read.deleteDynamicField("name");
        assertNotSame(before,read.__internalLoadState());assertTrue(before.isLoaded("#name"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED,read.dynamicFields().field("name").state());
        read.auditAs("delete only the namespaced extension control").save(context);
        System.out.println("PASS generated Java LF19 fixed derived and persistent same-name namespace isolation");
        System.out.println("PASS generated Java LF23 dynamic availability detaches only one view");
        return read;
    }
}
