package com.tibco.devtools.workspace.installer.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.installer.utils.SiteUtilities;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Represents a notion of a site where the site is maintained on a file system, not
 * generated from other data sources.
 */
public class FileBasedSite extends FileBasedFeatureSet {

	public FileBasedSite(File location) {
		super(location);
	}
	
	@Override
	protected void deleteFeature(FeatureDescriptor fd) {
		deleteTargetJarFromFolder(getFeaturesFolder(), fd.getTarget());
	}

	@Override
	protected void deletePlugin(Target<VersionInfo> pluginTarget) {
		deleteTargetJarFromFolder(getPluginsFolder(), pluginTarget);
	}

	/**
	 * Scan the features folder for feature JAR files, and read their feature
	 * descriptions.
	 */
	@Override
	protected List<FeatureDescriptor> discoverExistingFeatures()
			throws FileNotFoundException, IOException {
		List<FeatureDescriptor> featureDescriptors = DataUtils.newList();
		File featureFiles[] = getFeaturesFolder().listFiles();
		for (File featureJar : featureFiles) {
			// is the item in question a folder, if not skip it.
			if (!featureJar.isFile() || !featureJar.getName().endsWith(".jar")) {
				continue;
			}

			URL featureJarUrl = featureJar.toURI().toURL();
			
			FileInputStream fis = new FileInputStream(featureJar);
			FeatureDescriptor fd =
				SiteUtilities.getFeatureDescriptorFromZipStream(fis, featureJarUrl);
			
			if (fd != null) {
				featureDescriptors.add(fd);
			}
		}
		
		return featureDescriptors;
	}

	protected void deleteTargetJarFromFolder(File folder, Target<VersionInfo> target) {
		File toDelete = new File(folder, target.toString() + ".jar");
		// Note that we might be deleting a feature from a site because it is
		// incomplete, so check to see if the file exists before deleting.
		if (toDelete.exists()) {
			boolean success = FileUtils.deleteViaFileSystemOrSubverion(toDelete);
			if (!success) {
				throw new IllegalStateException("Unable to delete file " + toDelete.toString());
			}
			
		}
	}
}
