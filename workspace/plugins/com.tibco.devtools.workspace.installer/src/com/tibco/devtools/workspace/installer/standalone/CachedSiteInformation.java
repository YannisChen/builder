package com.tibco.devtools.workspace.installer.standalone;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import com.tibco.devtools.workspace.installer.standalone.UrlCache.SaveFilter;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Combines a set of Eclipse update sites into a single combined view of the data,
 * while simultaneously living on top of a caching mechanism that supports off-line
 * use.
 */
public class CachedSiteInformation implements SiteInformation {

	/**
	 * Construct a roll-up of a bunch of sites, with a given caching configuration,
	 * a set of destinations, and a roll-up reporting destination.
	 * 
	 * @param cache
	 * @param destinations
	 * @param reporter
	 */
	public CachedSiteInformation(UrlCache cache, URL[] destinations, ProgressReport reporter) {
		m_cache = cache;
		m_destinations = new URL[destinations.length];
		System.arraycopy(destinations, 0, m_destinations, 0, destinations.length);
		m_reporter = reporter;
	}
	
	public Map<String, List< Target<VersionInfo> >> getAvailableFeatures() {

		retrieveSites();
		return m_allFeatures;
	}

	public FeatureDescriptor getFeatureModel(Target<VersionInfo> target) {
        enforceCache();

        SiteFeatureInfo info = m_versionedTargetToInfo.get(target);

        FeatureDescriptor descriptor = info.getModel();
        if (descriptor == null) {
            m_versionedTargetToInfo.remove(target);
        }
        return descriptor;
	}

    public void installTargetSet(ProgressReport reporter, FilePlacer placer,
			Collection<Target<VersionInfo> > toInstall,
			TargetPlatformData targetPlatform) throws IOException {

    	retrieveSites();
    	
        for (Target<VersionInfo> target : toInstall) {
        	if (!m_versionedTargetToInfo.containsKey(target) ) {
        		throw new FileNotFoundException("ERROR: Unable to install feature " + target.toString() + " - not found on any site.");
        	}
            installOneTarget(reporter, placer, target, targetPlatform);
        }

	}

	public Collection<String> getAvailableFeatureIds() {
        return getAvailableFeatures().keySet();
	}

	public List<Target<VersionInfo>> getFeatureVersionSetById(
			String featureId) {

		retrieveSites();
		
		// allow a null return here if there are no features with the given ID.
        List< Target<VersionInfo> > set = m_allFeatures.get(featureId);
        return set != null ? Collections.unmodifiableList( m_allFeatures.get(featureId) ) : null;
	}

    /**
     * Install one feature into the destination, using the given placer.
     * @param reporter messages sent here.
     * @param placer    Controls where to place the files.
     * @param target    Which target to install
     * @param targetPlatform Which target platform to target?
     * @param windowSystem Which windowing system to match?
     *
     * @throws IOException
     */
    private void installOneTarget(ProgressReport reporter, FilePlacer placer,
            Target<VersionInfo> target, TargetPlatformData targetPlatform) throws IOException {

        reporter = reporter.createChild("Installing " + target.toString() );
        
        SiteFeatureInfo info = m_versionedTargetToInfo.get(target);

        FeatureDescriptor model = info.getModel();
        // loop through each plugin referenced by the feature....
        Collection<PluginReference<VersionInfo> > pluginRefs = model.getProvidedPlugins();
        for (PluginReference<VersionInfo> pr : pluginRefs) {
            // does it match on OS filter?
            String os = pr.getOs();
            String arch = pr.getArch();
            String windowSystem = pr.getWindowSystem();
            if ( UpdateSiteUtils.isFilterMatch(os, targetPlatform.getOS() )
                    && UpdateSiteUtils.isFilterMatch(arch, targetPlatform.getArch() )
                    && UpdateSiteUtils.isFilterMatch(windowSystem, targetPlatform.getWindowSystem() ) ) {

                // yup, so construct paths and URLs for source and destination.
                String destPath = null;
                String srcPath;
                StringBuffer destBuf = new StringBuffer();
                destBuf.append("plugins/");
                destBuf.append(pr.getTarget().getTargetId());
                destBuf.append("_");
                destBuf.append(pr.getTarget().getVersion().toString(true) );
                if (pr.isMeantToUnpack() ) {
                    destPath = destBuf.toString();
                }
                destBuf.append(".jar");
                srcPath = destBuf.toString();
                if (!pr.isMeantToUnpack()) {
                    destPath = srcPath;
                }

                boolean installSuccess = false;
                try {
                    // now go get the stream.
                	URL pluginUrl = info.getPluginUrl(pr);
                    SaveFilter<InputStream> filter = new UrlCache.SaveFilter<InputStream>() {
                        public InputStream preAction(InputStream stream, CachingState mode, boolean isExistent) {
                            //do nothing, just return
                            return stream;
                        }
                    };
                    InputStream stream = m_cache.getUrl(reporter, pluginUrl, false, filter);
                    BufferedInputStream bis = new BufferedInputStream(stream);

                    if (pr.isMeantToUnpack()) {
                        placer.placeExpandedZip(destPath, bis);
                    }
                    else {
                        placer.placeFile(destPath, bis);
                    }
                    installSuccess = true;
                } finally {
                    // if we failed to install the plugin, report the error.
                    if (!installSuccess) {
                        System.out.println("Error installing plugin to " + destPath);
                    }
                }
            }

        }

        // NOTE: Write the feature metadata *AFTER* writing all the plugins, so that if
        // the user does a Ctrl+C to interrupt the download, it restarts installing the
        // feature.
        InputStream contents = info.getFeatureContents();

        // get the folder for the feature
        StringBuffer folderBuf = new StringBuffer("features/");
        folderBuf.append( target.getTargetId() );
        folderBuf.append("_");
        folderBuf.append( target.getVersion().toString() );
        String featureFolder = folderBuf.toString();

        placer.placeExpandedZip(featureFolder, contents);

    }

