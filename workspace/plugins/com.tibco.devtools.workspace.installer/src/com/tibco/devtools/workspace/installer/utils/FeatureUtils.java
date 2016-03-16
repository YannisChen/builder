package com.tibco.devtools.workspace.installer.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Utility methods for dealing with features.
 */
public class FeatureUtils {

	public static final String WS_IGNORE = ".ws-ignore";

	/**
	 * This method will sort through a given set of feature models, and return a "sorted" list
	 * by dependency, with models with no dependencies first, and the ones with the most dependencies
	 * last.
	 * 
	 * @param features	The list of features to "sort."
	 * @return The sorted list.
	 */
	public static List<FeatureDescriptor> sortFeaturesByDependency(List<FeatureDescriptor> features) {
		
		Map<String, FeatureDescriptor> idToModel = DataUtils.newMap();
		
		for (FeatureDescriptor item : features) {
			idToModel.put(item.getTarget().getTargetId(), item);
		}
		
		List<FeatureDescriptor> result = new ArrayList<FeatureDescriptor> ();
		for (FeatureDescriptor item: features) {
			depthFirstSearch(result, item, idToModel);
		}
		return result;
		
	}

	/**
	 * This is our little bit of recursive joy, to find the leaf-most entries in our dependency graph,
	 * and output those first.
	 * 
	 * @param result	The collection with the ordered results of the search.
	 * @param item		The item we're currently traversing.
	 * @param idToModel	A map of feature identifier to corresponding feature file and model.
	 */
	private static void depthFirstSearch(Collection<FeatureDescriptor> result, FeatureDescriptor item, Map<String, FeatureDescriptor> idToModel) {
		
		// has this item already been added to the output list?
		if (!result.contains(item) ) {
			
			// nope, get the model, and traverse the imports.

			List<TargetConstraint<VersionInfo, FeatureDescriptor >> imports = item.getFeatureConstraints();
			for ( TargetConstraint<VersionInfo, ?> imp : imports) {
				FeatureDescriptor referencedLfm = idToModel.get( imp.getTargetName() );
				
				// does the referenced feature model exist in our set of features?
				if (referencedLfm != null) {
					// yes, scan it.
					depthFirstSearch(result, referencedLfm, idToModel);
				}
			}
				
			result.add(item);
			
		}
	}

	/**
	 * This function returns true if the directory in question is a special Eclipse folder,
	 * so that searching for feature.xml simply skips the folder.
	 * 
	 * @param folder  The folder to check.
	 * @return <code>false</code> if the folder is a normal folder, <code>true</code>
	 * otherwise.
	 */
	public static boolean isSpecialEclipseFolder(File folder) {
		
		String name = folder.getName();
		if ( name.equals(".metadata") || name.equals(".settings") ) {
			return true;
		}
		
		File eclipseExtension = new File(folder, ".eclipseextension");
		if (eclipseExtension.exists()) {
			return true;
		}
		
		File eclipseProduct = new File(folder, ".eclipseproduct");
		if (eclipseProduct.exists()) {
			return true;
		}
		
		// Our special indicator that this folder should be ignored.
		File toIgnore = new File(folder, WS_IGNORE);
		if (toIgnore.exists() ) {
			return true;
		}
		
		return false;
	}

	/**
	 * Use recursion to scan the folders looking for any folder that contains a ".project" file.
	 * @param folder  The folder to search.
	 * @param collectedResults    The list of all of the found files.
	 */
	public static void searchFolderForFeatureFiles(File folder, List<File> collectedResults) {
	
	    // does this folder have a ".project" file?
	    File featureFile = new File(folder, "feature.xml");
	    if (featureFile.isFile()) {
	        collectedResults.add(featureFile);
	    }
	    else {
	        // to truly shortcut this process, do not progress further if this is
	        // a project folder...
	        File projectFile = new File(folder, ".project");
	        if (!projectFile.exists() ) {
	            File contents[] = folder.listFiles();
	            if (contents == null) {
	                throw new IllegalArgumentException("Attempting to recursively scan " + folder.toString() + " but that location is not a folder.");
	            }
	            for (File item : contents) {
	                // Not sure exactly why this is, but the WizardProjectsImportPage.collectProjectFilesFromDirectory
	                // also excludes the ".metadata" folder from its search.
	                if (item.isDirectory()
	                		&& !isSpecialEclipseFolder(item) ) {
	                    searchFolderForFeatureFiles(item, collectedResults);
	                }
	            }
	        }
	    }
	}

	/**
	 * Go through all of the search folders looking for build features....
	 * @return The list of {@link File}s pointing to feature.xmls.
	 */
	public static List<File> enumerateFeatureFiles(List<File> searchPath) {
	
	    List<File> buildFeatures = new ArrayList<File>();
	    for (File searchFolder : searchPath) {
	        searchFolderForFeatureFiles(searchFolder, buildFeatures);
	    }
	
	    return buildFeatures;
	}

	/**
	 * For a list of {@link File}s of features, create a list of {@link FeatureDescriptor}s.
	 * @param featureFiles    The files to turn into feature models.
	 *
	 * @return The appropriate list of {@link FeatureDescriptor}s.
	 *
	 * @throws IOException If unable to read one of the files.
	 * @throws SAXException If unable to parse one of the files.
	 */
	public static List<FeatureDescriptor> getLocatedFeatureList(List<File> featureFiles) throws IOException, SAXException {
	
	    List<FeatureDescriptor> results = new ArrayList<FeatureDescriptor>();
	    if (featureFiles != null) {
	        for (File featureFile : featureFiles) {
	        	FeatureDescriptor descriptor = FeatureDescriptor.fromFile(featureFile);
	            results.add( descriptor );
	        }
	    }
	    return results;
	}

	public static List<FeatureDescriptor> getBuildFeatures(List<File> featureSearchPath)
        throws IOException, SAXException {
		List<File> featureFiles = enumerateFeatureFiles(featureSearchPath );
	    return getLocatedFeatureList(featureFiles);
	}
}
