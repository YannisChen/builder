package com.tibco.devtools.workspace.installer.tasks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.types.resources.URLResource;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.ChoiceAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.ConstraintResult;
import com.tibco.devtools.workspace.installer.standalone.ConstraintSolver;
import com.tibco.devtools.workspace.installer.standalone.DefaultConstraintSource;
import com.tibco.devtools.workspace.installer.standalone.ExtensionLocation;
import com.tibco.devtools.workspace.installer.standalone.LatestAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.LatestBuildOfLeastMatchAlgorithm;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.ProgressReportToConsole;
import com.tibco.devtools.workspace.installer.standalone.SiteInformation;
import com.tibco.devtools.workspace.installer.standalone.SolverConstraintSource;
import com.tibco.devtools.workspace.installer.standalone.UrlCache;
import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Evaluates the set of inputs to determine all required dependencies.
 * 
 * <p>The output of the dependency evaluation is stashed in a reference stuck on the
 * project (which one is specified by the refname attribute).  This is of type
 * {@link TargetList}.
 * </p>
 */
public class EvaluateDependenciesTask extends Task {

	//=========================================================================
	// Task parameter setting.
	//=========================================================================

	/**
	 * Attribute "refname"
	 */
	public void setRefIdName(String refIdName) {
		m_refIdName = refIdName;
	}
	
