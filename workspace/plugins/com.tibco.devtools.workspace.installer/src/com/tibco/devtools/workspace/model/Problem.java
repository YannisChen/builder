package com.tibco.devtools.workspace.model;

/**
 * Any problem discovered about a feature or bundle fits into
 * this class. 
 */
public abstract class Problem {

	public Problem(String identifier) {
		m_identifier = identifier;
	}
	
	/**
	 * Get a String identifier for this particular problem - each type of
	 * problem gets a different string....
	 * @return
	 */
	public String getIdentifier() {
		return m_identifier;
	}
	
	private String m_identifier;
	
}
