package com.tibco.devtools.workspace.installer.standalone;

import java.util.Collection;
import java.util.List;

import com.tibco.devtools.workspace.model.Feature;
import com.tibco.devtools.workspace.model.Target;

/**
 * Provides the minimal information needed by the {@link ConstraintSolver} to resolve
 * feature data.
 */
public interface FeatureSource<V extends Comparable<V>, F extends Feature<V, F> > {

	/**
	 * Get a feature set by identifier.
	 * 
	 * @param featureId	The feature for which the "set" is desired.
	 * 
	 * @return	The 
	 */
	List<Target<V> > getFeatureVersionSetById(String featureId);
	
	/**
	 * Get a feature model for a particular feature.
	 * 
	 * <p>Note that this method is expected to cache its results, so that subsequent
	 * invocations will not access the network again.</p>
	 * 
	 * @param target	The target feature version and ID to retrieve.
	 * 
	 * @return	An instance of a feature model.
	 */
	F getFeatureModel(Target<V> target);

	/**
	 * Return the list of available feature ids.
	 * 
	 * @return	The list of available feature identifiers.
	 */
	Collection<String> getAvailableFeatureIds();
}
