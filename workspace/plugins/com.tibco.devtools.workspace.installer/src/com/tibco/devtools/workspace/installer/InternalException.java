package com.tibco.devtools.workspace.installer;

/**
 * In cases where the workspace installer is simply bailing out of
 * an operation, with error information already printed to the console,
 * it uses this exception so that the ultimate recipient knows not to
 * bother printing a stack trace.
 * 
 */
public class InternalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InternalException(String message) {
		super(message);
	}
}
