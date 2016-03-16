package com.tibco.devtools.workspace.installer.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.StandaloneFeatureInstaller;
import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.ExtensionLocation;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.SiteInformation;
import com.tibco.devtools.workspace.installer.standalone.TargetPlatformData;
import com.tibco.devtools.workspace.installer.standalone.UrlCache;
import com.tibco.devtools.workspace.installer.tasks.AnyItem.UrlItem;
import com.tibco.devtools.workspace.installer.utils.EclipseUtils;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Takes an update site, and installs all of the features identified by a target list.
 */
public class InstallTargets extends Task {

	//=========================================================================
	// Shared utility methods
	//=========================================================================
	
	public static final String CACHING_BOTH = "both";
	public static final String CACHING_LOCAL = "local";
	public static final String CACHING_REMOTE = "remote";
	
	public static CachingState cachingStateFromString(String state) {
		CachingState result = CachingState.LOCAL_AND_REMOTE;
		if (state.equalsIgnoreCase(CACHING_BOTH)) {
			result = CachingState.LOCAL_AND_REMOTE;
		}
		else if (state.equalsIgnoreCase(CACHING_LOCAL)) {
			result = CachingState.ONLY_LOCAL;
		}
		else if (state.equalsIgnoreCase(CACHING_REMOTE)) {
			result = CachingState.ONLY_REMOTE;
		}
		else {
			throw new BuildException("Only 'both', 'local', and 'remote' allowed for caching state.");
		}
		
		return result;
	}
	
	private boolean removeOtherVersionsOfSameFeature = true;

	public void setRemoveOtherVersionsOfSameFeature(boolean value) {
		this.removeOtherVersionsOfSameFeature = value;
	}

	public static SiteInformation getSiteInformation(ProgressReport reporter,
			List<UrlList> sites, UrlCache cache)
			throws MalformedURLException {
		List<URL> urls = DataUtils.newList();
		for (UrlList oneList : sites) {
			urls.addAll( oneList.normalizeAnyItems() );
		}
		if (urls.size() == 0) {
			throw new BuildException("No sites specified in updatesites child element.");
		}
		return StandaloneFeatureInstaller.createUpdateSiteScanner(cache, urls, reporter);
	}

	/**
	 * Parses a "known" mapping file used for converting things like org.eclipse.emf and com.tibco.tpcl.emf
	 * 
	 * @param reporter
	 * @param cache
	 * @return
	 * @throws IOException
	 */
	public static Map<Target<VersionInfo>, List< Target<VersionInfo> > > getKnownMappings(ProgressReport reporter, UrlCache cache, URL mappingFile) throws IOException {
		UrlCache.SaveFilter<InputStream> filter = new UrlCache.SaveFilter<InputStream>() {
			public InputStream preAction(InputStream stream, CachingState mode, boolean isExistent) throws IOException {
				//do nothing, just return
				return stream;
			}
		};
		InputStream is = cache.getUrl(reporter, mappingFile, true, filter);
		return parseKnownMappings(is, mappingFile);
	}

