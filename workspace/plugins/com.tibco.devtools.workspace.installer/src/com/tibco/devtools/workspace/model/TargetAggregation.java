package com.tibco.devtools.workspace.model;

import java.util.List;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * An aggregation of a set of targets.
 * 
 * <p>In a sense, this is sort of like a feature's relationship to its plugins,
 * but this is intended, typically to represent a set of features.
 * </p> 
 */
public class TargetAggregation<V extends Comparable<V> > {

	/**
	 * Construct an aggregation with a particular identity.
	 * 
	 * @param identity	What's the identity of the target?
	 */
	public TargetAggregation(AspectIdentity<V> identity) {
		m_identity = identity;
	}
	
	public AspectIdentity<V> getIdentity() {
		return m_identity;
	}
	
	public List<Target<V>> getConstituents() {
		return m_constituents;
	}
	
	//=========================================================================
	// Object method overrides.
	//=========================================================================

//	@Override
//	public boolean equals(Object obj) {
//		throw new UnsupportedOperationException("equals not supported.");
//	}
//
//	@Override
//	public int hashCode() {
//		throw new UnsupportedOperationException("hashCode not supported.");
//	}

	//=========================================================================
	// Member data.
	//=========================================================================

	private AspectIdentity<V> m_identity;
	
	private List<Target<V>> m_constituents = DataUtils.newList();
}
