package com.tibco.devtools.workspace.installer.standalone;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;


/**
 * Keeps track of why a particular feature choice has been made
 */
public class ChoiceDetails<V extends Comparable<V>, F > {

	public ChoiceDetails(Target<V> target, F fd, boolean fixed) {
		m_target = target;
		m_feature = fd;
		m_constraints = new HashSet<TargetConstraint<V,SolverConstraintSource>>();
		m_isFixedChoice = fixed;
	}
	
	/**
	 * Is this particular choice a fixed choice?
	 * 
	 * @return	<code>true</code> if the choice is fixed.
	 */
	public boolean isFixedChoice() {
		return m_isFixedChoice;
	}

	public Target<V> getTarget() {
		return m_target;
	}
	
	public F getFeature() {
		return m_feature;
	}
	
	public Collection< TargetConstraint<V, SolverConstraintSource > > getConstraints() {
		return m_constraints;
	}
	
	private Target<V> m_target;
	
	private F m_feature;

	/**
	 * Keeps track of the set of all constraints used to make a particular
	 * choice.
	 * 
	 * <p>It is actually fairly useful that this is a Set, so that duplicate additions
	 * are ignored.  This makes messages we might put out much more concise.
	 * </p>
	 */
	private Set<TargetConstraint<V, SolverConstraintSource >> m_constraints;
	
	/**
	 * Is this a fixed choice result, that is one of the fixed inputs to the
	 * resolution process?
	 */
	private boolean m_isFixedChoice;

}
