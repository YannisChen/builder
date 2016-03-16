package com.tibco.devtools.workspace.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.tibco.devtools.workspace.util.DataUtils;
import com.tibco.devtools.workspace.util.DomUtilities;

/**
 * Partial model of a feature, as parsed from feature.xml file.
 * 
 * <p>Note that this does not capture all of the data that you can find in a feature.xml
 * file, rather it is a subset that is interesting and useful to a tool trying to
 * use Eclipse update sites or some other similar form of install infrastructure.
 * </p>
 * 
 * <p>Note that a feature may have URL location, a File location, both, or
 * neither.
 * </p>
 */
public class FeatureDescriptor extends AbstractFeature<VersionInfo, FeatureDescriptor> {
	
	// ========================================================================
	//	Public methods
	// ========================================================================
	
	/**
	 * Construct a feature model...
	 */
	public FeatureDescriptor(Target<VersionInfo> target) {
		super(target);
	}

	/**
	 * Create a feature descriptor from a {@link File}.
	 * 
	 * @param featureFile	The file from which the feature description should be created.
	 * @return	The description.
	 * 
	 * @throws FileNotFoundException In the event that the feature cannot be found.
	 */
	public static FeatureDescriptor fromFile(File featureFile) throws FileNotFoundException {

    	FileInputStream fis = new FileInputStream(featureFile);
    	FeatureDescriptor descriptor = FeatureDescriptor.fromStream(fis);
    	descriptor.setFileLocation(featureFile);
    	
    	return descriptor;
	}
	
	public static Target<VersionInfo> targetFromElement(Element elem, String nameAttrName, String versionAttrName) {
		
		String featureId = DomUtilities.getAttributeValue(elem, nameAttrName, "");
		
		String version = DomUtilities.getAttributeValue(elem, versionAttrName, "0.0.0");
		VersionInfo vers = VersionInfo.parseVersion(version);
		
		return new Target<VersionInfo>(featureId, vers);
	}
	
	private static Target<VersionInfo> targetFromElementWithId(Element elem) {
		return targetFromElement(elem, "id", "version");
	}
	
