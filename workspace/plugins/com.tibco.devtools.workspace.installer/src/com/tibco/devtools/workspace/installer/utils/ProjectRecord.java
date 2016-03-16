package com.tibco.devtools.workspace.installer.utils;

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * See WizardProjectsImportPage.
 *
 */
public class ProjectRecord {

	public ProjectRecord(File file) {
		m_fileSystemLoc = file;
		setProjectName();
	}
	
	public File getLocation() {
		return m_fileSystemLoc;
	}
	
	public IProjectDescription getProjectDescription() {
		return m_projectDescription;
	}
	
	public String getProjectName() {
		return m_projectName;
	}
	
	/**
	 * Set the name of the project based on the projectFile.
	 */
	private void setProjectName() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath path = new Path(m_fileSystemLoc.getPath());

		m_projectName = ""; // default name.
		try {
			m_projectDescription = workspace.loadProjectDescription(path);
			if (m_projectDescription != null) {
				m_projectName = m_projectDescription.getName();
			}
		} catch (CoreException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private IProjectDescription m_projectDescription;
	
	private File	m_fileSystemLoc;
	
	private String	m_projectName;
}
