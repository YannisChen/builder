package com.tibco.devtools.workspace.installer.standalone;

/**
 * For the stand-alone installation of packages, a progress interface
 * of sorts.
 */
public interface ProgressReport {

	void message(String message);
	
	ProgressReport createChild(String message);
}
