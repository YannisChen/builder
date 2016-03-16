package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.utils.EclipseUtils;

public class EclipseJarLocatorTask
    extends Task
{

    public void setEclipseLocation(File location)
    {
        m_eclipseLocation = location;
    }

    public void setProperty(String propertyName)
    {
        if ( (propertyName != null) && (propertyName.length() > 0) &&
             !(propertyName.indexOf('&') >= 0) )
            m_propertyName = propertyName;
    }

    @Override
    public void execute()
        throws BuildException
    {
        validate();
        File jarFile = null;
        try
        {
            jarFile = EclipseUtils.findStartupJar(m_eclipseLocation);
            if (jarFile != null)
                setNewProperty(jarFile);
            else
                throw new BuildException("Could not ascertain startup jar in location " + m_eclipseLocation);
        }
        catch(IOException ioe)
        {
            throw new BuildException("Could not ascertain startup jar in location " + m_eclipseLocation + " : " + ioe.getMessage());
        }
    }

    private void validate()
        throws BuildException
    {
        if (m_eclipseLocation == null)
            throw new BuildException("The eclipseLocation attribute must be specified");
        if (!m_eclipseLocation.exists())
            throw new BuildException("The eclipseLocation attribute points at a nonexistent location");
        if (!m_eclipseLocation.isDirectory())
            throw new BuildException("The eclipseLocation attribute does not point at a directory");
        File features = new File(m_eclipseLocation, FEATURES_DIR);
        File plugins = new File(m_eclipseLocation, PLUGINS_DIR);
        if (! (features.exists() && features.isDirectory()) ||
            ! (plugins.exists() && plugins.isDirectory()) )
            throw new BuildException("The directory pointed at by the eclipseLocation attribute must contain child directories named 'features' and 'plugins'");
    }

    private void setNewProperty(File location)
    {
        String canonicalLocation;
        try
        {
            canonicalLocation = location.getCanonicalPath();
        }
        catch (IOException ioe)
        {
            canonicalLocation = location.toString();
        }
        getProject().setNewProperty(m_propertyName, canonicalLocation);
    }

    private File m_eclipseLocation = null;

    private String m_propertyName = AUTOBUILD_DIR_ECLIPSE_STARTUP;

    private static final String FEATURES_DIR = "features";
    private static final String PLUGINS_DIR = "plugins";
    private static final String AUTOBUILD_DIR_ECLIPSE_STARTUP = "autobuild.dir.eclipse.startup";
}
