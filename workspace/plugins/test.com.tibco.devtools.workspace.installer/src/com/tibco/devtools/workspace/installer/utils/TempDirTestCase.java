package com.tibco.devtools.workspace.installer.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

public abstract class TempDirTestCase extends TestCase {

	protected File m_testDir;

	public TempDirTestCase() {
		super();
	}

	public TempDirTestCase(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		m_testDir = new File("__tests__");
		m_testDir = m_testDir.getAbsoluteFile();
		
		// make sure we've cleaned up any previously aborted runs.
		if (m_testDir.isDirectory()) {
			FileUtils.deleteFolder(m_testDir);
		}
		if (!m_testDir.mkdir())
			throw new IllegalStateException("Unable to create tests directory");
		
	}

	@Override
	protected void tearDown() throws Exception {
		FileUtils.deleteFolder(m_testDir);
	}

	protected void extractZipToFolder(String name) throws IOException,
			FileNotFoundException {
				InputStream tempWC = getClass().getResourceAsStream(name);
				ZipInputStream zis = new ZipInputStream(tempWC);
				
				FileUtils.extractZipStream(zis, m_testDir);
			}

}