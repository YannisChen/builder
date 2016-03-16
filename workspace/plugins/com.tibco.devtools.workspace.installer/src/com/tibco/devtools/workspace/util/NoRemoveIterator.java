package com.tibco.devtools.workspace.util;

import java.util.Iterator;

/**
 * Silly little base class which flags that an iterator doesn't have the
 * remove method.
 *
 * @param <E>	Whatever type is being iterated over.
 */
public abstract class NoRemoveIterator<E> implements Iterator<E>, Iterable<E> {
	
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported on this iterator.");
	}

	public Iterator<E> iterator() {
		return this;
	}
	
	
}
