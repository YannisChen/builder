package com.tibco.devtools.workspace.installer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.tibco.devtools.workspace.installer.utils.FeatureUtils;
import com.tibco.devtools.workspace.installer.utils.ProjectRecord;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Installs projects into a workspace.
 */
public class SetupWorkspaceCommand {

	public SetupWorkspaceCommand(List<File> featureSearchPath) {
        m_featureSearchPath = featureSearchPath;
	}
    
	public boolean run(IProgressMonitor monitor) {
		
		boolean success = false;
        monitor.beginTask("Configuring Workspace", 100);

        try {
            monitor.subTask("Searching for feature files.");
			m_buildFeatures = FeatureUtils.getBuildFeatures( m_featureSearchPath );
			monitor.worked(10);
			
			monitor.subTask("Enumerating projects.");
            List<ProjectRecord> projects = enumerateProjects();
            monitor.worked(10);
            
            pruneProjectsListOfExistingProjects(projects);
            
            if (projects.size() > 0) {
            	monitor.subTask("Importing projects: ");
                addProjectsToWorkspace(projects,
                        new SubProgressMonitor(monitor, 80,
                                SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK
                                + SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
            }
            else {
            	monitor.subTask("All projects already added...");
            	monitor.worked(80);
            }
			success = true;
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			monitor.done();
		}
        
		return success;
	}

    /**
     * Add projects to the workspace.
     *
     * @param projects    The set of project descriptions to add to the workspace.
     * @param monitor    A progress monitor.
     * @throws CoreException
     */
    private void addProjectsToWorkspace(List<ProjectRecord> projects, IProgressMonitor monitor) throws CoreException {

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        pruneProjectsListOfExistingProjects(projects);

        // if there are any projects left to add, add them.
        if (projects.size() > 0) {
            IWorkspaceRunnable addProjects = new AddProjectsOperation(workspace, projects);
            workspace.run(addProjects,
                    new SubProgressMonitor(monitor, projects.size(), SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK ));

        }
        else {
        	monitor.done();
        }
    }
	private void pruneProjectsListOfExistingProjects(List<ProjectRecord> projects) {

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();

        // iterate through all of the projects identified, and remove them from the list of
        // projects to add, if they are already in the workspace.
        Iterator<ProjectRecord> projIter = projects.iterator();
        while (projIter.hasNext() ) {
            ProjectRecord projRec = projIter.next();
            IProject proj = root.getProject(projRec.getProjectName() );
            if (proj.exists()) {
                projIter.remove();
            }
        }
	}

    /**
     * Loop through all of the features in "buildFeatures", and create a list of
     * projects to open in the workspace.
     *
     * @return The list of projects to open.
     */
    private List<ProjectRecord> enumerateProjects() {

        List<File> featureProjectFolders = DataUtils.newList();
        Set<String> setOfProjectNamesToOpen = new HashSet<String>();
        // First, go off and build a list of all the projects expected by the features.
        for (FeatureDescriptor feature : m_buildFeatures) {
            
        	File parentFolder = feature.getFileLocation().getParentFile();
            featureProjectFolders.add(parentFolder);

            // then loop through all the plugins from the feature...
            for (PluginReference<VersionInfo> pluginProj : feature.getProvidedPlugins() ) {
                String pluginIdentifier = pluginProj.getTarget().getTargetId();
                setOfProjectNamesToOpen.add(pluginIdentifier);
            }
        }

        List<File> collectedResults = findProjectFiles();

        Map<String, List<File>> projNameToFileList = DataUtils.newMap();
        Set<String> duplicateProjNames = new HashSet<String>();
        
        // turn each project file into a project record, add to the list if
        // it is an expected project.
        List<ProjectRecord> records = new ArrayList<ProjectRecord>();
        for (File projFile : collectedResults) {

            // what's the folder containing the project...?
            File parentFolder = projFile.getParentFile();
            ProjectRecord proj = new ProjectRecord(projFile);
            String projName = proj.getProjectName();
            List<File> fileList = projNameToFileList.get(projName);
            if (fileList != null) {
            	duplicateProjNames.add(projName);
            	fileList.add(projFile);
            }
            else {
            	fileList = new ArrayList<File>();
            	fileList.add(projFile);
            	projNameToFileList.put(projName, fileList);
            }
            if ( featureProjectFolders.contains(parentFolder) ||
                    setOfProjectNamesToOpen.contains(projName) ) {
                records.add(proj);
                
                // remove the project name from the set to add, so we can
                // identify projects that aren't in the list.
                setOfProjectNamesToOpen.remove(projName);
            }
            else {
            	String[] natureIds = proj.getProjectDescription().getNatureIds();
            	List<String> naturesList = Arrays.asList(natureIds);
            	if (naturesList.contains("org.eclipse.pde.PluginNature")) {
                	System.out.println(" WARNING: Found unused plugin project " + projName + " at " + projFile.toString() );
            	}
            	else {
                	System.out.println(" Found unused project " + projName + " at " + projFile.toString() );
            	}
            }
        }

        outputWarnings(setOfProjectNamesToOpen, projNameToFileList, duplicateProjNames);
        return records;
    }

	/**
	 * Output projects that have duplicate names, and projects that cannot be found.
	 * 
	 * @param setOfProjectNamesToOpen
	 * @param projNameToFileList
	 * @param duplicateProjNames
	 */
    private void outputWarnings(Set<String> setOfProjectNamesToOpen, Map<String, List<File>> projNameToFileList, Set<String> duplicateProjNames) {
		for (String unusedProjName : setOfProjectNamesToOpen) {
        	System.out.println(" Unable to find project " + unusedProjName);
        }
        
        for (String dupProjName : duplicateProjNames) {
        	List<File> dupFiles = projNameToFileList.get(dupProjName);
        	System.out.println("Project name " + dupProjName + ": found duplicates.");
        	for (File dupFile : dupFiles) {
        		System.out.println("  Found at: " + dupFile.toString() );
        	}
        }
	}

	private List<File> findProjectFiles() {
		// search for all of the project files.
        List<File> collectedResults = DataUtils.newList();
        for (File pathItem : m_featureSearchPath) {
            searchFolderForProjectFiles(pathItem, collectedResults);
        }
		return collectedResults;
	}

    /**
     * Use recursion to scan the folders looking for any folder that contains a ".project" file.
     * @param folder  The folder to search.
     * @param collectedResults    The list of all of the found files.
     */
    private static void searchFolderForProjectFiles(File folder, List<File> collectedResults) {

        // does this folder have a ".project" file?
        File projectFile = new File(folder, IProjectDescription.DESCRIPTION_FILE_NAME);
        if (projectFile.isFile()) {
            collectedResults.add(projectFile);
        }
        else {
        	File ignoreIndicator = new File(folder, FeatureUtils.WS_IGNORE);
        	if (!ignoreIndicator.exists()) {
                File contents[] = folder.listFiles();
                for (File item : contents) {
                    // Not sure exactly why this is, but the WizardProjectsImportPage.collectProjectFilesFromDirectory
                    // also excludes the ".metadata" folder from its search.
                    if (item.isDirectory() && !item.getName().equals(".metadata") ) {
                        searchFolderForProjectFiles(item, collectedResults);
                    }
                }
        	}
        }
    }

    /**
     * Opens the set of projects passed to it.
     */
    private static class AddProjectsOperation implements IWorkspaceRunnable {

        public AddProjectsOperation(IWorkspace workspace, List<ProjectRecord> projects) {
            m_workspace = workspace;
            m_projects = projects;
        }
        public void run(IProgressMonitor monitor) throws CoreException {

            monitor.beginTask("Opening projects", m_projects.size() * 2 );

            IWorkspaceRoot workspaceRoot = m_workspace.getRoot();
            
            for (ProjectRecord projRec : m_projects) {
                String projName = projRec.getProjectName();
                IProject project = workspaceRoot.getProject(projName);
                if (!project.exists()) {
	                project.create(projRec.getProjectDescription(), new SubProgressMonitor(
	                        monitor, 1));
	                project.open(0,
	                        new SubProgressMonitor(monitor, 1));
                }
                else {
                	System.out.println("Skipped attempt to add duplicate project " + projName
                			+ " found at " + project.getFullPath().toString() ); 
                }

            }

            // save the workspace, so that we don't get a notice about incomplete shutdown.
            m_workspace.save(false, new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK) );

            monitor.done();
        }

        private List<ProjectRecord> m_projects;
        
        private IWorkspace m_workspace;
    }

    private List<File> m_featureSearchPath;
    
	private List<FeatureDescriptor> m_buildFeatures;
}
