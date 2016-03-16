package com.tibco.devtools.workspace.installer.standalone;

import java.io.IOException;
import java.io.InputStream;

/**
 * Defines an abstraction for placing files in a target location, so that
 * unit testing need not actually write files to disk, and can just verify
 * that the required files are being attempted.
 * 
 */
public interface FilePlacer {

	/**
	 * Create a file with the content given by <code>stream</code> at the
	 * location given by <code>path</code>
	 * 
	 * @param path	The relative location to store the file.
	 * @param stream	The desired contents of the file.
	 */
	void placeFile(String path, InputStream stream) throws IOException;
	
	/**
	 * Expand a ZIP file with the given base path relative to the file 
	 * @throws IOException If something goes awry while reading from the given stream,
	 * or writing to the destination.
	 */
	void placeExpandedZip(String path, InputStream zipStreamToExpand) throws IOException;
}
