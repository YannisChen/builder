package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;

import com.tibco.devtools.workspace.installer.utils.TempDirTestCase;

public class TestRemoveUnusedItemsFromSite extends TempDirTestCase {

	public void testRemoveUnused() throws IOException {
		
		extractZipToFolder("brokensite.zip");
		
		File toCleanup = new File(m_testDir, "brokensite");
		RemoveUnusedItemsFromSite.removeUnusedBundles(toCleanup);
		
		File shouldBeGone1 = new File(m_testDir, "brokensite/features/com.example.test.feature.a_1.0.0.002.jar");
		File shouldBeGone2 = new File(m_testDir, "brokensite/plugins/com.example.test.bundle.a2_1.0.0.002.jar");
		File shouldBeGone3 = new File(m_testDir, "brokensite/plugins/com.example.test.bundle.unused_1.0.0.001.jar");
		
		assertFalse(shouldBeGone1.exists());
		assertFalse(shouldBeGone2.exists());
		assertFalse(shouldBeGone3.exists());

		File shouldBeThere1 = new File(m_testDir, "brokensite/plugins/com.example.test.bundle.a1_1.0.0.001.jar");
		File shouldBeThere2 = new File(m_testDir, "brokensite/features/com.example.test.feature.a_1.0.0.001.jar");
		assertTrue(shouldBeThere1.exists());
		assertTrue(shouldBeThere2.exists());
	}
}
