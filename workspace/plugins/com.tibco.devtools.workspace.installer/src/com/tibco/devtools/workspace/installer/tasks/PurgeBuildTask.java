package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.standalone.FileBasedSite;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

public class PurgeBuildTask extends Task {

	public PurgeBuildTask() {
		
	}
	
	@Override
	public void execute() throws BuildException {
		if ( !m_location.isDirectory() ) {
			throw new BuildException(m_location.toString() + " is not a valid directory.");
		}

		try {
			if (m_isBuildSite) {
				purgeBuildSite();
			} else {
				purgeCandidates();
			}
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}
	
	private void purgeCandidates() throws IOException {
		Set<File> buidlsHub = findBuidlsHub(m_location);
		for (File each : buidlsHub) {
			List<VersionedBuild> builds = new ArrayList<VersionedBuild>();
			List<File> toDelete = new ArrayList<File>();
			for (File subDir : each.listFiles()) {
				if (isBuildDir(subDir)) {
					builds.add(new VersionedBuild(getFeatureVersion(subDir), subDir));
				}else{
					toDelete.add(subDir);
				}
			}
			if (builds.size() > 0) {
				String standardName = builds.get(0).dir.getName();
				for (File dir : toDelete) {
					boolean flag = true;
					String name = dir.getName();
					if (standardName.length() == name.length()) {
						for (int i = 0; i < standardName.length(); i++) {
							// ASCII code '9' - '0' = 11
							if (Math.abs(standardName.indexOf(i) - name.indexOf(i)) > 11) {
								flag = false;
								break;
							}
						}
					} else {
						flag = false;
					}
					if (flag) {
						deleteDir(dir);
					}
				}
			}
			purgeCandidatesBuilds(builds);
		}
	}

	private Set<File> findBuidlsHub(File candidatesDir) {
		Set<File> buildsHub = new HashSet<File>();
		Queue<File> queue = new LinkedList<File>();
		queue.add(candidatesDir);
		while (queue.size() != 0) {
			File head = queue.poll();
			if (isBuildDir(head)) {
				buildsHub.add(head.getParentFile());
			} else {
				for (File file : head.listFiles()) {
					if (file.isDirectory()) {
						queue.add(file);
					}
				}
			}
		}
		return buildsHub;
	}
	
	private void purgeCandidatesBuilds(List<VersionedBuild> builds) throws IOException {
		List<VersionedBuild> toKeep = new ArrayList<VersionedBuild>();
		List<VersionedBuild> toRemove = new ArrayList<VersionedBuild>();
		for (VersionedBuild each : builds) {
			VersionedBuild toAdd = each;
			VersionedBuild toDelete = null;
			for (VersionedBuild inner : toKeep) {
				if (isSameReleaseVersion(each.version, inner.version)) {
					if (isFirstOneBigger(each.version, inner.version)) {
						toDelete = inner;
					} else {
						toDelete = each;
						toAdd = null;
					}
					break;
				}
			}
			if (toAdd != null) {
				if (toAdd.version.compareTo(new VersionInfo(0, 0, 0, "0")) > 0) {
					toKeep.add(toAdd);
				} else {
					toRemove.add(toAdd);
				}
			}
			if (toDelete != null) {
				toKeep.remove(toDelete);
				toRemove.add(toDelete);
			}
		}

		for (VersionedBuild each : toRemove) {
			deleteDir(each.dir);
		}
	}

	private static class VersionedBuild implements Comparable<VersionedBuild> {
		public final VersionInfo version;
		public final File dir;

		public VersionedBuild(VersionInfo version, File dir) {
			this.version = version;
			this.dir = dir;
		}

		public int compareTo(VersionedBuild o) {
			return this.version.compareTo(o.version);
		}
	}
	
	void deleteDir(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				deleteDir(c);
			}
		}
		if (!f.delete()) throw new BuildException("Failed to delete file: " + f);
	}

	private VersionInfo getFeatureVersion(File oneBuild) throws FileNotFoundException, IOException {
		File assembly = new File(new File(oneBuild, "logs"), "assembly.txt");
		if(!assembly.exists()){
			return new VersionInfo(0, 0, 0, "0");
		}
		InputStream input = null;
		try {
			input = new FileInputStream(assembly);
			Properties prop = new Properties();
			prop.load(input);
			String feature = (String) prop.get("feature");
			return VersionInfo.parseVersion(feature.substring(feature.lastIndexOf('_') + 1, feature.lastIndexOf('.')));
		} finally {
			if (input != null) {
				input.close();
			}
		}
	}
	
	/*
	 * If one dir include sub dir 'debug', 'release' and 'logs', we will say this is build level dir
	 * 
	 * */
	private boolean isBuildDir(File dir) {
		if (dir.isDirectory()) {
			List<String> names = new ArrayList<String>();
			for (File each : dir.listFiles()) {
				names.add(each.getName());
			}
			if (names.indexOf("debug") >= 0 && names.indexOf("release") >= 0 && names.indexOf("logs") >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * keep latest build of each version
	 * 1.0.1.006 3.1.0.002
	 * */
	private void purgeBuildSite() throws IOException {
		FileBasedSite fbs = new FileBasedSite(m_location);
		Map<String, List<FeatureDescriptor>> toKeep = new HashMap<String, List<FeatureDescriptor>>();
		List<FeatureDescriptor> toDelete = new ArrayList<FeatureDescriptor>();

		for (FeatureDescriptor each : fbs.getFeatures()) {
			Target<VersionInfo> target = each.getTarget();
			if (!toKeep.containsKey(target.getTargetId())) {
				List<FeatureDescriptor> list = new LinkedList<FeatureDescriptor>();
				list.add(each);
				toKeep.put(target.getTargetId(), list);
			} else {
				List<FeatureDescriptor> innerList = toKeep.get(target.getTargetId());
				FeatureDescriptor needToAdd = each;
				FeatureDescriptor needToDelete = null;
				for (FeatureDescriptor next : innerList) {
					if (isSameReleaseVersion(each, next)) {
						if (isFirstOneBigger(each, next)) {
							needToDelete = next;
						} else {
							needToAdd = null;
							needToDelete = each;
						}
						break;
					}
				}
				if (needToAdd != null) {
					innerList.add(needToAdd);
				}
				if (needToDelete != null) {
					innerList.remove(needToDelete);
					toDelete.add(needToDelete);
				}
			}
		}
		for (FeatureDescriptor each : toDelete) {
			fbs.removeFeature(each);
		}
	}
	
	/**
	 * 1.0.0.001 and 1.0.0.002 are same release version
	 * 
	 * */
	private boolean isSameReleaseVersion(FeatureDescriptor fdA, FeatureDescriptor fdB) {
		VersionInfo a = fdA.getTarget().getVersion();
		VersionInfo b = fdB.getTarget().getVersion();
		return isSameReleaseVersion(a, b);
	}
	
	private boolean isSameReleaseVersion(VersionInfo a, VersionInfo b) {
		return (a.getMajorVersion() == b.getMajorVersion() && a.getMinorVersion() == b.getMinorVersion() && a.getPatchVersion() == b.getPatchVersion());
	}
	
	private boolean isFirstOneBigger(FeatureDescriptor first, FeatureDescriptor second){
		return isFirstOneBigger(first.getTarget().getVersion(), second.getTarget().getVersion());
	}
	
	private boolean isFirstOneBigger(VersionInfo first, VersionInfo second){
		return first.compareTo(second) > 0;
	}
	
	public void setDir(File dir) {
		m_location = dir;
	}
	
	public void setIsBuildSite(boolean isBuildSite) {
		m_isBuildSite = isBuildSite;
	}
	
	
	private boolean m_isBuildSite;
	
	private File m_location;
}
