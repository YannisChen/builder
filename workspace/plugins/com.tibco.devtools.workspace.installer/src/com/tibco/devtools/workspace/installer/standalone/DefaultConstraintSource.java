package com.tibco.devtools.workspace.installer.standalone;

/**
 * Default implementation of a constraint source.
 */
public class DefaultConstraintSource implements SolverConstraintSource {

	public DefaultConstraintSource(String logicalId, String description) {
		m_logicalId = logicalId;
		m_description = description;
	}
	public String getSourcePathString() {
		return m_description;
	}

	public String getSourceLogicalId() {
		return m_logicalId;
	}

	private String m_logicalId;
	
	private String m_description;
}
