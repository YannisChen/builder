package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;

import com.tibco.devtools.workspace.installer.utils.FileUtils;

public class AntProjectRunner {

	public AntProjectRunner(String fileName) throws IOException {

		// get a file location to use for build file.
		m_tempDir = File.createTempFile("ant_test", ".tmp");
		m_tempDir.delete();
		m_tempDir.mkdir();
		
		// get a file location to use for build file.
		m_tempFile = new File(m_tempDir, "build.xml");
		
		// copy data from my zip into that destination.
		InputStream inStream = this.getClass().getResourceAsStream(fileName);
		FileUtils.copyStreamToFile(inStream, m_tempFile);
		
		m_project = new Project();
		// Set the classloader for this class as the core classloader for ant.
		ClassLoader testClassLoader = this.getClass().getClassLoader();
		m_project.setCoreLoader( testClassLoader );
		
		// also set the classloader used by taskdef.
		AntClassLoader acl = new AntClassLoader(testClassLoader, true);
		m_project.addReference("test.loader", acl);
		
		// initialize the project (sets up all the default tasks)
		m_project.init();
		
		parseProject();
	}
	
	public void teardown() {
		
		// Just make sure we clean up our temp file.
		if (m_tempDir != null) {
			FileUtils.deleteFolder(m_tempDir);
		}
		m_tempDir = null;
		m_tempFile = null;
	}

	public File getTempDir() {
		return m_tempDir;
	}
	
	public Project getProject() {
		return m_project;
	}
	
	@SuppressWarnings("unchecked")
	public Target getTargetFromProject(String targetName) {
		// first, get the target that we want to test.
		Hashtable<String, Target> targets = m_project.getTargets();
		return targets.get(targetName);
	}
	
	private void parseProject() {
		// now parse the XML file.
		ProjectHelper.configureProject(m_project, m_tempFile);
	}

	private Project m_project;
	
	private File m_tempDir;
	
	private File m_tempFile;

}
