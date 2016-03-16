package com.tibco.devtools.workspace.installer.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.util.DomUtilities;

/**
 * Utility functions for test purposes.
 */
public class DomTestUtilities {

	/**
	 * Utility function to get a DOM source
	 * @param clz
	 * @param path
	 * @return
	 */
	public static InputSource getInputSourceFromClasspath(Class<?> clz, String path) {
	    URL urlRes = clz.getResource(path);
	    InputStream inStream = clz.getResourceAsStream(path);
	
	    InputSource source = new InputSource(inStream);
	    source.setSystemId( urlRes.toString() );
	    return source;
	}

    /**
     * Get a DOM from a classpath resource.
     *
     * @param clz   The class for which the given path is relative.
     * @param path  The path.
     * @return The DOM Document.
     *
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document domFromClasspath(Class<?> clz, String path) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilder docBuilder = DomUtilities.getNamespaceAwareDocumentBuilder();

        InputSource inSource = getInputSourceFromClasspath(clz, path);
        return docBuilder.parse(inSource);
    }

}
