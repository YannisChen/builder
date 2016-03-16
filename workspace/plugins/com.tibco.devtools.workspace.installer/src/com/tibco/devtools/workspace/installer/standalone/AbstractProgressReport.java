package com.tibco.devtools.workspace.installer.standalone;

/**
 * Captures the abstract notion of a progress reporter with parent/child relationships
 * without dictating how the messages are actually output.
 */
public abstract class AbstractProgressReport<PR extends AbstractProgressReport<PR> > implements ProgressReport {

	/**
	 * Subclasses should instantiate the appropriate child object.
	 * @param parent The parent of the new child.
	 * @return The newly created child.
	 */
	public abstract PR newChild(AbstractProgressReport<PR> parent);
	
	/**
	 * Output the message.
	 * 
	 * @param message
	 */
	public abstract void output(String message);

	public AbstractProgressReport(AbstractProgressReport<PR> parent) {
		m_parent = parent;
	}
	
	public void message(String message) {
		if (m_parent != null) {
			m_parent.messageFromChild(message);
		}
		else {
			output(message);
		}
	}

	public ProgressReport createChild(String message) {
		message(message);
		return newChild(this);
	}

	void messageFromChild(String message) {
		message("  " + message);
	}

	private AbstractProgressReport<PR> m_parent;

}
