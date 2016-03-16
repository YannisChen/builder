package com.tibco.devtools.workspace.installer.standalone;

import com.tibco.devtools.workspace.model.Feature;

/**
 * Any TargetConstraint used by the solver MUST have something
 * that implements this interface as the source of the constraint.
 * 
 */
public interface SolverConstraintSource {

	/**
	 * When a constraint source corresponds to a feature, this method returns
	 * the identity of the target of that feature.
	 * @return The identity of the target of the feature that provided this
	 * constraint, or <code>null</code> if the constraint did not come
	 * from a feature.
	 * 
	 * @see Feature#getTarget()
	 */
	public String getSourceLogicalId();
	
	/**
	 * For error messages, logging, and other, return a description of the
	 * constraint source.
	 * 
	 * <p>For a feature file, this will correspond to the {@link Target} string,
	 * plus a location for the file.</p>
	 * 
	 * @return	A string describing the source.
	 */
	public String getSourcePathString();
}
