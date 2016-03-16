package com.tibco.devtools.workspace.model;

public final class AspectIdentity<V extends Comparable<V> > {

	public AspectIdentity(Target<V> product, String name) {
		m_product = product;
		m_name = name;
		m_label = product.getTargetId() + "#" + m_name;
	}
	
	public Target<V> getProduct() {
		return m_product;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getAspectLabel() {
		return m_label;
	}
	
	public V getVersion() {
		return m_product.getVersion();
	}
	
	//=========================================================================
	// Object overrides.
	//=========================================================================
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof AspectIdentity) ) {
			return false;
			
		}
		
		AspectIdentity<V> other = (AspectIdentity<V>) obj;
		return m_product.equals(other.m_product) && m_name.equals(other.m_name); 
	}

	@Override
	public int hashCode() {
		return m_product.hashCode() ^ m_name.hashCode();
	}

	//=========================================================================
	// Member data
	//=========================================================================
	
	/**
	 * What product does this aspect belong to?
	 */
	private final Target<V> m_product;

	/**
	 * What is the name of this aspect within the product.
	 */
	private final String m_name;
	
	private final String m_label;
	
}
