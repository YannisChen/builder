package com.tibco.devtools.workspace.installer.standalone;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.w3c.dom.Document;

import com.tibco.devtools.workspace.installer.utils.FileUtils;

/**
 * Means to cache access to URLs.
 * 
 * <p>This is a fairly simple caching approach, and makes a variety of assumptions.</p>
 * <ul>
 * 	<li>no query portion for each URL</li>
 *  <li>last portion of the URL (the "file" if you will), works as a file name on the local OS</li>
 *  <li>there will be no name collisions on the file portion on the case insensitive file systems
 *  	used by Windows</li>
 *  <li>We're not handling a large number of paths on a large number of web sites</li>
 * </ul>
 * 
 * <p>The general approach taken here is pretty simple.  Drop the file portion of a URL,
 * and identify a unique folder for the remaining URL.  That is done by keeping a record
 * of the URL/folder mappings we have so far (in <code>mapping.xml</code>), and adding new
 * mappings by generating hex representations of the hash code of the string form of the
 * URL.  In case of collisions, this searches for new folder names by adding one to previous
 * attempt until no collision occurs.</p>
 * 
 * <p>Note that this class uses an agent to decide how to treat any individual URL, should it be
 * cached, does it exist at its original location, and to access the stream at that location.</p>
 */
public class UrlCache {
	
	/**
	 * All actual access to the URLs is conducted through an agent that does the real work of
	 * working with a URL.  This allows callers to override the default behavior for what
	 * should be cached and not.
	 */
	public interface UrlAgent {
		
		/**
		 * Is the URL in question cached, or is it always fetched from its original location?
		 * 
		 * <p>Note that this method must be consistent over time - once it makes a choice
		 * about a particular URL, it should always make the same choice about that URL.</p>
		 * 
		 * @param toFetch	The URL to fetch.
		 * @return	<code>true</code> if the URL is one that should be cached.
		 */
		boolean shouldCache(URL toFetch);
		
		/**
		 * For any URL used by the cache, this is what is used to get the contents of the URL.
		 * 
		 * @param toFetch  The URL's contents to fetch.
		 * @param cachedForm The cached form of the same resource. If there's no cached
		 * 		form, or we're not interested in the cached, form, returns null.
		 * 
		 * @return The appropriate stream for reading the URL. Note that <code>null</code>
		 * 		may be returned if the cached form is up-to-date (uses "if-modified-since" header).
		 * 
		 * @throws IOException
		 */
		InputStream openStream(URL toFetch, File cachedForm) throws IOException;

		/**
		 * Does the URL in question point to a resource that exists in its official location
		 * (not its cache location)?
		 * 
		 * <p>This method is only invoked for URLs where {@link #shouldCache(URL)} returns false.
		 * </p>
		 * 
		 * @param toTest	The URL to test.
		 * @return	<code>true</code> if the resource exists at the original location.
		 * 
		 * @see #shouldCache(URL)
		 */
		boolean existsAtOriginalLocation(URL toTest);
		
	}

	/**
	 * Do some custom actions before save it to local machine.
	 * 
	 * @param <In> 
	 */
	public interface SaveFilter<In>{
		In preAction(InputStream stream, CachingState mode, boolean isExistent) throws IOException;
	}
	
	public UrlCache(File cacheFolder, CachingState cachingState, UrlAgent agent) {
		m_cacheFolder = cacheFolder;
		m_cachingState = cachingState;
		m_agent = agent;

		try {
			FileUtils.createDirectories(m_cacheFolder);
			
			m_propsFile = new File(cacheFolder, "mapping.xml");
			RandomAccessFile raf = new RandomAccessFile(m_propsFile, "rw");
			FileChannel fileChan = raf.getChannel();
			FileLock fileLock = fileChan.lock();
			
			try {
				// now, read the properties file, or write it if it never existed.
				if (raf.length() > 0) {
					reloadMappingFile(fileChan);
				}
				else {
					writePropertiesFile(fileChan);
				}

			}
			finally {
				// make sure we release the lock and close the file.
				fileLock.release();
				raf.close();
			}
		}
		catch (IOException ioe) {
			throw new RuntimeException("Unable to read caching state, giving up." + ioe, ioe);
		}
	}

	/**
	 * Construct a cache at the given folder, with a given state, using the default
	 * caching algorithm.
	 * 
	 * @param cacheFolder	The folder into which cached files will go.
	 * @param cachingState	The state of caching - local only, remote only, both.
	 */
	public UrlCache(File cacheFolder, CachingState cachingState) {
		this(cacheFolder, cachingState, sm_defaultAgent);
	}
	
	/**
	 * Get the state of caching.
	 * 
	 * @return The current state of caching.
	 */
	public CachingState getCachingState() {
		return m_cachingState;
	}
	
