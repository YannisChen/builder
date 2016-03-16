package com.tibco.devtools.workspace.installer.standalone;

/**
 * The choices of how we are doing caching from remote sites
 */
public enum CachingState {

	/**
	 * Use both the local cache, and remote state when entries aren't in the cache.
	 */
	LOCAL_AND_REMOTE,
	
	/**
	 * Use only the local cache, with no access to remote sites whatsoever.
	 */
	ONLY_LOCAL,
	
	/**
	 * Use only the remote copies of files.
	 */
	ONLY_REMOTE
}
