package com.tibco.devtools.workspace.model;

import java.net.URL;

/**
 * Locates a product closure
 */
public class ProductClosureLocator<V extends Comparable<V> > {

	public ProductClosureLocator(Target<V> identity, URL url) {
		m_identity = identity;
		m_url = url;
	}
	
	public Target<V> getIdentity() {
		return m_identity;
	}
	
	public URL getURL() {
		return m_url;
	}
	
	private final Target<V> m_identity;
	
	private final URL m_url;
}
