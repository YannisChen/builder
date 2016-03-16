package com.tibco.devtools.workspace.installer.utils;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.tibco.devtools.workspace.util.IndentingXmlStreamWriter;

import junit.framework.TestCase;

public class TestXmlWriters extends TestCase {

	public void testIndentWriter() throws XMLStreamException {
		
		StringWriter sw = new StringWriter();
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		
		XMLStreamWriter xsw = xof.createXMLStreamWriter(sw);
		
		XMLStreamWriter indenter = new IndentingXmlStreamWriter(xsw, "  ", 0);
		indenter.writeEmptyElement("foo");
		indenter.writeAttribute("something", "of.value");
		indenter.writeStartElement("bar");
		indenter.writeCharacters("Testing");
		indenter.writeEndElement(); // bar
		
		indenter.close();
		
		String results = sw.getBuffer().toString();
		System.out.println(results);
	}
	
}

