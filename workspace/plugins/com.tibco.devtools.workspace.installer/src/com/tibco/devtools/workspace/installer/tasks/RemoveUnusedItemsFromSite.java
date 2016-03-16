package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.standalone.FileBasedSite;
import com.tibco.devtools.workspace.installer.utils.FileUtils;
import com.tibco.devtools.workspace.model.BundleDescriptor;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.model.parse.BadBundleException;
import com.tibco.devtools.workspace.model.parse.ManifestParser;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Ant task that will scan a local disk folder, and determine which features in that
 * folder are not complete (missing bundles), and which bundles don't belong to
 * any particular feature.
 */
public class RemoveUnusedItemsFromSite extends Task {

	public void setDir(File dir) {
		m_dir = dir;
	}
	
	@Override
	public void execute() throws BuildException {
		if (!m_dir.isDirectory()) {
			throw new BuildException("The folder " + m_dir + " is not a valid directory.");
		}
		try {
			removeUnusedBundles(m_dir);
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	public static void removeUnusedBundles(File dir) throws IOException {
		
		File pluginsFolder = new File(dir, "plugins");
		
		FileBasedSite fbs = new FileBasedSite(dir);
		
		Map<Target<VersionInfo>, List<FeatureDescriptor>> pluginReferencedBy =
			fbs.getPluginToFeaturesMap();
		
		Map<Target<VersionInfo>, BundleDescriptor> actualPlugins = DataUtils.newMap();
		List<BadFiles> badFiles = DataUtils.newList();
		
		Set<File> extraJarFiles = new HashSet<File>();
		Set<File> badBundles = new HashSet<File>();
		
		// loop through all potentially valid bundle JARs, and record the valid
		// entries, flag the invalid entries.
		for (File jarFile : ManifestParser.potentialBundleJars(pluginsFolder)) {
			File canonicalJarFile = jarFile.getCanonicalFile();
			extraJarFiles.add( canonicalJarFile );
			try {
				BundleDescriptor bd = ManifestParser.parseBundleFromFile(jarFile);
				if (bd != null) {
					actualPlugins.put(bd.getTarget(), bd);
				}
			}
			catch (BadBundleException bbe) {
				badBundles.add(canonicalJarFile);
				badFiles.add( new BadFiles(canonicalJarFile, bbe.getMessage()) );
			}
			catch (Exception e) {
				System.out.println("File " + canonicalJarFile + " generated error " + e.getMessage());
			}
		}
		
		List<Target<VersionInfo>> missingBundles = DataUtils.newList();
		
		// for every referenced plugin, see if there is a file.
		// note that this code *doesn't* use the symbolic name of the JAR file, because
		// at least for some Eclipse bundles, the MANIFEST.MF file is missing.  Weird.
		for (Target<VersionInfo> aBundle : pluginReferencedBy.keySet()) {
			File canonicalPluginFile = new File(pluginsFolder, aBundle.getTargetId() + "_" + aBundle.getVersion().toString(true) + ".jar").getCanonicalFile();
			extraJarFiles.remove(canonicalPluginFile);
			if (!canonicalPluginFile.isFile() || badBundles.contains(canonicalPluginFile)) {
				missingBundles.add(aBundle);
			}
		}
		
		// now go ahead and get the list of broken features.
		Map<Target<VersionInfo>, FeatureDescriptor> targetToFeature = DataUtils.newMap();
		Map<Target<VersionInfo>, List< Target<VersionInfo>> > featureToMissingBundles = DataUtils.newMap();
		
		for (Target<VersionInfo> missingBundle : missingBundles) {
			List<FeatureDescriptor> featuresUsingBundle = pluginReferencedBy.get(missingBundle);
			for (FeatureDescriptor oneFeature : featuresUsingBundle) {
				targetToFeature.put(oneFeature.getTarget(), oneFeature);
				List< Target<VersionInfo>> bundles = DataUtils.getMapListValue(featureToMissingBundles, oneFeature.getTarget() );
				bundles.add(missingBundle);
			}
		}
		
		// Flag and list bad files.
		for (BadFiles bf : badFiles) {
			System.out.println("Removing " + bf.badFile.toString() + " because: " + bf.reason);
			FileUtils.deleteViaFileSystemOrSubverion(bf.badFile);
		}
		
		// Now loop through and remove each of the broken features, also producing output.
		
		// first get it as a sorted list.
		List< Target<VersionInfo>> sortedBroken = DataUtils.newList();
		sortedBroken.addAll(featureToMissingBundles.keySet() );
		Collections.sort(sortedBroken);
		// now loop through.
		for (Target<VersionInfo> brokenFeature : sortedBroken ) {
			System.out.println("Removing feature " + brokenFeature.toString() + ", because the following bundles are missing:");
			for (Target<VersionInfo> bundle : featureToMissingBundles.get(brokenFeature)) {
				System.out.println("  Bundle " + bundle.toString());
			}
			FeatureDescriptor fdToRemove = targetToFeature.get(brokenFeature);
			fbs.removeFeature(fdToRemove);
			
		}
		
		// now go back and remove all the unreferenced bundles.
		List< File > sortedMissing = DataUtils.newList();
		sortedMissing.addAll(extraJarFiles );
		Collections.sort(sortedMissing);
		
		for (File nukeBundle : sortedMissing) {
			System.out.println("Removing bundle " + nukeBundle.toString() + " because it is never used.");
			FileUtils.deleteViaFileSystemOrSubverion(nukeBundle);
		}
	}
	
	private static class BadFiles {
		
		public BadFiles(File badFile, String reason) {
			this.badFile = badFile;
			this.reason = reason;
		}
		
		public File badFile;
		public String reason;
	}
	
	private File m_dir;
}
