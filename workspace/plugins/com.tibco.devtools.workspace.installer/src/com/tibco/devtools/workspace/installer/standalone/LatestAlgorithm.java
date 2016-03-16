package com.tibco.devtools.workspace.installer.standalone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.tibco.devtools.workspace.installer.standalone.ChoiceAlgorithm;
import com.tibco.devtools.workspace.model.Feature;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;

/**
 * Favors picking the latest possible match for a given feature.
 */
public class LatestAlgorithm<V extends Comparable<V> > implements ChoiceAlgorithm<V> {

	public Target<V> chooseTarget(List< Target<V> > options,
			Range<V> constraint) {
		
		List<Target<V> > copy = new ArrayList<Target<V>>(options);
		// OK, so if Java had reverse iterable out of the box, I'd use that....
		Collections.sort(copy);
		Collections.reverse(copy);
		return pickFirst(constraint, copy);
	}

	/**
	 * Partition constraints, such that every constraint that falls above the
	 * target in question must be impossible - we're trying to grab the latest
	 * after all, an everything under the range might be matched by a different
	 * version choice.
	 */
	public <TC extends TargetConstraint<V, ?>>
	void partitionConstaintsAroundVersion(
			Target<V> target,
			List<TC> constraints,
			List<TC> possible,
			List<TC> impossible) {

		for (TC constraint : constraints) {
			int match = constraint.getRange().match(target.getVersion() );
			if (match > 0) {
				impossible.add(constraint);
			}
			else if (match < 0) {
				possible.add(constraint);
			}
		}
	}

	public Range<V> pickNewSourceRange(List<Target<V>> sourceOptions,
			Target<V> target, Range<V> sourceRange) {

	    Range<V> newRange = new Range<V>(sourceRange.getMinimumRange(),
	    		sourceRange.isMinimumInclusive(), target.getVersion(), false);
	    
	    return atLeastOneMatchForRange(sourceOptions, newRange);
	}

	/**
	 * Returns the range passed in if at least one of the source options falls within
	 * that range - otherwise null.
	 * 
	 * @param options	options to verify against the range.
	 * @param range		The range we're testing.
	 * 
	 * @return	The range itself, if any matches are found.
	 */
	public static <T extends Comparable<T>> Range<T> atLeastOneMatchForRange(
			List<? extends Target<T> > options, Range<T> range) {
		for (Target<T> item : options) {
			if (range.isInRange(item.getVersion() ) )
				return range;
		}
		return null;
	}

	/**
	 * Utility method that returns the first target in the list that matches
	 * the given constraints.
	 * 
	 * @param constraint	The constraint to apply
	 * @param latest		The set of options to find a hit amongst.
	 * 
	 * @return	<code>null</code> if no match could be found, otherwise, the first one
	 * within the constraint.
	 */
	public static <V extends Comparable<V> > Target<V> pickFirst(Range<V> constraint,
			List<Target<V> > latest) {
		
		for (Target<V> target : latest) {
			if (constraint.isInRange( target.getVersion() ))
				return target;
		}
		return null;
	}

	/**
	 * For latest, we'll always just assume that all is OK.
	 */
	public MismatchedConstraintsResult<V, SolverConstraintSource>
	hasQuestionableStartingConstraints(
			List<TargetConstraint<V, SolverConstraintSource>> original,
			Collection<TargetConstraint<V, SolverConstraintSource>> all,
			Collection< Target<V> > available, V chosen) {
		return null;
	}

	public <F extends Feature<V, F> > void warningsOnResults(FeatureSource<V, F> siteInfo,
			Collection<Target<V>> results) {
		
		// The latest algorithm doesn't report any warnings.
	}

}