	public static Map<Target<VersionInfo>, List< Target<VersionInfo>> > parseKnownMappings(InputStream is, URL errLoc)
			throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		Map<Target<VersionInfo>, List< Target<VersionInfo>> > knownMappings = DataUtils.newMap();
		try {
			String line;
			while ( (line = br.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.length() > 0) {
					String pairs[] = line.split("\\s+");
					if (pairs.length % 2 != 0) {
						throw new BuildException("Bad format for line: " + line + " found in mapping file at URL " + errLoc);
					}
					// the first pair is the source feature and version.
					Target<VersionInfo> src = new Target<VersionInfo>(pairs[0], VersionInfo.parseVersion(pairs[1]));
					List<Target<VersionInfo> > replacements = DataUtils.newList();
					int idx = 2;
					while (idx < pairs.length) {
						Target<VersionInfo> target = new Target<VersionInfo>(pairs[idx], VersionInfo.parseVersion(pairs[idx + 1]));
						replacements.add(target);
						idx += 2;
					}
					List< Target<VersionInfo> > previous = knownMappings.put(src, replacements);
					if (previous != null) {
						throw new BuildException("A duplicate entry appears in " + errLoc + ". For mapping of "
								+ src.toString() + ":  Initial mapping " + previous.toString() + ", new mapping " + replacements.toString() );
					}
				}
			}
		} finally {
			br.close();
		}
		return knownMappings;
	}

	//=========================================================================
	// Ant methods for setting properties....
	//=========================================================================
	
	public void setCaching(String caching) {
    	m_cacheState = cachingStateFromString(caching);
    }
    
	public void setRefId(String refId) {
		m_refId = refId;
	}
	
	/**
	 * Directory that features and plugins will be installed into.
	 * 
	 * @param dir
	 */
	public void setDir(File dir) {
		m_dir = dir;
	}
	
    /**
     * Sub-element "updatesites" collects URLs
     */
    public UrlList createUpdateSites() {
    	// that this value is null flags that we need to recreate the value...
        UrlList list = new UrlList(getProject());
        m_updateSites.add(list);
        return list;
    }

    public void setLocalSiteCache(File dir) {
    	m_localCacheSite = dir;
    }
    
    public void setDualNatureExtension(String extension) {
    	m_dualNatureExtension = extension;
    }
    
    public void setFailOnDualNatureMixing(boolean fail) {
    	m_failOnDualNatureMixing = fail;
    }
    
	public void setFeatureMappingURL(String url) {
		try {
			m_featureMappingURL = new URL(url);
		} catch (MalformedURLException e) {
			throw new BuildException(e);
		}
	}
	
	//=========================================================================
	// Task method overrides
	//=========================================================================
	
	@Override
	public void execute() throws BuildException {
		
		validate();
		
		ProgressReport reporter = new AntLogProgressReport(this);
		try {
			// get the information about the available sites.
			UrlCache cache = new UrlCache(m_localCacheSite, m_cacheState);
			SiteInformation sites = getSiteInformation(reporter, m_updateSites, cache);
			
            TargetPlatformData targetPlatform = TargetPlatformData.getCurrentTargetPlatform();
            
            // get the targets I'm supposed to install.
            TargetList targets = (TargetList) getProject().getReference(m_refId);
            if (targets == null) {
            	throw new BuildException("No targets list defined for reference id " + m_refId);
            }
            Collection< Target<VersionInfo>> toInstall = targets.getTargets();
            
            // set up the target folder
            EclipseUtils.createEclipseFolder(m_dir, false);
            ExtensionLocation extLoc = new ExtensionLocation(m_dir);
            
            if (m_dualNatureExtension.length() > 0) {
            	
            	Map<Target<VersionInfo>, List< Target<VersionInfo> > > explicitMaps = Collections.emptyMap(); 
            	if (m_featureMappingURL != null) {
            		explicitMaps = getKnownMappings(reporter, cache, m_featureMappingURL);
            	}
            	validateDualNatureMix(sites, toInstall, explicitMaps);
            }
            
            // install the features.
			StandaloneFeatureInstaller.installFiles(reporter, sites, extLoc, targetPlatform, toInstall,removeOtherVersionsOfSameFeature);
			
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}
	
	private void validateDualNatureMix(SiteInformation sites,
			Collection<Target<VersionInfo>> toInstall, Map<Target<VersionInfo>, List<Target<VersionInfo>>> explicitMaps) {
		
		List<Target<VersionInfo>> unextended = DataUtils.newList();
		List<Target<VersionInfo>> extended = DataUtils.newList();
		
		Set<String> explicitMapKeys = new HashSet<String>();
		for (Target<VersionInfo> oneTarget : explicitMaps.keySet()) {
			explicitMapKeys.add(oneTarget.getTargetId());
		}
		
		Set<String> explicitMapTargets = new HashSet<String>();
		for (List<Target<VersionInfo>> oneList : explicitMaps.values()) {
			for (Target<VersionInfo> oneTarget : oneList) {
				explicitMapTargets.add(oneTarget.getTargetId());
			}
		}
		categorizeFeatures(sites, toInstall, explicitMapKeys, explicitMapTargets, unextended, extended);

		// we should only ever have a non zero # of one of these, not
		// both.
		if (unextended.size() > 0 && extended.size() > 0) {
			Collections.sort(unextended);
			Collections.sort(extended);
			
			int logLevel = m_failOnDualNatureMixing ? Project.MSG_ERR : Project.MSG_WARN;
			
			log("Mixed dual nature release units selected for installation.", logLevel);
			log("** Found the following with regular nature **", logLevel);
			
			outputTargets(unextended, logLevel);
			
			log("** Found the following with " + m_dualNatureExtension + " nature **", logLevel);
			outputTargets(extended, logLevel);
			
			if (m_failOnDualNatureMixing) {
				throw new BuildException("Error using mixed nature.");
			}
		}
	}

	private void categorizeFeatures(SiteInformation sites,
			Collection<Target<VersionInfo>> toInstall,
			Set<String> unextendedExplicit,
			Set<String> extendedExplicit,
			List<Target<VersionInfo>> unextended,
			List<Target<VersionInfo>> extended) {
		
		Set<String> features = new HashSet<String>();
		features.addAll(sites.getAvailableFeatureIds() );
		
		// loop through all the targets to identify those that
		// have a dual nature.
		int extLen = m_dualNatureExtension.length();
		for (Target<VersionInfo> oneTarget : toInstall) {
			String name = oneTarget.getTargetId();
			
			// is this one of the explicit mappings that we have?
			if (unextendedExplicit.contains(name)) {
				unextended.add(oneTarget);
			}
			else if (extendedExplicit.contains(name)) {
				extended.add(oneTarget);
			}
			else {
				String otherName;
				List<Target<VersionInfo>> whichList;
				// determine which list this target belongs to, if any,
				// and what the "other" name would be.
				if (name.endsWith(m_dualNatureExtension)) {
					whichList = extended;
					otherName = name.substring(0, name.length() - extLen);
				}
				else {
					whichList = unextended;
					otherName = name + m_dualNatureExtension;
				}
				
				if (features.contains(otherName)) {
					whichList.add(oneTarget);
				}
			}
		}
	}

	/**
	 * Output the targets that we've found, flagging the ones
	 * that are already installed and not being installed again.
	 * 
	 * @param toOutput
	 * @param logLevel
	 */
	private void outputTargets(List<Target<VersionInfo>> toOutput,
			int logLevel) {
		for (Target<VersionInfo> oneTarget : toOutput) {
			log("  " + oneTarget.toString(), logLevel);
		}
	}

	private void validate() {
		if (m_localCacheSite == null) {
			throw new BuildException("Must set localsitecache attribute.");
		}
		
		if (m_refId == null) {
			throw new BuildException("Must set targetsrefid attribute.");
		}
	}
	
	//=========================================================================
	// Private member data
	//=========================================================================
	
	private String m_refId;
	
	private List<UrlList> m_updateSites = DataUtils.newList();
	
	private File m_dir;
	
	private File m_localCacheSite;
	
	private CachingState m_cacheState = CachingState.LOCAL_AND_REMOTE;
	
	private boolean m_failOnDualNatureMixing = false;
	
	/**
	 * Defaulting it to Eclipse, which is what we use....
	 */
	private String m_dualNatureExtension = ".eclipse";
	
	private URL m_featureMappingURL = null;
}
