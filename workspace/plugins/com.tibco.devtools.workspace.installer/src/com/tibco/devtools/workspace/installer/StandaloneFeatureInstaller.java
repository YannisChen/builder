package com.tibco.devtools.workspace.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.standalone.CachedSiteInformation;
import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.ChoiceAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.ConstraintSolver;
import com.tibco.devtools.workspace.installer.standalone.ExtensionLocation;
import com.tibco.devtools.workspace.installer.standalone.FilePlacer;
import com.tibco.devtools.workspace.installer.standalone.FileSystemPlacer;
import com.tibco.devtools.workspace.installer.standalone.LatestAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.LatestBuildOfLeastMatchAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.SiteInformation;
import com.tibco.devtools.workspace.installer.standalone.TargetPlatformData;
import com.tibco.devtools.workspace.installer.standalone.UrlCache;
import com.tibco.devtools.workspace.installer.utils.FeatureUtils;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Does a stand-alone creation of a features, without using
 * Eclipse infrastructure.
 */
public class StandaloneFeatureInstaller {

    /**
     * Set up a stand-alone feature installation.
     *
     * @param baseEclipseInstall Do not download or install features found in the "base" eclipse installation.
     * @param config What configuration options should this use.
     * @param targetExtensionLoc What is the target folder to install features in?
     * @param targetPlatform What platforms should the install filter for?
     */
    public StandaloneFeatureInstaller(File baseEclipseInstall,
            WorkspaceInstallConfig config, File targetExtensionLoc,
            TargetPlatformData targetPlatform,
            CachingState cacheState) {
        m_config = config;
        m_cacheState = cacheState;
        if (baseEclipseInstall != null) {
            m_baseEclipseExtensions = new ExtensionLocation(baseEclipseInstall);
        }
        m_targetExtensionLoc = new ExtensionLocation(targetExtensionLoc);
        m_targetPlatform = targetPlatform;
    }

    /**
     * Filter a list of URLs, removing all of the "remote" URLs, if the
     * "useRemoteUrls" flag is false.
     *
     * @param urls    The list of URLs to filter.  This list is modified directly.
     * @param useRemoteUrls    If <code>false</code>, URLs will be removed.
     */
    public static void filterRemoteUrls(List<URL> urls, boolean useRemoteUrls) {
        if (!useRemoteUrls) {
            Iterator<URL> itSites = urls.iterator();
            while (itSites.hasNext()) {
                URL oneSite = itSites.next();
                // if the protocol is NOT file, remove it from the list.
                if ( oneSite.getProtocol().compareToIgnoreCase("file") != 0 ) {
                    itSites.remove();
                }
            }
        }
    }

    public void setKnownDescriptorList(List<FeatureDescriptor> knownDescriptors) {
        m_knownDescriptorList = knownDescriptors;
    }

    public void setSiteInformation(SiteInformation siteInfo) {
        m_siteInfo = siteInfo;
    }

    public boolean run(ProgressReport reporter) {

        boolean success = false;
        // Get all the features to install.
        try {
            createSiteScanner(reporter);

            m_siteInfo.getAvailableFeatures();

            List<FeatureDescriptor> startingFeatures = DataUtils.newList();

            // does this stand-alone install request use a known list of features,
            // or scanning for the features to satisfy?
            if (m_knownDescriptorList == null) {
                reporter.message("Scanning for features to import.");
                getAllFeatureLists();

                startingFeatures.addAll(m_buildFeatures);
                startingFeatures.addAll(m_otherFeatures);
                startingFeatures.addAll(m_testFeatures);
            }
            else {
                startingFeatures.addAll(m_knownDescriptorList);
            }

            populateExtensionLocation(reporter, startingFeatures, m_targetExtensionLoc);

            success = true;
        } catch (InternalException e) {
            // do nothing here. Message already reported.
        } catch (Exception e) {
            e.printStackTrace();
        }

        return success;
    }