	/**
	 * Makes sure that the cache has already been filled.
	 */
	private void enforceCache() {
	    if (!m_hasScannedSites) {
	        throw new IllegalStateException("You must call getAvailableFeatures() first.");
	    }
	}

	/**
	 * Retrieve the information about our sites.
	 * @throws IOException Should anything go wrong accessing the remote site
	 */
    private void retrieveSites() {
    	if (m_hasScannedSites) {
    		return;
    	}
    	
    	UpdateSiteUtils.normalizeDestinations(m_destinations);
    	
    	String message = (m_cache.getCachingState() == CachingState.ONLY_LOCAL) ?
    			"Scanning local cache of update sites." : "Scanning update sites.";
    	
        ProgressReport reporter = m_reporter.createChild(message);
        
		final Fetcher fetch = new Fetcher(reporter, m_cache);
		for (final URL destination : m_destinations) {
			
		    try {
				// read the site.xml and parse into DOM.
				Document doc = getSiteDocumentFromVariousSources(reporter, destination);
				// are we only using local items?
				final List<SiteFeatureInfo> features = DataUtils.newList();
				
				ProgressReport childReporter = reporter.createChild("Scanning " + destination.toString() );

				// just accumulate our FeatureInfo into an array.
				UpdateSiteUtils.SiteFeaturesHandler handler = new UpdateSiteUtils.SiteFeaturesHandler() {
					public void processFeature(Target<VersionInfo> target, URL url) {
						features.add( new SiteFeatureInfo(fetch, target, destination, url));
					}
				};
				
				UpdateSiteUtils.parseSiteXml(doc, destination, handler);
				
				if (m_cache.getCachingState() == CachingState.ONLY_LOCAL) {
					// yes - this gets complicated - make sure that we only put the features into our model that
					// have been fully cached.
					pruneListToWhatIsLocal(childReporter, features);
				}

				// now loop back through the features, and file them into the data structures.
				for (SiteFeatureInfo sfi : features) {
					Target<VersionInfo> target = sfi.getTarget(); 

				    List< Target<VersionInfo> > versionList = DataUtils.getMapListValue(m_allFeatures, target.getTargetId() );
				    versionList.add(target);
				    
					// so as to preserve the first version of the feature encountered,
				    // look to see if the entry was in the map already, and only if it
				    // isn't, add it.
				    if (m_versionedTargetToInfo.get(target) == null) {
				        m_versionedTargetToInfo.put(target, sfi);
				    }

				}
			} catch (IOException e) {
				throw new RuntimeException("Unable to cache site " + destination.toString() + ": ", e);
			}
		}
    	
    	m_hasScannedSites = true;
	}

