package io.teaql.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlicePopulationTest {
    @Test public void copiedDefaultIsNotAnExplicitNestedTopN() throws Exception {
        var defaults = new Slice();
        assertEquals(1000, defaults.getSize());
        assertFalse(defaults.hasExplicitPage()); assertFalse(defaults.copy().hasExplicitPage());
        var explicit = defaults.copy(); explicit.setSize(1000);
        assertTrue(explicit.hasExplicitPage()); assertTrue(explicit.copy().hasExplicitPage());
        assertFalse(defaults.hasExplicitPage());
        assertFalse(new ObjectMapper().writeValueAsString(explicit).contains("explicit"));
    }

    @Test public void offsetAndZeroAreExplicitBusinessSelections() {
        var offset = new Slice(); offset.setOffset(5); assertTrue(offset.copy().hasExplicitPage());
        assertEquals(5, offset.copy().getOffset());
        var zero = new Slice(); zero.setSize(0); assertTrue(zero.copy().hasExplicitPage());
        assertEquals(0, zero.copy().getSize());
    }
}
