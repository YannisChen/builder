package com.tibco.devtools.workspace.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An immutable representation of a four-part version number.
 */
public class VersionInfo implements Version<VersionInfo> {

	/**
	 * Use this constant to represent an unbounded upper limit.
	 */
	public static final VersionInfo UNBOUNDED = new VersionInfo(
			Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "");
	
	/**
	 * Simplified constructor when the qualifier is gratuitous.
	 * 
	 * @param major	Major version.
	 * @param minor	Minor version
	 * @param patch	Patch version.
	 */
	public VersionInfo(int major, int minor, int patch) {
		this(major, minor, patch, "");
	}
	
	/**
	 * Create a version number given the four parts.
	 * 
	 * @param major The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version
	 * @param qualifier	The qualifier for the version.
	 */
	public VersionInfo(int major, int minor, int patch, String qualifier) {
		m_majorVersion = major;
		m_minorVersion = minor;
		m_patchVersion = patch;
		m_qualifier = qualifier != null ? qualifier : "";
		
		if (major < 0 || minor < 0 || patch < 0) {
			throw new IllegalArgumentException("Version components must be greater than or equal to zero.");
		}
	}
	
	/**
	 * Parse a version string into an instance of VersionInfo.
	 * 
	 * @param toParse The string to parse.
	 * 
	 * @return The parsed data.
	 * 
	 * @throws IllegalArgumentException If the version String does not match the accepted format.
	 */
	public static VersionInfo parseVersion(String toParse) throws IllegalArgumentException {
		
        if (toParse.equals(INFINITY)) {
            return UNBOUNDED;
        }
		Matcher match = sm_pattern.matcher(toParse);
		if (match.matches() ) {
			int major = Integer.parseInt(match.group(1));
			int minor = 0;
			int patch = 0;
			String qualifier = "";
			String group = match.group(2);
			if (group != null) {
				minor = Integer.parseInt(group);
			}
			group = match.group(3);
			if (group != null) {
				patch = Integer.parseInt(group);
			}
			group = match.group(4);
			if (group != null) {
				qualifier = group;
			}
			
			return new VersionInfo(major, minor, patch, qualifier);
		}
		else {
			throw new IllegalArgumentException("Version string " + toParse + " is in an unexpected format.");
		}
	}
	
	/**
	 * Get the major version number of the version.
	 * 
	 * @return	The major version number.
	 */
	public int getMajorVersion() {
		return m_majorVersion;
	}
	
	public VersionInfo nextMajor() {
		return this.equals(UNBOUNDED) ? this : new VersionInfo(m_majorVersion + 1, 0, 0, "");
	}
	
	public VersionInfo nextMinor() {
		return this.equals(UNBOUNDED) ? this : new VersionInfo(m_majorVersion, m_minorVersion + 1, 0, "");
	}
    
    public VersionInfo nextPatch() {
        return this.equals(UNBOUNDED) ? this : new VersionInfo(m_majorVersion, m_minorVersion, m_patchVersion + 1, "");
    }
	
	/**
	 * Get the minor version number.
	 * 
	 * @return The minor version number.
	 */
	public int getMinorVersion() {
		return m_minorVersion;
	}
	
	public int getPatchVersion() {
		return m_patchVersion;
	}
	
	public String getQualifier() {
		return m_qualifier;
	}
	
	/**
	 * Utility function for comparing integers that avoids the overhead of creating an object
	 * and using {@link Integer#compareTo(Integer)}.
	 * 
	 * <p>Does the standard less than 0 if aThis is less than aThat, zero if they're the
	 * same, and greater than 0 if aThis is greater than aThat.
	 * </p>
	 * 
	 * @param aThis	first value to compare
	 * @param aThat second value to compare
	 * @return -1, 0, or 1 as follows the semantics of the {@link Comparable#compareTo(Object)}
	 * method.
	 */
	public static int compareInts(int aThis, int aThat) {
		return aThis < aThat ? -1 : (aThis == aThat ? 0 : 1);
	}
	// ==================================================================================
	// Comparable
	// ==================================================================================

	/**
	 * Compare the two versions to figure out their relationship.
	 */
	public int compareTo(VersionInfo o) {
		
		if (o.m_majorVersion != this.m_majorVersion) {
			return compareInts(this.m_majorVersion, o.m_majorVersion);
		}
		if (o.m_minorVersion != this.m_minorVersion) {
			return compareInts(this.m_minorVersion, o.m_minorVersion);
		}
		if (o.m_patchVersion != this.m_patchVersion) {
			return compareInts(this.m_patchVersion, o.m_patchVersion);
		}
		
		return this.m_qualifier.compareTo(o.m_qualifier);
	}
	
	// ==================================================================================
	// Object methods
	// ==================================================================================
	
	@Override
	public boolean equals(Object other) {
		
		if (this == other) {
			return true;
		}
		
		if ( ! (other instanceof VersionInfo) ) {
			return false;
		}
		
		VersionInfo o = (VersionInfo) other;
		return this.m_majorVersion == o.m_majorVersion
			&& this.m_minorVersion == o.m_minorVersion
			&& this.m_patchVersion == o.m_patchVersion
			&& this.m_qualifier.equals(o.m_qualifier);
	}

	@Override
	public int hashCode() {
		int combined = m_majorVersion << 16 + m_minorVersion << 8 + m_patchVersion;
		return combined ^ m_qualifier.hashCode();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean full) {
        if ( m_majorVersion == Integer.MAX_VALUE) {
            return INFINITY;
        }
        else {
    		StringBuffer result = new StringBuffer();
    		
    		result.append( m_majorVersion );
    		if (full || m_minorVersion != 0 || m_patchVersion != 0 || m_qualifier.length() > 0) {
	    		result.append( "." );
	    		result.append( m_minorVersion );
	    		if (full || m_patchVersion != 0 || m_qualifier.length() > 0) {
		    		result.append( "." );
		    		result.append( m_patchVersion );
		    		
		    		if (m_qualifier.length() > 0) {
		    			result.append( "." );
		    			result.append( m_qualifier );
		    		}
	    		}
    		}
    		return result.toString();
        }
	}
	// ==================================================================================
	// Private data
	// ==================================================================================
	
	private static final String sm_regex = "(\\d+)(?:$|\\.(\\d+)(?:$|\\.(\\d+)(?:$|\\.(.*)$)))";
	private static final Pattern sm_pattern = Pattern.compile(sm_regex);
	
	private final int m_majorVersion;
	
	private final int m_minorVersion;
	
	private final int m_patchVersion;
	
	private final String m_qualifier;

    private static final String INFINITY = "UNBOUNDED";
    //private static final String INFINITY = "\u221e";
}
