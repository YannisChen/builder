package com.tibco.devtools.workspace.model.parse;

/**
 * Thrown on a bundle that is unambiguously a bad bundle.
 */
public class BadBundleException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BadBundleException(String str) {
		super(str);
	}
}
