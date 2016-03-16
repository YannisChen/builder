package com.tibco.devtools.workspace.installer.tasks;

import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.standalone.AbstractProgressReport;

/**
 * Progress reporter that uses ant logging mechanisms - so that output can be captured
 * in all the standard ways that Ant captures output.
 */
public class AntLogProgressReport extends AbstractProgressReport<AntLogProgressReport> {

	public AntLogProgressReport(Task owningTask) {
		super(null);
		m_task = owningTask;
	}
	
	private AntLogProgressReport(Task owningTask, AbstractProgressReport<AntLogProgressReport> parent) {
		super(parent);
		m_task = owningTask;
	}
	
	@Override
	public AntLogProgressReport newChild(AbstractProgressReport<AntLogProgressReport> parent) {
		return new AntLogProgressReport(m_task, parent);
	}

	@Override
	public void output(String message) {
		m_task.log(message);
	}

	private Task m_task;
}
