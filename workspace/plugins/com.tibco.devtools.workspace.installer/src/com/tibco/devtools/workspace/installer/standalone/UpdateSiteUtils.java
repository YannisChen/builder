package com.tibco.devtools.workspace.installer.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DomUtilities;

/**
 * Utility functions for working with update sites.
 * 
 */
public class UpdateSiteUtils {

	/**
	 * Normalize destination URLs in place in the given array.
	 * 
	 * @param destinations	The URLs to normalize
	 */
	public static void normalizeDestinations(URL[] destinations) {
	    for (int destIdx = 0 ; destIdx < destinations.length; destIdx++) {
	        URL oneDest = destinations[destIdx];
	        String path = oneDest.getPath();
	        if ( !path.endsWith("site.xml") ) {
	            if (!path.endsWith("/")) {
	                path = path + "/";
	            }
	            path = path + "site.xml";
	            try {
	                oneDest = new URL(oneDest, path);
	            } catch (MalformedURLException e) {
	                // this shouldn't happen, so we're eating it by turning it into a runtime exception.
	                throw new RuntimeException(e);
	            }
	            destinations[destIdx] = oneDest;
	        }
	    }
	}

	public static Document parseSiteDocument(InputStream bais,
			String sourceName) throws IOException {
		DocumentBuilder db = DomUtilities.getNamespaceAwareDocumentBuilder();
	    Document doc;
	    try {
			doc = db.parse(bais, sourceName );
	        Element docElem = doc.getDocumentElement();
	        if (! docElem.getNodeName().equals("site")) {
	            doc = null;
	        }
	        return doc;
	    } catch (SAXException e) {
	        // Intentionally ignoring this for now - return null.
	    }
	
	    return null;
	}

	/**
	 * An interface for processing the features of a site.xml file.
	 */
    public static interface SiteFeaturesHandler {
    	
    	void processFeature(Target<VersionInfo> target, URL url);
    }
    
    /**
     * Parse through a site.xml document, and for each feature entry, call back a handler to see what to
     * do with the entry.
     * 
     * @param doc
     * @param destination
     * @param handler
     * @throws MalformedURLException
     */
    public static void parseSiteXml(Document doc, URL destination, SiteFeaturesHandler handler) throws MalformedURLException {
        // traverse the DOM extracting what we care about.
        Element siteElem = doc.getDocumentElement();
        NodeList features = siteElem.getElementsByTagNameNS("", "feature");
        
        for (int nodeIdx = 0; nodeIdx < features.getLength() ; nodeIdx++) {
            Element featureElem = (Element) features.item(nodeIdx);

            String targetId = DomUtilities.getAttributeValue(featureElem, "id", "##missingAttribute##");
            String versStr = DomUtilities.getAttributeValue(featureElem, "version", "##missingVersion##");
            String isPatch = DomUtilities.getAttributeValue(featureElem, "patch", "false");
            String urlStr = DomUtilities.getAttributeValue(featureElem, "url", "##missingURL##");

            // For the moment, do not capture "patch" features, just full ones.
            if ("false".equals(isPatch)) {
                VersionInfo vers = VersionInfo.parseVersion(versStr);
                Target<VersionInfo> targ = new Target<VersionInfo>(targetId, vers);

                URL url = new URL(destination, urlStr);
                
                handler.processFeature(targ, url);
            }
        }
    }

	public static boolean isFilterMatch(String value, String toMatch) {
	    return (value == null || value.equals("") || value.equals(toMatch) );
	}

}
