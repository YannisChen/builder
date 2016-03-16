package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.tibco.devtools.workspace.installer.utils.FileUtils;

import junit.framework.TestCase;

public class TestFileSystemPlacer extends TestCase {

	public File m_testDir;
	
	@Override
	protected void setUp() throws Exception {
		m_testDir = new File("TempTestDir");
		m_testDir.mkdir();
	}

	@Override
	protected void tearDown() throws Exception {
		FileUtils.deleteFolder(m_testDir);
	}

	public void testPlacerZip() throws IOException {
		
		FilePlacer placer = new FileSystemPlacer(m_testDir);
		
		InputStream inStream = this.getClass().getResourceAsStream("ziptest.zip");
		
		placer.placeExpandedZip("mypath", inStream);
		File myPath = new File(m_testDir, "mypath");
		File zipContentsRoot = new File(myPath, "ziptest");
		File dFile = new File(zipContentsRoot, "foo/d.txt");
		
		assertTrue( myPath.isDirectory() );
		assertTrue( zipContentsRoot.isDirectory() );
		assertTrue( dFile.isFile() );
	}
	
	public void testPlacerFile() throws IOException {
		FilePlacer placer = new FileSystemPlacer(m_testDir);
		
		InputStream inStream = this.getClass().getResourceAsStream("ziptest.zip");
		
		placer.placeFile("Some/Path/Name/zipfile.zip", inStream);
		
		File target = new File(m_testDir, "Some/Path/Name/zipfile.zip");
		assertTrue( target.isFile() );
	}
}
