package com.tibco.devtools.workspace.installer.standalone;

import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

import junit.framework.TestCase;

/**
 * Tests the substantive parts of the {@link Target<V>} class.
 */
public class TestVersionedTarget extends TestCase {

	public void testComparable() {
		
		VersionInfo vi1 = VersionInfo.parseVersion("1.2.3");
		VersionInfo vi2 = VersionInfo.parseVersion("3.2.1");
		
		Target<VersionInfo> vt1 = new Target<VersionInfo>("foo", vi1);
		Target<VersionInfo> vt2 = new Target<VersionInfo>("foo", vi2);

		assertTrue(vt1.compareTo(vt2) < 0);
		
		assertTrue(vt2.compareTo(vt1) > 0);
		
		assertFalse(vt1.equals(vt2) );
	}

	public void testToString() {
		
		VersionInfo vi1 = VersionInfo.parseVersion("1.2.3");
		Target<VersionInfo> vt1 = new Target<VersionInfo>("foo", vi1);
		
		assertEquals("foo_1.2.3", vt1.toString() );
	}
	
	/**
	 * Utility function to turn a string consisting of an identifier and a version number
	 * into a VersionedTarget.
	 * 
	 * @param line	String to parse.
	 * @return
	 */
	public static Target<VersionInfo> parseTarget(String line) {
		String[] parts = line.split("\\s");
		VersionInfo vers = VersionInfo.parseVersion(parts[1]);
		Target<VersionInfo> target = new Target<VersionInfo>(parts[0], vers);
		return target;
	}
}
