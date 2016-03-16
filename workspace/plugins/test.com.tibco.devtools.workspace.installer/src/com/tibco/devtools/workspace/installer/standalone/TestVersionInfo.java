package com.tibco.devtools.workspace.installer.standalone;

import com.tibco.devtools.workspace.model.VersionInfo;

import junit.framework.TestCase;

public class TestVersionInfo extends TestCase {

	public void testVersionInfo() {

		VersionInfo vi = VersionInfo.parseVersion("3");
		assertEquals(3, vi.getMajorVersion());
		
		vi = VersionInfo.parseVersion("3.2");
		assertEquals(3, vi.getMajorVersion());
		assertEquals(2, vi.getMinorVersion());

		vi = VersionInfo.parseVersion("3.2.1");
		assertEquals(3, vi.getMajorVersion() );
		assertEquals(2, vi.getMinorVersion() );
		assertEquals(1, vi.getPatchVersion() );
		
		vi = VersionInfo.parseVersion("3.2.1.foobar");
		assertEquals(3, vi.getMajorVersion() );
		assertEquals(2, vi.getMinorVersion() );
		assertEquals(1, vi.getPatchVersion() );
		assertEquals("foobar", vi.getQualifier() );
		
	}

	public void testCompareHashAndEquals() {
		
		VersionInfo v1 = new VersionInfo(3, 2, 1, "");
		VersionInfo v2 = new VersionInfo(3, 2, 0, "");
		VersionInfo v3 = new VersionInfo(3, 1, 1, "");
		VersionInfo v4 = new VersionInfo(2, 2, 1, "");
		VersionInfo v5 = new VersionInfo(3, 2, 1, "");
		
		assertTrue(v1.compareTo(v2) > 0 );
		assertTrue(v2.compareTo(v1) < 0 );
		
		assertTrue(v1.compareTo(v5) == 0);
		assertTrue( v1.equals(v5) );
		
		assertTrue(v1.compareTo(v3) > 0);
		assertTrue(v1.compareTo(v4) > 0);
		
		// verify that the hash code for equivalent items is equivalent...
		assertEquals(v1.hashCode(), v5.hashCode() );
	}
}
