package io.teaql.data.dynamic;

import org.junit.Test;

import static org.junit.Assert.*;

public class InMemoryDynamicFieldsProviderTest {

    private InMemoryDynamicFieldsProvider createProvider() {
        InMemoryDynamicFieldsProvider provider = new InMemoryDynamicFieldsProvider();

        // Register a STRING field
        DynamicFieldDef stringDef = new DynamicFieldDef();
        stringDef.setScope(DynamicFieldScope.global());
        stringDef.setOwnerType("Platform");
        stringDef.setCode("customer_asset_no");
        stringDef.setName("Customer Asset No");
        stringDef.setDataType(DynamicDataType.STRING);
        stringDef.setStatus(DynamicFieldStatus.ACTIVE);
        provider.registerFieldDef(stringDef);

        // Register a NUMBER field
        DynamicFieldDef numberDef = new DynamicFieldDef();
        numberDef.setScope(DynamicFieldScope.global());
        numberDef.setOwnerType("Platform");
        numberDef.setCode("priority_score");
        numberDef.setName("Priority Score");
        numberDef.setDataType(DynamicDataType.NUMBER);
        numberDef.setStatus(DynamicFieldStatus.ACTIVE);
        provider.registerFieldDef(numberDef);

        // Register a BOOL field
        DynamicFieldDef boolDef = new DynamicFieldDef();
        boolDef.setScope(DynamicFieldScope.global());
        boolDef.setOwnerType("Platform");
        boolDef.setCode("enabled_for_custom_flow");
        boolDef.setName("Enabled for Custom Flow");
        boolDef.setDataType(DynamicDataType.BOOL);
        boolDef.setStatus(DynamicFieldStatus.ACTIVE);
        provider.registerFieldDef(boolDef);

        return provider;
    }

    private final java.util.concurrent.atomic.AtomicLong idGen = new java.util.concurrent.atomic.AtomicLong(100000);

    private DynamicFieldContext globalCtx() {
        return new DynamicFieldContext() {
            @Override public String scopeType() { return "GLOBAL"; }
            @Override public String scopeId() { return "default"; }
            @Override public String userId() { return "test-user"; }
            @Override public String purpose() { return "unit test"; }
            @Override public String comment() { return "testing dynamic fields"; }
            @Override public boolean strictIntent() { return false; }
            @Override public long nextId(String typeName) { return idGen.incrementAndGet(); }
        };
    }

