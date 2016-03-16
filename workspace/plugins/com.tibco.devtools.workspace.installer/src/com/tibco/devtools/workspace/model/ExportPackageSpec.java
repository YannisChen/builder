package com.tibco.devtools.workspace.model;

import java.util.List;
import java.util.Map;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Represents a single package being exported.
 */
public class ExportPackageSpec {

	public String getPackageName() {
		return m_packageName;
	}
	
	public void setPackageName(String packageName) {
		m_packageName = packageName;
	}
	
	public Version<VersionInfo> getVersion() {
		return m_version;
	}
	
	public void setVersion(Version<VersionInfo> version) {
		m_version = version;
	}
	
	public List<String> getUses() {
		if (m_uses == null) {
			m_uses = DataUtils.newList();
		}
		
		return m_uses;
	}
	
	public Map<String, String> getAttributes() {
		return m_attributes;
	}
	
	private Map<String, String> m_attributes = DataUtils.newMap();
	
	/**
	 * List of packages used by this exported pacakge.
	 */
	private List<String> m_uses;
	
	private Version<VersionInfo> m_version;
	
	private String m_packageName;
}
