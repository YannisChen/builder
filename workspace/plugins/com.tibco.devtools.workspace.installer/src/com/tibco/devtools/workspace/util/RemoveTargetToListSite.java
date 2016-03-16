package com.tibco.devtools.workspace.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;

import com.tibco.devtools.workspace.installer.standalone.FileBasedSite;
import com.tibco.devtools.workspace.installer.utils.SiteUtilities;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * RemoveTargetToListSite is a file that contains some targets that should be removed.
 * 
 * */
public class RemoveTargetToListSite extends FileBasedSite{
	public RemoveTargetToListSite(File location){
		super(location);
		listFile = new File(location, "remove_list.txt");
		if(listFile.exists()){
			parseListFile();
		}
	}
	
	private void parseListFile(){
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(listFile));
			String line;
			while ((line = br.readLine()) != null) {
				if("features".equals(new File(line).getParentFile().getName())){
					Target<VersionInfo> target = SiteUtilities.extractModelFromFeatureJar(new File(line)).getTarget();
					featureTargetsLoggedinFile.add(target);
				}
			}
		} catch (IllegalArgumentException e) {
			throw new BuildException("Failed to parse " + listFile.getAbsolutePath(), e);
		} catch (IOException e) {
			throw new BuildException("Failed to parse " + listFile.getAbsolutePath(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new BuildException("Failed to parse " + listFile.getAbsolutePath(), e);
				}
			}
		}
	}
	
	/**
	 * Log those targets that should be deleted in a file rather than to delete them.
	 * 
	 * */
	@Override
	protected void deleteTargetJarFromFolder(File folder, Target<VersionInfo> target) {
		if (featureTargetsLoggedinFile.contains(target)) { return; }
		String jar = target.toString() + ".jar";
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(listFile, true));
			writer.append(new File(folder, jar).getCanonicalPath() + System.getProperty("line.separator"));
		} catch (IOException e) {
			throw new BuildException("Failed to append " + jar + " to " +  listFile.getAbsolutePath(), e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					throw new BuildException("Failed to append " + jar + " to " +  listFile.getAbsolutePath(), e);
				}
			}
		}
	}
	
	/**
	 * we should remove the feature that be logged in remove_list.txt
	 * 
	 * */
	@Override
	protected List<FeatureDescriptor> discoverExistingFeatures() throws IOException {
		List<FeatureDescriptor> features = super.discoverExistingFeatures();
		
		Iterator<FeatureDescriptor> iterator = features.iterator();
		while(iterator.hasNext()){
			FeatureDescriptor feature = iterator.next();
			for(Target<VersionInfo>  toRemove : featureTargetsLoggedinFile){
				if(feature.getTarget().equals(toRemove)){
					iterator.remove();
					break;
				}
			}
		}
		return features;
	}

	private File listFile;
	private Set<Target<VersionInfo>> featureTargetsLoggedinFile = new HashSet<Target<VersionInfo>>();
}