	/**
	 * Get the URL value....
	 * 
	 * <p>This implementation uses Java FileLocks to prevent concurrent access to the same file
	 * from multiple OS-level
	 * @param reporter	Status about which URLs are being fetched is written here.
	 * @param toFetch	The URL to fetch
	 * @param needsCheckForRemoteChange If we're in LOCAL_AND_REMOTE mode, recheck the remote location to
	 * 	see whether we need a new copy.  A <code>false</code> value lets clients short-circuit checking if
	 * 	it is generally safe to assume that once a resource has been fetched, it will not change.
	 * 
	 * @return A stream for reading the given URL.
	 * 
	 * @throws IOException	If problems arise actually reading the cached or remote copy of the file. 
	 */
	public InputStream getUrl(ProgressReport reporter, URL toFetch, boolean needsCheckForRemoteChange, SaveFilter<InputStream> filter) throws IOException {

		// straight up front - if this is a URL we're not supposed to cache (file URL),
		// simply return the original data - do NOT consider caching.
		if (!m_agent.shouldCache(toFetch)) {
			return m_agent.openStream(toFetch, null);
		}
		File category = extractCategory(toFetch);
		String fileName = extractFileName(toFetch);
		File target = new File(category, fileName);
		boolean targetExists = target.isFile();
		
		// note that the "part" file is just a semaphore for an incomplete download
		File targetPart = new File(category, fileName + ".part");
		boolean partFlagExists = targetPart.isFile();
		// if we're not doing just local access, or we don't have the file
		// cached...
		if ((m_cachingState == CachingState.LOCAL_AND_REMOTE &&
				(!targetExists || partFlagExists || (needsCheckForRemoteChange && isTooOld(target) ) ) )
				|| m_cachingState == CachingState.ONLY_REMOTE) {

			BufferedInputStream bis = null;
			try {
				InputStream stream;
				// we have a file cached locally, see if we need to refetch.
				if (!partFlagExists && m_cachingState == CachingState.LOCAL_AND_REMOTE && targetExists) {
					// its either too old, or we need to check for a new remote version.
					stream = m_agent.openStream(toFetch, target);
				} else {
					// local file doesn't exist (at least not fully), or pure remote access, don't point at cached version.
					stream = m_agent.openStream(toFetch, null);
				}

				stream = filter.preAction(stream, m_cachingState, !partFlagExists&&targetExists);
				if(stream != null){
					// AHA, we got a stream, which means cached copy is not newer....
					bis = new BufferedInputStream(stream);
					
					reporter.message("Downloading " + toFetch.toString());

					// go fetch it.
					OutputStream targetPartStream = new FileOutputStream(targetPart);
					// wrapped in a try to make sure the output stream gets closed.
					try {
						targetPartStream.write("Partial download marker.".getBytes() );
					} finally {
						targetPartStream.close();
					}
					
					FileUtils.copyStreamToLockedFile(bis, target);
					
					FileUtils.blindlyDelete(targetPart);
				}
				else {
					reporter.message("Using cached copy of " + toFetch.toString() );
				}
			} finally {
				if (bis != null) {
					bis.close();
				}
			}
		}
		// if we're only doing local access, and the file isn't there.
		else if (m_cachingState == CachingState.ONLY_LOCAL && !targetExists) {
			// throw up our hands.
			throw new FileNotFoundException("Unable to find " + toFetch.toString() + " cached locally at " + target.toString() ); 
		}
		
		// now, get a read lock on the file, and keep the lock until we close the file
		RandomAccessFile raf = new RandomAccessFile(target, "r");
		FileChannel readChannel = raf.getChannel();
		readChannel.lock(0, Long.MAX_VALUE, true);
		InputStream is = Channels.newInputStream(readChannel);
		return new RandomAccessFileInputStream(raf, readChannel, is);
	}

	/**
	 * Delete the cached file from local-cache-site.
	 * 
	 * @param toDelete	The URL to delete.
	 * @throws	Thrown if there is some operation problem from toFetch or mapping.xml
	 */
	public void deleteCachedFile(URL toDelete) throws IOException{
		if(m_agent.shouldCache(toDelete)){
			File category = extractCategory(toDelete);
			String fileName = extractFileName(toDelete);
			File target = new File(category, fileName);
			if(target.exists()) target.delete();
		}
	}
	
	/**
	 * Is the file in question cached?
	 * 
	 * @param toFetch	The URL to check for caching.
	 * @return	<code>true</code> if the file is cached.
	 * @throws IOException Thrown if there is some problem with the caching.
	 */
	public boolean isCached(URL toFetch) throws IOException {
		if ( !m_agent.shouldCache(toFetch) ) {
			return m_agent.existsAtOriginalLocation(toFetch);
		}
		return extractTargetFile(toFetch).isFile();
	}

