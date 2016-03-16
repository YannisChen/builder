package com.tibco.devtools.workspace.model;

/**
 * Represents a generic version number in four parts, with the ability to
 * increment up to each next part.
 * 
 * @param <T>	The class that implements this interface.
 */
public interface Version<T extends Version<T> & Comparable<T>> extends Comparable<T> {

	/**
	 * Bump up to the next "patch" version representation.
	 * 
	 * @return	The next patch version representation
	 */
	T nextPatch();
	
	/**
	 * Bump up to the next "minor" version representation.
	 * 
	 * @return	The next minor version representation
	 */
	T nextMinor();
	
	/**
	 * Bump up to the next "major" version representation.
	 * 
	 * @return	The next minor version representation
	 */
	T nextMajor();
}
