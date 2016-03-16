package com.tibco.devtools.workspace.installer.standalone;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.tibco.devtools.workspace.installer.standalone.ChoiceAlgorithm;
import com.tibco.devtools.workspace.model.Feature;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.Version;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Determines the latest build of the least match of possible matches.
 */
public class LatestBuildOfLeastMatchAlgorithm<V extends Version<V> > implements ChoiceAlgorithm<V> {

	public Target<V> chooseTarget(List<Target<V> > options,
			Range<V> constraint) {
		
		List<Target<V>> filteredOptions = DataUtils.newList();
		for (Target<V> oneOption : options) {
			if (constraint.isInRange(oneOption.getVersion())) {
				filteredOptions.add(oneOption);
			}
		}
		
		List<Target<V>> latestBuilds = LatestBuildOfLeastMatchAlgorithm.dropNonLatest(filteredOptions);
		return LatestAlgorithm.pickFirst(constraint, latestBuilds);
	}

	/**
	 * For LBLM, impossible constraints are ones where the target already falls below
	 * the low end of the stated constraints, and possible constraints are where the
	 * target falls above the constraint range.
	 */
	public <TC extends TargetConstraint<V, ?>> void partitionConstaintsAroundVersion(Target<V> target,
			List<TC> constraints,
			List<TC> possible,
			List<TC> impossible) {

		for (TC constraint : constraints) {
			int match = constraint.getRange().match(target.getVersion() );
			if (match < 0) {
				impossible.add(constraint);
			}
			else if (match > 0) {
				possible.add(constraint);
			}
		}
	}

	public Range<V> pickNewSourceRange(
			List<Target<V>> sourceOptions, Target<V> target,
			Range<V> sourceRange) {
		
	    Range<V> newRange = new Range<V>(target.getVersion(), false, sourceRange.getMaximumRange(), sourceRange.isMaximumInclusive());
	    
		return LatestAlgorithm.atLeastOneMatchForRange(sourceOptions, newRange);
	}

	/**
	 * Drop the not-latest VersionedTarget entries from a collection, returning
	 * a new list of them (sorted).
	 * 
	 * @param source	The list of targets to filter.
	 * @return
	 */
	public static <V extends Version<V> > List< Target<V> > dropNonLatest(Collection<? extends Target<V>> source) {
		
		// quickly bail if appropriate.
		if (source.size() == 0) {
			return Collections.emptyList();
		}
		
		// sort the array...
		List<Target<V> > intermediate = DataUtils.newList();
		intermediate.addAll(source);
		Collections.sort(intermediate);
		
		Target<V> lastTarget = null;
		Target<V> firstTarget = intermediate.get(0);
		String lastTargetName = firstTarget.getTargetId();
		V nextLimit = firstTarget.getVersion().nextPatch();
		
		List<Target<V>> results = DataUtils.newList();
		
		// with a now sorted list, we can simply walk through and drop all but the highest
		// entry below the next patch version...
		for (Iterator<Target<V>> itTargets = intermediate.iterator() ; itTargets.hasNext() ; ) {
			Target<V> current = itTargets.next();
			V currentVers = current.getVersion();
			
	        // first check - is the name of the first thing the same as the last,
	        // if not, must have had a "highest" entry for that 
	        if ( !current.getTargetId().equals(lastTargetName) ) {
	        	// add last target to the list, and prep to find highest match with this name.
	        	results.add(lastTarget);
	        	lastTargetName = current.getTargetId();
	        	nextLimit = currentVers.nextPatch();
	        }
	        else if (currentVers.compareTo(nextLimit) >= 0) {
	        	// the current version is at or above the previous patch limit, so the
	        	// last bundle we saw must have been the highest build.
	        	results.add(lastTarget);
	        	nextLimit = currentVers.nextPatch();
	        }
	        lastTarget = current;
		}
		
		// The last item in the array, by definition, must always be one of the latest,
		// and it isn't picked up by the loop above.
		results.add(lastTarget);
		return results;
	}

	/**
	 * Looks for questionable starting constraints.
	 * 
	 * <p>In the case of LBLM, this means finding the lowest possible constraint, and
	 * see whether the chosen option is higher than the next patch for that lowest
	 * constraint.</p>
	 */
	public MismatchedConstraintsResult<V, SolverConstraintSource>
	hasQuestionableStartingConstraints(
			List<TargetConstraint<V, SolverConstraintSource> > original,
			Collection<TargetConstraint<V, SolverConstraintSource>> all,
			Collection<Target<V>> available,
			V chosen) {
		
		// find the constraint that has the *lowest* low end.
		V lowest = original.get(0).getRange().getMinimumRange();
		List< TargetConstraint<V, SolverConstraintSource> > lower = DataUtils.newList();
		for (TargetConstraint<V, SolverConstraintSource> oneConstraint : original) {
			V currentLow = oneConstraint.getRange().getMinimumRange(); 
			V lowEnd = currentLow.nextPatch();
			if (lowEnd.compareTo(chosen) < 0) {
				lower.add(oneConstraint);
			}
			
			// also find the constraint that has the lowest range.
			if (lowest.compareTo(currentLow) > 0) {
				lowest = currentLow;
			}
		}
		
		MismatchedConstraintsResult<V, SolverConstraintSource> result = null;
		
		// now check to see if the version chosen greater than the next patch on the
		// low end?
		V nextPatch = lowest.nextPatch();
		if (lower.size() > 0) {
			// yes, it is, now go back through and find all constraints that have
			// a low end greater than that next patch.
			List< TargetConstraint<V, SolverConstraintSource>> narrower = DataUtils.newList();
			for (TargetConstraint<V, SolverConstraintSource> oneConstraint : all) {
				if (oneConstraint.getRange().getMinimumRange().compareTo(nextPatch) >= 0) {
					narrower.add(oneConstraint);
				}
			}
			
			result = new MismatchedConstraintsResult<V, SolverConstraintSource>(lower, narrower, chosen);
		}
		return result;
	}

	public <F extends Feature<V, F> > void warningsOnResults(FeatureSource<V, F> siteInfo, Collection<Target<V>> results) {
        for (Target<V> item : results) {
            List<Target<V>> sameIdTargets = siteInfo.getFeatureVersionSetById(item.getTargetId());
            sameIdTargets = LatestBuildOfLeastMatchAlgorithm.dropNonLatest(sameIdTargets);
            Target<V> last = sameIdTargets.get(sameIdTargets.size() - 1);
            // is the last entry in the list bigger than our chosen item?
            if (last.compareTo(item) > 0) {
                // yes, so lets print out all the options.
                System.out.println("Other options for " + item.toString() + " include:");
                for (Target<V> possibleLaterTarget : sameIdTargets) {
                    if (possibleLaterTarget.compareTo(item) > 0) {
                        System.out.println("  " + possibleLaterTarget.getVersion().toString() );
                    }
                }
            }
        }
    }

}
