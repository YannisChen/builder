package com.tibco.devtools.workspace.installer.standalone;

import java.util.Collections;
import java.util.List;

import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

public class TestRange extends TestCase {

    /**
     * Verifies that the {@link VersionConstraint#isMatchForTarget(VersionedTarget)
     * method performs correctly.
     */
    public void testIsMatch() {
        
        // verify matches on low and high.
        Range<String> inclusiveLowAndHigh = new Range<String>("B", true, "D", true);
        
        // check that inclusive match works on low, high, and middle.
        assertTrue(inclusiveLowAndHigh.isInRange("D"));
        assertTrue(inclusiveLowAndHigh.isInRange("B"));
        assertTrue(inclusiveLowAndHigh.isInRange("C"));
        
        assertFalse(inclusiveLowAndHigh.isInRange("A"));
        assertFalse(inclusiveLowAndHigh.isInRange("E"));

        assertTrue(inclusiveLowAndHigh.match("B") == 0);
        assertTrue(inclusiveLowAndHigh.match("D") == 0);
        assertTrue(inclusiveLowAndHigh.match("A") < 0);
        assertTrue(inclusiveLowAndHigh.match("E") > 0);
        
        Range<String> exclusiveLowAndHigh = new Range<String>("B", false, "D", false);
        
        assertFalse(exclusiveLowAndHigh.isInRange("B") );
        assertFalse(exclusiveLowAndHigh.isInRange("D") );
        
    }
    
    public void testToString() {
    	assertEquals("[A,C)", new Range<String>("A", true, "C", false).toString() );
    	assertEquals("(A,D]", new Range<String>("A", false, "D", true).toString() );
    }
    /**
     * Verifies that the method {@link Range#computeIntersection(List)
     * returns appropriate values.
     */
    public void testIntersection() {

    	// verify that an intersection gets computed properly.
    	Range<String> range1 = new Range<String>("B", true, "F", false);
    	Range<String> range2 = new Range<String>("C", true, "H", false);
    	
    	List< Range<String> > list1 = DataUtils.newList();
    	list1.add(range1);
    	list1.add(range2);
    	
    	Range<String> result1 = Range.computeIntersection(list1);
    	assertEquals("C", result1.getMinimumRange() );
    	assertEquals("F", result1.getMaximumRange() );
    	assertTrue( result1.isMinimumInclusive() );
    	assertFalse( result1.isMaximumInclusive() );
        
        // verify that an empty list gets a null
        List< Range<String> > list2 = Collections.emptyList();
        Range<String> result2 = Range.computeIntersection( list2 );
        assertNull(result2);
        
        // now, try two non-intersecting ranges.
        Range<String> range3 = new Range<String>("A", true, "D", false);
        Range<String> range4 = new Range<String>("E", true, "H", false);
        
    	List< Range<String> > list3 = DataUtils.newList();
    	list3.add(range3);
    	list3.add(range4);
    	
    	Range<String> result3 = Range.computeIntersection(list3);
    	assertNull(result3);
    	
    }
}
