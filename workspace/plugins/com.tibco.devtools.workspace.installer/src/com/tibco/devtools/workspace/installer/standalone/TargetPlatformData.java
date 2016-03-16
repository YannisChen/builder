package com.tibco.devtools.workspace.installer.standalone;

/**
 * Captures a notion of target platform, including operating system,
 * processor, and windowing system.
 */
public class TargetPlatformData {

	public static final String ARCH_X86 = "x86";

	public static final String OS_WIN32 = "win32";
	
	/**
	 * Get the target platform information for the current operating environment.
	 * 
	 * @return	A TargetPlatformData reflecting the current operating environment.
	 */
	public static TargetPlatformData getCurrentTargetPlatform() {
		TargetPlatformData result = new TargetPlatformData();
		result.computeOsString();
		
		return result;
	}
	
	/**
	 * Get a default TargetPlatformData with no platform set.
	 */
	public TargetPlatformData() {
		m_os = "";
		m_arch = "";
		m_windowSystem = "";
	}

	public String getOS() {
		return m_os;
	}
	
	public String getArch() {
		return m_arch;
	}
	
	public String getWindowSystem() {
		return m_windowSystem;
	}
	
	@Override
	public String toString() {
		return m_os + "." + m_windowSystem + "." + m_arch;
	}

	
	//=========================================================================
	// Private methods
	//=========================================================================
	
	/**
	 * This is an effort in enumeration, and only as good as the information
	 * we have on all the possible values.
	 * 
	 * <p>Fortunately, you can look up the possibilities:
	 * http://lopica.sourceforge.net/os.html </p>
	 *
	 */
	private void computeOsString() {
        String systemId = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
		if (systemId.indexOf("windows") >= 0) {
			m_os = OS_WIN32;
			if (arch.equals(ARCH_X86)) {
				m_arch = arch;
			}
			m_windowSystem = "win32";
		}
		else if (systemId.indexOf("linux") >= 0) {
			m_os = "linux";
			if (arch.equals("i386") || arch.equals(ARCH_X86) || arch.equals("i686")) {
				m_arch = ARCH_X86;
			}
			else if (arch.equals("x86_64")) {
				m_arch = "x86_64";
			}
			// So far, this only gets hard-wired to the GTK...
			m_windowSystem = "gtk";
		}
		else if (systemId.indexOf("mac") >= 0) {
			m_os = "macosx";
			m_arch = arch;
			m_windowSystem = "carbon";
		}
		else if (systemId.indexOf("hp-ux") >= 0) {
			m_os = "hpux";
			if (arch.equals("pa_risc")) {
				m_arch = "PA_RISC";
			}
			else if (arch.equals("ia64n")) {
				m_arch = "ia64_32";
			}
			m_windowSystem = "motif";
		}
		else if (systemId.indexOf("solaris") >= 0 || systemId.indexOf("sun") >= 0) {
			m_os = "solaris";
			if (arch.equals("sparc")) {
				m_arch = "sparc";
			}
			m_windowSystem = "motif";
		}
		else if (systemId.indexOf("aix") >= 0) {
			m_os = "aix";
			m_arch = "ppc";
			m_windowSystem = "motif";
		}
		else {
			throw new IllegalArgumentException("Unrecognized os.name " + systemId);
		}

		if (m_arch == null) {
			throw new IllegalArgumentException("Unrecognized architecture " + arch + " for " + m_os);
		}
	}

	//=========================================================================
	// Member data
	//=========================================================================
	
	/**
	 * Which operating system are we looking for?
	 */
    private String m_os;
    
    /**
     * Which processor architecture are we looking for?
     */
    private String m_arch;
    
    /**
     * Which windowing system?
     */
    private String m_windowSystem;

}
