package io.teaql.core.sql.portable;

import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlTemporalPrecisionTest {
    private static final LocalDateTime NANO=LocalDateTime.of(1960,2,29,23,59,59,999999999);
    private static final LocalDateTime MILLI=NANO.withNano(999000000);

    @Test public void flatCollectionsAndTypedArraysAreCopiedOnlyWhenNeeded() {
        var list=Arrays.asList(NANO,null,MILLI);
        LocalDateTime[] array={NANO,null,MILLI};
        Object[] input={list,array};
        Object[] actual=SqlTemporalPrecision.parameters(input);
        assertNotSame(input,actual);
        assertEquals(Arrays.asList(MILLI,null,MILLI),actual[0]);
        assertArrayEquals(new LocalDateTime[]{MILLI,null,MILLI},(LocalDateTime[])actual[1]);
        assertSame(NANO,list.get(0));assertSame(NANO,array[0]);
    }
    @Test public void alignedBindingsAndBatchesReuseTheirExistingContainers() {
        Object[] row={MILLI,null,"unchanged",12L};var batch=Collections.singletonList(row);
        assertSame(row,SqlTemporalPrecision.parameters(row));assertSame(batch,SqlTemporalPrecision.batch(batch));
        assertNull(SqlTemporalPrecision.parameters(null));assertNull(SqlTemporalPrecision.batch(null));
    }
    @Test public void batchNormalizationDoesNotRewriteAnyCallerOwnedRow() {
        Object[] changed={NANO},aligned={MILLI};var batch=List.of(changed,aligned);
        var actual=SqlTemporalPrecision.batch(batch);
        assertNotSame(batch,actual);assertNotSame(changed,actual.get(0));assertSame(aligned,actual.get(1));
        assertSame(NANO,changed[0]);assertEquals(MILLI,actual.get(0)[0]);
    }
    @Test public void nativeTimeInstantAndJsonObjectsAreNotPrecisionConverted() {
        var map=Map.of("datetime",NANO);var time=NANO.toLocalTime();var instant=Instant.parse("1960-02-29T23:59:59.999999999Z");
        Object[] input={map,time,instant,NANO.toLocalDate()};
        assertSame(input,SqlTemporalPrecision.parameters(input));assertEquals(NANO,map.get("datetime"));
        assertEquals(999999999,time.getNano());assertEquals(999999999,instant.getNano());
    }
    @Test public void cyclicAndNestedListsAreNotTraversedAsObjectGraphs() {
        var list=new ArrayList<Object>();list.add(list);list.add(Map.of("datetime",NANO));
        Object[] input={list};assertSame(input,SqlTemporalPrecision.parameters(input));
    }
    @Test public void wallTimeFormattingDoesNotNormalizeDstGapsOrConsultTheDefaultZone() {
        var original=TimeZone.getDefault();var gap=LocalDateTime.of(2024,3,10,2,30,0,123000000);
        try { for(var zone:List.of("UTC","America/New_York","Asia/Shanghai")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            assertEquals("2024-03-10 02:30:00.123",io.teaql.core.utils.LocalDateTimeUtil.formatSql(gap));
            assertEquals("1960-02-29 23:59:59.999999999",io.teaql.core.utils.LocalDateTimeUtil.formatSql(NANO));
            assertEquals("2026-01-01 00:00:00.0",io.teaql.core.utils.LocalDateTimeUtil.formatSql(LocalDateTime.of(2026,1,1,0,0)));
        }} finally { TimeZone.setDefault(original); }
    }
}
