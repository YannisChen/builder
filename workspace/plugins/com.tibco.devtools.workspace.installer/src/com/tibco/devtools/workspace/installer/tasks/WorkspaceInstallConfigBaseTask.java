package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.tibco.devtools.workspace.installer.WorkspaceInstallConfig;
import com.tibco.devtools.workspace.installer.tasks.AnyItem.UrlItem;

public abstract class WorkspaceInstallConfigBaseTask extends Task implements WorkspaceInstallConfig {

    public void setExtensionlocation(File extensionLocation) {
        m_extensionLocation = AnyItem.checkNullVariable(extensionLocation);
    }

    public FeatureList createExcludedFeatures() {
    	System.out.println("Excluded features functionality has been deprecated.");
        return new FeatureList(getProject());
    }

    public void addFeatureSearchPath(Path featureSearchPath) {
        if (m_featureSearchPath == null)
            m_featureSearchPath = new Path(getProject());
        m_featureSearchPath.add(featureSearchPath);
    }

    public void setLocalsitecache(File localSiteCache) {
        m_localSiteCache = AnyItem.checkNullVariable(localSiteCache);
    }

    public void addOtherFeature(Path otherFeature) {
        if (m_otherFeatures == null)
            m_otherFeatures = new PathItem(getProject());
        m_otherFeatures.add(otherFeature);
    }

    /**
     * Note that this is an alias for "setOutputFolder."
     * @param outputFolder
     */
    public void setDestDir(File outputFolder) {
        System.out.println("destdir is deprecated and doesn't do anything.");
    }

    public void setOutputFolder(File outputFolder) {
        System.out.println("outputfolder is deprecated and doesn't do anything.");
    }

    public void addProjectSearchPath(Path projectSearchPath) {
        if (m_projectSearchPath == null)
            m_projectSearchPath = new PathItem(getProject());
        m_projectSearchPath.add(projectSearchPath);
    }

    public void addTestFeature(Path testFeature) {
        if (m_testFeatures == null)
            m_testFeatures = new PathItem(getProject());
        m_testFeatures.add(testFeature);
    }

    public UrlList createUpdateSites() {
    	// that this value is null flags that we need to recreate the value...
    	m_updateSitesUrls = null;
        UrlList list = new UrlList(getProject());
        m_updateSites.add(list);
        return list;
    }

    public void setMatchLatest(boolean matchLatest) {
        m_matchLatest = matchLatest;
    }

    //=========================================================================
    //Interface WorkspaceInstallConfig
    //=========================================================================
    public File getEclipseExtensionLocation() {
        return m_extensionLocation;
    }

   public List<File> getFeatureSearchPath() {
        return m_featureSearchPathFiles;
    }

    public File getLocalSiteCache() {
        return m_localSiteCache;
    }

    public List<File> getOtherFeatures() {
        return m_otherFeatureFiles;
    }

    public List<File> getProjectSearchPath() {
        return m_projectSearchPathFiles;
    }

    public List<File> getTestFeatures() {
        return m_testFeatureFiles;
    }

    public List<URL> getUpdateSites() {
    	if (m_updateSitesUrls == null) {
    		m_updateSitesUrls = new ArrayList<URL>();
    		for (UrlList oneList : m_updateSites) {
    			m_updateSitesUrls.addAll( oneList.normalizeAnyItems() );
    		}
    	}
    	return m_updateSitesUrls;
    }

    public boolean isMatchingLatest() {
        return m_matchLatest;
    }

    protected void validate() {
        if (m_featureSearchPathFiles.size() == 0 && m_otherFeatureFiles.size() == 0)
            throw new BuildException("Feature search path (featuresearchpath nested element) or other features required (otherfeature nested element) not specified");
    }