	/**
	 * Create a feature descriptor from a stream.
	 * 
	 * @param stream
	 * @return
	 */
	public static FeatureDescriptor fromStream(InputStream stream) {
    	FeatureDescriptor featureModel;
    	DocumentBuilder db = DomUtilities.getNamespaceAwareDocumentBuilder();
    	Document doc;
		try {
			doc = db.parse(stream);
			featureModel = FeatureDescriptor.fromDocument(doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return featureModel;
	}
	
	/**
	 * Parse a feature descriptor from a DOM Document.
	 * 
	 * @see #fromElement(Element)
	 */
	public static FeatureDescriptor fromDocument(Document dom) {
		return fromElement(dom.getDocumentElement() );
	}
	
	/**
	 * Parse a feature model from a DOM element.
	 *  
	 * @param elem	The element corresponding to the "feature" element,
	 * 	as outlined in the <a href="http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/feature_manifest.html">
	 * 	feature manifest</a> spec.
	 * 
	 * @return The parsed version of the feature model.
	 */
	public static FeatureDescriptor fromElement(Element elem) {

		Target<VersionInfo> target = targetFromElementWithId(elem);
		FeatureDescriptor result = new FeatureDescriptor(target);
		
		Element required = DomUtilities.getNamedChild(elem, QN_REQUIRES, true);
		if (required != null) {
			result.getFeatureImports(required);
		}

		NodeList includesList = elem.getElementsByTagNameNS("", "includes");
		if (includesList != null && includesList.getLength() > 0) {
			result.getIncludes(includesList);
		}
		
		result.getPluginImports(elem);
		
		return result;
	}
	
	public String getSourcePathString() {
		return getTarget().toString() + " " + getLocationString();
	}
	
	@Override
	public String toString() {
		return getTarget().toString();
	}

	/**
	 * Get the file location of the model, if any.
	 * 
	 * @return	The File for the location of the model.
	 * 
	 * @see #getUrlLocation()
	 */
	public File getFileLocation() {
		return m_fileLocation;
	}
	
	/**
	 * Get the URL location of the model, if any.
	 * 
	 * @return The URL for the location of the model.
	 */
	public URL getUrlLocation() {
		return m_urlLocation;
	}
	
	/**
	 * Get the location of a feature description, if it is known.
	 * 
	 * @return A string representing where the feature descriptor can be found,
	 * 	if any location is known.
	 */
	public String getLocationString() {
		if (m_fileLocation != null) {
			return m_fileLocation.toString();
		}
		else if (m_urlLocation != null) {
			return m_urlLocation.toString();
		}
		else
			return "<<Unknown location>>";
	}

	/**
	 * Set the location of this feature model as a file.
	 * 
	 * <p>This also sets a URL location for the feature.</p>
	 * 
	 * @param loc The location.
	 */
	public void setFileLocation(File loc) {
		m_fileLocation = loc;
		try {
			m_urlLocation = loc.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Unable to turn file " + loc.toString() + " into a URL.");
		}
	}
	
	/**
	 * Set the location of the model as a URL.
	 */ 
	public void setUrlLocation(URL loc) {
		m_urlLocation = loc;
		m_fileLocation = null;
	}
	
	// ========================================================================
	//	Private methods
	// ========================================================================
	
	private void getIncludes(NodeList includes) {

		for (int nodeIdx = 0 ; nodeIdx < includes.getLength() ; nodeIdx++) {
			Element elem = (Element) includes.item(nodeIdx);
			
			String optionalStr = DomUtilities.getAttributeValue(elem, "optional", "false");
			boolean optional = "true".equals(optionalStr);
			String featureId = DomUtilities.getAttributeValue(elem, "id", "");
			String version = DomUtilities.getAttributeValue(elem, "version", "0.0.0");
			VersionInfo min = VersionInfo.parseVersion(version);

			Range<VersionInfo> range = new Range<VersionInfo>(min, true, min, true);
            TargetConstraint<VersionInfo, FeatureDescriptor> constraint
            	= new TargetConstraint<VersionInfo, FeatureDescriptor>(this, optional, featureId, range);
			getFeatureConstraints().add(constraint);
			
			// os, arch, ws, nl filters not supported for features.
		}
	}

	private void getPluginImports(Element elem) {
		
		for (Element pluginElem : DomUtilities.namedChildren(elem, "", "plugin")) {
			
			Target<VersionInfo> target = targetFromElementWithId(pluginElem);
			
			String os = DomUtilities.getAttributeValue(pluginElem, "os", "");
			String arch = DomUtilities.getAttributeValue(pluginElem, "arch", "");
			String windowSystem = DomUtilities.getAttributeValue(pluginElem, "ws", "");
			String unpackStr = DomUtilities.getAttributeValue(pluginElem, "unpack", "true");
			
			boolean unpack = "true".equals(unpackStr);
			
			PluginReference<VersionInfo> plugRef = new PluginReference<VersionInfo>(target);
			plugRef.setArch(arch);
			plugRef.setOs(os);
			plugRef.setWindowSystem(windowSystem);
			plugRef.setIsMeantToUnpack(unpack);
			
			getProvidedPlugins().add(plugRef);
		}
	}

	private void getFeatureImports(Element elem) {
		
		for (Element importElem : DomUtilities.namedChildren(elem, "", "import")) {
			
			Collection< TargetConstraint<VersionInfo, FeatureDescriptor > > constraintList;
			String targetId;
			if (importElem.hasAttribute("feature")) {
				targetId = DomUtilities.getAttributeValue(importElem, "feature", "");
				constraintList = getFeatureConstraints();
			}
			else if ( importElem.hasAttribute("plugin") ){
				targetId = DomUtilities.getAttributeValue(importElem, "plugin", "");
				constraintList = getPluginConstraints();
			}
			else {
				throw new IllegalArgumentException("Expecting either a feature or plugin requirement.");
			}
			String version = DomUtilities.getAttributeValue(importElem, "version", "0.0.0");
			VersionInfo min = VersionInfo.parseVersion(version);
			VersionInfo max = min;
			String matchCriteria = DomUtilities.getAttributeValue(importElem, "match", RANGE_GREATER_OR_EQUAL);
			
			Range<VersionInfo> range = null;
			if (matchCriteria.equals(RANGE_PERFECT) ) {
				range = new Range<VersionInfo>(min, true, max, true);
			}
			else if (matchCriteria.equals(RANGE_EQUIVALENT) ) {
				range = new Range<VersionInfo>(min, true, min.nextMinor(), false);
			}
			else if (matchCriteria.equals(RANGE_COMPATIBLE)) {
				range = new Range<VersionInfo>(min, true, min.nextMajor(), false);
			}
			else if (matchCriteria.equals(RANGE_GREATER_OR_EQUAL)) {
				range = new Range<VersionInfo>(min, true, VersionInfo.UNBOUNDED, true);
			}
			else {
				throw new IllegalArgumentException("Unrecognized match criteria " + matchCriteria);
			}
			
            TargetConstraint<VersionInfo, FeatureDescriptor > constraint
            	= new TargetConstraint<VersionInfo, FeatureDescriptor >(this, true, targetId, range);
			constraintList.add(constraint);
		}
		
	}
	
	// ========================================================================
	//	Private data
	// ========================================================================
	
	public static Map<Target<VersionInfo>, List<FeatureDescriptor>> createPluginToFeaturesMap(Iterable<FeatureDescriptor> featuresToScan) {
		Map<Target<VersionInfo>, List<FeatureDescriptor> > targetToFeature = DataUtils.newMap();
		for ( FeatureDescriptor feature : featuresToScan) {
			Collection<PluginReference<VersionInfo> > plugRefs = feature.getProvidedPlugins();
			// for each plugin, map to a list of features.
			for ( PluginReference<VersionInfo> plugRef : plugRefs) {
				Target<VersionInfo> target = plugRef.getTarget();
				List<FeatureDescriptor> features = DataUtils.getMapListValue(targetToFeature, target);
				features.add(feature);
			}
		}
		return targetToFeature;
	}

	private File m_fileLocation;
	
	private URL m_urlLocation;
	
    // ========================================================================
    //  Private constants
    // ========================================================================
    
    private static final String RANGE_PERFECT = "perfect";

    private static final String RANGE_GREATER_OR_EQUAL = "greaterOrEqual";

    private static final String RANGE_EQUIVALENT = "equivalent";

    private static final String RANGE_COMPATIBLE = "compatible";

	private static final QName QN_REQUIRES = new QName("", "requires");

}
