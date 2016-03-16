package com.tibco.devtools.workspace.installer;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * This interface captures the configuration details that are shared with the Eclipse
 * portion of the Workspace installer.
 *
 */
public interface WorkspaceInstallConfig {

    /**
     * The list of test features to install.
     *
     * @return
     */
    List<File> getTestFeatures();

    /**
     * A list of other features (useful for development purposes), that should be installed.
     * @return
     */
    List<File> getOtherFeatures();

    /**
     * Return the extension location - includes the "eclipse" folder
     *
     * @return The "eclipse" folder inside of the extension location.
     */
    File getEclipseExtensionLocation();

    /**
     * return the search path for projects to be included in the workspace.
     * @return
     */
    List<File> getProjectSearchPath();

    /**
     * return the search path for projects to be included in the workspace.
     * @return
     */
    List<File> getFeatureSearchPath();

    /**
     * Get the list of update sites to use...
     *
     * @return A list of URLs corresponding to update sites.
     */
    List<URL> getUpdateSites();

    /**
     * Return the location of the folder for the local site cache.
     *
     * @return A file location that may or may not exist.
     */
    File getLocalSiteCache();

    /**
     * For build features, it is useful to force that the latest build of the earliest
     * match be used (the default), however in some cases you want to override that to match latest.
     *
     * <p>If this is true, everything will match the latest versions.</p>
     *
     * @return <code>true</code> if latest compatible matching should be forced on features.
     */
    boolean isMatchingLatest();
}