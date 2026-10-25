package io.teaql.core.sql;

import io.teaql.core.BaseEntity;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.SqlParameterLogPolicy;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class BlobSQLPropertyTest {
    private static class Slot extends BaseEntity {
        private BlobRef document;
        @Override public void __internalSet(String name, Object value) {
            if (name.equals("document")) document = (BlobRef) value;
            else super.__internalSet(name, value);
        }
        @Override public Object __internalGet(String name) {
            return name.equals("document") ? document : super.__internalGet(name);
        }
    }
    @Test public void sqlStoresTextAndHydratesOnlySelectedStableKeys() throws Exception {
        var property = new BlobSQLProperty();
        property.setName("document"); property.setTableName("asset"); property.setColumnName("document");
        property.setType(new io.teaql.core.meta.SimplePropertyType(BlobRef.class));
        property.setColumnType("VARCHAR(128)");
        var ref = BlobRef.of("blob_sql_fixture");
        Object raw = property.toDBRaw(null, null, ref).get(0).getValue();
        Assert.assertEquals(ref.key(), raw);
        Assert.assertNull(property.toDBRaw(null, null, null).get(0).getValue());
        Assert.assertThrows(IllegalArgumentException.class, () -> property.toDBRaw(null, null, "https://example.invalid/file"));
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:"); var stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE asset(document TEXT)");
            try (var insert = connection.prepareStatement("INSERT INTO asset(document) VALUES (?)")) {
                insert.setObject(1, raw); insert.executeUpdate();
            }
            Slot slot = new Slot();
            try (var rows = stmt.executeQuery("SELECT document, typeof(document) AS physical FROM asset")) {
                rows.next(); Assert.assertEquals("text", rows.getString("physical"));
                property.setPropertyValue(null, slot, rows); Assert.assertEquals(ref, slot.document);
            }
            try (var rows = stmt.executeQuery("SELECT 1 AS other")) {
                rows.next(); property.setPropertyValue(null, slot, rows); Assert.assertEquals(ref, slot.document);
            }
            try (var rows = stmt.executeQuery("SELECT 'https://example.invalid/file' AS document")) {
                rows.next(); Assert.assertThrows(IllegalArgumentException.class, () -> property.setPropertyValue(null, slot, rows));
            }
            try (var rows = stmt.executeQuery("SELECT NULL AS document")) {
                rows.next(); property.setPropertyValue(null, slot, rows); Assert.assertNull(slot.document);
            }
        }
    }
    @Test public void queryBindingsEncodeTypedKeysAndCollections() {
        var parameters = new HashMap<String, Object>();
        var ref = BlobRef.of("blob_query");
        SqlParameters.bind(parameters, "one", ref, SqlParameterLogPolicy.UNKNOWN);
        SqlParameters.bind(parameters, "many", List.of(ref), SqlParameterLogPolicy.UNKNOWN);
        Assert.assertEquals(ref.key(), parameters.get("one"));
        Assert.assertEquals(List.of(ref.key()), parameters.get("many"));
    }
}
