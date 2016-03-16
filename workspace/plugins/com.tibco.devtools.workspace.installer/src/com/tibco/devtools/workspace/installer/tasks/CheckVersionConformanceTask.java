package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;

import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.ProgressReportToConsole;
import com.tibco.devtools.workspace.installer.standalone.SiteInformation;
import com.tibco.devtools.workspace.installer.standalone.UrlCache;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Given a set of feature files for input, and a set of update sites
 * determine whether the version numbers in the features in question
 * conform to our best practices for version numbers.
 * 
 * <p>There are several tests we can do here against the GA update site(s).
 * <ul>
 *   <li>For any given three part version X.Y.Z, verify that no such three part
 *   version number already appears on spin, that is in the range of [X.Y.Z,X.Y.Z+1).</li>
 *   <li>For any given version number X.Y.Z where Z > 0, verify that either
 *   Z is a multiple of 100, or that some feature in the range [X.Y.Z-1,X.Y.Z)
 *   can be found on the site.</li>
 *   <li>For any given version number X.Y.0, where Y > 0, verify that
 *   some features in the range of [X.Y-1.0,X.Y.0) can be found on the site.</li>
 *   <li>For any given version number X.0.0, where X > 1, verify that some version
 *   in the range [X-1.0.0,X.0.0) appears on the update site.</li>
 * </ul>
 * </p>
 * 
 * <p>Since the first of the above conditions is more serious than the others, there
 * are two flags that control the behavior of this task.  The flag "failonmatch"
 * defaults to "true", and indicates that this task will fail if the the version number
 * matches an existing three part version number.  The other three cases are tracked
 * by "failonskip", when a GA version number has been skipped (1.2.3 is followed by
 * 1.2.5, for example).  "failonskip" defaults to "false".</p> 
 */
public class CheckVersionConformanceTask extends Task {

	public void setFailOnMatch(boolean failOnMatch) {
		m_failOnMatch = failOnMatch;
	}
	
	public void setFailOnSkip(boolean failOnSkip) {
		m_failOnSkip = failOnSkip;
	}
	
	public void setErrorMessage(String message) {
		m_errorMessage = message;
	}
	
