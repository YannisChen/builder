package com.tibco.devtools.workspace.util;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Takes a standard {@link XMLStreamWriter}, and makes sure its contents
 * are indented according to standard element nesting expectations.
 */
public class IndentingXmlStreamWriter extends FilterXmlStreamWriter {

	public IndentingXmlStreamWriter(XMLStreamWriter xsw, String indentString, int indentLevel) {
		super(xsw);
		m_indentLevel = indentLevel;
		m_indentString = indentString;
		// the following sets it up so that the first "start element" doesn't indent
		m_currentHasChildren = true;
	}

	public void writeCData(String data) throws XMLStreamException {
		checkIndent();
		super.writeCData(data);
	}

	public void writeCharacters(String text) throws XMLStreamException {
		checkIndent();
		super.writeCharacters(text);
	}

	public void writeCharacters(char[] text, int start, int len)
			throws XMLStreamException {
		checkIndent();
		super.writeCharacters(text, start, len);
	}

	public void writeComment(String data) throws XMLStreamException {
		checkIndent();
		super.writeComment(data);
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		checkIndent();
		super.writeEmptyElement(localName);
	}

	public void writeEmptyElement(String namespaceURI, String localName)
			throws XMLStreamException {
		checkIndent();
		super.writeEmptyElement(namespaceURI, localName);
	}

	public void writeEmptyElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		checkIndent();
		super.writeEmptyElement(prefix, localName, namespaceURI);
	}

	public void writeEndElement() throws XMLStreamException {
		
		if (m_currentHasChildren) {
			m_indentLevel = m_indentLevel - 1;
			super.writeCharacters(getIndentString(m_indentLevel));
		}
		m_currentHasChildren = true;
		super.writeEndElement();
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		checkIndent();
		super.writeEntityRef(name);
	}

	public void writeProcessingInstruction(String target)
			throws XMLStreamException {
		checkIndent();
		super.writeProcessingInstruction(target);
	}

	public void writeProcessingInstruction(String target, String data)
			throws XMLStreamException {
		checkIndent();
		super.writeProcessingInstruction(target, data);
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		checkIndent();
		super.writeStartElement(localName);
		m_currentHasChildren = false;
	}

	public void writeStartElement(String namespaceURI, String localName)
			throws XMLStreamException {
		checkIndent();
		super.writeStartElement(namespaceURI, localName);
		m_currentHasChildren = false;
	}

	public void writeStartElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		checkIndent();
		super.writeStartElement(prefix, localName, namespaceURI);
		m_currentHasChildren = false;
	}

	/**
	 * If we've not already added children to the current element, then do so and bump
	 * the indent level
	 * @throws XMLStreamException 
	 */
	private void checkIndent() throws XMLStreamException {
		if (!m_currentHasChildren) {
			m_currentHasChildren = true;
			m_indentLevel = m_indentLevel + 1;
		}
		
		super.writeCharacters(getIndentString(m_indentLevel));
	}
	
	/**
	 * Computes and caches the indent strings up to, and including the
	 * requested indent level.
	 * 
	 * @param indent	How much should the current item be indented?
	 * 
	 * @return	The appropriate string.
	 */
	private String getIndentString(int indent) {
		if (indent >= m_returnAndIndentStrs.size() ) {
			for (int idx = m_returnAndIndentStrs.size() ; idx <= indent ; idx++) {
				StringBuffer buf = new StringBuffer("\n");
				for (int toAdd = 0 ; toAdd < idx ; toAdd++) {
					buf.append(m_indentString);
				}
				m_returnAndIndentStrs.add(buf.toString());
			}
		}
		
		return m_returnAndIndentStrs.get(indent);
	}
	
	private boolean m_currentHasChildren;
	private String m_indentString;
	private int m_indentLevel;

	private List<String> m_returnAndIndentStrs = DataUtils.newList();
}
