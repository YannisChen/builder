package com.tibco.devtools.workspace.installer.standalone;

import java.util.ResourceBundle;

public enum ConstraintResult {

	// Informational messages
	SUCCESSS,
	PLUGIN_CONSTRAINTS,
	
	// error messages
	NO_INTERSECTION,
	NOT_FOUND,
	UNMATCHED,
	DONUT_HOLE,
	INVALID_INITIAL_STATE,
	FIXED_MISMATCH,
	STARTING_MISMATCH_ON_FIXED,
	EXHAUSTED_OPTIONS_ON_FIXED,
	UNMATCHED_PLUGINS,
	MULTIPLE_POSSIBLE_FEATURES_FOR_PLUGIN,
	
	// Warnings
	EXHAUSTIVE_PLUGIN_SEARCH,
	NO_FEATURES_WITH_PLUGIN_CONSTRAINT,
	ASSUMING_CONSTRAINT,
	ADDING_FUTURE_CONSTRAINT,
	INFERRED_CONSTRAINT;
	
	public static String getResourceString(ConstraintResult code) {
		return sm_resources.getString( code.toString() );
	}
	
	private static ResourceBundle sm_resources;

	static {
		sm_resources = ResourceBundle.getBundle("com.tibco.devtools.workspace.installer.standalone.ConstraintExceptionMessages");
	}
}
