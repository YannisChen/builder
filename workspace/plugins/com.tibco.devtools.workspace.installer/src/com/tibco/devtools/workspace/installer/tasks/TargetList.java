package com.tibco.devtools.workspace.installer.tasks;

import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Represents a list of targets.
 */
public class TargetList extends DataType {

	public TargetList() {
	}

	public TargetList(Collection<Target<VersionInfo>> targets) {
		m_targets.addAll(targets);
	}
	
	public Collection<Target<VersionInfo>> getTargets() {
		return m_targets;
	}
	
	public void addConfiguredTarget(TargetEntry te) {
		Target<VersionInfo> target = te.getTarget();
		m_targets.add(target);
	}
	
	public static class TargetEntry {
		
		public Target<VersionInfo> getTarget()  {
			
			if (m_featureId == null || m_featureId.length() == 0 ||
				m_version == null) {
				throw new BuildException("Unrecognized target entry with feature <" + m_featureId + "> and version <" + m_version + ">" );
			}
			
			return new Target<VersionInfo>(m_featureId, m_version);
		}

		public void setFeature(String featureId) {
			m_featureId = featureId.trim();
		}
		
		public void setVersion(String version) {
			m_version = TaskUtils.parseVersion(version);
		}
		
		private VersionInfo m_version;
		
		private String m_featureId;
		
	}

	private List< Target<VersionInfo> > m_targets = DataUtils.newList();
	
}