	public void setCaching(String caching) {
    	m_cacheState = InstallTargets.cachingStateFromString(caching);
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

    public ResourceCollection createFeatures() {
    	if (m_featureResources != null) {
    		throw new BuildException("Only one child element called 'features' allowed.");
    	}
    	m_featureResources = new Resources();
    	
    	return m_featureResources;
    }
    
    public void setLocalsitecache(File localSiteCache) {
        m_localSiteCache = AnyItem.checkNullVariable(localSiteCache);
    }
    
	@Override
	public void execute() throws BuildException {
		if (m_featureResources == null) {
			throw new BuildException("Must specify a child element 'features' that is a resource collection.");
		}

    	// get the sites.
        UrlCache cache = new UrlCache(m_localSiteCache, m_cacheState);

        ProgressReport reporter = new ProgressReportToConsole();

        try {
        	
			log("  local site cache: " + m_localSiteCache.toString(), Project.MSG_VERBOSE);
			
            SiteInformation siteInfo = InstallTargets.getSiteInformation(
    				reporter, m_updateSites, cache);
            
    	    List<FeatureDescriptor> features = TaskUtils.resourceCollectionToFeaturesList(this, m_featureResources);
    	    
    	    validate(siteInfo, features);
        }
        catch (IOException ioe) {
			throw new BuildException(ioe);
        }
		
	}


    private void validate(SiteInformation siteInfo, List<FeatureDescriptor> features) {
    	
    	for (FeatureDescriptor feature : features) {
	    	log("Checking feature " + feature.getTarget().toString(), Project.MSG_VERBOSE);
    		Target<VersionInfo> target = feature.getTarget();
    		VersionInfo current = target.getVersion();
    		List<Target<VersionInfo>> unmodifiableVersions = siteInfo.getFeatureVersionSetById(target.getTargetId() );
    		
    		if (unmodifiableVersions != null) {
    			// sort the list for reporting purposes...
    			List<Target<VersionInfo>> versions = DataUtils.newList();
    			versions.addAll(unmodifiableVersions);
        		Collections.sort(versions);
	    		int major = current.getMajorVersion();
	    		int minor = current.getMinorVersion();
	    		int patch = current.getPatchVersion();
	    		VersionInfo threePart = new VersionInfo(major, minor, patch, null);
	    		
	    		Range<VersionInfo> matchRange = new Range<VersionInfo>(
	    				threePart, true, threePart.nextPatch(), false);
	    		
	    		VersionInfo matchVersion = anyInRange(versions, matchRange);
	    		if ( matchVersion != null) {
					warnOrFailForMatchingVersions(target, matchVersion);
	    		}
	    		else {
	    			checkPluginVersions(siteInfo, feature, versions);
	    		}
	    		
	    		// Now for the various scenarios in which we want to warn, go ahead and warn...
	    		if (patch > 0) {
	    			// is this a jump to the next 100?
	    			if (patch % 100 != 0) {
	    				// nope, standard hotfix version bump
		        		warnOrFailForMissingEntry(target, versions, major, minor, patch - 1, threePart, "hotfix");
	    			}
	    			else {
	    				// yes, service pack version bump.
		        		warnOrFailForMissingEntry(target, versions, major, minor, patch - 100, threePart, "service pack");
	    			}
	    		}
				if (patch == 0 && minor > 0) {
					warnOrFailForMissingEntry(target, versions, major, minor - 1, 0, threePart, "minor change");
				}
				if (patch == 0 && minor == 0 && major > 1) {
					warnOrFailForMissingEntry(target, versions, major - 1, 0, 0, threePart, "major change");
				}
    		}
    	}
    	
    	if (m_hadWarnings) {
    		if (m_needsToFail) {
    			throw new BuildException(m_errorMessage);
    		}
    		else {
        		log(m_errorMessage, Project.MSG_ERR);
    		}
    	}
    	
	}

	private void warnOrFailForMatchingVersions(Target<VersionInfo> target,
			VersionInfo matchVersion) {
		String mode = codeForFailOrWarn(m_failOnMatch);
		String msg = MessageFormat.format( "{0}: For feature {1} found a matching version {2} on one of the GA sites.\n" +
				"  The version of the feature must be bumped before an official build can be completed.\n", mode,
				target.toString(), matchVersion.toString() );
		
		log(msg, getLogLevel(m_failOnMatch) );
		
		if (m_failOnMatch) {
			m_needsToFail = true;
		}
		m_hadWarnings = true;
	}

	private String codeForFailOrWarn(boolean isError) {
		String mode = isError ? "ERROR" : "Warning";
		return mode;
	}

	/**
	 * Verifies that the versions of the plugins from the previously GA'd version
	 * fall into the expected range.
	 * 
	 * @param siteInfo	The site(s) from which we're getting these features.
	 * @param feature	The feature we're checking.
	 * @param versions	Other available versions of the feature.
	 */
	private void checkPluginVersions(SiteInformation siteInfo, FeatureDescriptor feature,
			List<Target<VersionInfo>> versionsMaster) {
		
		// sort the list.
		List<Target<VersionInfo>> versions = DataUtils.newList();
		versions.addAll(versionsMaster);
		Collections.sort(versions);
		Target<VersionInfo> target = feature.getTarget();

		Target<VersionInfo> largestLessThan = null;
		for (Target<VersionInfo> itTarget : versions) {
			if (itTarget.getVersion().compareTo(target.getVersion() ) < 0) {
				largestLessThan = itTarget;
			}
			else {
				break;
			}
		}
		if (largestLessThan != null) {
			log("  For feature " + feature.getTarget().toString() + " next highest version is "
					+ largestLessThan.getVersion().toString(), Project.MSG_DEBUG);
			// now loop through all the plugins of this feature, and find the version of each plugin
			// from the previous version of the feature we just found.
			
			FeatureDescriptor olderFeature = siteInfo.getFeatureModel(largestLessThan);
			checkPluginVersionDelta(olderFeature, feature);
			
		}
		else {
			String msg = MessageFormat.format(
					"  For feature {0}, didn't find a previously GA'd version.\n", feature.getTarget().toString() );
			log(msg, Project.MSG_VERBOSE);
		}
	}

	private static final int MAJOR_CHANGE = 1;
	private static final int MINOR_CHANGE = 2;
	private static final int SERVICE_PACK = 3;
	private static final int HOTFIX = 4;
	
	/**
	 * For two versions of a feature, check to see whether the older version of the feature
	 * has plugin versions that are at least lesser than the expected range.
	 * 
	 * @param olderFeature	The older feature.
	 * @param newerFeature	The newer version of the feature.
	 */
	private void checkPluginVersionDelta(FeatureDescriptor olderFeature, FeatureDescriptor newerFeature) {
		
		List<PluginReference<VersionInfo>> newerPlugins = newerFeature.getProvidedPlugins();
		List<PluginReference<VersionInfo>> olderPlugins = olderFeature.getProvidedPlugins();

		VersionInfo newerFeatureVers = newerFeature.getTarget().getVersion();
		VersionInfo olderFeatureVers = olderFeature.getTarget().getVersion();
		
		// categorize the change between these two versions - major, minor, service pack, hotfix?
		int changeType = MAJOR_CHANGE;
		String changeStr = "major";
		if (olderFeatureVers.getMajorVersion() == newerFeatureVers.getMajorVersion()) {
			changeType = MINOR_CHANGE;
			changeStr = "minor";
			if (olderFeatureVers.getMinorVersion() == newerFeatureVers.getMinorVersion()) {
				changeType = SERVICE_PACK;
				changeStr = "service pack";
				int nextServicePack = olderFeatureVers.getPatchVersion();
				nextServicePack = nextHundred(nextServicePack);
				if (nextServicePack > newerFeatureVers.getPatchVersion() ) {
					changeType = HOTFIX;
					changeStr = "hotfix";
				}
			}
		}
		
		for (PluginReference<VersionInfo> newerPlugin : newerPlugins) {
			
			String pluginId = newerPlugin.getTarget().getTargetId();
			
			PluginReference<VersionInfo> olderPluginRef = findMatchingPlugin(olderPlugins, pluginId);
			
			if (olderPluginRef != null) {
				VersionInfo olderPluginVers = olderPluginRef.getTarget().getVersion();
				VersionInfo nextVersion = minimumExpectedNextVersion(
						changeType, olderPluginVers);
				
				VersionInfo currentVers = newerPlugin.getTarget().getVersion();
				boolean wrongRange = currentVers.compareTo(nextVersion) < 0;
				
				// now check whether the plugin version for the later version is outside
				// the range of the previous GA'd version.
				if (wrongRange) {
					String msg = MessageFormat.format("{0}: Was expecting a {1} version change\n  Instead found an old " +
							"version of plugin <{2}> with version {3} and a current version of {4}\n" +
							"  It may be appropriate to bump the version number of the plugin.\n",
							codeForFailOrWarn(m_failOnSkip), changeStr, pluginId, olderPluginVers.toString(),
							currentVers.toString());
					
					log(msg, getLogLevel(m_failOnSkip) );
					m_hadWarnings = true;
					if (m_failOnSkip) {
						m_needsToFail = true;
					}
				}
				else {
					log("  Checked plugin " + newerPlugin.getTarget() + " for version bump conformance - PASSED.", Project.MSG_VERBOSE);
				}
			}
			else {
				log("  Did not find plugin " + pluginId + " in the older version (" + olderFeatureVers + ") of feature.\n" +
					"  If this is a new plugin, you can disregard this message.\n" +
					"  If this is an existing plugin that has moved from a different feature, " +
					"please verify that the version number of the plugin follows best practices.", Project.MSG_WARN);
			}
		}
	}

	/**
	 * Loop through the available plugins to see which one matches the same target ID.
	 * 
	 * @param olderPlugins
	 * @param targetId
	 * @return
	 */
	private PluginReference<VersionInfo> findMatchingPlugin(
			List<PluginReference<VersionInfo>> olderPlugins, String targetId) {
		PluginReference<VersionInfo> pluginToCheck = null;
		for (PluginReference<VersionInfo> olderPlugin : olderPlugins) {
			if (olderPlugin.getTarget().getTargetId().equals(targetId) ) {
				pluginToCheck = olderPlugin;
				break;
			}
		}
		return pluginToCheck;
	}

	/**
	 * Given a particular type of change of version numbers, compute the
	 * next expected version number that we'll encounter.
	 * 
	 * @param changeType	MAJOR_CHANGE, MINOR_CHANGE, SERVICE_PACK, HOTFIX
	 * @param olderVers		The version that is the basis for the bump.
	 * 
	 * @return	The minimum expected change in the version number.
	 */
	private VersionInfo minimumExpectedNextVersion(int changeType,
			VersionInfo olderVers) {
		VersionInfo nextVersion = null;
		switch (changeType) {
		case MAJOR_CHANGE:
			nextVersion = new VersionInfo(olderVers.getMajorVersion(),
					nextHundred( olderVers.getMinorVersion() ), 0);
			break;
		case MINOR_CHANGE:
			nextVersion = olderVers.nextMinor();
			break;
		case SERVICE_PACK:
			nextVersion = new VersionInfo(olderVers.getMajorVersion(), olderVers.getMinorVersion(),
					nextHundred( olderVers.getPatchVersion() ) );
			break;
		case HOTFIX:
			nextVersion = olderVers.nextPatch();
			break;
		default:
			throw new BuildException("Unexpected case in switch statement.");
		}
		return nextVersion;
	}

	private int nextHundred(int versionPart) {
		return versionPart - (versionPart % 100) + 100;
	}

	private void warnOrFailForMissingEntry(Target<VersionInfo> target,
			List<Target<VersionInfo>> versions, int major, int minor,
			int patch, VersionInfo upperEnd, String rangeType) {
		VersionInfo matchVersion;
		Range<VersionInfo> previousPatch = new Range<VersionInfo>(
				new VersionInfo(major, minor, patch, null), true, upperEnd, false);
		
		matchVersion = anyInRange(versions, previousPatch);
		if (matchVersion == null) {
			
			StringBuffer buf = new StringBuffer();
			for (Target<VersionInfo> oneTarget : versions) {
				buf.append( oneTarget.getVersion().toString() );
				buf.append(" ");
			}
			String msg = MessageFormat.format("{0}: Feature {1} appears to be a {2}.\n" +
					"  Therefore expected to find a version in the range {3} on the GA site(s).\n" +
					"  However, the only available versions are:\n" +
					"  {4}\n" +
					"  The version bump appears to be unnecessary.\n", codeForFailOrWarn(m_failOnSkip),
					target.toString(), rangeType,
					previousPatch.toString(), buf.toString());
			
			log(msg, getLogLevel(m_failOnSkip) );
			
			if (m_failOnSkip) {
				m_needsToFail = true;
			}
			else {
				m_hadWarnings = true;
			}
		}
	}

	/**
	 * Get the logging level to use for this message.
	 * @param fail
	 * @return
	 */
	private int getLogLevel(boolean fail) {
		return fail ? Project.MSG_ERR : Project.MSG_WARN; 
	}
	
	private VersionInfo anyInRange(List<Target<VersionInfo>> versions,
			Range<VersionInfo> matchRange) {
		
		for (Target<VersionInfo> target : versions) {
			VersionInfo version = target.getVersion();
			if (matchRange.isInRange(version) ) {
				return version;
			}
		}
		
		return null;
	}


	/**
     * What are the URLs for the update sites?
     */
    private List<UrlList> m_updateSites = DataUtils.newList();

    /**
     * Do we fail if we have an exact match for a three part version #?
     */
	private boolean m_failOnMatch = true;
	
	/**
	 * Do we fail if we have apparently skipped a version #?
	 */
	private boolean m_failOnSkip = false;
	
	private boolean m_needsToFail = false;
	
	/**
	 * Where are files cached locally?
	 */
    private File m_localSiteCache;

    /**
     * Error message to show.
     */
    private String m_errorMessage = "";
    
    private boolean m_hadWarnings = false;
    
	private ResourceCollection m_featureResources;

	private CachingState m_cacheState = CachingState.LOCAL_AND_REMOTE;
	
}
