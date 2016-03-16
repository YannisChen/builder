package com.tibco.devtools.workspace.installer.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Tests the functionality of the {@link FileUtils} class.
 */
public class TestFileUtils extends TempDirTestCase {

	public void testCreateDirectories() throws IOException {
		
		File dummyFile = new File(m_testDir, "dummy");
		FileOutputStream fos = new FileOutputStream(dummyFile);
		fos.write("Dummy".getBytes());
		fos.close();
		
		File shouldFail = new File(dummyFile, "subdir");
		try {
			FileUtils.createDirectories(shouldFail);
			fail("Was supposed to throw an exception");
		}
		catch (IOException e) {
			// supposed to get here.
		}
			
	}
	
	public void testSubversionDelete() throws IOException {
		
		extractZipToFolder("wc.zip");
		
		File toDelete = new File(m_testDir, "wc/somefile.txt");
		
		FileUtils.deleteViaFileSystemOrSubverion(toDelete);
		
		// make sure the file is no longer there.
		assertFalse(toDelete.isFile());
		
		// TODO - is there an easy way to tell if the file was deleted in the Subversion sense,
		// rather than just as a code-coverage question?
	}
}
