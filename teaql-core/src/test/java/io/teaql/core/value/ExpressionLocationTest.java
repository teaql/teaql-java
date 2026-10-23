package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.checker.WireLocationSegment;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExpressionLocationTest {
    static class Row extends BaseEntity {
        Row child;
        SmartList<Row> children = new SmartList<>();
        String text;
        Row child(Row value) { child = value; markPropertyLoaded("school_type"); return this; }
    }

    private Expression<Row, String> childText(Row row, JsonFieldNamingProfile profile) {
        Expression<Row, Row> root = new ValueExpression<>(row, profile);
        var child = new LoadedPropertyExpression<>(root, "school_type", r -> r.child);
        return new LoadedPropertyExpression<>(child, "user_url", r -> r.text);
    }

    @Test public void canonicalIdentityNativeDisplayAndWirePointerAreSeparate() {
        Row root = new Row().child(new Row());
        var error = assertThrows(TeaQLNotLoadedException.class,
                () -> childText(root, JsonFieldNamingProfile.CAMEL_CASE).eval());
        assertEquals("school_type.user_url", error.getModelPath());
        assertEquals("schoolType.userUrl", error.getAccessPath());
        assertEquals("/schoolType/userUrl", error.getInstancePath());
        assertEquals(java.util.List.of(WireLocationSegment.property("school_type"),
                WireLocationSegment.property("user_url")), error.toWire().location());
        assertEquals("NOT_LOADED", error.toWire().code());
    }

    @Test public void selectedWireProfileDoesNotChangeModelOrJavaNames() {
        for (var profile : JsonFieldNamingProfile.values()) {
            var expression = childText(new Row().child(new Row()), profile);
            var error = assertThrows(TeaQLNotLoadedException.class, expression::eval);
            assertEquals("school_type.user_url", error.getModelPath());
            assertEquals("schoolType.userUrl", error.getAccessPath());
            assertEquals(ObjectLocation.hashRoot("school_type").member("user_url").instancePath(profile),
                    error.getInstancePath());
        }
    }

    @Test public void listElementAndFirstPreserveIndexRatherThanPretendingItIsAField() {
        Row root = new Row(); root.markPropertyLoaded("order_items"); root.children.add(new Row());
        var list = new SmartListExpression<>(new LoadedPropertyExpression<>(new ValueExpression<>(root),
                "order_items", r -> r.children));
        for (var item : java.util.List.of(list.first(), list.get(0))) {
            var text = new LoadedPropertyExpression<>(item, "user_url", r -> r.text);
            var error = assertThrows(TeaQLNotLoadedException.class, text::eval);
            assertEquals("order_items[0].user_url", error.getModelPath());
            assertEquals("/orderItems/0/userUrl", error.getInstancePath());
            assertEquals("index", error.toWire().location().get(1).kind());
        }
        assertNull(new LoadedPropertyExpression<>(list.get(1), "user_url", r -> r.text).eval());
    }

    @Test public void pointerEscapingDoesNotSplitLiteralMemberNames() {
        var expression = new LoadedPropertyExpression<>(new ValueExpression<>(new Row()), "a~/b.c", r -> r.text);
        var error = assertThrows(TeaQLNotLoadedException.class, expression::eval);
        assertEquals("/a~0~1b.c", error.getInstancePath());
        assertEquals(1, error.getLocation().segments().size());
    }

    @Test public void loadedNullEmptyAndNotLoadedStayDifferentAndDoNotCreateLedgers() {
        Row root = new Row().child(new Row());
        var expression = childText(root, JsonFieldNamingProfile.CAMEL_CASE);
        assertThrows(TeaQLNotLoadedException.class, () -> expression.orIfNull("fallback"));
        assertFalse(root.__internalHasMutationLedger());
        root.child.markPropertyLoaded("user_url");
        assertEquals("fallback", expression.orIfNull("fallback"));
        root.child.text = "";
        assertEquals("", expression.orIfNull("fallback"));
        assertFalse(root.child.__internalHasMutationLedger());
    }

    @Test public void wireJsonIsExplicitAndDoesNotExposeTheInternalRoot() throws Exception {
        var expression = childText(new Row().child(new Row()), JsonFieldNamingProfile.SNAKE_CASE);
        var error = assertThrows(TeaQLNotLoadedException.class, expression::eval);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var wire = mapper.readTree(mapper.writeValueAsBytes(error.toWire()));
        assertEquals("/school_type/user_url", wire.get("instancePath").asText());
        assertEquals("school_type", wire.get("location").get(0).get("name").asText());
        assertFalse(wire.has("root"));
        assertFalse(wire.has("inputValue"));
        assertFalse(wire.has("accessPath"));
        assertFalse(wire.get("location").get(0).has("index"));
    }

    @Test public void earlyMissingRelationRetainsTheFullExpressionAndOnePositionalBreakpoint() {
        Row row = new Row();
        Expression<Row, Row> root = new ValueExpression<>(row, JsonFieldNamingProfile.CAMEL_CASE, "E.row(<root>)");
        var parent = new LoadedPropertyExpression<>(root, "parent", r -> r.child);
        var repeated = new LoadedPropertyExpression<>(parent, "parent", r -> r.child);
        var leaf = new LoadedPropertyExpression<>(repeated, "user_url", r -> r.text);
        var error = assertThrows(TeaQLNotLoadedException.class, leaf::eval);
        var wire = error.toWire();
        assertEquals("/parent", wire.instancePath());
        assertEquals("/parent/parent/userUrl", wire.attemptedInstancePath());
        assertEquals("E.row(<root>).getParent().getParent().getUserUrl().eval()", wire.originalExpression());
        assertEquals("E.row(<root>).getParent()<broken>.getParent().getUserUrl().eval()", wire.expressionWithBrokenPoint());
        assertEquals(0, wire.breakPointIndex());
        assertEquals(wire.location(), wire.suggestedFix().location());
        assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void listFirstSizeAndFallbackOperationsAreReconstructedWithoutArgumentValues() {
        Row row = new Row();
        var root = new ValueExpression<>(row, JsonFieldNamingProfile.SNAKE_CASE, "E.row(<root>)");
        var list = new SmartListExpression<>(new LoadedPropertyExpression<>(root, "order_items", r -> r.children));
        var first = new LoadedPropertyExpression<>(list.first(), "user_url", r -> r.text);
        var missing = assertThrows(TeaQLNotLoadedException.class, () -> first.orIfNull("PRIVATE-FALLBACK"));
        assertEquals("E.row(<root>).getOrderItems()<broken>.first().getUserUrl().orIfNull(<fallback>)", missing.getExpressionWithBrokenPoint());
        assertEquals("/order_items/0/user_url", missing.toWire().attemptedInstancePath());
        assertFalse(missing.getMessage().contains("PRIVATE-FALLBACK"));
        var size = assertThrows(TeaQLNotLoadedException.class, () -> list.size().eval());
        assertEquals("E.row(<root>).getOrderItems()<broken>.size().eval()", size.getExpressionWithBrokenPoint());
        assertEquals("/order_items", size.toWire().attemptedInstancePath());
        var indexed = new LoadedPropertyExpression<>(list.get(12), "user_url", r -> r.text);
        assertEquals("/order_items/12/user_url", assertThrows(TeaQLNotLoadedException.class, indexed::eval).toWire().attemptedInstancePath());
    }

    @Test public void explicitRootEvaluationRetainsTheTerminalWithoutExportingTheArgument() {
        Row row = new Row();
        var root = new ValueExpression<>(row, JsonFieldNamingProfile.CAMEL_CASE, "E.row(<root>)");
        var child = new LoadedPropertyExpression<>(root, "school_type", r -> r.child);
        var name = new LoadedPropertyExpression<>(child, "user_url", r -> r.text);
        var error = assertThrows(TeaQLNotLoadedException.class, () -> name.eval(row));
        assertEquals("E.row(<root>).getSchoolType()<broken>.getUserUrl().eval(<root>)", error.toWire().expressionWithBrokenPoint());
        assertEquals("/schoolType", error.getInstancePath());
        assertEquals("/schoolType/userUrl", error.toWire().attemptedInstancePath());
    }
}
