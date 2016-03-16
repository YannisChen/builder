package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.tibco.devtools.workspace.installer.utils.EclipseUtils;

public class CreateWorkspaceTask
    extends Task
{

    public void execute() throws BuildException {
        int status = 0;
        validate();

        try {
            EclipseUtils.createOsShortcut( m_projectName, m_eclipseLocation,
                                       m_configurationLocation, m_outputFolder  );

            // Convert files into parameters...
            List<String> args = new ArrayList<String>();
            List<File> searchPath = getFeatureSearchPath();
            for (File searchEntry : searchPath) {
                args.add(searchEntry.toString() );
            }
                
            status = EclipseUtils.launchEclipseWithArgs(
                    m_eclipseLocation, m_isInternalDebug, m_workspaceLocation,
                    m_configurationLocation,
                    "com.tibco.devtools.workspace.installer.workspaceSetup",
                    args, m_outputFolder);
            
            
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        } catch (InterruptedException ie) {
            throw new BuildException(ie);
        }
        if (status != 0) {
            throw new ExitStatusException("Eclipse exited with status " + status, status);
        }
    }

    /**
     * Trigger whether to do debugging configuration for Eclipse.
     * 
     * @param debug	If <code>true</code>, we do debugging.
     */
    public void setDebug(boolean debug) {
    	m_isInternalDebug = debug;
    }
    
    public void setProjectName(String project) {
        m_projectName = AnyItem.checkNullVariable(project);
    }

    public void setEclipseLocation(File eel) {
        m_eclipseLocation = AnyItem.checkNullVariable(eel);
    }

    public void setOutputFolder(File of) {
        m_outputFolder = AnyItem.checkNullVariable(of);
    }

    public void setWorkspaceLocation(File wl) {
        m_workspaceLocation = AnyItem.checkNullVariable(wl);
    }

    public void setConfigurationLocation(File cl) {
        m_configurationLocation = AnyItem.checkNullVariable(cl);
    }

    public void addFeatureSearchPath(Path featureSearchPath) {
        if (m_featureSearchPath == null)
            m_featureSearchPath = new PathItem(getProject());
        m_featureSearchPath.add(featureSearchPath);
    }

    public List<File> getFeatureSearchPath() {
        return m_featureSearchPath.getFileList(true);
    }

    private void validate() {
        if (m_eclipseLocation == null)
            throw new BuildException("Eclipse location (eclipselocation attribute) not specified");
        if (m_outputFolder == null)
            throw new BuildException("Output folder (outputfolder attribute) not specified");
        if (m_workspaceLocation == null)
            throw new BuildException("Workspace location (workspacelocation attribute) not specified");
        if (m_configurationLocation == null)
            throw new BuildException("Configuration location (configurationlocation attribute) not specified");
        if (m_featureSearchPath.list().length == 0)
            throw new BuildException("Feature search path (featuresearchpath nested element) not specified");
    }

    private static class PathItem extends Path {
        public PathItem(Project p) {
            super(p);
        }

        List<File> getFileList(boolean isDir) {
            List<File> result = new ArrayList<File>();
            String [] pathList = list();
            for (String element : pathList) {
                if ((element == null) || (element.length() == 0) ||
                     (element.trim().length() == 0) || (element.indexOf("${") >= 0) ) {
                    continue;
                }
                File file = new File(element);
                boolean dirTest = (isDir) ? file.isDirectory() : file.isFile();
                if ((file != null) && file.exists() && dirTest) {
                    result.add(file);
                }
            }
            return result;
        }
    }

    private String m_projectName = "Unnamed Project";

    private File m_eclipseLocation;

    private File m_workspaceLocation;

    private File m_configurationLocation;

    private File m_outputFolder;

    private PathItem m_featureSearchPath;

    private boolean m_isInternalDebug = false;

}
