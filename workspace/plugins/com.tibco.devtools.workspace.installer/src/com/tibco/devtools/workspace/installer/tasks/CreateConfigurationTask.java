package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.utils.EclipseUtils;

public class CreateConfigurationTask
    extends Task
{

    public void execute() throws BuildException {

        validate();

        try {
            EclipseUtils.createConfigFolder(m_eclipseLocation, m_configurationLocation, m_extensionLocation, m_workspaceLocation, m_toolExtensionLocation );
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    public void setEclipseLocation(File eclipseLocation) {
        m_eclipseLocation = AnyItem.checkNullVariable(eclipseLocation);
    }

    public void setExtensionLocation(File extensionLocation) {
        m_extensionLocation = AnyItem.checkNullVariable(extensionLocation);
    }

    public void setToolsExtensionLocation(File toolsExtensionLocation) {
        m_toolExtensionLocation = AnyItem.checkNullVariable(toolsExtensionLocation);
    }

    public void setWorkspaceLocation(File workspaceLocation) {
        m_workspaceLocation = AnyItem.checkNullVariable(workspaceLocation);
    }

    public void setConfigurationLocation(File configurationLocation) {
        m_configurationLocation = AnyItem.checkNullVariable(configurationLocation);
    }

    private void validate() throws BuildException {
        if (m_eclipseLocation == null)
            throw new BuildException("Eclipse location (baseeclipse attribute) not specified");
        if (m_workspaceLocation == null)
            throw new BuildException("Workspace location (workspace attribute) not specified");
        if (m_configurationLocation == null)
            throw new BuildException("Configuration location (configuration attribute) not specified");
    }

    private File m_eclipseLocation;

    private File m_extensionLocation;

    private File m_toolExtensionLocation;

    private File m_workspaceLocation;

    private File m_configurationLocation;

}
