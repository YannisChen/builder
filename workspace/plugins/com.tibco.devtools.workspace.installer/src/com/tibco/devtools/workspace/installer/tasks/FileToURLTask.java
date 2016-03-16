package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class FileToURLTask extends Task {

    public void setPathProperty(String pathProp) {
        m_pathProperty = AnyItem.checkNullVariable(pathProp);
        if (m_pathProperty != null) {
            try {
                m_path = new File(getProject().getProperty(m_pathProperty)).getCanonicalFile();
            } catch (IOException ioe) {
                m_path = null;
            }
        }
    }

    public void setUrlProperty(String urlProp) {
        m_urlProperty = AnyItem.checkNullVariable(urlProp);
    }

    @Override
    public void execute() throws BuildException {
        validate();
        getProject().setProperty(m_urlProperty, m_path.toURI().toString());
    }

    private void validate() throws BuildException {
        if (m_path == null)
            throw new BuildException("Path property name (pathproperty attribute) not set.");
        if (m_urlProperty == null)
            throw new BuildException("URL property name (urlproperty attribute) not set.");
    }

    private String m_pathProperty;
    private File m_path;
    private String m_urlProperty;
}
