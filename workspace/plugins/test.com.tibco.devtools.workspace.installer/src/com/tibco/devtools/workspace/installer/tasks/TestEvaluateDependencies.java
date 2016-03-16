package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;

/**
 * Verifies that the Ant task portion of the logic for evaluating dependencies
 * correctly enforces its constraints.
 * 
 * <p>This does not attempt to actually run the task, as that would require network
 * access.</p>
 */
public class TestEvaluateDependencies extends TestCase {
	
	private AntProjectRunner m_projRunner;
	
	@Override
	protected void setUp() throws Exception {
		
		m_projRunner = new AntProjectRunner("test-evaluate-dependencies.xml");
	}

	@Override
	protected void tearDown() throws Exception {
		m_projRunner.teardown();
	}


	public void testFailureScenarios() {
		
		Project proj = m_projRunner.getProject();
		
		File m_tempDir = m_projRunner.getTempDir();
		File m_featureDir = new File(m_tempDir, "features");
		m_featureDir.mkdir();
		
		try {
			proj.executeTarget("failure1");
		} catch (BuildException e) {
			assertTrue(e.getMessage().contains("Must set localsitecache"));
		}
		
		try {
			proj.executeTarget("failure2");
		}
		catch (BuildException e) {
			assertTrue(e.getMessage().contains("updatesites"));
		}

		try {
			proj.executeTarget("failure3");
		} catch (BuildException e) {
			assertTrue(e.getMessage().contains("featuresearchpath"));
		}

		try {
			proj.executeTarget("failure5");
		} catch (BuildException e) {
			assertTrue(e.getMessage().contains("refidname"));
		}

		AntLogCollector myListener = new AntLogCollector();
		proj.addBuildListener(myListener);
		
		try {
			proj.executeTarget("failure4");
		} catch (BuildException e) {
			assertTrue(e.getMessage().contains("No features found"));
		}
		
		assertTrue(myListener.containsMessage("Discarding", "exist", "foodir"));
		assertTrue(myListener.containsMessage("Discarding", "bogus", "exist"));
	}
	
}
