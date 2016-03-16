package com.tibco.devtools.workspace.model.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tibco.devtools.workspace.model.BundleDescriptor;
import com.tibco.devtools.workspace.model.Problem;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

public class TestBundleParse extends TestCase {

	public void testPackageNames() {
		assertTrue(ManifestParser.isValidPackageName("org.w3c.dom") );
	}
	public void testGeneralParse() throws IOException {
		InputStream is = getClass().getResourceAsStream("testManifest01.mf");
		BundleDescriptor bd = ManifestParser.parseManifestFromStream("testManifest01.mf", is); 
		
		assertEquals(bd.getTarget().getTargetId(), "com.tibco.devtools.workspace.installer");
	}
	
	public void testGeneralParse02() throws IOException {
		InputStream is = getClass().getResourceAsStream("testManifest02.mf");
		ManifestParser.parseManifestFromStream("testManifest02.mf", is); 
	}
	
	public void testParsingLotsOfBundles() throws IOException {
		
		List<Problem> allProblems = DataUtils.newList();
		File scanDir = new File("/home/eric/dev/spin/milestones/release/plugins/");
		//File scanDir = new File("/home/eric/dev/spin/eclipse-3.4/thirdparty/plugins");
		for (File jarFile : ManifestParser.potentialBundleJars(scanDir)) {
			try {
				BundleDescriptor bd = ManifestParser.parseBundleFromFile(jarFile);
				allProblems.addAll(bd.getProblems() );
			} catch (BadBundleException e) {
				System.out.println("File " + jarFile + " is a bad bundle because " + e.getMessage() );
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage() + " from file " + jarFile);
			}
		}
		
		// Sort all the problems.
		Collections.sort(allProblems, new Comparator<Problem>() {

			public int compare(Problem o1, Problem o2) {
				return o1.getIdentifier().compareTo(o2.getIdentifier());
			}
			
		});
		
		// Now output the problems in a grouping.
		String identifier = null;
		for (Problem oneProblem : allProblems) {
			String currentIdentifier = oneProblem.getIdentifier();
			if (!currentIdentifier.startsWith("NON_TIBCO")) {
				if (!currentIdentifier.equals(identifier)) {
					identifier = currentIdentifier;
					System.out.println(identifier);
				}
				System.out.println("  " + oneProblem.toString());
			}
		}
	}
	
}
