/**
 * 
 */
package com.tibco.devtools.workspace.installer.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.tibco.devtools.workspace.util.DataUtils.newList;

class FilePlacerRecorder implements FilePlacer {

	List<String> m_zipPaths = newList();
	List<String> m_filePaths = newList();
	public void placeExpandedZip(String path, InputStream zipStreamToExpand) throws IOException {
		if (zipStreamToExpand == null) {
			throw new IllegalStateException("Unexpected null stream.");
		}
		zipStreamToExpand.close();
		m_zipPaths.add(path);
	}

	public void placeFile(String path, InputStream stream) throws IOException {
		if (stream == null) {
			throw new IllegalStateException("Unexpected null stream.");
		}
		stream.close();
		m_filePaths.add(path);
	}
	
	public List<String> getZipPaths() {
		return m_zipPaths;
	}
	
	public List<String> getFilePaths() {
		return m_filePaths;
	}
}