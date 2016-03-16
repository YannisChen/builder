package com.tibco.devtools.workspace.model;

import java.util.List;

/**
 * Declares a immutable object consisting of a name and version, where the
 * version is a generic type.
 *
 * @param <I>
 * @param <V>
 */
public class Target<V extends Comparable<V> >
	implements Comparable< Target<V> > {
	
	/**
	 * Used to generate a string of the available versions of a feature for a better error
	 * message.
	 * 
	 * @param features	The features that I want the versions of.
	 * @return	A string of the form v1, v2, v3, v4
	 */
	public static <V extends Comparable<V> > String joinVersions(List<? extends Target<V>> features, String separator) {
		StringBuffer available = new StringBuffer();
		boolean first = true;
		for (Target<V> feature : features) {
			if (!first)
				available.append(separator);
			
			available.append(feature.getVersion().toString() );
			first = false;
		}
		String availableStr = available.toString();
		return availableStr;
	}

	
	// ========================================================================
	//	public methods
	// ========================================================================
	
	public Target(String targetId, V vers) {
		m_targetId = targetId;
		m_version = vers;
	}
	
	public String getTargetId() {
		return m_targetId;
	}
	
	public V getVersion() {
		return m_version;
	}
	
	// ========================================================================
	//	Comparable implementation
	// ========================================================================

	/**
	 * Compares a VersionedTarget to another, as per standard
	 * {@link Comparable#compareTo(Object)} rules.
	 */
	public int compareTo(Target<V> o) {
		int result = this.m_targetId.compareTo(o.m_targetId);
		if (result == 0) {
			result = this.m_version.compareTo(o.m_version);
		}
		
		return result;
	}
	
	// ========================================================================
	//	Object overrides
	// ========================================================================
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj == this)
			return true;
		
		if ( obj == null || (!obj.getClass().equals( getClass() ) ) )
			return false;
		
		Target<?> other = (Target<?>) obj;
		return m_targetId.equals(other.m_targetId) && m_version.equals(other.m_version);
	}

	@Override
	public int hashCode() {
		
		return m_targetId.hashCode() ^ m_version.hashCode();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer( m_targetId );
		
		result.append("_");
		result.append(m_version.toString() );
		return result.toString();
	}

	// ========================================================================
	//	Private data
	// ========================================================================
	
	/**
	 * What is the identifier for the target?
	 */
	private String m_targetId;
	
	/**
	 * What is the version number for the target?
	 */
	private V m_version;
	
}
