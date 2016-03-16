package com.tibco.devtools.workspace.installer.standalone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

/**
 * Verifies that the UrlCache class works as designed.
 */
public class TestUrlCache extends TestCase {

	@Override
	protected void setUp() {
		
		try {
			// may seem weird to get a temporary file and delete it, but
			// I'm interested in a path to a temporary *folder*
			m_cacheDir = File.createTempFile("wi_cache_", ".tmp");
			m_cacheDir.delete();
			m_msh = new MemoryStreamHandler();
			m_reporter = new EmptyProgressReport();
			filter = new UrlCache.SaveFilter<InputStream>() {
				public InputStream preAction(InputStream stream,
						CachingState mode, boolean isExistent)
						throws IOException {
					//do nothing, just return
					return stream;
				}
			};
		} catch (IOException e) {
			throw new RuntimeException("Unable to create file for temporary folder.");
		}
		
	}
	
	@Override
	protected void tearDown() {
		FileUtils.deleteFolder(m_cacheDir);
	}

	public void testLocalAndRemote() throws IOException {
		
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE);
		
		URL url1 = new URL("http", "local", -1, "/path-one/a", m_msh);
		cache.getUrl(m_reporter, url1, false, filter).close();
		cache.getUrl(m_reporter, url1, false, filter).close();
		
		// verify that the number of items accessed is 1
		assertEquals(1, m_msh.getAccessList().size() );
	}

	public void testRemoteOnly() throws IOException {
		
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.ONLY_REMOTE);
		
		URL url1 = new URL("http", "local", -1, "/path-one/a", m_msh);
		cache.getUrl(m_reporter, url1, false, filter).close();
		cache.getUrl(m_reporter, url1, false, filter).close();
		
		// verify that the number of items accessed is 1
		assertEquals(2, m_msh.getAccessList().size() );
	}
	
	public void testLocalOnly() throws IOException {
		
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE);
		
		// first, fill the cache.
		URL url1 = new URL("http", "local", -1, "/path-one/a", m_msh);
		cache.getUrl(m_reporter, url1, false, filter).close();
		
		URL url2 = new URL("http", "local", -1, "/path-one/b", m_msh);
		cache.getUrl(m_reporter, url2, false, filter).close();
		
		UrlCache onlyLocalCache = new UrlCache(m_cacheDir, CachingState.ONLY_LOCAL);
		
		onlyLocalCache.getUrl(m_reporter, url1, false, filter).close();
		onlyLocalCache.getUrl(m_reporter, url2, false, filter).close();
		
		URL url3 = new URL("http", "local", -1, "/path-one/c", m_msh);
		try {
			onlyLocalCache.getUrl(m_reporter, url3, false, filter).close();
			fail("Was supposed to get a file not found exception.");
		} catch (FileNotFoundException fnfe) {
			// this is supposed to happen, but if any other exception occurs, test failure.
		}
		
		// the access list should only have the accesses for the first two actions
		// that set up the cache.
		assertEquals(2, m_msh.getAccessList().size() );
	}
	
	public void testPreSave() throws IOException, InterruptedException {
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE);
		UrlCache.SaveFilter<InputStream> c_filter = new UrlCache.SaveFilter<InputStream>() {
			public InputStream preAction(InputStream stream, CachingState mode,
					boolean isExistent) throws IOException {
                if(mode == CachingState.LOCAL_AND_REMOTE && stream != null && isExistent){
                    return new ByteArrayInputStream("hello".getBytes());
                }
                return stream;
			}
		};
		
		URL url1 = new URL("http", "local", -1, "/path-one/a", m_msh);
		InputStream stream1 = cache.getUrl(m_reporter, url1, false, c_filter);
		assertEquals("contents\nof\n1.a\n", getContentFromStream(stream1));
		stream1.close();
		
		Thread.sleep(65*1000);
		
		InputStream stream2 = cache.getUrl(m_reporter, url1, true, c_filter);
		assertEquals("hello", getContentFromStream(stream2));
		stream2.close();
	}
	
	/**
	 * Verify that for the default agent, the file URLs are not cached, but their existence is
	 * properly checked.
	 */
	public void testFileSchemeNotCached() throws IOException {
		
		UrlCache onlyLocalCache = new UrlCache(m_cacheDir, CachingState.ONLY_LOCAL);
		
		// create a file in our cache folder.
		File tempFile = new File(m_cacheDir, "uncachedFile.txt");
		FileWriter writer = new FileWriter(tempFile);
		writer.write("Some text goes here.");
		writer.close();
		
		URL toFetch = tempFile.toURI().toURL();
		
		// first, try to read.
		InputStream stream = onlyLocalCache.getUrl(m_reporter, toFetch, false, filter);
		stream.close();
		
		// second, verify existence.
		assertTrue( onlyLocalCache.isCached(toFetch) );
	}
	
	public static String getContentFromStream(InputStream stream) throws IOException{
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		byte[] buffer = new byte[8*1024];
		for(int size=0; (size = stream.read(buffer)) >= 0; ){
			bao.write(buffer, 0, size);
		}
		String result = new String(bao.toByteArray());
		return result;
	}
	
	static class MemoryStreamHandler extends URLStreamHandler {

		public MemoryStreamHandler() {
			
			try {
				for (int i = 0 ; i < sm_urlData.length ; i += 2) {
					String key = sm_urlData[i];
					byte[] value = sm_urlData[i + 1].getBytes("UTF-8");
					m_mapping.put(key, value);
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unexpected failure converting to bytes.");
			}
		}
		
		public List<URL> getAccessList() {
			return m_accessList;
		}
		
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			m_accessList.add(u);
			byte[] value = m_mapping.get(u.toString() );
			if (value == null) {
				throw new FileNotFoundException("Unable to find " + u.toString());
			}
			
			return new MemoryUrlConnection(u, value);
		}
		
		
		Map<String, byte[]> m_mapping = DataUtils.newMap();
		
		List<URL> m_accessList = DataUtils.newList();
		
	}
	
	static class MemoryUrlConnection extends URLConnection {

		public MemoryUrlConnection(URL url, byte[] data) {
			super(url);
			m_data = data;
		}

		@Override
		public void connect() throws IOException {
		}
		
		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(m_data);
		}


		private byte[] m_data;
	}
	
	private UrlCache.SaveFilter<InputStream> filter;
	
	private File m_cacheDir;
	
	private MemoryStreamHandler m_msh;
	
	private ProgressReport m_reporter;
	
	private static final String[] sm_urlData = {
		"http://local/path-one/a", "contents\nof\n1.a\n",
		"http://local/path-one/b", "contents\nof\n1.b\n",
		"http://local/path-one/c", "contents\nof\n1.c\n",
		"http://local/path-one/d", "contents\nof\n1.d\n",
		"http://local/path-one/e", "contents\nof\n1.e\n",
		"http://local/path-two/f", "contents\nof\n2.f\n",
		"http://local/path-two/g", "contents\nof\n2.g\n",
		"http://local/path-two/h", "contents\nof\n2.h\n",
		"http://local/path-two/i", "contents\nof\n2.i\n" 
	};
}
