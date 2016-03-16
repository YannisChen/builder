package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.model.BundleDescriptor;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.model.parse.ManifestParser;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Captures the notion of an extension location, so that features can
 * be safely installed an removed with overlapping plugin sets.
 */
public class ExtensionLocation extends FileBasedFeatureSet {
	
	/**
	 * Instantiate an extension location at a given file location.
	 * 
	 * @param location Where is this extension location?
	 */
	public ExtensionLocation(File location) {
		super(location);
	}

	protected void deleteFeature(FeatureDescriptor fd) {
		// delete the folder for the feature.
		FileUtils.deleteFolder(fd.getFileLocation().getParentFile());
	}

	protected void deletePlugin(Target<VersionInfo> pluginTarget) {
		String targetName = pluginTarget.toString();
		String jarName = targetName + ".jar";
		File pluginJarFile = new File(getPluginsFolder(), jarName);
		File pluginFolder = new File(getPluginsFolder(), targetName );
		if (pluginJarFile.exists()) {
			if (!pluginJarFile.isFile()) {
				System.out.println("Found " + pluginJarFile.toString() + " but as a folder, not a file.");
			}
			FileUtils.blindlyDelete(pluginJarFile);
		}
		else if (pluginFolder.exists() ) {
			if (!pluginFolder.isDirectory()) {
				System.out.println("Found " + pluginFolder.toString() + " but as a file, not a folder.");
			}
			FileUtils.deleteFolder(pluginFolder);
		}
	}

	/**
	 * Get the feature descriptors associated with an extension location.
	 * 
	 * @param eclipseFolder	The "eclipse" folder that contains a "features" folder that contains
	 * 	a collection of folders, one for each feature.
	 * 
	 * @return	The descriptors for all of the features in question.
	 * @throws FileNotFoundException
	 */
	protected List<FeatureDescriptor> discoverExistingFeatures() throws FileNotFoundException {
		
		List<FeatureDescriptor> featureDescriptors = DataUtils.newList();
		File featureFolders[] = getFeaturesFolder().listFiles();
		for (File featureFolder : featureFolders) {
			// is the item in question a folder, if not skip it.
			if (!featureFolder.isDirectory() ) {
				continue;
			}
			
			// does the folder contain feature.xml? if not skip it.
			File featureXmlFile = new File(featureFolder, "feature.xml");
			if (!featureXmlFile.isFile()) {
				continue;
			}
			
			FeatureDescriptor fd = FeatureDescriptor.fromFile(featureXmlFile);
			featureDescriptors.add(fd);
		}
		
		return featureDescriptors;
	}
	
	/**
	 * There are some plugins(only one for now) in eclipse-4.4/plugins, but it doesn't belong to any features in eclipse-4.4/features.
	 * Example: org.eclipse.equinox.concurrent_1.1.0.v20130327-1442.jar
	 * @throws IOException 
	 * 
	 * */
	public FeatureDescriptor discoverPluginsWithoutFeature() throws IOException  {
		// this feature will master the plugins that doesn't belong to any features
		FeatureDescriptor featureMasteringNoFeaturesPlugin = new FeatureDescriptor(new Target<VersionInfo>("com.tibco.tools.he.mock.feature", new VersionInfo(1, 0, 0)));

		Iterator<BundleDescriptor> it = ManifestParser.bundlesInDir(getPluginsFolder()).iterator();
		while (it.hasNext()) {
			boolean flag = true;
			BundleDescriptor plugin = it.next();
			for (Target<VersionInfo> each : getPluginToFeaturesMap().keySet()) {
				if (each.equals(plugin.getTarget())) {
					flag = false;
					break;
				}
			}
			if (flag) {
				featureMasteringNoFeaturesPlugin.getProvidedPlugins().add(new PluginReference<VersionInfo>(plugin.getTarget()));
			}
		}

		return featureMasteringNoFeaturesPlugin;
	}
}
