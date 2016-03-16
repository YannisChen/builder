package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.RemoveTargetToListSite;

/**
 * The basis for either an extension location or a file-based site representation.
 * 
 */
public abstract class FileBasedFeatureSet {

	public FileBasedFeatureSet(File location) {
		m_location = location;

		m_featuresFolder = new File(location, "features");
		m_pluginsFolder = new File(location, "plugins");
	}

	/**
	 * Get the file-system location of the extension location.
	 * 
	 * @return The file system location.
	 */
	public File getLocation() {
		return m_location;
	}
	
	/**
	 * Get the list of features in the extension location.
	 * 
	 * @return The list of feature descriptors for the features.
	 * @throws IOException 
	 */
	public List<FeatureDescriptor> getFeatures() throws IOException {
		ensureFolderHasScanned();
		return m_unmodFeatures;
	}

	public Map<Target<VersionInfo>, List<FeatureDescriptor> > getPluginToFeaturesMap() throws IOException {
		ensureFolderHasScanned();
		return m_unmodPluginToFeatureList;
	}
	
	/**
	 * Remove a feature from the extension location.
	 * 
	 * @param fd The description of the feature.
	 * @throws IOException 
	 */
	public boolean removeFeature(FeatureDescriptor fd) throws IOException {
		
		ensureFolderHasScanned();
		if (!m_features.contains(fd)) {
			return false;
		}
		
		// Now loop through each of the referenced plugins, and if it is the last reference
		// to the plugin, delete it.
		Collection<PluginReference<VersionInfo> > plugins = fd.getProvidedPlugins();
		for (PluginReference<VersionInfo> pluginRef : plugins) {
			Target<VersionInfo> pluginTarget = pluginRef.getTarget();
			List<FeatureDescriptor> featuresWithPlugin = m_pluginToFeatureList.get(pluginTarget);
			boolean removed = featuresWithPlugin.remove(fd);
			if (!removed) {
				System.out.println("Warning: feature " + fd.toString() + " reports containing plugin " + pluginTarget.toString() + ", but feature not found on list of containing features for this plugin.");
			}
			
			// is this the last reference to this feature?
			if (featuresWithPlugin.size() == 0) {
				deletePlugin(pluginTarget);
			}
		}
		
		deleteFeature(fd);
		
		return true;
	}

	/**
	 * How to delete a feature changes depending whether we're dealing with an update
	 * site or an extension location.
	 * 
	 * @param fd
	 */
	protected abstract void deleteFeature(FeatureDescriptor fd);

	/**
	 * How to delete a plugin changes depending whether we're dealing with an update
	 * site or an extension location.
	 * 
	 * @param fd
	 */
	protected abstract void deletePlugin(Target<VersionInfo> pluginTarget);
	
	protected File getFeaturesFolder() {
		return m_featuresFolder;
	}
	
	protected File getPluginsFolder() {
		return m_pluginsFolder;
	}
	
	protected abstract List<FeatureDescriptor> discoverExistingFeatures() throws IOException;
	
	private void ensureFolderHasScanned() throws IOException {
		if (m_needsScanning) {
			if ( !getFeaturesFolder().isDirectory() ) {
				throw new FileNotFoundException("Expecting to find folder " + getFeaturesFolder().toString() );
			}
			rescanFolder();
			m_needsScanning = false;
		}
	}

	/**
	 * Scan the contents of the folder to reload what is actually contained at the
	 * extension location.
	 * @throws IOException 
	 */
	private void rescanFolder() throws IOException {
		
		// get the list of features.
		m_features = discoverExistingFeatures();
		m_unmodFeatures = Collections.unmodifiableList(m_features);
		
		// now create a map of plugin targets to the list of features they contain.
		m_pluginToFeatureList = FeatureDescriptor.createPluginToFeaturesMap(m_features);
		
		// now create a wrapper that we can pass back to callers, that they cannot modify.
		m_unmodPluginToFeatureList = Collections.unmodifiableMap(m_pluginToFeatureList);
	}

	/**
	 * where is the plugins folder for this extension location?
	 */
	private File m_pluginsFolder;
	
	/**
	 * Where is the features folder for this extension location?
	 */
	private File m_featuresFolder;
	
	/**
	 * Where is the extension location?
	 */
	private File m_location;
	
	/**
	 * List of the features in the extension location.
	 */
	private List<FeatureDescriptor> m_features;
	
	private List<FeatureDescriptor> m_unmodFeatures;
	
	/**
	 * Have we scanned the folder yet?
	 */
	private boolean m_needsScanning = true;

	/**
	 * The mapping of versioned target form of a plugin reference to the list of features that
	 * it came from.
	 */
	private Map<Target<VersionInfo>, List<FeatureDescriptor> > m_pluginToFeatureList;
	
	private Map<Target<VersionInfo>, List<FeatureDescriptor> > m_unmodPluginToFeatureList;
}
