package com.tibco.devtools.workspace.installer.standalone;

import java.util.Collection;
import java.util.List;

import com.tibco.devtools.workspace.model.Feature;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;

/**
 * Chooses which version of a feature to pick based on the set of available
 * features, and the constraints in play.
 */
public interface ChoiceAlgorithm<V extends Comparable<V> > {

	/**
	 * Given a list of target options, and a set of constraints, pick the choice that best
	 * satisfies the constraints, according to whatever rules the algorithm applies.
	 * 
	 * @param options	The choices that the algorithm must choose amongst
	 * @param constraints	The constraint range to apply
	 * 
	 * @return	A matching target if one exists, or <code>null</code> if one cannot be found.
	 */
	Target<V> chooseTarget(List<Target<V> > options, Range<V> constraint);
	
	/**
	 * Take a set of constraints, and partition them around a particular target version, where
	 * constraints may be classified as either possible or impossible.
	 * 
	 * <p>This is used to figure out how to retry around mis-matches on "fixed" features.
	 * For example, if we have a fixed feature of version 2.2, and we have constraints that
	 * require 2.2.1 or later, what happens?</p>
	 * 
	 * <p>If we're using the "LBLM" algorithm, then this is a failure case, because we're already
	 * picking the lowest possible version that can match.  However, if we're using a "latest"
	 * algorithm, it is possible that there is an earlier version of the features with the
	 * constraints that would allow version 2.2.</p>
	 * 
	 * <p>This method merely partitions constraints into two groups - some that might possibly
	 * be satisfied by re-applying the algorithm, and some that won't ever be satisfied.
	 * </p>
	 * 
	 * @param target	The fixed target version around which we want to partition.
	 * 
	 * @param constraints	The constraints to partition.
	 * @param possible		The list of possible constraints gets stuck here.
	 * @param impossible	The list of impossible constraints gets put here.
	 */
	<TC extends TargetConstraint<V, ?>> void partitionConstaintsAroundVersion(Target<V> target,
			List<TC> constraints,
			List<TC> possible,
			List<TC> impossible);

	/**
	 * Identify a new constraint based on the available source options, a target that currently doesn't
	 * work, and the range of constraints on the specified feature.
	 * 
	 * @param sourceOptions
	 * @param target
	 * @param sourceRange
	 * @return
	 */
	Range<V> pickNewSourceRange(List<Target<V>> sourceOptions,
			Target<V> target, Range<V> sourceRange);
	
	
	/**
	 * For a given feature, look at the original constraints on it, all of the constraints
	 * on it, the available version, the chosen version, and decide whether or not the
	 * constraints should be adjusted.
	 *  
	 * @param original	The original constraints specified for a feature.
	 * @param all	All of the constraints eventually applied.
	 * @param available	The available versions of a feature.
	 * @param chosen	The chosen version.
	 * 
	 * @return
	 */
	MismatchedConstraintsResult<V, SolverConstraintSource> hasQuestionableStartingConstraints(
			List< TargetConstraint<V, SolverConstraintSource> > original,
			Collection< TargetConstraint<V, SolverConstraintSource>> all,
			Collection<Target<V> > available, V chosen);
	
	/**
	 * Report appropriate warnings as a result of doing a search for features.
	 * 
	 * <p>Mostly exists so that the LBLM algorithm can report warnings about later versions of
	 * featurs that were chosen.</p>
	 * @param siteInfo Provides the available features to consider.
	 * @param results The results for which we want to generate warnings.
	 */
	<F extends Feature<V, F> > void warningsOnResults(FeatureSource<V, F> siteInfo, Collection<Target<V>> results);
}