	/**
	 * sub-element "featureSearchPath" - collects a path of folders to search for features.
	 */
    public void addFeatureSearchPath(Path featureSearchPath) {
    	if (m_featureSearchPath == null) {
    		m_featureSearchPath = new Path( getProject() );
    	}
        m_featureSearchPath.add(featureSearchPath);
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

    /**
     * Corresponds to the child element(s) "fixedfeaturepath"
     * @param baseEclipsePath
     */
    public void addFixedFeaturePath(Path fixedFeaturePath) {
    	m_fixedFeaturePath.add(fixedFeaturePath);
    }
    
    /**
     * Set the contingent constraints on evaluate dependencies, if there is any.
     * @param collection
     */
    public ResourceCollection createContingentConstraints() {
    	if (m_contingentConstraints != null) {
    		throw new BuildException("Only one contingentconstraints child element allowed.");
    	}
    	m_contingentConstraints = new Resources();
    	
    	return m_contingentConstraints;
    }
    
    public ResourceCollection createFeatures() {
    	if (m_features != null) {
    		throw new BuildException("Only one 'features' element allowed as a child element of 'evaluate.dependencies'.");
    	}
    	m_features = new Resources();
    	
    	return m_features;
    }
    
    /**
     * Corresponds to "baseeclipse" attribute.
     * 
     * This is an older method for setting a single fixed feature set folder.
     * 
     * @see #addFixedFeaturePath(Path)
     */
    public void setBaseEclipse(File baseEclipse) {
    	Path newPath = new Path(getProject(), baseEclipse.toString() );
    	m_fixedFeaturePath.add(newPath);
    }
    
    public void setCaching(String caching) {
    	m_cacheState = InstallTargets.cachingStateFromString(caching);
    }
    
	public void setAlgorithm(String algorithm) {
		m_algorithm = algorithm;
	}
	
    public void setLocalsitecache(File localSiteCache) {
        m_localSiteCache = AnyItem.checkNullVariable(localSiteCache);
    }

	//=========================================================================
	// Task overrides
	//=========================================================================
	
	@Override
	public void init() throws BuildException {
		super.init();
		m_featureSearchPath = null;
		m_fixedFeaturePath = new Path( getProject() );
		
	}

	@Override
	public void execute() throws BuildException {

        validate();

        ProgressReport reporter = new ProgressReportToConsole();

        ChoiceAlgorithm<VersionInfo> algorithm = pickAlgorithm();
        
        try {
        	// get the sites.
            UrlCache cache = new UrlCache(m_localSiteCache, m_cacheState);

            SiteInformation siteInfo = InstallTargets.getSiteInformation(
					reporter, m_updateSites, cache);
			
			// get the starting features.
			List<FeatureDescriptor> starting = getStartingContributions();
			log ("Using the following starting features:", Project.MSG_VERBOSE);
			logFeatures(starting);
			
			List<FeatureDescriptor> fixedFeatures = getFixedFeatures();
			if (fixedFeatures.size() > 0) {
				log("Using the following fixed features:", Project.MSG_VERBOSE);
				logFeatures(fixedFeatures);
			}
			
			log("Evaluate dependencies configuration:", Project.MSG_VERBOSE);
			log("  local site cache: " + m_localSiteCache.toString(), Project.MSG_VERBOSE);
			log("       cache state: " + m_cacheState, Project.MSG_VERBOSE);
			log("         algorithm: " + m_algorithm, Project.MSG_VERBOSE);
			
	        ConstraintSolver<VersionInfo, FeatureDescriptor> solver =
	        	ConstraintSolver.create(siteInfo, starting, fixedFeatures, algorithm);

	        // route logging through this task. 
	        solver.setLogger( new AntTaskOutputLogger<ConstraintResult, String>(this) );
	        
	        // add the contingent constraints that we might have.
	        solver.addContingentConstraints( getContingentConstraints(reporter, cache) );
	        
	        List< Target<VersionInfo> > results = solver.searchAndWarn();
	        
	        if (results == null) {
	            // nope - failure - ditch.
	            throw new BuildException("Resolution failed.");
	        }

			TargetList targetList = new TargetList(results);
			getProject().addReference(m_refIdName, targetList);
			
			// now for verbose logging - output the features picked.
			log("Results evaluated to:", Project.MSG_VERBOSE);
			for (Target<VersionInfo> target: results) {
				log( "  " + target.toString(), Project.MSG_VERBOSE );
			}
			
		} catch (IOException e) {
			throw new BuildException(e);
		} catch (SAXException e) {
			throw new BuildException(e);
		}
        
	}

    //=========================================================================
    // Private methods
    //=========================================================================
    
	/**
	 * For debug logging purposes, output the features that we're using as constraints.
	 */
	private void logFeatures(List<FeatureDescriptor> starting) {
		for (FeatureDescriptor fd : starting) {
			log("  " + fd.getTarget().toString(), Project.MSG_VERBOSE);
			log("    from " + fd.getLocationString(), Project.MSG_VERBOSE);
		}
	}

	private List<FeatureDescriptor> getFixedFeatures() throws IOException {
		
		List<File> extensionLocDirs = TaskUtils.extractNormalizedDirs(this, m_fixedFeaturePath);
		List<FeatureDescriptor> fixedFeatures = DataUtils.newList();
		
		for (File oneDir : extensionLocDirs) {
			ExtensionLocation extLoc = new ExtensionLocation(oneDir);
			List<FeatureDescriptor> features = extLoc.getFeatures();
			FeatureDescriptor featureMasteringNoFeaturesPlugin = extLoc.discoverPluginsWithoutFeature();
			if (featureMasteringNoFeaturesPlugin.getProvidedPlugins().size() > 0) {
				features = new ArrayList<FeatureDescriptor>(features);
				features.add(featureMasteringNoFeaturesPlugin);
				features = Collections.unmodifiableList(features);
			}
			fixedFeatures.addAll(features);
		}
		
		return fixedFeatures;
	}
	
	/**
	 * Get the contingent constraints passed to the task...
	 * 
	 * @param progress	Report what progress we're making.
	 * @param cache	Use this cache for URL fetches.
	 * 
	 * @return
	 * @throws IOException
	 */
	private List<TargetConstraint<VersionInfo, SolverConstraintSource>>
	getContingentConstraints(ProgressReport progress, UrlCache cache)
	throws IOException {
		
		List<TargetConstraint<VersionInfo, SolverConstraintSource>> result = DataUtils.newList();
		
		// do we have constraints?  If it is not null and either:
		//  is not a reference
		//  or the reference target exists....
		if (m_contingentConstraints != null &&
			(!m_contingentConstraints.isReference() ||
			getProject().getReference(m_contingentConstraints.getRefid().getRefId() ) != null)) {
			
			// now loop through the references.
			Iterator<Resource> itRes = TaskUtils.safeIterator(m_contingentConstraints);
			
			// log the source of contingent constraints.
			log("  Using contingent constraints:", Project.MSG_VERBOSE);
			
			while (itRes.hasNext()) {
				Resource genericRes = itRes.next();
				if (genericRes instanceof StringResource) {
					log("   string source: " + genericRes.getLocation().toString(), Project.MSG_VERBOSE);
					result.addAll( stringResourceToConstraints((StringResource) genericRes) );
				}
				else {
					log("          source: " + genericRes.toString(), Project.MSG_VERBOSE );
					result.addAll( otherResourceToConstraints( progress, genericRes, cache) );
				}
			}
			
			// TODO - possibly log contingent constraints in debug mode?
		}
		return result;
	}

	private Collection<TargetConstraint<VersionInfo, SolverConstraintSource>>
	otherResourceToConstraints(ProgressReport progress, Resource genericRes, UrlCache cache) throws IOException {
		
		Collection<TargetConstraint<VersionInfo, SolverConstraintSource>> results = null;
		
		InputStream source = null;
		// note that we specifically call out URL resources here so that we can
		// employ our caching mechanism, instead of using the mechansims that Ant
		// might employ, which are not tuned to this use-case.
		if (genericRes instanceof URLResource) {
			URLResource urlRes = (URLResource) genericRes;
			UrlCache.SaveFilter<InputStream> filter = new UrlCache.SaveFilter<InputStream>() {
				public InputStream preAction(InputStream stream, CachingState mode,
						boolean isExistent) throws IOException {
					//do nothing, just return
					return stream;
				}
			};
			source = cache.getUrl(progress, urlRes.getURL(), false, filter);
		}
		else {
			source = genericRes.getInputStream();
		}
		
		// Just making up a default size for the buffer, here.
		byte[] contents = FileUtils.transferStreamToBytes(source, 1024);
		
		// now that I've got the resource as bytes, I want to know whether I should treat it
		// as a feature XML file, or as a text file.  How to know?  Check to see whether it
		// starts with XML's prefix "<?xml"...
		if (FileUtils.hasXmlStartingBytes(contents)) {
			// now we treat as XML.
			results = constraintsFromFeature("ant-string-resource", genericRes.toString(), contents);
		}
		else {
			// Assume this is a string using the default charset.
			String strContents = new String(contents, Charset.defaultCharset().name() );
			results = constraintsFromString("ant-string-resource", genericRes.toString(), strContents);
		}
		
		return results;
	}

	/**
	 * Get constraints from a feature....
	 * @param logicalSourceId The logical identifier for the source of these constraints.
	 * @param description	The description of where the contents came from.
	 * @param contents		The contents of the feature.
	 * 
	 * @return A new set of constraints...
	 */
	private Collection<TargetConstraint<VersionInfo, SolverConstraintSource>> constraintsFromFeature(
			String logicalSourceId, String description, byte[] contents) {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(contents);
		FeatureDescriptor feature = FeatureDescriptor.fromStream(bais);
		if (feature.getProvidedPlugins().size() > 0) {
			throw new BuildException("Feature descriptor at " + description + " is being used " +
					"for contingent constraints, but it provides plugins.");
		}
		if (feature.getPluginConstraints().size() > 0) {
			throw new BuildException("Feature descriptor at " + description + " is being used " +
				"for contingent constraints, but it has plugin constraints.");
		}
		
		List<TargetConstraint<VersionInfo, SolverConstraintSource>> results = DataUtils.newList();
		
		// go through and "launder" all the constraints so that they're treated as
		// generic constraints from a file, rather than feature constraints.
		DefaultConstraintSource dcs = new DefaultConstraintSource( logicalSourceId, description );
		for (TargetConstraint<VersionInfo, ?> constraint : feature.getFeatureConstraints() ) {
			TargetConstraint<VersionInfo, SolverConstraintSource> replacement = 
				new TargetConstraint<VersionInfo, SolverConstraintSource>(dcs, true,
						constraint.getTargetName(), constraint.getRange());
			results.add(replacement);
		}
		return results;
	}

	/**
	 * Parse a string resource into a set of constraints.
	 * 
	 * @param strRes	The string resource to parse.
	 * @return	A set of constraints - won't be null, but might be empty.
	 * 
	 * @throws IOException	Thrown when there's a problem reading the file.
	 */
	private Collection<TargetConstraint<VersionInfo, SolverConstraintSource>>
	stringResourceToConstraints( StringResource strRes) throws IOException {
		
		Location loc = strRes.getLocation();
		String strLoc = loc.getFileName() + ":" + loc.getLineNumber();
		return constraintsFromString("string-resource:" + strLoc, "Ant string resource at " + strLoc, strRes.getValue());
	}

	/**
	 * Turn a string into a set of constraints.
	 * @param logicalId Logical identifier for source of constraints.
	 * @param locationDescription	The description of where these constraints came from.
	 * @param strContents	The contents of the file containing constraints.
	 * 
	 * @return A new set of constraints.
	 * 
	 * @throws IOException
	 */
	private Collection<TargetConstraint<VersionInfo, SolverConstraintSource>> constraintsFromString(
			String logicalId, String locationDescription, String strContents) throws IOException {
		
		StringReader strReader = new StringReader(strContents);
		
		SolverConstraintSource source = new DefaultConstraintSource(logicalId, locationDescription);
		return ParsingUtils.readerIntoConstraintsList(source, strReader);
	}
	
	/**
	 * Get the starting features from the featuresearchpath member
	 */
    private List<FeatureDescriptor> getStartingContributions() throws IOException, SAXException {

    	ResourceCollection resources = m_features;
    	if (m_featureSearchPath != null) {
    		resources = FindFeaturesTask.getFeatureResourceCollection(this, m_featureSearchPath);
    	}
    	
	    return TaskUtils.resourceCollectionToFeaturesList(this, resources);
    }

	private void validate() {
		if (m_localSiteCache == null) {
			throw new BuildException("Must set localsitecache attribute.", getLocation() );
		}
		
		if (m_refIdName == null) {
			throw new BuildException("Must set refidname attribute for the results.");
		}

		// "features", and "featuresearchpath" are mutually exclusive.
		if (m_features != null && m_featureSearchPath != null) {
			throw new BuildException("May only specify one of 'featuresearchpath' or 'features' child element.");
		}
		
		// but at least one of them must be specified.
		if (m_features == null && m_featureSearchPath == null) {
			throw new BuildException("Must specify one of 'features' or 'featuresearchpath' child elements.");
		}
	}

	private ChoiceAlgorithm<VersionInfo> pickAlgorithm() {
		
		ChoiceAlgorithm<VersionInfo> result = null;
		if (m_algorithm.equalsIgnoreCase("lblm") ) {
			result = new LatestBuildOfLeastMatchAlgorithm<VersionInfo>();
		}
		else if (m_algorithm.equalsIgnoreCase("latest")) {
			result = new LatestAlgorithm<VersionInfo>();
		}
		else {
			throw new BuildException("Unrecognized algorithm name " + m_algorithm);
		}
		
		return result;
	}
	
    //=========================================================================
    // Member data
    //=========================================================================
    
	/**
	 * What features are we starting with.
	 */
	private Resources m_features;
	
	/**
	 * What is the path for searching for features?
	 */
	private Path m_featureSearchPath;
	
	/**
	 * What is the path for fixed features (normally, a "base" eclipse version)
	 */
	private Path m_fixedFeaturePath;
	
	/**
	 * Where are files cached locally?
	 */
    private File m_localSiteCache;

    /**
     * What are the URLs for the update sites?
     */
    private List<UrlList> m_updateSites = DataUtils.newList();

    /**
     * What is our caching mode - both, local, or remote
     */
    private CachingState m_cacheState = CachingState.LOCAL_AND_REMOTE;
    
    /**
     * What algorithm are we using for matching?
     */
    private String m_algorithm = "lblm";
    
    /**
     * The name by which the output of this task can be referenced.
     */
    private String m_refIdName;
    
    /**
     * The contingent constraints are captured in this resource set.
     */
    private Resources m_contingentConstraints = null;
}
