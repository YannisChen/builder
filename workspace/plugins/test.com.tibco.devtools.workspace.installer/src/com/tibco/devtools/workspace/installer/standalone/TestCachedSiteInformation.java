package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

public class TestCachedSiteInformation extends TestCase {

	/**
	 * This may seem like a lot of setup code, but it is really quite simple.
	 * 
	 * <p>We're creating a temporary folder, unzipping a set of files into it,
	 * and saving off a bunch of pointers into what we just unzipped.
	 * </p> 
	 */
	@Override
	protected void setUp() {
		
		try {
			// may seem weird to get a temporary file and delete it, but
			// I'm interested in a path to a temporary *folder*
			m_scratchDir = File.createTempFile("wi_site_test_", ".tmp");
			m_scratchDir.delete();

			m_cacheDir = new File(m_scratchDir, "cache");
			
			File sites = new File(m_scratchDir, "sites");

			FileUtils.createDirectories(sites);
			FilePlacer placer = new FileSystemPlacer(m_scratchDir);
			InputStream inStream = this.getClass().getResourceAsStream("sitesTest.zip");
			placer.placeExpandedZip("sites", inStream);

			// store a URL to our site.
			File site1 = new File(sites, "sitesTest/site1/site.xml");
			m_site1 = site1.toURI().toURL();

			m_reporter = new EmptyProgressReport();
		} catch (IOException e) {
			throw new RuntimeException("Unable to create file for temporary folder.");
		}
		
	}

	@Override
	protected void tearDown() {
		FileUtils.deleteFolder(m_scratchDir);
	}

	public void testBasicInstall() throws IOException {
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE);
		
		URL[] destinations = new URL[] { m_site1 };
		Collection< Target<VersionInfo> > toInstall = makeInstallList("test.feature.a 1.1.0.002");

		FilePlacerRecorder placer = doAndRecordInstall(cache, destinations,
				toInstall);
		
		List<String> filePaths = placer.getFilePaths();
		List<String> zipPaths = placer.getZipPaths();
		assertEquals(1, filePaths.size());
		assertEquals(1, zipPaths.size());
		
	}

	public void testCachedInstall() throws IOException {
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE,
				new EverythingCachesAgent());
		
		Collection< Target<VersionInfo> > toInstall = makeInstallList(
				"test.feature.a 1.1.0.002", "test.feature.b 2.1.2.004");

		URL[] destinations = new URL[] { m_site1 };
		
		FilePlacerRecorder placer = doAndRecordInstall(cache, destinations,
				toInstall);
		
		// for sanity, verify that the two requested features get "installed".
		assertEquals(2, placer.getFilePaths().size());
		assertEquals(2, placer.getZipPaths().size());
		
		// in this case, all of the above is just the setup.  Now I blow
		// away the site from which these features come from, and try again
		// if it succeeds - then the "ONLY_LOCAL" caching worked.
		File sites = new File(m_scratchDir, "sites");
		FileUtils.deleteFolder(sites);
		
		cache = new UrlCache(m_cacheDir, CachingState.ONLY_LOCAL, new EverythingCachesAgent() );
		placer = doAndRecordInstall(cache, destinations, toInstall);
		
		// this is the real check - should be same as above.
		assertEquals(2, placer.getFilePaths().size());
		assertEquals(2, placer.getZipPaths().size());
		
	}
	
	// Verify that if a feature is only partially cached, it is not available
	// to install from ONLY_LOCAL caching.
	public void testPartiallyCachedInstall() throws IOException {
		
		UrlCache cache = new UrlCache(m_cacheDir, CachingState.LOCAL_AND_REMOTE,
				new EverythingCachesAgent() );
		
		URL[] destinations = new URL[] { m_site1 };
		Collection< Target<VersionInfo> > toInstall = makeInstallList(
				"test.feature.a 1.1.0.002", "test.feature.b 2.1.2.004");
		
		doAndRecordInstall(cache, destinations, toInstall);
		
		// As with testCachedInstall, blow away "site" to guarantee we're not using it.
		File sites = new File(m_scratchDir, "sites");
		FileUtils.deleteFolder(sites);
		
		// now, find the plugin "test.plugin.b.b1_2.1.2.004.jar", and remove it from the cache, and
		// see whether the list of available features in the cache changes from three to two.
		File[] cacheDirs = m_cacheDir.listFiles();
		for (File cacheDir : cacheDirs) {
			File pluginToDelete = new File(cacheDir, "test.plugin.b.b1_2.1.2.004.jar");
			if (pluginToDelete.isFile()) {
				FileUtils.blindlyDelete(pluginToDelete);
				break;
			}
		}
		
		cache = new UrlCache(m_cacheDir, CachingState.ONLY_LOCAL, new EverythingCachesAgent() );
		SiteInformation si = new CachedSiteInformation(cache, destinations, m_reporter);
		Collection<String> availableFeatures = si.getAvailableFeatureIds();
		assertEquals(1, availableFeatures.size() );
	}
	
	private FilePlacerRecorder doAndRecordInstall(UrlCache cache,
			URL[] destinations, Collection< Target<VersionInfo> > toInstall) throws IOException {
		
		FilePlacerRecorder placer = new FilePlacerRecorder();
		SiteInformation si = new CachedSiteInformation(cache, destinations, m_reporter);
		si.getAvailableFeatureIds();
		si.installTargetSet(m_reporter, placer, toInstall, m_tpd);
		return placer;
	}

	/**
	 * As a syntactic convenience, this will prevent having to explicitly create
	 * an array to then go off and build a collection - instead I get to just
	 * pass a comma separated list to this function.
	 * 
	 * @param toInstall	The list of features to install, as a string.
	 * @return
	 */
	private Collection< Target<VersionInfo> > makeInstallList(String... toInstall) {

		Collection< Target<VersionInfo> > results = DataUtils.newList();
		for (String oneStr : toInstall) {
			Target<VersionInfo> vt = TestVersionedTarget.parseTarget(oneStr);
			results.add(vt);
		}
		return results;
	}
	
	/**
	 * The default agent behavior for the cache is to skip caching of file URLs.
	 * 
	 * <p>To avoid writing tests tied to network access, we override this behavior,
	 * get the cache to cache everything, and test behavior that way.</p>
	 */
	private static class EverythingCachesAgent implements UrlCache.UrlAgent {

		public boolean existsAtOriginalLocation(URL toTest) {
			throw new IllegalStateException("Shouldn't get called - everything cached.");
		}

		public InputStream openStream(URL toFetch, File cacheTarget) throws IOException {
			return toFetch.openStream();
		}

		public boolean shouldCache(URL toFetch) {
			return true;
		}
		
	}
	
	private ProgressReport m_reporter;
	
	private File m_cacheDir;
	
	private URL m_site1;
	
	private File m_scratchDir;

	private TargetPlatformData m_tpd = new TargetPlatformData();
}
