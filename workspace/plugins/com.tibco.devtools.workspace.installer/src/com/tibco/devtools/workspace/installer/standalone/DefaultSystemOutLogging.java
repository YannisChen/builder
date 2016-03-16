package com.tibco.devtools.workspace.installer.standalone;

/**
 * A default logger that merely writes everything to System.out.
 *
 * @param <E>	An enumeration type.
 * @param <M>	An object holding a message.
 */
public class DefaultSystemOutLogging<E extends Enum<E>, M extends Object > implements LogOutput<E, M> {

	public void debug(E code, M message) {
		System.out.println(code.toString() + ": " + message.toString() );
	}

	public void error(E code, M message) {
		System.out.println(code.toString() + ": " + message.toString() );
	}

	public void warning(E code, M message) {
		System.out.println(code.toString() + ": " + message.toString() );
	}

}
