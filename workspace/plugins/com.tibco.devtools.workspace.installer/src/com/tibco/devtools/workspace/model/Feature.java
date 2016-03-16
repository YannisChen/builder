package com.tibco.devtools.workspace.model;

import java.util.Collection;
import java.util.List;

import com.tibco.devtools.workspace.installer.standalone.SolverConstraintSource;

/**
 * Representation of a feature, with a set of feature and plugin constraints,
 * as well as plugins it provides.
 * 
 */
public interface Feature<V extends Comparable<V>, F extends Feature<V, F> > extends SolverConstraintSource {

	/**
	 * Get the target for this feature descriptor.
	 * 
	 * @return
	 */
	Target<V> getTarget();

	/**
	 * Get the list of required and optional features that this feature depends upon,
	 * stated as constraints, meaning a minimum and maximum version target applies.
	 * 
	 * @return	The constraints defined by the feature.
	 */
	List< TargetConstraint<V, F > > getFeatureConstraints();
	
	/**
	 * Get the list of plugin constraints that a feature has.
	 * 
	 * @return	A {@link Collection} of plugin constraints.
	 */
	Collection<TargetConstraint<V, F > > getPluginConstraints();
	
	/**
	 * Return the list of plugins provided by a feature.
	 */
	List< PluginReference<V> > getProvidedPlugins();
}