    private void pruneListToWhatIsLocal(ProgressReport reporter, List<SiteFeatureInfo> features) throws IOException {
    	
    	reporter.message("Identifying which of " + features.size() + " features are available locally.");
    	for (Iterator<SiteFeatureInfo> itFeatures = features.iterator() ; itFeatures.hasNext() ; ) {
    		
    		SiteFeatureInfo fi = itFeatures.next();
    		// is this feature JAR cached locally?
    		if (!m_cache.isCached( fi.getUrl() ) ) {
    			// nope, remove it.
    			itFeatures.remove();
    		}
    		else {
    			// yes, it is cached locally, but do we have all of its plugins?
    			FeatureDescriptor fd = fi.getModel();
                Collection<PluginReference<VersionInfo>> pluginRefs = fd.getProvidedPlugins();
                
                // loop through all of the referenced plugins, and if any are not cached, remove feature.
                for (PluginReference<VersionInfo> pr : pluginRefs) {
                	URL pluginUrl = fi.getPluginUrl(pr);
                	if ( !m_cache.isCached(pluginUrl) ) {
                		itFeatures.remove();
                		break;
                	}
                }
    		}
    	}
	}

    private Document getSiteDocumentFromVariousSources(ProgressReport reporter, URL destination) throws IOException {
        // did we get the document at the indicated URL?
        Document doc = getSiteDocument(reporter, destination);
        if (doc == null) {
            m_cache.deleteCachedFile(destination);
            throw new FileNotFoundException("Unable to find site document at(cannot parse it) " + destination.toString() );
        }
        return doc;
	}

	private Document getSiteDocument(ProgressReport reporter, URL destination) throws IOException {
        SaveFilter<InputStream> filter = new UrlCache.SaveFilter<InputStream>() {
            /**
             * To verify whether that remote site.xml is OK.
             * If cached file is existent and the caching mode is LOCAL_AND_REMOTE, when the remote site.xml is malformed,
             * Builder prefer to use cached file rather than bad remote site.xml
             * @param stream	A stream will be check.
             * @param mode	Caching mode
             * @param isExistent	Cached file is OK.
             * @throws	IOException
             * @return	Original stream OR new copy of original stream OR null
             */
            public InputStream preAction(InputStream stream, CachingState mode, boolean isExistent) throws IOException {
                if(mode == CachingState.LOCAL_AND_REMOTE && stream != null && isExistent){
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8*1024];
                    for(int size = 0; (size = stream.read(buffer)) >= 0;){
                        bao.write(buffer, 0, size);
                    }
                    byte[] copy = bao.toByteArray();
                    Document doc = UpdateSiteUtils.parseSiteDocument(new ByteArrayInputStream(copy), "Is this site.xml OK");
                    return (doc != null) ? new ByteArrayInputStream(copy) : null;
                }
                return stream;
            }
        };
        // note the use of "true" here to force the site file to be re-read every time.
        InputStream is = m_cache.getUrl(reporter, destination, true, filter);
        return UpdateSiteUtils.parseSiteDocument(is, destination.toString() );
	}

	private static class Fetcher implements SiteFeatureInfo.FetchUrl {
		
		Fetcher(ProgressReport reporter, UrlCache cache) {
			m_reporter = reporter;
			m_cache = cache;
		}
		
		private ProgressReport m_reporter;

		private UrlCache m_cache;

        SaveFilter<InputStream> filter = new UrlCache.SaveFilter<InputStream>() {
            public InputStream preAction(InputStream stream, CachingState mode, boolean isExistent) {
                //do nothing, just return
                return stream;
            }
        };
		
		public InputStream openStream(URL url) throws IOException {
			return m_cache.getUrl(m_reporter, url, false, filter);
		}
	};
	
	private Map<Target<VersionInfo>, SiteFeatureInfo> m_versionedTargetToInfo = DataUtils.newMap();

    private Map<String, List< Target<VersionInfo> > > m_allFeatures = DataUtils.newMap();

	private UrlCache m_cache;
	
	ProgressReport m_reporter;
	
	/**
	 * Have we scanned all the site information?
	 */
	private boolean m_hasScannedSites = false;
	
	/**
	 * Which sites are we reading from?
	 */
    private URL[] m_destinations;
}