    @Test
    public void testWriteAndReadStringValue() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        // Write
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "A-10086",
                "test", "set asset no"));

        // Read
        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));

        assertTrue(values.isSelected("customer_asset_no"));
        assertEquals("A-10086", values.getString("customer_asset_no"));
    }

    @Test
    public void testWriteAndReadNumberValue() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        provider.saveValue(context, DynamicSetCommand.of(
                owner, "priority_score", DynamicDataType.NUMBER, 80,
                "test", "set priority"));

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectNumber("priority_score"));

        assertEquals(80, values.getNumber("priority_score"));
    }

    @Test
    public void testWriteAndReadBoolValue() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        provider.saveValue(context, DynamicSetCommand.of(
                owner, "enabled_for_custom_flow", DynamicDataType.BOOL, true,
                "test", "enable custom flow"));

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectBool("enabled_for_custom_flow"));

        assertTrue(values.getBool("enabled_for_custom_flow"));
    }

    @Test
    public void testUnselectedFieldThrows() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));

        try {
            values.getString("nonexistent_field");
            fail("Expected DynamicFieldException");
        } catch (DynamicFieldException e) {
            assertEquals("DYNAMIC_FIELD_NOT_SELECTED", e.errorCode());
        }
    }

    @Test
    public void testSelectAllLoadsActiveFields() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        // Write some values
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "A-10086",
                "test", "set"));
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "priority_score", DynamicDataType.NUMBER, 42,
                "test", "set"));

        // Select all
        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectAll());

        assertEquals(2, values.size()); // two stored fields; the known absent field is synthesized as NotLoaded
        assertEquals("A-10086", values.getString("customer_asset_no"));
        assertEquals(42, values.getNumber("priority_score"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED, values.field("enabled_for_custom_flow").state());
    }

    @Test
    public void testNullValueIsDistinguishedFromUnselected() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        provider.saveValue(context, DynamicSetCommand.of(owner, "customer_asset_no", DynamicDataType.STRING,
                null, "test", "explicit null"));
        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));

        assertTrue("Field should be selected", values.isSelected("customer_asset_no"));
        assertTrue("Value should be null", values.isNull("customer_asset_no"));
    }

    @Test
    public void batchSharesDefinitionsButNotValuesAndKnownUnselectedFieldIsNotLoaded() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef first = DynamicOwnerRef.of("Platform", 1L);
        DynamicOwnerRef second = DynamicOwnerRef.of("Platform", 2L);
        provider.saveValue(context, DynamicSetCommand.of(first, "customer_asset_no", DynamicDataType.STRING, "private", "verify", "set first value"));
        provider.saveValue(context, DynamicSetCommand.of(second, "customer_asset_no", DynamicDataType.STRING, null, "verify", "set explicit null"));
        java.util.Map<DynamicOwnerRef, DynamicFieldValues> batch = provider.loadValues(context,
                java.util.List.of(first, second), new DynamicFieldSelection().selectString("customer_asset_no"));
        assertSame(batch.get(first).metadata(), batch.get(second).metadata());
        assertEquals(DynamicFieldValue.State.VALUE, batch.get(first).field("customer_asset_no").state());
        assertEquals(DynamicFieldValue.State.NULL, batch.get(second).field("customer_asset_no").state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, batch.get(first).field("priority_score").state());
        assertEquals("private", batch.get(first).getString("customer_asset_no"));
        assertNull(batch.get(second).getString("customer_asset_no"));
        assertEquals("DYNAMIC_FIELD_NOT_FOUND", assertThrows(DynamicFieldException.class,
                () -> batch.get(first).field("unknown")).errorCode());
    }

    @Test
    public void testFacadeEndToEnd() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DefaultDynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);

        // Write via facade
        facade.purpose("Update asset number")
                .comment("Setting customer asset")
                .owner("Platform", 1001L)
                .string("customer_asset_no")
                .set("A-10086");

        // Read via facade
        String value = facade.purpose("Read asset number")
                .comment("Loading customer asset")
                .owner("Platform", 1001L)
                .string("customer_asset_no")
                .get();

        assertEquals("A-10086", value);
    }

    @Test
    public void testFacadeTypeCheckEnforced() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DefaultDynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);

        // Try to read a STRING field as NUMBER
        try {
            facade.owner("Platform", 1001L)
                    .number("customer_asset_no") // this is a STRING field
                    .get();
            fail("Expected DynamicFieldException for type mismatch");
        } catch (DynamicFieldException e) {
            assertEquals("DYNAMIC_FIELD_TYPE_MISMATCH", e.errorCode());
        }
    }

    @Test
    public void testFieldNotFoundThrows() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DefaultDynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);

        try {
            facade.owner("Platform", 1001L)
                    .string("nonexistent_field")
                    .get();
            fail("Expected DynamicFieldException");
        } catch (DynamicFieldException e) {
            assertEquals("DYNAMIC_FIELD_NOT_FOUND", e.errorCode());
        }
    }

    @Test
    public void testListFieldDefs() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();

        java.util.List<DynamicFieldDef> defs = provider.listFieldDefs(context, "Platform");
        assertEquals(3, defs.size());
    }

    @Test
    public void testDeleteValue() {
        InMemoryDynamicFieldsProvider provider = createProvider();
        DynamicFieldContext context = globalCtx();
        DynamicOwnerRef owner = DynamicOwnerRef.of("Platform", 1001L);

        // Write
        provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.STRING, "A-10086",
                "test", "set"));

        // Verify written
        DynamicFieldValues values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertEquals("A-10086", values.getString("customer_asset_no"));

        // Find the field def to get its ID
        DynamicFieldDef def = provider.loadFieldDef(context,
                DynamicFieldRef.of(DynamicFieldScope.global(), "Platform", "customer_asset_no"));

        // Delete
        provider.deleteValue(context, DynamicValueRef.of(owner, def.getId()));

        // Deleted is absent, not a persisted explicit NULL.
        values = provider.loadValues(context, owner,
                new DynamicFieldSelection().selectString("customer_asset_no"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED, values.field("customer_asset_no").state());
        assertFalse(values.isNull("customer_asset_no"));
    }

    @Test
    public void incompatibleWriteCannotEraseAStoredValueOrTurnAbsenceIntoNull() {
        var provider = createProvider();
        var context = globalCtx();
        var owner = DynamicOwnerRef.of("Platform", 1001L);
        var selection = new DynamicFieldSelection().selectString("customer_asset_no");
        assertEquals(DynamicFieldValue.State.NOT_LOADED, provider.loadValues(context, owner, selection)
                .field("customer_asset_no").state());
        provider.saveValue(context, DynamicSetCommand.of(owner, "customer_asset_no", DynamicDataType.STRING,
                "retained", "test", "store original"));
        assertThrows(DynamicFieldException.class, () -> provider.saveValue(context, DynamicSetCommand.of(
                owner, "customer_asset_no", DynamicDataType.NUMBER, 0, "test", "reject wrong type")));
        assertEquals("retained", provider.loadValues(context, owner, selection).getString("customer_asset_no"));
        provider.saveValue(context, DynamicSetCommand.of(owner, "customer_asset_no", DynamicDataType.STRING,
                null, "test", "store explicit null"));
        assertEquals(DynamicFieldValue.State.NULL, provider.loadValues(context, owner, selection)
                .field("customer_asset_no").state());
    }
}
