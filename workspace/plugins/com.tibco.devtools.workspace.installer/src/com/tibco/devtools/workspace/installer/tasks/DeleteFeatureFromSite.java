package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.standalone.FileBasedSite;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.RemoveTargetToListSite;

public class DeleteFeatureFromSite extends Task {

	public DeleteFeatureFromSite() {
		
	}
	
	@Override
	public void execute() throws BuildException {
		
		if ( !m_location.isDirectory() ) {
			throw new BuildException(m_location.toString() + " is not a valid directory.");
		}
		
		FileBasedSite fbs = null;
		if (m_echoList) {
			fbs = new RemoveTargetToListSite(m_location);
		} else {
			fbs = new FileBasedSite(m_location);
		}
		
		try {
			List<FeatureDescriptor> features = fbs.getFeatures();
			
			FeatureDescriptor match = null;
			for (FeatureDescriptor fd : features) {
				Target<VersionInfo> target = fd.getTarget();
				if (target.getTargetId().equals(m_featureId) && target.getVersion().equals(m_version)) {
					match = fd;
					break;
				}
			}
			if (match == null) {
				throw new BuildException("Unable to find feature " + m_featureId + " with version " + m_version.toString() );
			}
			
			fbs.removeFeature(match);
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	public void setDir(File dir) {
		m_location = dir;
	}
	
	public void setFeatureId(String featureId) {
		m_featureId = featureId;
	}
	
	public void setVersion(String version) {
		m_version = VersionInfo.parseVersion(version);
	}
	
	public void setEchoList(boolean echoList) {
		this.m_echoList = echoList;
	}

	private VersionInfo m_version;
	
	private String m_featureId;
	
	private File m_location;
	
	private boolean m_echoList;
}