    public void normalizeSettings(boolean usingRemotes) {

        if (m_featureSearchPath != null) {
            m_featureSearchPathFiles.addAll(TaskUtils.extractNormalizedDirs(this, m_featureSearchPath));
        }
        if (m_projectSearchPath != null) {
            m_projectSearchPathFiles.addAll(TaskUtils.extractNormalizedDirs(this, m_projectSearchPath));
        }
        if ((m_projectSearchPath == null) || (m_projectSearchPath.size() == 0)) {
           m_projectSearchPathFiles.addAll(m_featureSearchPathFiles);
        }
        if (m_otherFeatures == null) {
            m_otherFeatures = new PathItem(getProject());
        }
        m_otherFeatureFiles.addAll(extractNormalizedFiles(m_otherFeatures));
        if (m_testFeatures == null) {
            m_testFeatures = new PathItem(getProject());
        }
        m_testFeatureFiles.addAll(extractNormalizedFiles(m_testFeatures));
    }

    protected void dumpConfiguration(File outputLocation, List<String> additionalData) {
        try
        {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outputLocation));
            writer.println("Match latest: " + m_matchLatest);
            writer.println("Local site cache: " + m_localSiteCache.toString());
            if (m_featureOrderOutput != null) {
                writer.println("Feature order output: " + m_featureOrderOutput.toString());
            }
            writer.println("Extension location: " + m_extensionLocation.toString());

            if (m_testFeatures != null)
                for (String s : m_testFeatures.list())
                    writer.println("Test feature path: " + s);
            if (m_otherFeatures != null)
                for (String s : m_otherFeatures.list())
                    writer.println("Other feature path: " + s);
            if (m_projectSearchPath != null)
                for (String s : m_projectSearchPath.list())
                    writer.println("Project search path: " + s);
            if (m_featureSearchPath != null)
                for (String s : m_featureSearchPath.list())
                    writer.println("Feature search path: " + s);

            for (UrlList oneList : m_updateSites) {
                for (UrlItem url : oneList.getResources())
                    writer.println("Update site item: " + url.getValue());
            }

            for (URL url : getUpdateSites() )
                writer.println("Update site: " + url);
            for (File feature : m_featureSearchPathFiles)
                writer.println("Feature search: " + feature.toString());
            for (File other : m_otherFeatureFiles)
                writer.println("Other feature: " + other.toString());
            for (File project : m_projectSearchPathFiles)
                writer.println("Project search: " + project.toString());
            for (File test : m_testFeatureFiles)
                writer.println("Test feature: " + test.toString());

            for (String line : additionalData)
                writer.println(line);

            writer.flush();
            writer.close();

        }
        catch (RuntimeException e)
        {
            System.out.println("Oops.  Can't write debug info:");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("Unable to create file at " + outputLocation.toString() );
            e.printStackTrace();
        }
    }

    private static List<File> extractNormalizedFiles(PathItem path) {
        List<File> result = new ArrayList<File>();
        String [] pathList = path.list();
        for (String element : pathList) {
            if (TaskUtils.isInvalidPathElement(element) )
                continue;
            File file = new File(element);
            if ((file != null) && file.exists() && file.isFile())
                result.add(file);
        }
        return result;
    }

    static class PathItem extends Path {
        public PathItem(Project p) {
            super(p);
        }
    }

    private File m_localSiteCache;

    private File m_featureOrderOutput = null;

    private File m_extensionLocation;

    private PathItem m_testFeatures;

    private PathItem m_otherFeatures;

    private PathItem m_projectSearchPath;

    private Path m_featureSearchPath;

    private List<UrlList> m_updateSites = new ArrayList<UrlList>();

    private boolean m_matchLatest = false;

    private List<URL> m_updateSitesUrls = null;

    private List<File> m_featureSearchPathFiles = new ArrayList<File>();

    private List<File> m_otherFeatureFiles = new ArrayList<File>();

    private List<File> m_projectSearchPathFiles = new ArrayList<File>();

    private List<File> m_testFeatureFiles = new ArrayList<File>();

}
