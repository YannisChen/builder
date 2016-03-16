package com.tibco.devtools.workspace.model;

import java.util.List;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Parsed information from a bundle.
 */
public class BundleDescriptor {

	public void init() {
		// initialize defaults that aren't already set.
	}
	
	public Target<VersionInfo> getTarget() {
		return m_target;
	}
	
	public void setTarget(Target<VersionInfo> target) {
		m_target = target;
	}
	
	public List<ImportPackageSpec> getImportPackages() {
		return m_importPackages;
	}
	
	public List<ExportPackageSpec> getExportPackages() {
		return m_exportPackages;
	}
	
	public List<RequireBundleSpec> getRequireBundles() {
		return m_requireBundles;
	}
	
	public List<String> getBundleClassPath() {
		return m_bundleClassPath;
	}
	
	public boolean isSingleton() {
		return m_isSingleton;
	}
	
	public void setIsSingleton(boolean isSingleton) {
		m_isSingleton = isSingleton;
	}

	public void setIsLazyActivate(boolean isLazyActivate) {
		m_isLazyActivate = isLazyActivate;
	}
	
	public boolean isLazyActivate() {
		return m_isLazyActivate;
	}
	
	public List<String> getRequiredExecutionEnvironments() {
		return m_requiredExecutionEnvironments;
	}
		
	public void setActivator(String activator) {
		m_activator = activator;
	}
	
	public String getActivator() {
		return m_activator;
	}
	
	public List<Problem> getProblems() {
		return m_problems;
	}
	
	/**
	 * What problems have been found with this bundle?
	 */
	private List<Problem> m_problems = DataUtils.newList();
	
	private List<RequireBundleSpec> m_requireBundles = DataUtils.newList();
	
	private List<ExportPackageSpec> m_exportPackages = DataUtils.newList();
	
	private List<ImportPackageSpec> m_importPackages = DataUtils.newList();
	
	private List<String> m_bundleClassPath = DataUtils.newList();
	
	private List<String> m_requiredExecutionEnvironments = DataUtils.newList();

	private Target<VersionInfo> m_target;
	
	private boolean m_isSingleton = false;
	
	private boolean m_isLazyActivate = false;
	
	private String m_activator;
	
}
