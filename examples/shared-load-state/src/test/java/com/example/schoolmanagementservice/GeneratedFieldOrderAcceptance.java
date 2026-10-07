package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import io.teaql.core.LoadState;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Q/E behavior plus trusted test-only driver ordinals, not an application SQL recipe. */
final class GeneratedFieldOrderAcceptance {
    static void verify(UserContext context, DataSource source, long id) throws Exception {
        var left = Q.schoolsWithMinimalFields().withIdIs(id).selectName().selectAddress().limit(1)
                .comment("what: select name then address").purpose("why: verify Q selection order cannot change fixed slots")
                .executeForOne(context);
        var right = Q.schoolsWithMinimalFields().withIdIs(id).selectAddress().selectName().limit(1)
                .comment("what: select address then name").purpose("why: compare reversed Q selection order")
                .executeForOne(context);
        assertEquals(E.school(left).getName().eval(), E.school(right).getName().eval());
        assertEquals(E.school(left).getAddress().eval(), E.school(right).getAddress().eval());
        assertEquals(left.__internalLoadState(), right.__internalLoadState());
        var selected = new SmartList<School>(List.of(left,right));
        assertSame(selected.get(0).__internalLoadState(), selected.get(1).__internalLoadState());
        assertFalse(left.isPropertyLoaded("active"));
        assertFalse(left.__internalHasMutationLedger());assertFalse(right.__internalHasMutationLedger());

        List<School> permuted = new ArrayList<>();
        // Read only rows seeded through the generated audited mutation API.
        try (var connection = source.getConnection()) {
            for (String columns : List.of("name,address,id,version", "address,version,name,id")) {
                try (var statement = connection.prepareStatement("SELECT " + columns + " FROM school_data WHERE id = ? LIMIT 1")) {
                    statement.setLong(1,id);
                    try (var rows = statement.executeQuery()) {
                        assertTrue(rows.next());
                        var labels = new ArrayList<String>();
                        for (int ordinal = 1; ordinal <= rows.getMetaData().getColumnCount(); ordinal++)
                            labels.add(rows.getMetaData().getColumnLabel(ordinal));
                        assertEquals(Arrays.asList(columns.split(",")), labels);
                        School entity = new School();
                        entity.__internalUseLoadState(LoadState.projection(School.__TEAQL_FIELD_LAYOUT, labels));
                        for (int ordinal = 1; ordinal <= labels.size(); ordinal++) {
                            String canonical = labels.get(ordinal-1);
                            Object value;
                            if (canonical.equals("id") || canonical.equals("version")) {
                                value = rows.getLong(ordinal);assertFalse(rows.wasNull());
                            } else value = rows.getString(ordinal);
                            String member = School.__TEAQL_FIXED_FIELD_MAPPINGS.get(canonical).get(0);
                            entity.__internalHydrate(member, value, School.__TEAQL_FIXED_FIELD_INDEXES.get(canonical));
                        }
                        assertFalse(rows.next());
                        assertSame(School.__TEAQL_FIELD_LAYOUT, entity.__internalLoadState().layout());
                        for (var field : School.__TEAQL_FIXED_FIELD_INDEXES.entrySet())
                            assertEquals(field.getValue(), entity.__internalLoadState().layout().findIndex(field.getKey()));
                        assertEquals(left.getId(), entity.getId());assertEquals(left.getVersion(), entity.getVersion());
                        assertEquals(E.school(left).getName().eval(), E.school(entity).getName().eval());
                        assertEquals(E.school(left).getAddress().eval(), E.school(entity).getAddress().eval());
                        assertFalse(entity.isPropertyLoaded("active"));
                        assertFalse(entity.__internalHasMutationLedger());assertTrue(entity.getUpdatedProperties().isEmpty());
                        permuted.add(entity);
                    }
                }
            }
        }
        var joined = new SmartList<School>(permuted);
        assertSame(joined.get(0).__internalLoadState(), joined.get(1).__internalLoadState());
        System.out.println("PASS generated Java Q selection and real JDBC column-order invariance");
    }
}
