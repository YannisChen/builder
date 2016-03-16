package com.tibco.devtools.workspace.model;

import java.util.List;

/**
 * A Range represents a minimum and maximum value that (inclusive or exclusive),
 * and is meant to wrap the OSGi and Eclipse concepts of version ranges, but
 * generic around the notion of what is being compared.
 * 
 * @param <T>	A {@link Comparable} type.
 */
public class Range<T extends Comparable<T> > {	

	/**
	 * Initialize with a particular target and version range.
	 * @param minimum	The minimum acceptable version.
	 * @param isMinimumInclusive	Include minimal version?
	 * @param maximum		The maximum acceptable version.
	 * @param isMaximumInclusive	Include the maximal version?
	 */
	public Range(T minimum,
			boolean isMinimumInclusive,
			T maximum, boolean isMaximumInclusive) {
		
		m_minimum = minimum;
		m_minInclusive = isMinimumInclusive;
		m_maximum = maximum;
		m_maxInclusive = isMaximumInclusive;
        
        int comparison = minimum.compareTo(maximum);
        if (comparison > 0 || (comparison == 0 && !isMinimumInclusive && !isMaximumInclusive) ) {
            throw new IllegalArgumentException("Empty version constraint specified, where minimum is " + minimum.toString() +
            		" and maximum is " + maximum.toString() );
        }
	}
	
	protected Range(Range<T> toCopy) {
		this(toCopy.getMinimumRange(), toCopy.isMinimumInclusive(),
				toCopy.getMaximumRange(), toCopy.isMaximumInclusive() );
	}
	/**
	 * Return the low end of this range.
	 * @return	The low end of the range....
	 */
	public T getMinimumRange() {
		return m_minimum;
	}
	
	/**
	 * Returns true if the range in question is an exact match.
	 * 
	 * @return <code>true</code> if the range is a precise match for one item.
	 */
	public boolean isExact() {
		return m_minInclusive && m_maxInclusive && m_minimum.equals(m_maximum);
	}
	/**
	 * Is the minimum constraint value included in the range?
	 * 
	 * @return <code>true</code> if the minimum version is in the range.
	 */
	public boolean isMinimumInclusive() {
		return m_minInclusive;
	}
	
	/**
     *  Get the maximum range of the version constraint. 
     * @return  The upper end of the range.
	 */
    public T getMaximumRange() {
		return m_maximum;
	}
	
	/**
     * Is the maximum constraint value included in the range? 
     * @return  <code>true</code> if the maximum version is in the range.
	 */
    public boolean isMaximumInclusive() {
		return m_maxInclusive;
	}
	
	/**
	 * Does the indicated target item fall within the range of this version constraint?
	 */
	public boolean isInRange(T target) {
		int lowCompare = m_minimum.compareTo(target);
		if (lowCompare > 0 || (!m_minInclusive && lowCompare == 0) ) {
			return false;
		}
		
		int highCompare = m_maximum.compareTo(target);
		if (highCompare < 0 || (!m_maxInclusive && highCompare == 0 )) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Compute whether a value falls below a range (-1), within the range 0,
	 * or above the range (1).
	 * 
	 * @param value	The value to test.
	 * 
	 * @return	Similar to the {@link Comparable#compareTo(Object)} method, except that
	 * zero is returned for any value within the range.
	 */
	public int match(T value) {
		int result = 0;
		int compare = value.compareTo(m_minimum);
		if ( (m_minInclusive && compare < 0 ) || ( !m_minInclusive && compare <= 0) ) {
			result = -1;
		}
		else {
			compare = value.compareTo(m_maximum);
			if ( (m_maxInclusive && compare > 0) || ( !m_maxInclusive && compare >= 0 ) ) {
				result = 1;
			}
		}
		
		return result;
	}
    //=========================================================================
    // Public static methods
    //=========================================================================
    
    /**
     * Take a list of constraints, and collapse them into a single constraint,
     * or return null if there is no overlapping set.
     * 
     * @param importReq The list of constraints to collapse.
     * @return  The intersection of the given constraints.
     */
    public static <T extends Comparable<T> > Range<T> computeIntersection(List< Range<T> > constraints) {

        if (constraints.size() == 0) {
            return null;
        }
        
        Range<T> baseline = constraints.get(0);
        if (constraints.size() == 1) {
            return baseline;
        }
        
        Range<T> possibleResult = new Range<T>(baseline);
        
        for (int idx = 1 ; idx < constraints.size() ; idx++) {
        	Range<T> next = constraints.get(idx);
            
            // reset new low, as appropriate.
            int lowCompare = possibleResult.m_minimum.compareTo(next.getMinimumRange() ); 
            if (lowCompare < 0) {
                possibleResult.m_minimum = next.getMinimumRange();
                possibleResult.m_minInclusive = next.isMinimumInclusive();
            }
            else if (lowCompare == 0 && possibleResult.m_minInclusive && !next.isMinimumInclusive() ) {
                possibleResult.m_minInclusive = false;
            }
            
            // reset new high as appropriate.
            int highCompare = possibleResult.m_maximum.compareTo(next.getMaximumRange());
            if (highCompare > 0) {
                possibleResult.m_maximum = next.getMaximumRange();
                possibleResult.m_maxInclusive = next.isMaximumInclusive();
            }
            else if (highCompare == 0 && possibleResult.m_maxInclusive && !next.isMaximumInclusive()) {
                possibleResult.m_maxInclusive = false;
            }
        }
        
        // now see if the low and the high overlap.
        Range<T> result = null;
        int endComparison = possibleResult.m_minimum.compareTo(possibleResult.m_maximum);
        
        // if low and high are equal, only match if also inclusive of both.
        if (endComparison == 0 && possibleResult.m_minInclusive && possibleResult.m_maxInclusive) {
            result = possibleResult;
        }
        // low less than high - take it.
        else if (endComparison < 0) {
            result = possibleResult;
        }
        
        return result;
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
		buf.append( isMinimumInclusive() ? "[" : "(" );
		buf.append( m_minimum.toString() );
		buf.append( "," );
		buf.append( m_maximum.toString() );
		buf.append( isMaximumInclusive() ? "]" : ")" );
		
		return buf.toString();
	}

    //=========================================================================
    // Member data
    //=========================================================================
    
	private T m_minimum;
	
	private boolean m_minInclusive;
	
	private T m_maximum;

	private boolean m_maxInclusive;

}
