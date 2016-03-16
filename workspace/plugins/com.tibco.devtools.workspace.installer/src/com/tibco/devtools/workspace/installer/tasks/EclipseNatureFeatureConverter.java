package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.FeatureSource;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.ProgressReportToConsole;
import com.tibco.devtools.workspace.installer.standalone.SiteInformation;
import com.tibco.devtools.workspace.installer.standalone.UrlCache;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;
import com.tibco.devtools.workspace.util.DomUtilities;
import com.tibco.devtools.workspace.util.NodeListIterator;

/**
 * Ant task to take a feature.xml as input, and write a new one as output, translating
 * references to other features that have "dual" nature into their dual counterpart.
 * 
 * <p>For example, if a feature references com.tibco.some.feature, and that feature
 * has a dual nature (com.tibco.some.feature.eclipse) exists, then the feature.xml will
 * be re-written with the reference to the feature name with the ".eclipse" extension.
 * </p>
 *
 * <p>Attributes of this task include:
 * <ul>
 *   <li>inputfile - the source feature to maniplate</li>
 *   <li>outputfile - the transformed output</li>
 *   <li>extension - the "extension" to look for - our expected usage is for ".eclipse"</li>
 *   <li>featuremappingurl - a URL to a file that contains, one per line, the normal name,
 *   	space characters, and then the transformed name.  Likely we'll use this to map our
 *   	"TPCL" features to standard eclipse ones, when they overlap.</li>
 *   <li>caching - controls how caching is done</li>
 *   <li>localsitecache - folder where files will be cached when downloaded</li>
 * </ul>
 * 
 * And child elements include:
 * <ul>
 *   <li>dualbundles - a ResourceCollection reference to "MANIFEST.MF" format files - those
 *    bundles that are having their names mapped.</li>
 *   <li>updatesites - the update sites to scan.</li>
 * </ul>
 * </p>
 */
public class EclipseNatureFeatureConverter extends Task {

	public static final String DEFAULT_EXTENSION = ".eclipse";

	public void setInputFile(File input) {
		m_input = input;
	}
	
	public void setOutputFile(File output) {
		m_output = output;
	}
	
	public void setExtension(String extension) {
		m_extension = extension;
		if (!m_extension.startsWith(".")) {
			m_extension = "." + extension;
		}
	}
	
	public void setFeatureMappingURL(String url) {
		try {
			m_featureMappingURL = new URL(url);
		} catch (MalformedURLException e) {
			throw new BuildException(e);
		}
	}
	
	public void setCaching(String caching) {
    	m_cacheState = InstallTargets.cachingStateFromString(caching);
    }
	