    public List<FeatureDescriptor> getBuildFeatures() throws IOException, SAXException {
	
	    return FeatureUtils.getBuildFeatures(m_config.getFeatureSearchPath());
	}

	/**
	 * Sets up to scan the available update sites...
	 * @param localCacheSite Path to the local cache folder.
	 * @param updateSites List of update sites.
	 * @param reporter Output messages get sent here.
	 * @param cacheState How should we use the cache?
	 * @return A SiteInformation that includes the local cache site.
	 * @throws MalformedURLException
	 */
	public static SiteInformation createUpdateSiteScanner(
			UrlCache cache, List<URL> updateSites, ProgressReport reporter) throws MalformedURLException {
	
	    // now add the URLs for the workspace definition.
	    List<URL> toScan = DataUtils.newList();
	    toScan.addAll(updateSites);
	
	    URL toScanArray[] = new URL[toScan.size() ];
	    toScan.toArray(toScanArray);
	    return new CachedSiteInformation(cache, toScanArray, reporter);
	    //return new EclipseUpdateSiteInfo(localCacheSite, toScanArray, reporter);
	}

	/**
     * Populate an extension location with a set of features.
     *
     * @param reporter Status reported here.
     * @param buildTargets    What are the build targets that we want to exclude from the request?
     * @param targetExtensionLocation    Where should those features be installed?
     * @param baseConfiguration    What base configuration, if any, should be excluded from the request?
     * @throws IOException
     */
    private Collection< Target<VersionInfo> > populateExtensionLocation(ProgressReport reporter,
            List<FeatureDescriptor> buildTargets,
            ExtensionLocation targetExtensionLocation) throws IOException {

        // pick the choice algorithm to use....
        ChoiceAlgorithm<VersionInfo> algorithm = m_config.isMatchingLatest() ?
                new LatestAlgorithm<VersionInfo>() : new LatestBuildOfLeastMatchAlgorithm<VersionInfo>();

        // get the fixed features from base Eclipse install.
        List<FeatureDescriptor> fixedFeatures = null;
        if (m_baseEclipseExtensions != null) {
            fixedFeatures = m_baseEclipseExtensions.getFeatures();
        }
        
        Collection< Target<VersionInfo> > toInstall = chooseFeatures(m_siteInfo,
        		fixedFeatures, buildTargets, algorithm);

        installFiles(reporter, m_siteInfo, targetExtensionLocation, m_targetPlatform, toInstall, true);

        return toInstall;
    }