	//=========================================================================
	// Private methods
	//=========================================================================
	
	/**
	 * Reloads the mapping file from disk.
	 * 
	 * @throws IOException
	 * @throws InvalidPropertiesFormatException
	 */
	private void reloadMappingFile(FileChannel fileChan) throws IOException,
			InvalidPropertiesFormatException {
		
		fileChan.position(0);
		InputStream is = Channels.newInputStream(fileChan);
		
		// OK, the following malarky relates to the fact that although loadFromXML is
		// *explicitly* documented as leaving the stream open, in fact, it doesn't!
		// instead we read the contents ourselves into a byte array, then pass a
		// stream to read that byte array.
		byte[] asBytes = FileUtils.transferStreamToBytes(is, (int) fileChan.size());

		// OK - finally we can read that darn file. 
		ByteArrayInputStream bais = new ByteArrayInputStream(asBytes);
		m_urlToFolder.loadFromXML(bais);
	}

	/**
	 * Determines whether a file is too old for our caching algorithm...
	 */
	private boolean isTooOld(File target) {
		
		long mustBeNewerThan = System.currentTimeMillis() - TOUCH_DELAY_IN_MILLISECONDS;
		return target.lastModified() < mustBeNewerThan;
	}

	private File extractTargetFile(URL url) throws IOException {
		String fileName = extractFileName(url);
		return new File(extractCategory(url), fileName);
	}

	/**
	 * Get the filename portion of the URL so that we can use that
	 * as part of the caching...
	 * Trick: Sometimes that URL is not general, since it has some query string. 
	 * Like: http://www.eclipse.org/downloads/download.php?r=1&file=/technology/swtbot/ganymede/dev-build/update-site/features/org.eclipse.swtbot.eclipse_2.0.3.20110219_0655-4d933cf-dev-e34.jar
	 * Firstly I have to connect to that original URL, so that we can get the real URL.
	 * Since getInputStream() method spend so much time, so we add a IF condition.
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	private String extractFileName(URL url) throws IOException {
		String path = url.getPath();
		if(url.toString().indexOf('?') >= 0){
			URLConnection conn = url.openConnection();
			conn.getInputStream();
			path = conn.getURL().getPath();
		}
		int idx = path.lastIndexOf('/');
		String fileName = path.substring(idx + 1);
		if (fileName.length() == 0) {
			throw new IllegalStateException("Unable to get the file name....");
		}
		return fileName;
	}
	
	/**
	 * Map a given URL to a folder.
	 * 
	 * @param url	The URL to map to a folder.
	 * 
	 * @return	The name of the folder.
	 * @throws IOException 
	 */
	private File extractCategory(URL url) throws IOException {
		String fullURL = url.toString();
		int idx = fullURL.lastIndexOf('/');
		String base = fullURL.substring(0, idx);
		if (!m_urlToFolder.containsKey(base)) {
			RandomAccessFile raf = new RandomAccessFile(m_propsFile, "rw");
			FileChannel fileChan = raf.getChannel();
			FileLock fileLock = fileChan.lock();
			
			try {
				// OK - here's the trick of it all - with the file locked, reload the
				// mapping file to see if any entries have been added since we last saved
				// from this process.
				reloadMappingFile(fileChan);
				if (!m_urlToFolder.containsKey(base)) {
					fileChan.position(0);
					String newFolder = computeNewFolderName(url, base);
					m_urlToFolder.setProperty(base, newFolder);
					writePropertiesFile(fileChan);
				}
			}
			finally {
				fileLock.release();
				raf.close();
			}
		}
		String result = m_urlToFolder.getProperty(base);
		return new File(m_cacheFolder, result);
		
	}

	/**
	 * Compute the name for a folder based on a URL.
	 * @param url The URL that we want to map to a local folder.
	 * @param base	The base URL for which we need a folder.
	 * @return	The new String for the folder name.
	 */
	private String computeNewFolderName(URL url, String base) {
		String prefix = "";
		
		// this little bit of mumbo-jumbo makes our folder names slightly more obvious
		// to a human trying to scan them.  To delete all cache entries from
		// files
		String scheme = url.getProtocol().toLowerCase();
		if (scheme.equals("file")) {
			prefix = "file-";
		}
		else if (scheme.equals("http")) {
			prefix = "http-" + url.getHost() + "-";
		}
		else if (scheme.equals("https")) {
			prefix = "https-" + url.getHost() + "-";
		}
		int trial = base.hashCode();
		boolean keepTrying = true;
		String trialStr = null;
		File trialFile = null; 
		while (keepTrying) {
			trialStr = prefix + Integer.toHexString(trial);
			trialFile = new File(m_cacheFolder, trialStr);

			// I'm really only interested in whether I can create the folder, not the reason why I cannot.
			keepTrying = !trialFile.mkdir();
			
			// now bump to the next one to try, in case we have to repeat.
			trial++;
		}
		return trialStr;
	}
	
