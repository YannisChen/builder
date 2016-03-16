package com.tibco.devtools.workspace.model;

import java.util.List;

import com.tibco.devtools.workspace.installer.standalone.SolverConstraintSource;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Represents a "closure" for an aggregated set of release units.
 *  
 * @param <V>	A "version" type - must be comparable.
 */
public class AspectClosure<V extends Comparable<V> > {

	public AspectClosure(TargetAggregation<V> identity) {
		m_primary = identity;
	}
	
	/**
	 * Return the identity of the closure
	 * 
	 * @return The identity, including the name of the aggregation, and its
	 * constituent RUs that form its identity.
	 */
	public TargetAggregation<V> getPrimary() {
		return m_primary;
	}
	
	public List< TargetAggregation<V> > getNeededSubsets() {
		return m_neededSubsets;
	}
	
	public List< TargetAggregation<V> > getUnneededSubsets() {
		return m_unneededSubsets;
	}
	
	/**
	 * Which targets were resolved but don't belong to any of the aspects referenced.
	 * 
	 * @return	The list of unattached targets.
	 */
	public List< Target<V> > getUnattachedTargets() {
		return m_unattachedTargets;
	}
	
	public List< TargetConstraint<V, SolverConstraintSource> > getContingentConstraints() {
		return m_contingentConstraints;
	}
	
	/**
	 * What is the identity of this particular closure, including the
	 * constituent RUs?
	 */
	private TargetAggregation<V> m_primary;
	
	/**
	 * Which subset of items from the identity targs of each closure do we need?
	 */
	private List< TargetAggregation<V> > m_neededSubsets = DataUtils.newList();
	
	/**
	 * Which parts of the original closure are not needed for this resolution?
	 */
	private List< TargetAggregation<V> > m_unneededSubsets = DataUtils.newList();
	
	/**
	 * Which targets don't appear as part of any other AggregationClosure, but are
	 * needed as a dependency?
	 */
	private List<Target<V> > m_unattachedTargets = DataUtils.newList();
	
	/**
	 * What contingent constraints, if any, apply to this aspect?
	 */
	private List<TargetConstraint<V, SolverConstraintSource>> m_contingentConstraints = DataUtils.newList();
}
