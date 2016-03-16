package com.tibco.devtools.workspace.installer.standalone;

public class EmptyProgressReport implements ProgressReport {

	public void message(String message) {
		// does nothing...
	}

	public ProgressReport createChild(String message) {
		// does nothing
		return this;
	}

}
