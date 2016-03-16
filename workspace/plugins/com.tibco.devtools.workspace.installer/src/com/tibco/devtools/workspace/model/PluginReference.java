package com.tibco.devtools.workspace.model;


/**
 * Reference to a plugin from a feature, capturing operating system,
 * architecture, and unpack flag, 
 */
public class PluginReference<V extends Comparable<V> > {
	
	public PluginReference(Target<V> target) {
		m_target = target;
	}

	public Target<V> getTarget() {
		return m_target;
	}
	
	public String getOs() {
		return m_os;
	}
	
	public void setOs(String os) {
		m_os = os;
	}
	
	public void setArch(String arch) {
		m_arch = arch;
	}
	
	public String getArch() {
		return m_arch;
	}
	
	public void setWindowSystem(String windowSystem) {
		m_windowSystem = windowSystem;
	}
	
	public String getWindowSystem() {
		return m_windowSystem;
	}
	
	public void setIsMeantToUnpack(boolean isMeantToUnpack) {
		m_isMeantToUnpack = isMeantToUnpack;
	}
	
	public boolean isMeantToUnpack() {
		return m_isMeantToUnpack;
	}
	
    private Target<V> m_target;
    
    private String m_arch;
	
	private String m_os;
	
	private String m_windowSystem;
	
	private boolean m_isMeantToUnpack = false;
}
