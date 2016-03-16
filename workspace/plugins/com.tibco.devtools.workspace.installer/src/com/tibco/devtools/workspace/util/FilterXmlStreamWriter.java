package com.tibco.devtools.workspace.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This is a base class for implementing a delegating XMLStreamWriter,
 * so that subclasses only have to worry about the parts they want to
 * override.
 */
public class FilterXmlStreamWriter implements XMLStreamWriter {

	public FilterXmlStreamWriter(XMLStreamWriter xsw) {
		m_writer = xsw;
	}
	public void close() throws XMLStreamException {
		m_writer.close();
	}

	public void flush() throws XMLStreamException {
		m_writer.flush();
	}

	public NamespaceContext getNamespaceContext() {
		return m_writer.getNamespaceContext();
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return m_writer.getPrefix(uri);
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return m_writer.getProperty(name);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		m_writer.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		m_writer.setNamespaceContext(context);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		m_writer.setPrefix(prefix, uri);
	}

	public void writeAttribute(String localName, String value)
			throws XMLStreamException {
		m_writer.writeAttribute(localName, value);
	}

	public void writeAttribute(String namespaceURI, String localName,
			String value) throws XMLStreamException {
		m_writer.writeAttribute(namespaceURI, localName, value);
	}

	public void writeAttribute(String prefix, String namespaceURI,
			String localName, String value) throws XMLStreamException {
		m_writer.writeAttribute(prefix, namespaceURI, localName, value);
	}

	public void writeCData(String data) throws XMLStreamException {
		m_writer.writeCData(data);
	}

	public void writeCharacters(String text) throws XMLStreamException {
		m_writer.writeCharacters(text);
	}

	public void writeCharacters(char[] text, int start, int len)
			throws XMLStreamException {
		m_writer.writeCharacters(text, start, len);
	}

	public void writeComment(String data) throws XMLStreamException {
		m_writer.writeComment(data);
	}

	public void writeDTD(String dtd) throws XMLStreamException {
		m_writer.writeDTD(dtd);
	}

	public void writeDefaultNamespace(String namespaceURI)
			throws XMLStreamException {
		m_writer.writeDefaultNamespace(namespaceURI);
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		m_writer.writeEmptyElement(localName);
	}

	public void writeEmptyElement(String namespaceURI, String localName)
			throws XMLStreamException {
		m_writer.writeEmptyElement(namespaceURI, localName);
	}

	public void writeEmptyElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		m_writer.writeEmptyElement(prefix, localName, namespaceURI);
	}

	public void writeEndDocument() throws XMLStreamException {
		m_writer.writeEndDocument();
	}

	public void writeEndElement() throws XMLStreamException {
		m_writer.writeEndElement();
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		m_writer.writeEntityRef(name);
	}

	public void writeNamespace(String prefix, String namespaceURI)
			throws XMLStreamException {
		m_writer.writeNamespace(prefix, namespaceURI);
	}

	public void writeProcessingInstruction(String target)
			throws XMLStreamException {
		m_writer.writeProcessingInstruction(target);
	}

	public void writeProcessingInstruction(String target, String data)
			throws XMLStreamException {
		m_writer.writeProcessingInstruction(target, data);
	}

	public void writeStartDocument() throws XMLStreamException {
		m_writer.writeStartDocument();
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		m_writer.writeStartDocument(version);
	}

	public void writeStartDocument(String encoding, String version)
			throws XMLStreamException {
		m_writer.writeStartDocument(encoding, version);
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		m_writer.writeStartElement(localName);
	}

	public void writeStartElement(String namespaceURI, String localName)
			throws XMLStreamException {
		m_writer.writeStartElement(namespaceURI, localName);
	}

	public void writeStartElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		m_writer.writeStartElement(prefix, localName, namespaceURI);
	}

	protected XMLStreamWriter m_writer;
}