	/**
	 * Write out our record of what URL maps to what folder.
	 */
	private void writePropertiesFile(FileChannel fileChan) {
		try {
			// make sure we truncate the file, so we don't leave stuff hanging at the
			// end, if it the representation on disk somehow gets shorter.
			fileChan.truncate(0);
			OutputStream os = Channels.newOutputStream(fileChan);
			m_urlToFolder.storeToXML(os, "Mapping of URL base to folder.");
		} catch (IOException e) {
			throw new RuntimeException("Error writing out a new properties file.");
		}
	}
	
	//=========================================================================
	// Private member data.
	//=========================================================================
	
	/**
	 * The default URL agent assumes that all File URLs should not be cached,
	 * and instead always fetched from their original location.
	 */
	private static class DefaultUrlAgent implements UrlAgent {

		public boolean existsAtOriginalLocation(URL toTest) {
			File fileDest;
			try {
				fileDest = new File(toTest.toURI() );
			} catch (URISyntaxException e) {
				return false;
			}
			return fileDest.exists();
		}

		public InputStream openStream(URL toFetch, File cachedVersion) throws IOException {
			
			URLConnection conn = toFetch.openConnection();
			/*
			 * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value 
			 * specifies the timeout when reading from Input stream when a connection is established 
			 * to a resource. If the timeout expires before there is data available for read, 
			 * a java.net.SocketTimeoutException is raised. A timeout of zero is interpreted 
			 * as an infinite timeout.
			 * 
			 * Some non-standard implementation of this method ignores the specified timeout. 
			 * To see the read timeout set, please call getReadTimeout().
			 */
			conn.setReadTimeout(60 * 1000);//TOOL-1202
			conn.setConnectTimeout(30 * 1000);//TOOL-1202
			// Is this an HTTP connection - can I do "if-modified-since?"
			if (cachedVersion != null && (conn instanceof HttpURLConnection) ) {
				// yes, I can.
				HttpURLConnection httpconn = (HttpURLConnection) conn;
				httpconn.setIfModifiedSince( cachedVersion.lastModified() );
				int code = httpconn.getResponseCode();
				if (code == HttpURLConnection.HTTP_OK) {
					return conn.getInputStream();
				}
				else if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
					long newTime = System.currentTimeMillis();
					if (!cachedVersion.setLastModified(newTime) ) {
						// Note that we used to throw an exception here, but apparently setLastModified returns
						// false in a variety of scenarios where it may not be an actual error.
						// so instead, this simply ignores the error.
						System.out.println("Unable to set modification time for " + cachedVersion.toString() +
								".  Current time is " + cachedVersion.lastModified() + " - attempted to set to " + newTime);
					}
					return null;
				}
				else {
					throw new IOException("Got unexpected HTTP status code of " + code + " for URL " + toFetch.toString() );
				}
			}
			else {
				// nope, get the input stream.
				return conn.getInputStream();
			}
		}

		public boolean shouldCache(URL toFetch) {
			// is it file scheme - then *DON'T* cache.
			return !isFileProtocol(toFetch);
		}

		private boolean isFileProtocol(URL url) {
			return url.getProtocol().equalsIgnoreCase("file");
		}
	}
	
	private static class RandomAccessFileInputStream extends FilterInputStream {

		protected RandomAccessFileInputStream(RandomAccessFile raf, FileChannel fileChan, InputStream in) {
			super(in);
			m_raf = raf;
			m_fileChan = fileChan;
		}

		@Override
		public void close() throws IOException {
			super.close();
			
			m_fileChan.close();
			m_raf.close();
		}

		private RandomAccessFile m_raf;
		
		private FileChannel m_fileChan;
	}
	
	//=========================================================================
	// Private member data.
	//=========================================================================
	
	/**
	 * What agent do we use to access the URLs on behalf of the cache?
	 */
	private UrlAgent m_agent;
	
	/**
	 * Where does our cache go?
	 */
	private File m_cacheFolder;
	
	/**
	 * What kind of caching are we doing?
	 */
	private CachingState	m_cachingState;
	
	/**
	 * This keeps track of where we record our state about which URLs map to which folder.
	 */
	private File m_propsFile;
	
	/**
	 * The properties that map which URL goes to which folder.
	 */
	private Properties m_urlToFolder = new Properties();

	/**
	 * Captures the default agent that should be used for caching.
	 */
	private static UrlAgent sm_defaultAgent = new DefaultUrlAgent();
	
	/**
	 * Static value for how long to wait before refetching a file from a remote site.
	 */
	private static final int TOUCH_DELAY_IN_MILLISECONDS = 60 * 1000;

}
