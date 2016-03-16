package com.tibco.devtools.workspace.installer.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.installer.standalone.LogOutput;

/**
 * Does log output of enumerated errors, but using Ant task facilities, so that
 * Ant configuration controls the output.
 * 
 * @param <E>	The enumeration of error codes.
 * @param <M>	The message associated with the enumerated error.
 */
public class AntTaskOutputLogger<E extends Enum<E>, M> implements LogOutput<E, M> {

	public AntTaskOutputLogger(Task task) {
		m_task = task;
	}
	
	public void debug(E code, M message) {
		log(Project.MSG_DEBUG, code, message);
	}

	public void error(E code, M message) {
		log(Project.MSG_ERR, code, message);
	}

	public void warning(E code, M message) {
		log(Project.MSG_WARN, code, message);
	}

	private void log(int level, E code, M message) {
		m_task.log(code.toString() + ": " + message.toString(), level);
	}
	
	private Task m_task;
}
