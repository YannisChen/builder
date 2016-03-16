package com.tibco.devtools.workspace.util;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * A bunch of static functions useful for extracting information from a DOM.
 *
 * <p>These functions are meant to be particularly fast or sneaky, just meant to
 * work with basic DOM, and avoid coupling with other libraries.</p>
 * @author eric
 *
 */
public class DomUtilities {

	/**
	 * Utility method to iterate the named children of an element.
	 * 
	 * @param parent	The parent element.
	 * @param namespace	
	 * @param localName
	 * @return
	 */
	public static Iterable<Element> namedChildren(Element parent, String namespace, String localName) {
		NodeList nodes = parent.getElementsByTagNameNS(namespace, localName);
		
		return new NodeListIterator(nodes);
	}
	
	public static List<String> getContentListForNamedChild(Element elem, QName name) {
        List<String> result = new ArrayList<String>();

        NodeList nodes = elem.getElementsByTagNameNS(name.getNamespaceURI(), name.getLocalPart() );
        for (int idx = 0 ; idx < nodes.getLength() ; idx++) {
            Element child = (Element) nodes.item(idx);
            result.add(getElementTextContents(child));
        }

        return result;
    }

    /**
     * Get a particular named child of the given element, but only returns a non-null result if there is
     * precisely one.
     *
     * @param elem  The parent element.
     * @param name  The name for the child element.
     * @param optional Is the element optional, or must it be found?
     * 
     * @return The element that matches, if any.  If optional is "false", then this will not be null.
     */
    public static Element getNamedChild(Element elem, QName name, boolean optional) {
        NodeList nodes = elem.getElementsByTagNameNS(name.getNamespaceURI(), name.getLocalPart() );
        if (!optional && nodes.getLength() != 1) {
            throw new IllegalArgumentException("Unable to find precisely one element " + name.getLocalPart());
        }

        return nodes.getLength() == 1 ? (Element) nodes.item(0) : null;
    }

    public static String getNamedChildContents(Element parent, QName childName, boolean optional) {

        Element child = getNamedChild(parent, childName, optional);
        return child != null ? getElementTextContents(child) : null;
    }

    /**
     * Get the contents of an element, namely all of the text elements aggregated together.
     *
     * @param elem  The element for which the text contents are desired.
     *
     * @return The String representing the text contents.
     */
    public static String getElementTextContents(Element elem) {
        NodeList children = elem.getChildNodes();
        StringBuffer buf = new StringBuffer();
        for (int idx = 0 ; idx < children.getLength() ; idx++) {
            Node childNode = children.item(idx);
            int nodeType = childNode.getNodeType();
            if (nodeType != Node.TEXT_NODE ) {
                throw new IllegalArgumentException("Unexpected node found as child of " + nodeToPath(elem) );
            }
            buf.append(childNode.getNodeValue() );
        }

        return buf.toString();
    }

    public static StringBuffer nodeToPath(StringBuffer buff, Node node) {
        if (buff == null) {
            buff = new StringBuffer();
        }
        int nodeType = node.getNodeType();
        if (nodeType == Node.ATTRIBUTE_NODE) {
            buff.insert(0, node.getNodeName());
            buff.insert(0, "@");
        }
        else if (nodeType == Node.ELEMENT_NODE) {
            if (buff.length() > 0) {
                buff.insert(0, "/");
            }
            buff.insert(0, node.getNodeName() );
        }
        else if (nodeType == Node.DOCUMENT_NODE || nodeType == Node.DOCUMENT_FRAGMENT_NODE) {
            buff.insert(0, "/");
        }

        Node parent = node.getParentNode();
        if (parent != null) {
            nodeToPath(buff, parent);
        }

        return buff;
    }

    /**
     * Get the path to a node for debugging and exception purposes.
     *
     * @param node  The node to which a path is desired.
     *
     * @return The path.
     */
    public static String nodeToPath(Node node) {
        return nodeToPath(null, node).toString();
    }

    /**
     * Get a namespace aware document builder.
     *
     * @return
     * @throws ParserConfigurationException
     */
    public static DocumentBuilder getNamespaceAwareDocumentBuilder() {
    	
        DocumentBuilder db;
		try {
			db = sm_docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// If we cannot get a parser, we're deeply hosed.
			throw new RuntimeException(e);
		}
        return db;
    }

    /**
     * Get a default value for an attribute.
     *
     * @param elem
     * @param attrName
     * @param defaultVal
     * @return
     */
    public static String getAttributeValue(Element elem, String attrName, String defaultVal) {
        Attr attr = elem.getAttributeNode(attrName);
        String value = attr != null ? attr.getValue() : defaultVal;

        return value;
    }

    private static DocumentBuilderFactory sm_docBuilderFactory;
    
    static {
    	sm_docBuilderFactory = DocumentBuilderFactory.newInstance();
    	sm_docBuilderFactory.setNamespaceAware(true);
    }

	public static DocumentBuilder getDocumentBuilder()
			throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		return dbf.newDocumentBuilder();
	}

	/**
	 * Write a DOM to a result - this effectively does a "null" transform conversion
	 * to an output stream or writer.
	 * 
	 * @param doc	The document to output
	 * @param streamResult	The place to put the result.
	 * 
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws TransformerConfigurationException
	 */
	public static void documentToResult(Document doc, OutputStream output)
			throws TransformerFactoryConfigurationError, TransformerException,
			TransformerConfigurationException {
		// now write out the DOM.
		DOMImplementationLS domLS = (DOMImplementationLS) doc.getImplementation();
		LSOutput lsOut = domLS.createLSOutput();
		lsOut.setByteStream(output);
		LSSerializer serializer = domLS.createLSSerializer();
		serializer.write(doc, lsOut);
	}
	
	/**
	 * Write a DOM to a well-formed OutputStream
	 * 
	 * @param doc	The document to output
	 * @param out	The place to put the result.
	 * 
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws TransformerConfigurationException
	 */
	public static void documentToStream(Document doc, OutputStream out)
			throws TransformerFactoryConfigurationError, TransformerException, TransformerConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}
}
