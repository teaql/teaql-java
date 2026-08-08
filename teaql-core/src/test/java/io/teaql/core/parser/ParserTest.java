package io.teaql.core.parser;

import org.junit.Test;
import static org.junit.Assert.*;

public class ParserTest {

    @Test
    public void testSplit() {
        // Basic split
        String[] parts = Parser.split("a,b,c", ',');
        assertArrayEquals(new String[]{"a", "b", "c"}, parts);
        
        // Escape separator
        parts = Parser.split("a\\,b,c", ',');
        assertArrayEquals(new String[]{"a,b", "c"}, parts);
        
        // Escape escape char
        parts = Parser.split("a\\\\b,c", ',');
        assertArrayEquals(new String[]{"a\\b", "c"}, parts);
        
        // Empty parts skipped
        parts = Parser.split("a,,c", ',');
        assertArrayEquals(new String[]{"a", "c"}, parts);
        
        // Separator at end
        parts = Parser.split("a,b,", ',');
        assertArrayEquals(new String[]{"a", "b"}, parts);
        
        // Multi separators
        parts = Parser.split("a,b;c", ',', ';');
        assertArrayEquals(new String[]{"a", "b", "c"}, parts);
    }
    
    @Test
    public void testSplitToPair() {
        // Basic split to pair
        Parser.StringPair pair = Parser.splitToPair("key=value", '=');
        assertEquals("key", pair.pre());
        assertEquals("value", pair.post());
        
        // Escape separator
        pair = Parser.splitToPair("key\\=part=value", '=');
        assertEquals("key=part", pair.pre());
        assertEquals("value", pair.post());
        
        // Escape escape char
        pair = Parser.splitToPair("key\\\\=value", '=');
        assertEquals("key\\", pair.pre());
        assertEquals("value", pair.post());
        
        // No separator found
        pair = Parser.splitToPair("keyvalue", '=');
        assertEquals("keyvalue", pair.pre());
        assertEquals("", pair.post());
        
        // Multiple separators provided
        pair = Parser.splitToPair("key:val1=val2", '=', ':');
        assertEquals("key", pair.pre());
        assertEquals("val1=val2", pair.post());
    }
}
