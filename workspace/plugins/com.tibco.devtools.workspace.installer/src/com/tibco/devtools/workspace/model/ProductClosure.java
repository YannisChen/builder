package com.tibco.devtools.workspace.model;

import java.util.List;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * A product closure contains a bunch of aspect closures.
 */
public class ProductClosure<V extends Comparable<V> > {

	public ProductClosure(Target<V> identity) {
		m_identity = identity;
	}
	
	public Target<V> getIdentity() {
		return m_identity;
	}
	
	public List<AspectClosure<V>> getAspects() {
		return m_aspects;
	}
	
	public List<ProductClosureLocator<V>> getExcluded() {
		return m_excluded;
	}
	
	private List<AspectClosure<V>> m_aspects = DataUtils.newList();
	
	private Target<V> m_identity;
	
	private List<ProductClosureLocator<V>> m_excluded = DataUtils.newList();
}
