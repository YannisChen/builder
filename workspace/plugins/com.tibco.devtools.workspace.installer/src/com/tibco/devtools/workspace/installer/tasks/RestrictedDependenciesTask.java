package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.utils.SiteUtilities;

/** Calls the necessary bits to update LGPL stuff from TIBCO_HOME to the cache.
 *
 * @author amyzing
 *
 */
public class RestrictedDependenciesTask
    extends Task
{

    public void execute()
        throws BuildException
    {
        validate();

        try
        {
            m_targetFeaturesStrs = m_targetFeatures.normalizeAnyItems();
            // we don't want the default to be caching all features, so instead
            // we simply skip the caching if the caller didn't specify any tasks.
            if (m_targetFeaturesStrs != null && m_targetFeaturesStrs.size() > 0 ) {
                SiteUtilities.cacheFeaturesFromExtensionLoc(m_targetLocation, m_extensionLocation,
                        m_targetFeaturesStrs);
            }
            else {
            	System.out.println("Warning: no restricted dependencies specified - nothing cached.");
            }
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }

    public void setInstalledLocation(File path)
    {
        m_extensionLocation = AnyItem.checkNullVariable(path);
    }

    public void setTargetLocation(File path)
    {
        m_targetLocation = AnyItem.checkNullVariable(path);
    }

    public FeatureList createFeatures()
    {
        if (m_targetFeatures == null)
            m_targetFeatures = new FeatureList(getProject());
        return m_targetFeatures;
    }

    private void validate()
        throws BuildException
    {
        if (m_extensionLocation == null)
            throw new BuildException("Existing installation location (installedlocation attribute) not specified.");
        if (!m_extensionLocation.exists() || !m_extensionLocation.isDirectory())
            throw new BuildException("Existing installation location + " + m_extensionLocation + " does not exist or is not a directory.");

        if (m_targetLocation == null)
            throw new BuildException("Target location (targetlocation attribute) not specified.");
        if (!m_targetLocation.exists() || !m_targetLocation.isDirectory())
            throw new BuildException("Target location " + m_targetLocation + " does not exist or is not a directory.");
    }

    private File m_extensionLocation;
    private File m_targetLocation;
    private FeatureList m_targetFeatures;
    private List<String> m_targetFeaturesStrs;

}
