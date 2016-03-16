package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.FeatureOrderAnalyst;
import com.tibco.devtools.workspace.installer.InternalException;
import com.tibco.devtools.workspace.installer.StandaloneFeatureInstaller;
import com.tibco.devtools.workspace.installer.standalone.CachingState;
import com.tibco.devtools.workspace.installer.standalone.ProgressReport;
import com.tibco.devtools.workspace.installer.standalone.ProgressReportToConsole;
import com.tibco.devtools.workspace.installer.standalone.TargetPlatformData;
import com.tibco.devtools.workspace.installer.utils.EclipseUtils;

/**
 * An Ant task that does the same thing as the command line tool for the workspace
 * installer.
 *
 * This one doesn't do any eclipse; but is otherwise identical (all eclipse calls commented out).
 */
public class GatherDependenciesTask extends WorkspaceInstallConfigBaseTask {


    /*
     * Creates a new Eclipse configuration.
     */
    @Override
    public void execute() throws BuildException {

        normalizeSettings(m_isUsingRemoteSites);
        validate();

        try {

            TargetPlatformData targetPlatform = TargetPlatformData.getCurrentTargetPlatform();
            System.out.println("Starting.");
            ProgressReport reporter = new ProgressReportToConsole();

            // create folder for eclipse
            EclipseUtils.createEclipseFolder(getEclipseExtensionLocation(), false);

            CachingState state = m_isUsingRemoteSites ? CachingState.LOCAL_AND_REMOTE : CachingState.ONLY_LOCAL;
            
            StandaloneFeatureInstaller standaloneInstall =
                new StandaloneFeatureInstaller(m_eclipseLocation, this,
                        getEclipseExtensionLocation(), targetPlatform, state);

            if (!standaloneInstall.run(reporter) ) {
                throw new InternalException("Stand-alone install failed.");
            }

            // the following is deprecated...
            if (m_featureOrderOutput != null) {
            	new FeatureOrderAnalyst(getFeatureSearchPath(), m_featureOrderOutput ).generateFeatureOrderOutput();
            }
            
        } catch (IOException e) {
            throw new BuildException(e);
        } catch (SAXException e) {
            throw new BuildException(e);
		}

    }

    public void setFeatureOrderOutput(File featureOrder) {
    	System.out.println("Setting feature order output is deprecated.  Use create.feature.order task directly.");
    	m_featureOrderOutput = featureOrder;
    }

    /**
     * @deprecated Not used.
     */
    public void setWorkspace(File workspaceLocation) {
        System.out.println("workspace is deprecated and doesn't do anything.");
    }

    /**
     * @deprecated Not used.
     */
    public void setProjectname(String projectName) {
        System.out.println("projectname is deprecated and doesn't do anything.");
    }

    public void setConfiguration(File configurationLocation) {
        System.out.println("configureation is deprecated and doesn't do anything.");
    }

    public void setBaseEclipse(File eclipseLocation) {
        m_eclipseLocation = AnyItem.checkNullVariable(eclipseLocation);
    }

    public void setToolsextensionlocation(File toolExtensionLocation) {
        System.out.println("toolsextensionlocation is deprecated and doesn't do anything.");
    }

    /**
     * @deprecated No longer supported, instead see {@link #setMatchLatest}.
     */
    public void setForceequivalentmatchforbuildfeatures(boolean force) {
        System.out.println("forcequivalaentmatchforbuildfeatures is deprecated and doesn't do anything.");
    }

    public void setUseremotesites(boolean useRemoteSites) {
        m_isUsingRemoteSites = useRemoteSites;
    }

    public void setDebugOutputLocation(File debugOutputLocation) {
        m_debugOutputLocation = AnyItem.checkNullVariable(debugOutputLocation);
    }

    //=========================================================================
    // Private methods
    //=========================================================================
    protected void validate() {
        super.validate();
        if (m_eclipseLocation == null)
            throw new BuildException("Eclipse location (baseeclipse attribute) not specified");
        if (!m_isUsingRemoteSites && (getLocalSiteCache() == null))
            throw new BuildException("Local cache (localcachesite attribute) not set, but use remote sites is false (useremotesites attribute)");

        if (m_debugOutputLocation != null)
            dumpConfiguration();
    }

    private void dumpConfiguration() {
        List<String> extras = new ArrayList<String>(5);
        extras.add("Use remote sites: " + m_isUsingRemoteSites);
        extras.add("Eclipse location: " + m_eclipseLocation.toString());

        super.dumpConfiguration(m_debugOutputLocation, extras);
    }

    //=========================================================================
    // Member data
    //=========================================================================

    /**
     * Flag used for launching eclipse in remote debug mode, for debug purposes.
     */
//    private boolean m_isInternalDebug = false;

    private File m_eclipseLocation;

    private boolean m_isUsingRemoteSites = true;

    private File m_debugOutputLocation;
    
    private File m_featureOrderOutput;
    
}
