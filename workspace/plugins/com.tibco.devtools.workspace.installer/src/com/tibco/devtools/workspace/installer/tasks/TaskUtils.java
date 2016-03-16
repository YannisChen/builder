package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * Utility class for functions related to ant tasks.
 */
public class TaskUtils {

	/**
	 * Rote code for parsing a String into a version as part of an Ant task or data type.
	 * @param version	The string to parse.
	 * @return	The resulting version number.
	 * @throws BuildException
	 */
	public static VersionInfo parseVersion(String version) throws BuildException {
		
		try {
			return VersionInfo.parseVersion(version);
		} catch (IllegalArgumentException e) {
			// rethrow as a build exception so that the information goes to the console.
			throw new BuildException(e);
		}
	}
	/**
	 * Does the obvious checks on a path to make sure it is a valid path element.
	 * 
	 * @param element	The String to check.
	 * @return
	 */
	public static boolean isEmptyPathElement(String element) {
		return (element == null) || (element.length() == 0) ||
		     (element.trim().length() == 0);
	}

	/**
	 * Does the obvious checks on a path to make sure it is a valid path element.
	 * 
	 * @param element	The String to check.
	 * @return
	 */
	public static boolean isUndefinedPathElement(String element) {
		return (element.indexOf("${") >= 0);
	}

	/**
	 * Does the obvious checks on a path to make sure it is a valid path element.
	 * 
	 * @param element	The String to check.
	 * @return
	 */
	public static boolean isInvalidPathElement(String element) {
		return (element == null) || (element.length() == 0) ||
		     (element.trim().length() == 0) || (element.indexOf("${") >= 0);
	}

	/**
	 * Get the normalized directories for a path
	 * 
	 * @param path	The path to analyze.
	 * 
	 * @return the set of vetted results.
	 */
	public static List<File> extractNormalizedDirs(Task task, Path path) {
	    List<File> result = new ArrayList<File>();
	    String [] pathList = path.list();
	    for (String element : pathList) {
	        if (isEmptyPathElement(element) ) {
	        	task.log("Discarding path element <" + element + ">", Project.MSG_VERBOSE);
	            continue;
	        }
	        if (isUndefinedPathElement(element)) {
	        	task.log("Discarding path element <" + element + "> because it is unresolved.", Project.MSG_VERBOSE);
	        	continue;
	        }
	        
	        File file = new File(element);
	        if ((file != null) && file.exists() && file.isDirectory())
	            result.add(file);
	        else
	        	task.log("Discarding path element <" + element + "> because a directory was not found at that path.", Project.MSG_VERBOSE);
	    }
	    return result;
	}

	/**
	 * Go through a path value, and get all the non-empty items, throwing an error
	 * if there are no valid items remaining.
	 * 
	 * @param task	The task that we're doing this for.
	 * @param paths	The paths to scan.
	 * @param failureMsg	The failure message to show, should something go wrong.
	 * 
	 * @return	The list of file entries found.
	 */
	public static List<File> getNonEmptyDirectoryList(Task task, Path paths, String failureMsg) {
		List<File> result = extractNormalizedDirs(task, paths);
		if (result.size() == 0) {
			throw new BuildException(failureMsg, task.getLocation());
		}
		
		return result;
	}
	
	/**
	 * Ant Javadoc assures me that this iterator must be of type Resource....
	 * 
	 * @param coll	The collection for which I want a type-safe iterator.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Iterator<Resource> safeIterator(ResourceCollection coll) {
		return coll.iterator();
	}
	
	/**
	 * Turn a resource collection into a list of feature descriptors.
	 * 
	 * <p>Note that this method will output a warning if the resources are missing.</p>
	 * 
	 * @param resources
	 * @return
	 * @throws IOException
	 */
	public static List<FeatureDescriptor> resourceCollectionToFeaturesList(
			Task task, ResourceCollection resources) throws IOException {
		List<FeatureDescriptor> results = new ArrayList<FeatureDescriptor>();
	    
	    // now, loop through the resources, and turn them into feature descriptors.
	    Iterator<Resource> itRes = safeIterator(resources);
	    while (itRes.hasNext()) {
	    	Resource res = itRes.next();
	    	if (res.isExists()) {
		    	FeatureDescriptor descriptor = FeatureDescriptor.fromStream( res.getInputStream() );
		    	
		    	// so that we can maximize the amount of information we capture here,
		    	// check to see what sub-type of resource, and capture the info.
		    	if (res instanceof FileResource) {
		    		descriptor.setFileLocation( ((FileResource) res).getFile() );
		    	}
		    	else if (res instanceof URLResource) {
		    		descriptor.setUrlLocation( ((URLResource) res).getURL() );
		    	}
		    	
		        results.add( descriptor );
	    	}
	    	else {
	    		task.log("Resource " + res.toLongString() + " not found.", Project.MSG_WARN);
	    	}
	    }
		return results;
	}

}
