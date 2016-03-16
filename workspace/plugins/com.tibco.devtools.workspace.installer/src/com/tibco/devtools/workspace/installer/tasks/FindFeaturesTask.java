package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;

import com.tibco.devtools.workspace.installer.utils.FeatureUtils;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * A task that finds all the features from a path, and turns it into a resource set with
 * the given ID.
 * 
 * <p>Note that this task follows the pattern the "Concat" task sets in Ant 1.7.1, and
 * is <i>itself</i> a resource collection.  To use this task, you put an "id" attribute
 * on the task itself, and then refer to that ID elsewhere when a resource collection
 * is needed.</p>
 */
public class FindFeaturesTask extends Task implements ResourceCollection {

	public void add(ResourceCollection rc) {
		if (m_initialResources == null) {
			m_initialResources = new Resources();
		}
		
		m_initialResources.add(rc);
	}
	
	@Override
	public void execute() throws BuildException {
		
		if (m_initialResources == null) {
			throw new BuildException("Must specify one or more resource collection as child elements.");
		}
		
		m_resources = FindFeaturesTask.getFeatureResourceCollection(this, m_initialResources);
		m_initialResources = null;
	}

	/**
	 * Get the features from the path member
	 */
    public static ResourceCollection getFeatureResourceCollection(Task context, ResourceCollection rc) {

    	List<File> dirList = DataUtils.newList();
    	List<File> fileList = DataUtils.newList();
    	
    	Iterator<Resource> itRes = TaskUtils.safeIterator(rc);
    	
    	Resources result = new Resources();
    	
    	// Loop through the resources in my collection, and for each directory, add to a list.
    	while (itRes.hasNext()) {
    		Resource res = itRes.next();
    		
    		// Note that here we don't rely on res.isFilesystemOnly(), because we need to be
    		// able to do the cast to FileResource to get the file....
    		if (res instanceof FileResource && res.isDirectory()) {
    			
    			File dir = ( (FileResource) res).getFile();
    			dirList.add(dir);
    		}
    		else if (res.isExists() ) {
    			// add other resources to a collection.
    			result.add(res);
    		}
    		else {
    			context.log("Discarding feature element " + res.toString() + " because it doesn't exist.", Project.MSG_VERBOSE);
    		}
    	}

    	// now enumerate those feature files based on the directory list I've got.
	    List<File> features = FeatureUtils.enumerateFeatureFiles(dirList);
	    features.addAll(fileList);
	    
	    // now turn that list of Files into a ResourceCollection.
	    for (File oneFile : features) {
	    	FileResource fileRes = new FileResource(oneFile);
	    	result.add(fileRes);
	    }
	    
	    if (result.size() == 0) {
	    	throw new BuildException("No features found in the folders specified by featuresearchpath.");
	    }
	    return result;
    }

	//=========================================================================
	// ResourceCollection implementation - delegates to internal resource collection.
	//=========================================================================	

	public boolean isFilesystemOnly() {
		return m_resources.isFilesystemOnly();
	}

	// Suppress warnings here, it is an Ant API issue.
	@SuppressWarnings("unchecked")
	public Iterator iterator() {
		return m_resources.iterator();
	}

	public int size() {
		return m_resources.size();
	}
	
	
	//=========================================================================
	// internal data.
	//=========================================================================
	
	/**
	 * Resources we're created with.
	 */
	private Resources m_initialResources;

	/**
	 * Resources we evaluate to.
	 */
	private ResourceCollection m_resources;

}
