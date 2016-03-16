package com.tibco.devtools.workspace.installer.standalone;


/**
 * Reports messages to the console.
 */
public class ProgressReportToConsole extends AbstractProgressReport<ProgressReportToConsole> {

	public ProgressReportToConsole() {
		super(null);
	}
	
	private ProgressReportToConsole(AbstractProgressReport<ProgressReportToConsole> parent) {
		super(parent);
	}

	@Override
	public ProgressReportToConsole newChild(AbstractProgressReport<ProgressReportToConsole> parent) {
		return new ProgressReportToConsole(parent);
	}

	@Override
	public void output(String message) {
		System.out.println(message);
	}
	
}
