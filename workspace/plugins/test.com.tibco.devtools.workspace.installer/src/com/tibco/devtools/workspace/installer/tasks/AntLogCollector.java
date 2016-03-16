package com.tibco.devtools.workspace.installer.tasks;

import java.util.List;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

import com.tibco.devtools.workspace.util.DataUtils;

public class AntLogCollector implements BuildListener {

	List<BuildEvent> m_events = DataUtils.newList();
	
	/**
	 * This is a very crude method for verifying that at least one of the events produced by
	 * the ant activities contains the string I'm looking for.
	 * 
	 * @param msg	The message I want to verify.
	 * @return	<code>true</code> if the message was found.
	 */
	public boolean containsMessage(String ... msgs ) {
		for (BuildEvent evt : m_events) {
			boolean hasAll = true;
			int idx = 0;
			while (hasAll && idx < msgs.length ) {
				 hasAll = evt.getMessage().contains(msgs[idx]);
				 idx++;
			}
			if (hasAll)
				return true;
		}
		
		return false;
	}
	
	public void buildFinished(BuildEvent event) {
	}

	public void buildStarted(BuildEvent event) {
	}

	public void messageLogged(BuildEvent event) {
		m_events.add(event);
	}

	public void targetFinished(BuildEvent event) {
	}

	public void targetStarted(BuildEvent event) {
	}

	public void taskFinished(BuildEvent event) {
	}

	public void taskStarted(BuildEvent event) {
	}
	
}
