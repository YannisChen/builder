package com.tibco.devtools.workspace.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *	Identifies a range constraint on a target.
 *
 * @param <I>	Type of the identifying name for a target.
 * @param <V>	The type of a version representation for a target.
 */
public class TargetConstraint<V extends Comparable<V>, S > {

	/**
	 * Initialize with a particular target and version range.
	 * @param source	Where does this come from?
	 * @param isRequired Is the constraint required or optional?
	 * @param targetId	The target in question.
	 * @param range		The range of the constraint.
	 */
	public TargetConstraint(S source, boolean isRequired, String targetId,
			Range<V> range) {
		
		if (targetId == null)
			throw new IllegalArgumentException("Target id must not be null.");
		
		m_source = source;
        m_required = isRequired;
		m_target = targetId;
		m_range = range;
        
	}
	
	/**
	 * Need some notion of safely casting a TargetConstraint<A, B> to
	 * TargetConstraint(A, C), if B extends C.
	 * 
	 * If you can figure out a better way, please have at it.
	 * 
	 * @param <X>
	 * @param <T>
	 * @param <U>
	 * @param constraint
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Comparable<X>, T, U extends T> TargetConstraint<X, T >
	downcastSource( TargetConstraint<X, U> constraint) {
		return (TargetConstraint<X, T>) constraint;
	}
	
	public S getSource() {
		return m_source;
	}
	
	public String getTargetName() {
		return m_target;
	}

	public Range<V> getRange() {
		return m_range;
	}
	
    /**
     * Returns <code>true</code> if the constraint is required, or <code>false</code>
     * if it is optional.
     * 
     * @return  <code>true</code> for a required constraint.
     */
    public boolean isRequired() {
        return m_required;
    }
    
	/**
	 * Does the indicated target item fall within the range of this version constraint?
	 */
	public boolean isMatchForTarget(Target<V> target) {
		if (!m_target.equals(target.getTargetId() ) ) {
			return false;
		}
		
		return m_range.isInRange( target.getVersion() );
	}
	
    //=========================================================================
    // Public static methods
    //=========================================================================
    
    //=========================================================================
	// Member data
	//=========================================================================
	
	/**
	 * Returns true if the given target satisfies all of the given constraints.
	 * 
	 * @param target	The target to test to see whether it meets all constraints.
	 * @param constraints	The set of constraints to apply.
	 */
	public static <V extends Comparable<V>, S> boolean meetsAllConstraints(Target<V> target, List<TargetConstraint<V, S>> constraints) {
		
		boolean meetsAllConstraints = true;
		for (TargetConstraint<V, S> oneConstraint : constraints) {
			if (!oneConstraint.isMatchForTarget(target)) {
				meetsAllConstraints = false;
				break;
			}
		}
		return meetsAllConstraints;
	}

	/**
     * Take a list of constraints, and collapse them into a single constraint,
     * or return null if there is no overlapping set.
     * 
     * @param importReq The list of constraints to collapse.
     * @return  The intersection of the given constraints.
     */
    public static <V extends Comparable<V>, X, TC extends TargetConstraint<V, ?>> Range<V>
    	computeIntersection(Collection<TC> constraints) {

        if (constraints.size() == 0) {
            return null;
        }
        
        List<Range<V> > ranges = new ArrayList<Range<V> >();
        for (TC constraint : constraints) {
        	ranges.add(constraint.getRange());
        }
        
        return Range.computeIntersection(ranges);
    }
    

    //=========================================================================
    // Object methods.
    //=========================================================================
    
	/**
	 * Creates a string of the form targetId[low,high]
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getTargetName());
		buf.append( m_range.toString() );
		
		return buf.toString();
	}

    //=========================================================================
    // Member data
    //=========================================================================
    
    private S m_source;
    
    private boolean m_required;

	private String m_target;
	
	private Range<V> m_range;

}
