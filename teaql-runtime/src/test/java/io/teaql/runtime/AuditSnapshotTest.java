package io.teaql.runtime;

import io.teaql.core.EntityKey;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuditSnapshotTest {
    @Test public void laterStructuredEditsCannotRewriteADeferredAuditBatch() {
        try (var runtime=TeaQLRuntime.builder().metadata(new io.teaql.core.meta.SimpleEntityMetaFactory()).telemetry(RuntimeTelemetry.NOOP).build()) {
            byte[] bytes={1,2}; List<Object> values=new ArrayList<>(Arrays.asList("original",null));
            Map<String,Object> input=new LinkedHashMap<>(); input.put("items",values); input.put("bytes",bytes);
            var snapshot=(Map<?,?>)runtime.snapshotAuditValue(input); values.set(0,"later"); bytes[0]=9; input.clear();
            assertEquals(Arrays.asList("original",null),snapshot.get("items")); assertArrayEquals(new byte[]{1,2},(byte[])snapshot.get("bytes"));
            assertThrows(UnsupportedOperationException.class,() -> snapshot.clear());
        }
    }

    @Test public void relationIdentityDoesNotRetainTheMutableBusinessEntity() {
        try (var runtime=TeaQLRuntime.builder().metadata(new io.teaql.core.meta.SimpleEntityMetaFactory()).telemetry(RuntimeTelemetry.NOOP).build()) {
            var asset=new BlobMutationIntegrationTest.Asset(); asset.__internalSet("id",42L); asset.__internalSet("version",1L);
            Object snapshot=runtime.snapshotAuditValue(asset); asset.__internalSet("id",99L);
            assertEquals(new EntityKey("Asset",42L),snapshot);
        }
    }

    @Test public void cyclicAndExcessivelyDeepValuesRejectBeforeAnyCommitDelivery() {
        try (var runtime=TeaQLRuntime.builder().metadata(new io.teaql.core.meta.SimpleEntityMetaFactory()).telemetry(RuntimeTelemetry.NOOP).build()) {
            List<Object> cycle=new ArrayList<>(); cycle.add(cycle); assertThrows(io.teaql.core.TeaQLRuntimeException.class,() -> runtime.snapshotAuditValue(cycle));
            Object value="leaf"; for (int i=0;i<40;i++) value=List.of(value); Object deep=value;
            assertThrows(io.teaql.core.TeaQLRuntimeException.class,() -> runtime.snapshotAuditValue(deep));
        }
    }
}