	public void setBypasscheck(boolean bypass) {
		m_bypass = bypass;
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

    public ResourceCollection createDualBundles() {
    	if (m_bundles != null) {
    		throw new BuildException("Only one child element called 'dualbundles' allowed.");
    	}
    	m_bundles = new Resources();
    	
    	return m_bundles;
    }
     
    public void setLocalsitecache(File localSiteCache) {
        m_localSiteCache = AnyItem.checkNullVariable(localSiteCache);
    }
    
	@Override
	public void execute() throws BuildException {
		if (m_input == null || !m_input.isFile() ) {
			throw new BuildException("Must specify the 'input' attribute, and the file must exist.");
		}

		if (m_output == null ) {
			throw new BuildException("Must specify the 'output' attribute.");
		}
		
		if (m_localSiteCache == null ) {
			throw new BuildException("Must specify the 'localsitecache' attribute.");
		}
		
    	// get the sites.
        UrlCache cache = new UrlCache(m_localSiteCache, m_cacheState);

        ProgressReport reporter = new ProgressReportToConsole();

        try {
        	
			log("  local site cache: " + m_localSiteCache.toString(), Project.MSG_VERBOSE);
			
            SiteInformation siteInfo = InstallTargets.getSiteInformation(
    				reporter, m_updateSites, cache);
            
            FeatureDescriptor feature = FeatureDescriptor.fromFile(m_input);
            
    	    generateNewFeature(reporter, cache, siteInfo, feature);
        }
        catch (Exception ioe) {
			throw new BuildException(ioe);
        }
	}
	
    private void generateNewFeature(ProgressReport reporter, UrlCache cache, SiteInformation siteInfo,
			FeatureDescriptor feature) throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException, TransformerConfigurationException, TransformerException {
    	
    	Map<String, String> pluginNameMap = getBundleNameMaps();
    	Map<Target<VersionInfo>, List< Target<VersionInfo> > > targetMap = DataUtils.newMap();
    	
    	if (m_featureMappingURL != null) {
    		targetMap.putAll( InstallTargets.getKnownMappings(reporter, cache, m_featureMappingURL) );
    	}
    	
    	DocumentBuilder db = DomUtilities.getDocumentBuilder();
    	
    	Document doc = db.parse(m_input);
    	String newFeatureName = feature.getTarget().getTargetId() + m_extension;
    	mapDomToNewNature(siteInfo, doc, newFeatureName, targetMap, pluginNameMap, m_extension, m_bypass);
       	
    	FileOutputStream fos = new FileOutputStream(m_output);

       	DomUtilities.documentToResult(doc, fos);
	}

	/**
     * Does the mapping of a DOM to an Eclipse nature.
     * 
     * @param doc	The document to be mapped.
     * @param newFeatureName The new name for the feature.
     * @param featureNameMap	Mapping of feature name to feature name.
     * @param targetMap	Mapping of fully qualified targets to new fully qualified targets.
     * @param pluginNameMap	Mapping of plugin name to plugin name.
     * @param bypass bypass version match check.
     * 
     * @throws XPathExpressionException
     */
	public static void mapDomToNewNature(FeatureSource<VersionInfo, ?> sites, Document doc, String newFeatureName,
			Map<Target<VersionInfo>, List< Target<VersionInfo>> > targetMap,
			Map<String, String> pluginNameMap,
			String extension, boolean bypass) throws XPathExpressionException {
		Element featureElem = doc.getDocumentElement();
    	featureElem.setAttribute("id", newFeatureName);
    	
    	Map<String, String> featureNameMap = getMappingsFromSites(sites.getAvailableFeatureIds(), extension);
    	// get the set of all feature ids for which we have a target map
    	Set<String> targetMapFeatures = new HashSet<String>();
    	for (Target<VersionInfo> oneTarget : targetMap.keySet()) {
    		targetMapFeatures.add(oneTarget.getTargetId());
    	}
    	// loop through the feature "imports"
    	XPathFactory xpathfact = XPathFactory.newInstance();
    	XPath xpath = xpathfact.newXPath();
    	NodeList nodes = (NodeList) xpath.evaluate("requires/import", featureElem, XPathConstants.NODESET);
		for (Element importElem : new NodeListIterator(nodes)) {
    		String featureName = importElem.getAttribute("feature");
    		String versionStr = importElem.getAttribute("version");
    		VersionInfo version = VersionInfo.parseVersion(versionStr);
    		if (featureNameMap.containsKey(featureName)) {
    			if(bypass){
    				bypassModifySimpleFeatureNameMap(sites, featureNameMap, importElem, featureName, version);
    			}else{
    			    simpleFeatureNameMap(sites, featureNameMap, importElem, featureName, version);
    			}
    		}
    		else if (targetMapFeatures.contains(featureName)) {
    			if(bypass){
    				Target<VersionInfo> srcTarget = new Target<VersionInfo>(featureName, version);
    				List<Target<VersionInfo>> destTargets = targetMap.get(srcTarget);
    				if (destTargets == null) {// we will not modify feature.xml.
    					break;
    				}else{
    					substituteListOfKnownMappings(targetMap, importElem,featureName, version);
    				}
    			}else{
				    substituteListOfKnownMappings(targetMap, importElem,featureName, version);
    			}
    		}
       	}
       	
       	// loop through the plugin elements, and map plugins...
       	NodeList pluginList = (NodeList) xpath.evaluate("plugin", featureElem, XPathConstants.NODESET);
       	for (Element pluginElem : new NodeListIterator(pluginList)) {
       		String pluginId = pluginElem.getAttribute("id");
       		if (pluginNameMap.containsKey(pluginId)) {
       			pluginElem.setAttribute("id", pluginNameMap.get( pluginId ));
       		}
       	}
	}

	private static void substituteListOfKnownMappings(
			Map<Target<VersionInfo>, List<Target<VersionInfo>>> targetMap,
			Element importElem, String featureName, VersionInfo version) {
		Target<VersionInfo> srcTarget = new Target<VersionInfo>(featureName, version);
		List<Target<VersionInfo>> destTargets = targetMap.get(srcTarget);
		if (destTargets == null) {
			throw new BuildException("Have target to target mappings for feature " + featureName + ", but not for the specific version " + version);
		}
		Node parent = importElem.getParentNode();
		Node insertBeforeNode = importElem.getNextSibling();
		// first, determine text indenting, then delete the text node if there is one.
		String toIndent = "\n      ";
		Node prevSibling = importElem.getPreviousSibling(); 
		if (prevSibling.getNodeType() == Node.TEXT_NODE) {
			toIndent = ( (Text) prevSibling).getNodeValue();
			parent.removeChild(prevSibling);
		}
		String matchMode = importElem.getAttribute("match");
		// delete existing element.
		parent.removeChild(importElem);
		
		// loop through all the new replacements, and add them into the file.
		for (Target<VersionInfo> oneTarget : destTargets) {
			// with indentation, of course.
			Text indentTextNode = parent.getOwnerDocument().createTextNode(toIndent);
			parent.insertBefore(indentTextNode, insertBeforeNode);
			
			// now add the new feature constraint.
			Element newFeatureElem = parent.getOwnerDocument().createElement("import");
			newFeatureElem.setAttribute("feature", oneTarget.getTargetId());
			newFeatureElem.setAttribute("version", oneTarget.getVersion().toString() );
			if (matchMode != null && matchMode.length() > 0) {
				newFeatureElem.setAttribute("match", matchMode);
			}
			parent.insertBefore(newFeatureElem, insertBeforeNode);
		}
	}
	

	private static void simpleFeatureNameMap(
			FeatureSource<VersionInfo, ?> sites, Map<String, String> featureNameMap,
			Element importElem, String featureName, VersionInfo version) {
		
		String toName = featureNameMap.get(featureName);
		importElem.setAttribute("feature", toName);
		// now we need to make sure that there is an equivalent version
		// of the feature in the same range.
		Range<VersionInfo> validRange = new Range<VersionInfo>(version, true, version.nextPatch(), false);
		List<Target<VersionInfo>> targetVersionList = sites.getFeatureVersionSetById(toName);
		boolean foundMatch = false;
		for (Target<VersionInfo> anyMatch : targetVersionList) {
			if (validRange.isInRange(anyMatch.getVersion() )) {
				foundMatch = true;
				break;
			}
		}
		
		// If we didn't find a match, let the client know!
		if (!foundMatch) {
			Range<VersionInfo> allowedRange = new Range<VersionInfo>(version, true, VersionInfo.UNBOUNDED, false);
			//List<Target<VersionInfo>> fromVersionList = sites.getFeatureVersionSetById(featureName);
			List<Target<VersionInfo>> sortedTargetSubset = itemsInRange(allowedRange, targetVersionList);
			if (sortedTargetSubset.size() > 0) {
				VersionInfo bestGuess = sortedTargetSubset.get(0).getVersion();
				VersionInfo bestGuessTruncated = new VersionInfo(bestGuess.getMajorVersion(),
						bestGuess.getMinorVersion(), bestGuess.getPatchVersion());
				throw new BuildException("Attempt to map feature " + featureName + " to " + toName + " at version "
						+ version.toString(true) + " failed, as no match was found on the update sites.\n"
						+ "Almost certainly, you need to raise the minimum version of " + featureName
						+ " to correspond to the earliest available version of " + toName + "\n"
						+ "This appears to be " + bestGuessTruncated.toString(true) + "\n"
						+ "Available versions of " + toName + " include " + Target.joinVersions(sortedTargetSubset, ", ") + "\n"
					);
			}
			else {
				throw new BuildException("Attempt to map feature " + featureName + " to " + toName + " at version " +
						version.toString() + " failed, as no match was found on the update sites.\n"
						+ "There does not appear to be any version of " + toName + " in the range " + allowedRange.toString() + "\n");
			}
		}
	}
	
	/**
	 * Bypass modify feature.xml if we can not find match version.
	 * @param sites
	 * @param featureNameMap
	 * @param importElem
	 * @param featureName
	 * @param version
	 */
	private static void bypassModifySimpleFeatureNameMap(
			FeatureSource<VersionInfo, ?> sites, Map<String, String> featureNameMap,
			Element importElem, String featureName, VersionInfo version) {
		
		String toName = featureNameMap.get(featureName);
		
		// now we need to make sure that there is an equivalent version
		// of the feature in the same range.
		Range<VersionInfo> validRange = new Range<VersionInfo>(version, true, version.nextPatch(), false);
		List<Target<VersionInfo>> targetVersionList = sites.getFeatureVersionSetById(toName);
		boolean foundMatch = false;
		for (Target<VersionInfo> anyMatch : targetVersionList) {
			if (validRange.isInRange(anyMatch.getVersion() )) {
				importElem.setAttribute("feature", toName);
				foundMatch = true;
				break;
			}
		}
		// we will not modify feature.xml.
		if (!foundMatch) {
			return;
		}
	
	}

	private static List<Target<VersionInfo>> itemsInRange(Range<VersionInfo> range, List<Target<VersionInfo>> avail) {
		
		TreeSet<Target<VersionInfo>> subset = new TreeSet<Target<VersionInfo>>();
		for (Target<VersionInfo> target : avail) {
			if (range.isInRange(target.getVersion() )) {
				subset.add(target);
			}
		}
		
		List<Target<VersionInfo>> result = DataUtils.newList();
		result.addAll(subset);
		return result;
	}
	/**
	 * Collect a list of all features that have corresponding entries with
	 * ".eclipse" entries (well, extension entries, really
	 * 
	 * @param allFeatures	What features to check
	 * @param extension		What is the extension being used?
	 * 
	 * @return	A mapping of all names that have extensions to their extended name
	 */
    public static Map<String, String> getMappingsFromSites(
			Collection<String> allFeatures, String extension) {
    	 
    	Map<String, String> siteBasedMapping = DataUtils.newMap();
    	
    	Set<String> hashedFeatures = new HashSet<String>();
    	hashedFeatures.addAll( allFeatures );
    	
    	for (String featureName : hashedFeatures) {
    		if (!featureName.endsWith(extension)) {
        		String extendedName = featureName + extension;
        		if ( hashedFeatures.contains(extendedName) ) {
        			siteBasedMapping.put(featureName, extendedName);
        		}
    		}
    	}
    	
    	return siteBasedMapping;
	}

    /**
     * Loops through the bundles in the "bundles" resource collection, parses each,
     * and extracts the Bundle-SymbolicName attribute.
     * 
     * @return	A mapping from old name to new name.
     * @throws IOException
     */
	private Map<String, String> getBundleNameMaps() throws IOException {
    	
    	Map<String, String> nameMapping = DataUtils.newMap();
    	
    	Iterator<Resource> itRes = TaskUtils.safeIterator(m_bundles );
    	
    	while (itRes.hasNext()) {
    		Resource res = itRes.next();
    		String [] names = getBundleNames(res.getInputStream(), m_extension);
    		
    		String oldEntry = nameMapping.put(names[0], names[1]);
    		if (oldEntry != null) {
    			log("Found multiple bundles with the same name of " + oldEntry, Project.MSG_WARN);
    		}
    	}
    	
    	return nameMapping;
    }
	
	/**
	 * Get the two names for the bundle from a manifest stream.
	 * 
	 * @param manifestStream	A Stream representing a MANIFEST.MF file.
	 * @param extension		The extension in use.
	 * @return	Two strings, the "base" name, and the "extended name".
	 * @throws IOException
	 */
	public static String[] getBundleNames(InputStream manifestStream, String extension) throws IOException {
		Manifest mf = new Manifest( manifestStream );
		Attributes attribs = mf.getMainAttributes();
		
		// we need both an extended version of the bundle name, and an unextended one. 
		String bundleName = attribs.getValue("Bundle-SymbolicName");
		// make sure, if there is a singleton directive, that we trim that off.
		int semicolonIdx = bundleName.indexOf(";");
		if (semicolonIdx >= 0) {
			bundleName = bundleName.substring(0, semicolonIdx).trim();
		}
		String extendedName = null;
		// do we need to strip off the extension, or add it?
		if (bundleName.endsWith(extension)) {
			extendedName = bundleName;
			bundleName = bundleName.substring(0, bundleName.length() - extension.length() );
		}
		else {
			extendedName = bundleName + extension;
		}
		
		return new String[] { bundleName, extendedName };
	}
	
    private List<UrlList> m_updateSites = DataUtils.newList();
	
    private Resources m_bundles = null;
    
    private File m_localSiteCache = null;
    
	private URL m_featureMappingURL = null;
	private String m_extension = DEFAULT_EXTENSION;
	private File m_input;
	private File m_output;
	private boolean m_bypass;
	
	private CachingState m_cacheState = CachingState.LOCAL_AND_REMOTE;
	
}
