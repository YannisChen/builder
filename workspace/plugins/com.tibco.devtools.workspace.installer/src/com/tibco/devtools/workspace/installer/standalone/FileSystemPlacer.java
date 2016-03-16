package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import com.tibco.devtools.workspace.installer.utils.FileUtils;

/**
 * Writes files relative to a file system location.
 */
public class FileSystemPlacer implements FilePlacer {

	public FileSystemPlacer(File baseDir) {
		m_baseDir = baseDir;
	}
	
	/**
	 * Place a file at the intended location.
	 */
	public void placeFile(String path, InputStream stream) {
		
		File newDest = new File(m_baseDir, path);
		File parent = newDest.getParentFile();
		
		try {
			FileUtils.createDirectories(parent);
			FileUtils.copyStreamToFile(stream, newDest);
		} catch (IOException e) {
            FileUtils.blindlyDelete(newDest);
			throw new RuntimeException("Error writing to file " + newDest.toString() + ". " + e, e);
		}
	}

	/**
	 * Scans the zip file, and creates a directory or writes a file as appropriate.
	 */
	public void placeExpandedZip(String path, InputStream zipStreamToExpand) throws IOException {
		ZipInputStream zis = new ZipInputStream(zipStreamToExpand);
		
		// create a temporary buffer to hold transfer data.
		
		File parentDir = new File(m_baseDir, path);
		FileUtils.extractZipStream(zis, parentDir);
		
	}

	private final File m_baseDir;

}
