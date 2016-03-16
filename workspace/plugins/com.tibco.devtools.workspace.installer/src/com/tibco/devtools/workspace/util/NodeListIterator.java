/**
 * 
 */
package com.tibco.devtools.workspace.util;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Iterate over a DOM NodeList
 *
 */
public class NodeListIterator extends NoRemoveIterator<Element> implements Iterable<Element> {

	public NodeListIterator(NodeList nodes) {
		m_index = -1;
		m_nodes = nodes;
	}
	
	public boolean hasNext() {
		return m_index < (m_nodes.getLength() - 1);
	}

	public Element next() {
		if (m_index < (m_nodes.getLength() - 1) ) {
			m_index++;
		}
		else {
			throw new NoSuchElementException("Attempt to use more than the NodeList has.");
		}
		
		return (Element) m_nodes.item(m_index);
	}

	private int m_index;
	private NodeList m_nodes;
}
