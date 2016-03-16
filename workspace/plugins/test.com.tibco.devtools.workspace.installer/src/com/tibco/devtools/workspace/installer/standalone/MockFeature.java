package com.tibco.devtools.workspace.installer.standalone;

import com.tibco.devtools.workspace.model.AbstractFeature;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * Mock version of a feature.
 */
public class MockFeature extends AbstractFeature<VersionInfo, MockFeature> {

	public MockFeature(Target<VersionInfo> target, String sourceString) {
		super(target);
		m_sourceStr = sourceString;
	}
	
	public String getSourcePathString() {
		return getTarget().toString() + " " + m_sourceStr;
	}

	public String getConstraintSourceName() {
		return getTarget().getTargetId();
	}

	public void addFeatureConstraint(TargetConstraint<VersionInfo, MockFeature> constraint) {
		getFeatureConstraints().add(constraint);
	}
	
	public void addPluginConstraint(TargetConstraint<VersionInfo, MockFeature> constraint) {
		getPluginConstraints().add(constraint);
	}
	
	public void addProvidedPlugin(PluginReference<VersionInfo> oneTarget) {
		getProvidedPlugins().add(oneTarget);
	}
	
	private String m_sourceStr;
}
