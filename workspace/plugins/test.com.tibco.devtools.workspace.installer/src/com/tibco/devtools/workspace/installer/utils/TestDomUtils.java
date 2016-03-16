package com.tibco.devtools.workspace.installer.utils;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tibco.devtools.workspace.util.DomUtilities;

import junit.framework.TestCase;

public class TestDomUtils extends TestCase {

    public void testGetNamedChild() {
        Element rootElem = m_document.getDocumentElement();
        Element child = DomUtilities.getNamedChild(rootElem, QN_CHILD_ONE, false);
        assertEquals(child.getNamespaceURI(), TEST_NS);
        assertEquals("childOne", child.getLocalName());
    }

    public void testNodePath() {
        Element rootElem = m_document.getDocumentElement();
        Element child = DomUtilities.getNamedChild(rootElem, QN_CHILD_ONE, false);
        String result = DomUtilities.nodeToPath(child);
        assertEquals("/aDocument/childOne", result);
    }
    
    public void testGetNamedChildContents() {
        Element rootElem = m_document.getDocumentElement();
        String contents = DomUtilities.getNamedChildContents(rootElem, QN_CHILD_ONE, false);
        assertEquals(contents, "childOne contents");
    }

    private static String[] sm_items = new String[] { "item1", "item2", "item3", "item4" };

    public void testGetContentListForNamedChild() {
        Element rootElem = m_document.getDocumentElement();
        List<String> toCompare = Arrays.asList(sm_items);

        List<String> contents = DomUtilities.getContentListForNamedChild(rootElem, QN_CHILD_TWO);
        assertTrue( contents.equals(toCompare) );
    }

    @Override
    protected void setUp() throws Exception {
        m_document = DomTestUtilities.domFromClasspath(this.getClass(), "TestDomUtils.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        m_document = null;
    }

    private Document m_document;

    private static String TEST_NS = "http://www.example.com/namespaces/testing";
    private static QName QN_CHILD_ONE = new QName(TEST_NS, "childOne");
    private static QName QN_CHILD_TWO = new QName(TEST_NS, "childTwo");
}
