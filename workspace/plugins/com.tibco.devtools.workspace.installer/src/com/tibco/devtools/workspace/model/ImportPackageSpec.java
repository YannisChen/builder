package com.tibco.devtools.workspace.model;

import java.util.Map;

import com.tibco.devtools.workspace.util.DataUtils;

public class ImportPackageSpec {

	public String getPackageName() {
		return m_packageName;
	}
	
	public void setPackageName(String pkgName) {
		m_packageName = pkgName;
	}
	
	public Range<VersionInfo> getImportRange() {
		return m_range;
	}
	
	public void setImportRange(Range<VersionInfo> range) {
		m_range = range;
	}
	
	public boolean isMandatory() {
		return m_mandatory;
	}
	
	public void setMandatory(boolean mandatory) {
		m_mandatory = mandatory;
	}
	
	public Map<String, String> getAttributes() {
		return m_attributes;
	}
	
	private Map<String, String> m_attributes = DataUtils.newMap();
	
	private boolean m_mandatory;
	
	private Range<VersionInfo> m_range;
	
	private String m_packageName;
}