    /**
     * Does the work of installing the files I want into the target destination.
     * 
     * @param reporter
     * @param targetExtensionLocation
     * @param toInstall
     * @throws FileNotFoundException
     */
    public static void installFiles(ProgressReport reporter, SiteInformation siteInfo,
    		ExtensionLocation targetExtensionLocation,
    		TargetPlatformData targetPlatform,
    		Collection< Target<VersionInfo> > toInstall, boolean removeOtherVersionsOfFeature) throws IOException {

    	// now, remove all of the features currently installed from the download list, and uninstall
    	// based upon the value of removeOtherVersionsOfFeature, it either prunes or keeps the mismatched versions.

    	pruneAlreadyInstalledItems(reporter, targetExtensionLocation, toInstall,removeOtherVersionsOfFeature);

    	if (toInstall.size() > 0) {

    		reporter.message("Installing " + toInstall.size() + " features to "
    				+ targetExtensionLocation.getLocation().toString() );
    		FilePlacer placer = new FileSystemPlacer( targetExtensionLocation.getLocation() );
    		siteInfo.installTargetSet(reporter, placer, toInstall, targetPlatform);


    	}
    	else {
    		reporter.message("All required features already installed.");
    	}
    }
	
	
    /**
     * Look for features already installed, and make sure that only one version of each feature
     * ends up in the extension location, and don't bother to download features we already have.
     * @param reporter Where should messages be reported?
     * @param toInstall    The set of features to install.
     * @param removeOtherVersionsOfFeature This provides option whether to remove the conflicting versions or not.
     * @throws IOException 
     */
    private static void pruneAlreadyInstalledItems(ProgressReport reporter,
    		ExtensionLocation targetLocation,
    		Collection< Target<VersionInfo> > toInstall, boolean removeOtherVersionsOfFeature) throws IOException {
    	
        List<FeatureDescriptor> featuresAtTarget = targetLocation.getFeatures();
        // map all existing features by name.
        Map<String, FeatureDescriptor> nameToFeatureAtTarget = DataUtils.newMap();
        for (FeatureDescriptor feature : featuresAtTarget) {
            nameToFeatureAtTarget.put(feature.getTarget().getTargetId(), feature);
        }

        // for each one we intend to install, see if there is a feature by that name already?
        Iterator< Target<VersionInfo> > toInstallIter = toInstall.iterator();
        while ( toInstallIter.hasNext() ) {
        	Target<VersionInfo> targetToInstall = toInstallIter.next();
            FeatureDescriptor matchingFeature = nameToFeatureAtTarget.get(targetToInstall.getTargetId() );
            if (matchingFeature != null) {
                // there is a feature with that name, either it is the same version (don't redownload)
                if (matchingFeature.getTarget().getVersion().equals(targetToInstall.getVersion()) ) {
                    toInstallIter.remove();
                }
                // if removeOtherVersionsOfFeature then remove the conflicting version else keep both of them
                else if(removeOtherVersionsOfFeature){
                    // or uninstall the existing one.
                    reporter.message("Removing feature " + matchingFeature.getTarget().toString()
                            + " so that it can be replaced by " + targetToInstall.toString() );
                    targetLocation.removeFeature(matchingFeature);
                }
            }
        }
    }

    public static Collection< Target<VersionInfo> > chooseFeatures(SiteInformation siteInfo,
    		Collection<FeatureDescriptor> fixedFeatures, List<FeatureDescriptor> buildTargets,
    		ChoiceAlgorithm<VersionInfo> algorithm) {

        // go off and solve the constraints.
        ConstraintSolver<VersionInfo, FeatureDescriptor> solver;

        solver = ConstraintSolver.create(siteInfo, buildTargets, fixedFeatures, algorithm);

        List< Target<VersionInfo> > results = solver.searchAndWarn();
        
        if (results == null) {
            // nope - failure - ditch.
            throw new InternalException("Resolution failed.");
        }

        return results;
    }

    private void createSiteScanner(ProgressReport reporter) throws MalformedURLException {
        if (m_siteInfo == null) {
    	    UrlCache cache = new UrlCache(m_config.getLocalSiteCache(), m_cacheState);
            m_siteInfo = createUpdateSiteScanner(cache, m_config.getUpdateSites(), reporter);
        }
    }

    /**
     * Get all the features in the folders on the search path.
     *
     * @throws IOException
     * @throws SAXException
     */
    protected void getAllFeatureLists() throws IOException, SAXException {

        m_buildFeatures = getBuildFeatures();
        m_testFeatures = FeatureUtils.getLocatedFeatureList(m_config.getTestFeatures());
        m_otherFeatures = FeatureUtils.getLocatedFeatureList(m_config.getOtherFeatures());

    }

    //==============================================================================
    //    Member data
    //==============================================================================

    //=============================================================================
    // Member data
    //=============================================================================

    private SiteInformation m_siteInfo;

    private ExtensionLocation m_baseEclipseExtensions;

    private ExtensionLocation m_targetExtensionLoc;

    private TargetPlatformData m_targetPlatform;

    private List<FeatureDescriptor> m_knownDescriptorList;

    private List<FeatureDescriptor> m_buildFeatures;

    private List<FeatureDescriptor> m_testFeatures;

    private List<FeatureDescriptor> m_otherFeatures;

    private WorkspaceInstallConfig m_config;
    
    private CachingState m_cacheState;

}
