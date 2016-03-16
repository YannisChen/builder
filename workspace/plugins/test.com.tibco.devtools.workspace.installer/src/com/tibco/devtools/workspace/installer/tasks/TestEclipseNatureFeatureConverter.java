package com.tibco.devtools.workspace.installer.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;

import com.tibco.devtools.workspace.installer.standalone.FeatureSource;
import com.tibco.devtools.workspace.installer.standalone.MockFeatureSource;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;
import com.tibco.devtools.workspace.util.DomUtilities;

import junit.framework.TestCase;

/**
 * Tests for the class EclipseNatureFeatureConverter
 */
public class TestEclipseNatureFeatureConverter extends TestCase {

	/**
	 * This verifies the correct extraction of the bundle name from a manifest.
	 * 
	 * <p>Had a bug where I did this incorrectly.
	 * @throws IOException
	 */
	public void testSymbolicNameExtraction() throws IOException {
		
		InputStream stream = getStream("manifestWithSingleton.mf");
		String [] results = EclipseNatureFeatureConverter.getBundleNames(stream, ".eclipse");
		assertEquals("com.tibco.neo.model2", results[0]);
		assertEquals("com.tibco.neo.model2.eclipse", results[1]);
	}
	
	public void testDomMapping() throws Exception {
		Document doc = parseIntoDom("testFeatureNatureMapping.xml");
		
		FeatureSource<VersionInfo, ?> mockSite = MockFeatureSource.createMockSiteFromStream(this.getClass().getResourceAsStream("MockSiteWithEclipseFeatures.txt"));
		
		URL generic = new URL("http://localhost");
		Map<Target<VersionInfo>, List< Target<VersionInfo> > > targetMap =
			InstallTargets.parseKnownMappings(getStream("testMapping.txt"), generic);
		
		Map<String, String> pluginNameMap = DataUtils.newMap();
		pluginNameMap.put("com.tibco.amf.implementation.java.runtime", "com.tibco.amf.implementation.java.runtime.eclipse");
		EclipseNatureFeatureConverter.mapDomToNewNature(mockSite, doc,
				"com.tibco.amf.implementation.java.runtime.feature.eclipse",
				targetMap, pluginNameMap, ".eclipse", false);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		DomUtilities.documentToResult(doc, baos);
		
		String result = baos.toString("utf-8");
		assertTrue(result.contains("\"com.tibco.amf.common.model.feature.eclipse\""));
		assertTrue(result.contains("\"com.tibco.amf.implementation.java.runtime.eclipse\""));
		assertTrue(result.contains("\"com.tibco.amf.implementation.java.runtime.feature.eclipse\""));
		assertTrue(result.contains("\"org.eclipse.emf\""));
		
		// OK, now do the same mapping again, but with a different known mappings file....
		doc = parseIntoDom("testFeatureNatureMapping.xml");
		targetMap =
			InstallTargets.parseKnownMappings(getStream("testOtherMapping.txt"), generic);

		boolean didCatch = false;
		try {
			EclipseNatureFeatureConverter.mapDomToNewNature(mockSite, doc,
					"com.tibco.amf.implementation.java.runtime.feature.eclipse",
					targetMap, pluginNameMap, ".eclipse", false);
		}
		catch (BuildException be) {
			didCatch = true;
		}
		
		assertTrue(didCatch);
	}
	
	public void testBrokenDomMapping() throws Exception {
		
		Document doc = parseIntoDom("testBrokenFeatureNatureMap.xml");
		
		FeatureSource<VersionInfo, ?> mockSite = MockFeatureSource.createMockSiteFromStream(this.getClass().getResourceAsStream("MockSiteWithEclipseFeatures.txt"));
		
		Map<Target<VersionInfo>, List< Target<VersionInfo> > > targetMap = DataUtils.newMap();
		Map<String, String> pluginNameMap = DataUtils.newMap();
		try {
			EclipseNatureFeatureConverter.mapDomToNewNature(mockSite, doc,
					"com.tibco.amf.implementation.java.runtime.feature.eclipse",
					targetMap, pluginNameMap, ".eclipse", false);
			
			fail("Was supposed to fail due to a missing version of a feature.");
		} catch (BuildException e) {
			// exception supposed to be caught.
		}

	}
	
	public void testParseKnownMappings() throws IOException {
		URL generic = new URL("http://localhost");
		boolean caught = false;
		try {
			InstallTargets.parseKnownMappings( getStream("knownMappingsBadLine.txt"), generic);
		}
		catch (BuildException be){
			caught = true;
			String msg = be.getMessage();
			if (!msg.contains("Bad format") || !msg.contains("com.tibco.tpcl.this.is.bad") ) {
				fail("Didn't get right exception message.");
			}
		}
		assertTrue(caught);
		
		try {
			caught = false;
			InstallTargets.parseKnownMappings( getStream("knownMappingsDuplicateEntry.txt"), generic);
		}
		catch (BuildException be) {
			caught = true;
			String msg = be.getMessage();
			if (!msg.contains("duplicate") || !msg.contains("com.tibco.tpcl.emf") || !msg.contains("Initial")
					|| !msg.contains("org.eclipse.emf")) {
				fail("Didn't get right exception message.");
			}
		}

		assertTrue(caught);
	}
	
	private Document parseIntoDom(String path) throws Exception {
		DocumentBuilder db = DomUtilities.getDocumentBuilder();
		return db.parse( getStream(path) );
	}
	
	private InputStream getStream(String name) {
		return this.getClass().getResourceAsStream(name);
	}
}
