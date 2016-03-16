package com.tibco.devtools.workspace.model;

import java.util.Map;

import com.tibco.devtools.workspace.util.DataUtils;

public class RequireBundleSpec {

	public RequireBundleSpec(String bundle, Range<VersionInfo> range, boolean isMandatory, boolean reexport) {
		m_bundleName = bundle;
		m_range = range;
		m_isMandatory = isMandatory;
		m_reexport = reexport;
	}
	
	public boolean isMandatory() {
		return m_isMandatory;
	}
	
	public String getBundleName() {
		return m_bundleName;
	}
	
	public Range<VersionInfo> getBundleRange() {
		return m_range;
	}
	
	public void setReexport(boolean reexport) {
		m_reexport = reexport;
	}
	
	public Map<String, String> getAttributes() {
		return m_attributes;
	}
	
	public boolean isReexporting() {
		return m_reexport;
	}
	
	private Map<String, String> m_attributes = DataUtils.newMap();
	
	private boolean m_reexport = false;
	
	private Range<VersionInfo> m_range;
	
	private String m_bundleName;
	
	private boolean m_isMandatory;
}
