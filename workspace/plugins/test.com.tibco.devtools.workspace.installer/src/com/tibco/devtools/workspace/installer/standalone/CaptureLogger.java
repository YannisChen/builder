package com.tibco.devtools.workspace.installer.standalone;

import java.util.List;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * This logger exists to capture all the output from the constraint solver, so that
 * after the solver has run, we can go back and verify that it has output the right
 * messages.
 *
 * @param <E>	The enumeration type to check.
 * @param <M>	The message type.
 */
public class CaptureLogger<E extends Enum<E>, M extends Object > implements LogOutput<E, M> {

	public void debug(E code, M message) {
		m_debug.add ( new LogEntry<E, M>(code, message));
	}

	public void error(E code, M message) {
		m_error.add ( new LogEntry<E, M>(code, message));
	}

	public void warning(E code, M message) {
		m_warning.add ( new LogEntry<E, M>(code, message));
	}

	public List< LogEntry<E, M> > getErrors() {
		return m_error;
	}

	public List< LogEntry<E, M> > getWarnings() {
		return m_warning;
	}
	
	public List< LogEntry<E, M> > getDebug() {
		return m_debug;
	}
	
	public M matchMessage(List<LogEntry<E, M> > msgs, E enumVal, String ... params) {
		for ( LogEntry<E, M> msg : msgs) {
			// look for all messages that match the enumeration.
			if (enumVal == null || msg.getEnumValue().equals(enumVal)) {
				String msgStr = msg.getMessage().toString();
				boolean matchedAll = true;
				for (String item : params) {
					matchedAll = msgStr.contains(item);
					if (!matchedAll)
						break;
				}
				if (matchedAll)
					return msg.getMessage();
			}
		}
		
		return null;
	}
	
	public static class LogEntry<E extends Enum<E>, M extends Object > {
		
		public LogEntry(E enumVal, M message) {
			m_enumVal = enumVal;
			m_message = message;
		}
		
		public E getEnumValue() {
			return m_enumVal;
		}
		
		public M getMessage() {
			return m_message;
		}
		
		/**
		 * So it is easy to see in the debugger...
		 */
		public String toString() {
			return m_enumVal.toString() + " : " + m_message;
		}
		private E m_enumVal;
		private M m_message;
	}
	
	private List<LogEntry<E, M>> m_debug = DataUtils.newList();
	private List<LogEntry<E, M>> m_error = DataUtils.newList();
	private List<LogEntry<E, M>> m_warning = DataUtils.newList();
}
