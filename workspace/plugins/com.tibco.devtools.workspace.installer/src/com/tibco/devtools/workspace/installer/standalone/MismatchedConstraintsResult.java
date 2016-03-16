package com.tibco.devtools.workspace.installer.standalone;

import java.util.List;

import com.tibco.devtools.workspace.model.TargetConstraint;

/**
 * Mismatched constraint results arise, for example, when you're using LBLM algorithm,
 * and one of the starting features states a low-end range that gets raised by
 * other constraints - this class captures that result.
 * 
 * @param <T>	Type for a "version number".
 * @param <S>	Type of the source object for constraints.
 */
public class MismatchedConstraintsResult< T extends Comparable<T>, S> {

	public MismatchedConstraintsResult( List< TargetConstraint<T, S> > startingConstraint,
			List< TargetConstraint<T, S> > narrowerConstraints, T chosen) {
		m_startingConstraints = startingConstraint;
		m_narrowerConstraints = narrowerConstraints;
		m_chosen = chosen;
	}
	
	public List< TargetConstraint<T, S> > getStartingConstraints() {
		return m_startingConstraints;
	}
	
	public List< TargetConstraint<T, S> > getNarrowerConstraints() {
		return m_narrowerConstraints;
	}
	
	/**
	 * Get the name of the feature identifier for this result object.
	 * @return	The feature ID.
	 */
	public String getFeatureId() {
		return m_startingConstraints.get(0).getTargetName();
	}
	
	public T getChosenResult() {
		return m_chosen;
	}
	
	//=========================================================================
	// Private data
	//=========================================================================
	
	private List< TargetConstraint<T, S> > m_startingConstraints;
	
	private List< TargetConstraint<T, S> >	m_narrowerConstraints;
	
	private T m_chosen;

}
