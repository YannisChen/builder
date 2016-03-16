package com.tibco.devtools.workspace.installer.standalone;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tibco.devtools.workspace.installer.standalone.ConstraintResult.*;

import com.tibco.devtools.workspace.model.Feature;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.util.DataUtils;

import static com.tibco.devtools.workspace.util.DataUtils.*;


/**
 * Examines feature constraints, and "solves" them, picking appropriate
 * features based on the algorithm it has been given.
 * 
 * <p>The starting contributions include features that have dependencies that need to be solved,
 * either feature or plugin dependencies.</p>
 * 
 * <p>"Advice" constrains the resulting output.  When a constraint is found on a particular
 * feature and the caller provides advice on that feature, the particular version of the feature specified
 * by advice will be the one chosen - provided it fits into the constraints given.  If
 * there is more than one piece of advice for any given feature, the later version of advice
 * is always preferred.  There may be advice on features that are not required by any constraints.
 * That advice is simply ignored.
 * </p>
 * 
 * @param <V> How is a version # represented?
 * @param <F> How is a feature represented?
 */
public class ConstraintSolver<V extends Comparable<V>, F extends Feature<V, F> > {

	/**
	 * Initializes a chooser.
	 * 
	 * @param sites		The sites to scan for features.
	 * @param startingContributions	The starting contributions that dictate the constraints to solve.
	 * @param fixedContributions	The fixed contributions that, if used, must be these exact versions.
	 * 	May be <code>null</code> if there are no fixed contributions.
	 * @param chooser	The algorithm for choosing which features should be used.
	 */
	public ConstraintSolver(FeatureSource<V, F> sites, List<F> startingContributions,
			Collection<F> fixedContributions, ChoiceAlgorithm<V> chooser) {

		m_sites = sites;
		m_startingContributions = newList();
		for (F anItem : startingContributions) {
			m_startingContributions.add(anItem);
		}
		m_chooser = chooser;
		
		m_nameToFixedContributions = newMap();
		m_fixedContributions = fixedContributions;
		if (fixedContributions != null) {
			for (F fixedItem : fixedContributions) {
				m_nameToFixedContributions.put(fixedItem.getTarget().getTargetId(), fixedItem.getTarget());
			}
		}
		
		m_nameToStartingContributions = uniquelyPartition(m_startingContributions);
		
		m_nameToChoiceDetails = newMap();
		m_unmodifiableNameToChoiceDetails = Collections.unmodifiableMap(m_nameToChoiceDetails);

		m_logging = sm_defaultLogging;
	}
	
	/**
	 * Utility method to create a constraint solver with implicit typing.
	 * 
	 * @param <V>	What type are we using for a "version" number.
	 * @param <F>	What represents a "feature"
	 * @param sites	What site(s) are we getting data from
	 * @param startingContributions	What are the starting contributions for the search?
	 * @param fixedContributions	What are the fixed contributions for the search?
	 * @param chooser	How do we choose amongst available options?
	 * 
	 * @return	A new constraint solver instance.
	 */
	public static <V extends Comparable<V>, F extends Feature<V, F> > ConstraintSolver<V, F>
	create(FeatureSource<V, F> sites, List<F> startingContributions,
			Collection< F > fixedContributions, ChoiceAlgorithm<V> chooser) {
		
		return new ConstraintSolver<V, F>(sites, startingContributions, fixedContributions, chooser);
	}
	
	/**
	 * Add contingent constraints to the resolution.
	 * 
	 * @param <S>	Type dynamically matches the source of the target constraint collection.
	 * 
	 * @param constraints	The constraints to apply.
	 */
	public <S extends SolverConstraintSource> void addContingentConstraints(Collection<TargetConstraint<V, S> > constraints) {
		for (TargetConstraint<V, S> constraint: constraints) {
			String featureId = constraint.getTargetName();
			List<TargetConstraint<V, SolverConstraintSource>> perFeatureConstraints =
				getMapListValue(m_contingentConstraints, featureId);
			
			TargetConstraint<V, SolverConstraintSource> downgradedConstraint = 
				TargetConstraint.downcastSource(constraint);
			perFeatureConstraints.add( downgradedConstraint);
		}
	}
	/**
	 * Get the details about the results....
	 * 
	 * @return
	 */
	public Collection< ChoiceDetails<V, F> > getResults() {
		return m_nameToChoiceDetails.values();
	}

	/**
	 * Get the details of what was chosen by the search algorithm.
	 * @return
	 */
	public Map<String, ChoiceDetails<V, F> > getNameToChoiceDetails() {
		return m_unmodifiableNameToChoiceDetails;
	}
	
	/**
	 * Set the place where logging messages get sent.
	 * @param logger
	 */
	public void setLogger(LogOutput<ConstraintResult, String> logger) {
		m_logging = logger;
	}
	
	/**
	 * Do the feature search...
	 * 
	 * @return <code>null</code> if the search succeeds, and a failure object if it
	 * doesn't.
	 */
	public boolean search() {
		
		boolean success = true;
		m_nameToDiscoveredConstraint = newMap();
		m_nameToFutureConstraint = newMap();

		try {
			m_restartSearch = true;
			
			// keep trying the search when it fails.  Note that failure means that a particular
			// set of choices has been exhausted, but more remain to try.
			while (m_restartSearch) {
				
				m_restartSearch = false;
				m_nextFeaturesToCheck = m_startingContributions;

				// make sure we re-add our fixed contributions to our set of choices...
				resetNameToChoiceDetails();
				
				// loop through the features to check, so long as we have not yet determined that the search should be
				// restarted.
				while (m_nextFeaturesToCheck.size() > 0 && !m_restartSearch) {
					
					Map<String, List<TargetConstraint<V, SolverConstraintSource > > > nameToConstraintsList =
						getConstraintsMap(m_nextFeaturesToCheck, true);
					
					m_nextFeaturesToCheck = newList();
					
					// Now, for the constraints on each feature name, go and match them.
					for (Map.Entry<String, List<TargetConstraint<V, SolverConstraintSource > > > entry : nameToConstraintsList.entrySet() ) {
						matchOneSetOfConstraints(entry.getKey(), entry.getValue() );
					}

					// now if we think we have no more features to resolve, and we are not
					// restarting our search because of feature issues, try checking plugins!
					if (m_nextFeaturesToCheck.size() == 0 && !m_restartSearch) {
						checkPluginConstraints();
					}
				}
				
			}
			
		} catch (ConstraintException e) {
			// this doesn't do anything - the m_failure member gets set before the throw.
			success = false;
		}
		
		return success;
	}
	
	/**
	 * Does the same as the {@link #search()} method, except that it also warns of
	 * particular conditions along the way.
	 * 
	 * @return A non-null List of targets if the search is a success, and <code>null</code> if not.
	 */
	public List<Target<V> > searchAndWarn() {
		
        // our collected results go here.
        List< Target<V> > results = null;

        boolean success = search();

        // Did the resolution succeed?
        if (success) {
        	
        	// make the list where we stick the results.
            results = DataUtils.newList();
            
            // resolution success - get the targets.
            Collection< ChoiceDetails<V, F> > choices = getResults();
            for (ChoiceDetails<V, F> oneChoice : choices) {
                if (!oneChoice.isFixedChoice()) {
                    results.add( oneChoice.getTarget() );

                }
            }

            // print out list of mismatched constraints.
            List<MismatchedConstraintsResult<V, SolverConstraintSource>> mcrs =
                computeMismatchedInitialConstraints();

            for (MismatchedConstraintsResult<V, SolverConstraintSource > mcr : mcrs) {
                System.out.println("WARNING: For feature <<" + mcr.getFeatureId() + ">>, matched version " + mcr.getChosenResult().toString()
                        + ", however, some dependencies want earlier versions (or no earlier version is available):");
                List<TargetConstraint<V, SolverConstraintSource>> starting = mcr.getStartingConstraints();
                printConstraintList(starting);

                System.out.println("** The following constraints narrow the choice.");
                List<TargetConstraint<V, SolverConstraintSource> > narrower = mcr.getNarrowerConstraints();
                printConstraintList(narrower);

            }

            // sort the results for kicks.
            Collections.sort(results);
            
            // now, output algorithm specific warnings.
            m_chooser.warningsOnResults(m_sites, results);

        }

        return results;
	}
	
	/**
	 * Given a set of features to choose from, pick the one according to the algorithm in
	 * question.
	 * 
	 * @param featureId	The feature to look for
	 * @param intersection	The intersection of the provided constraints.
	 * @param constraints	The constraints.
	 * @return	The chosen feature version.
	 * @throws ConstraintException	If the feature cannot be matched...
	 */
	public F chooseFeature(String featureId,
			Range<V> intersection, List<TargetConstraint<V, SolverConstraintSource> > constraints) throws ConstraintException {

		// get the list of features with a given ID.
		List< Target<V> > features = m_sites.getFeatureVersionSetById(featureId);
		if (features == null || features.size() == 0) {
			reportAndThrowConstraintFailure(NOT_FOUND, constraints, featureId);
		}
		
		// ask the algorithm to choose the ideal one.
	    Target<V> chosenTarget = m_chooser.chooseTarget(features, intersection);
	    if (chosenTarget == null) {
	    	
	    	String availableStr = Target.joinVersions(features, ", ");
	    	reportAndThrowConstraintFailure(UNMATCHED, constraints, featureId, availableStr);
	    }
		
	    // return the feature model for that feature.
	    return m_sites.getFeatureModel(chosenTarget);
	}

	/**
	 * Do an exhaustive search of features that satisfy the indicated plugin constraints.
	 * 
	 * @param pluginIdToConstraints
	 * @return
	 */
	public Map<String, List< TargetConstraint<V, F> >> scanForFeaturesWithPlugins(
			Map<String, List<TargetConstraint<V, F> > > pluginIdToConstraints ) {
		
		Collection<String> featureIds = m_sites.getAvailableFeatureIds();
		Map<String, List<String>> pluginIdToFeatureIds = newMap();
		
		// Look at the latest version of each feature we've not already chosen...
		for (String featureName : featureIds) {
			if (!m_nameToChoiceDetails.containsKey(featureName) ) {
				// not one we've already chosen, so check the latest version of the feature
				// to see if it has this plugin.
				List< Target<V> > sortedOptions = newList();
				List< ? extends Target<V> > options = m_sites.getFeatureVersionSetById(featureName);
				sortedOptions.addAll(options);
				Collections.sort(sortedOptions);
				
				Target<V> latest = sortedOptions.get( options.size() - 1);
				F feature = m_sites.getFeatureModel(latest);
				
				// if we have a corrupted feature, we should just skip over it, rather than
				// fail ungloriously.
				if (feature != null) {
					Collection< PluginReference<V> > plugins = feature.getProvidedPlugins();
					
					// now loop through all of the plugins of this feature, and see if it
					// has any of the plugins in question.
					for (PluginReference<V> plugin : plugins) {
						String pluginId = plugin.getTarget().getTargetId();
						if (pluginIdToConstraints.containsKey( pluginId ) ) {
							List<String> featureNames = getMapListValue(pluginIdToFeatureIds, pluginId);
							featureNames.add(featureName);
						}
					}
				}
			}
		}
	
		return getMapOfPluginIdToFeatureConstraints(pluginIdToConstraints, pluginIdToFeatureIds);
	}

	/**
	 * Having chosen results, return a list of constraints that may be larger than resolved.
	 * 
	 * @return A list of what constraints apparently cannot be respected, because other constraints
	 * make them moot.
	 */
	public List<MismatchedConstraintsResult<V, SolverConstraintSource> > computeMismatchedInitialConstraints() {
		
		List< MismatchedConstraintsResult<V, SolverConstraintSource> > results = newList();
		Map<String, List<TargetConstraint<V, SolverConstraintSource> > > originals =
			getConstraintsMap(m_startingContributions, false);
		
		for (Map.Entry<String, List<TargetConstraint<V, SolverConstraintSource>>> entry : originals.entrySet()) {
			String featureId = entry.getKey();
			List<TargetConstraint<V, SolverConstraintSource> > originalConstraints = entry.getValue();
			
			MismatchedConstraintsResult<V, SolverConstraintSource> mcr = null;
			
			if (!m_nameToStartingContributions.containsKey(featureId))  {
				ChoiceDetails<V, F> choice = m_nameToChoiceDetails.get(featureId);
				V version = choice.getTarget().getVersion();
				
				// compute what options are available
				List<Target<V>> available;
				if (m_nameToFixedContributions.containsKey(featureId)) {
					// a fixed contribution - need not be on the update sites!
					List<Target<V> > fixedOption = newList();
					fixedOption.add( m_nameToFixedContributions.get(featureId) );
					available = fixedOption;
				}
				else {
					available = m_sites.getFeatureVersionSetById(featureId);
				}
				
				Collection<TargetConstraint<V, SolverConstraintSource> > allConstraints =
					choice.getConstraints();
				mcr = m_chooser.hasQuestionableStartingConstraints(
						originalConstraints, allConstraints, available, version);
				
			}
			else {
				// in the case of starting features, the only possible constraints on those
				// come from other starting features - check those!
				
				F startFeature = m_nameToStartingContributions.get(featureId); 
				List<Target<V> > startOption = newList();
				startOption.add( startFeature.getTarget() );

				// this is one of the starting features, so look for questionable constraints
				// by one starting feature on another.
				mcr = m_chooser.hasQuestionableStartingConstraints(originalConstraints,
						originalConstraints, startOption, startFeature.getTarget().getVersion() );
			}
			
			if (mcr != null) {
				results.add(mcr);
			}
		}
		
		// sort by feature name, so that I can then just get the expected results.
		Collections.sort(results, new Comparator<MismatchedConstraintsResult<?, ?>>() {

			public int compare(MismatchedConstraintsResult<?, ?> o1,
					MismatchedConstraintsResult<?, ?> o2) {
				return o1.getFeatureId().compareTo( o2.getFeatureId() );
			}

		});
		
		return results;
	}
	
    private static <TC extends TargetConstraint<?, SolverConstraintSource >>
    void printConstraintList(List<TC> constraints) {
        for (TC one : constraints) {
        	SolverConstraintSource scs = one.getSource();
        	String sourceFeatureId = scs.getSourceLogicalId();
        	String prefix = sourceFeatureId != null ? "  Feature " + sourceFeatureId :
        		"  " + scs.getSourcePathString();
            System.out.println(prefix + " wants " + one.toString() );
        }
    }

	//=========================================================================
	//	Private methods.
	//=========================================================================

	private void checkPluginConstraints() throws ConstraintException {
		
        List<TargetConstraint<V, F> > pluginConstraints = newList();
        List<PluginReference<V> > providedPlugins = newList();

        // nominally, the starting contributions should never be an issue by our
        // best practices, but they're here for completeness.
        accumulatePluginInfo(providedPlugins, pluginConstraints, m_startingContributions);
        accumulatePluginInfo(providedPlugins, pluginConstraints, getChosenFeatures() );
        
        Map<String, List<TargetConstraint<V, F> > > pluginIdToConstraints = newMap();
        
    	for (TargetConstraint<V, F> constraint : pluginConstraints) {
    		boolean constraintMatched = false;
    		for (PluginReference<V> plugin : providedPlugins) {
    			if (constraint.isMatchForTarget( plugin.getTarget() )) {
    				constraintMatched = true;
    				break;
    			}
    		}
    		
    		// accumulate all of the constraints that we didn't match.
    		if (!constraintMatched) {
    			List<TargetConstraint<V, F> > constraints =
    				getMapListValue(pluginIdToConstraints, constraint.getTargetName());
    			
    			constraints.add(constraint);
    		}
    	}
    	
    	// do we have plugin constraints?
    	if (pluginIdToConstraints.size() > 0) {
    		m_logging.warning(EXHAUSTIVE_PLUGIN_SEARCH, "Doing exhaustive search for plugin constraint matches - this may take some time.");
    		
    		// for the insight of people trying to eliminate plugin dependencies, dump the info.
    		List<String> pluginIds = newList();
    		pluginIds.addAll(pluginIdToConstraints.keySet());
    		Collections.sort(pluginIds);
    		
    		for (String plugin : pluginIds) {
    			StringBuffer toDump = new StringBuffer("  ");
    			toDump.append("Plugin: " + plugin + " has the following constraints:" + sm_linebreak);
    			
    			for (TargetConstraint<V, ? extends SolverConstraintSource > constraint : pluginIdToConstraints.get(plugin)) {
    				toDump.append("    " + constraint.toString() + " from ");
    				toDump.append(constraint.getSource().getSourcePathString() ) ;
    				toDump.append(sm_linebreak);
       			}
    			m_logging.debug(PLUGIN_CONSTRAINTS, toDump.toString());
    		}
    		
    		Map<String, List<TargetConstraint<V, F>>> pluginToFeatureConstraints =
    			scanForFeaturesWithPlugins(pluginIdToConstraints);
    		
    		extractSatisfiableConstraints(pluginToFeatureConstraints);
    		
    		// only if we're not restarting our search do we throw up our hands and cry uncle.
    		if (!m_restartSearch) {
    			failForUnsatisfiableConstraints(pluginToFeatureConstraints, pluginIdToConstraints);
    		}
    	}
	}

	private void failForUnsatisfiableConstraints(
			Map<String, List<TargetConstraint<V, F>>> pluginToFeatureConstraints,
			Map<String, List<TargetConstraint<V, F>>> pluginIdToPluginConstraints) throws ConstraintException {

		Map<String, List<TargetConstraint<V, F>>> unsatisfiedConstraints = newMap();
		
		for (Map.Entry<String, List<TargetConstraint<V, F>> >  constraintEntry : pluginToFeatureConstraints.entrySet() ) {

			String pluginId = constraintEntry.getKey();
			List<TargetConstraint<V, F>> featureConstraints = constraintEntry.getValue();
			
			// is there exactly one feature constraint?
			if (featureConstraints.size() != 1) {
				unsatisfiedConstraints.put(pluginId, pluginIdToPluginConstraints.get(pluginId));
			}
			if (featureConstraints.size() == 0) {
				m_logging.warning(NO_FEATURES_WITH_PLUGIN_CONSTRAINT,
						"No feature appears to match the constraints for plugin " + pluginId);
			}
			else if (featureConstraints.size() > 1) {
				StringBuffer toDump = new StringBuffer();
				toDump.append("More than one feature appears to match the constraints for plugin " + pluginId + ".  Unable to choose.");
				toDump.append(sm_linebreak);
				for (TargetConstraint<V, F> oneConstraint : featureConstraints) {
					toDump.append("  " + oneConstraint.toString() + sm_linebreak);
				}
				m_logging.error(MULTIPLE_POSSIBLE_FEATURES_FOR_PLUGIN, toDump.toString());
			}
		}
		
		// now, output the errors....
		if (unsatisfiedConstraints.size() > 0) {
			outputUnsatisfiedPluginConstraints(unsatisfiedConstraints);
    		throw new ConstraintException();
		}
	}

	private void outputUnsatisfiedPluginConstraints(
			Map<String, List<TargetConstraint<V, F>>> unsatisfiedConstraints) {
		
		for (Map.Entry<String, List<TargetConstraint<V, F>>> entry: unsatisfiedConstraints.entrySet() ) {
			
			StringBuffer buf = new StringBuffer();
			
			buf.append("Unmatched plugin <<");
			buf.append(entry.getKey());
			buf.append(">>");
			buf.append(sm_linebreak);
			
			for (TargetConstraint<V, F> oneConstraint : entry.getValue() ) {
				buf.append("  ");
				buf.append(oneConstraint.toString() );
				buf.append(sm_linebreak);
			}

			m_logging.error(UNMATCHED_PLUGINS, buf.toString());
		}
		
	}

	private void extractSatisfiableConstraints(
			Map<String, List<TargetConstraint<V, F>>> pluginToFeatureConstraints) {
		
		// We have to collect all the feature constraints, so that we can collapse them if need be.
		Map<String, List<TargetConstraint<V, F>>> featureIdToConstraints = newMap();
		
		for (List<TargetConstraint<V, F>> featureConstraints : pluginToFeatureConstraints.values() ) {

			// is there exactly one feature constraint?
			if (featureConstraints.size() == 1) {
				TargetConstraint<V, F> oneConstraint = featureConstraints.get(0);
				List<TargetConstraint<V, F>> collectedConstraints = getMapListValue(featureIdToConstraints, oneConstraint.getTargetName());
				collectedConstraints.add(oneConstraint);
			}
		}
		
		// For all the entries in the map, go back and 
		for (Map.Entry<String, List<TargetConstraint<V, F> > > constraintEntry : featureIdToConstraints.entrySet() ) {
			
			String featureId = constraintEntry.getKey();
			List<TargetConstraint<V, F>> featureConstraints = constraintEntry.getValue();
			DefaultConstraintSource dcs = new DefaultConstraintSource("derived:plugin dependencies", "Derived from import plugin dependency");
			Range<V> generateRange = TargetConstraint.computeIntersection(featureConstraints);
			TargetConstraint<V, DefaultConstraintSource> newConstraint =
				new TargetConstraint<V, DefaultConstraintSource>(dcs, true, featureId, generateRange);
			
			m_logging.warning(ASSUMING_CONSTRAINT,
					"Assuming a constraint of " + newConstraint + " to satisfy plugin constraints.");
			m_nameToDiscoveredConstraint.put(featureId, newConstraint);
			// now flag that the search needs to be restarted!
			m_restartSearch = true;
		}
	}

	/**
	 * Loop through all of the options for a feature id, and see which versions satisfy the
	 * requirement for the indicated plugin.
	 * 
	 * @param featureId	For this feature id
	 * @param featureTargets	For all of these targets (must have the feature id from above)
	 * @param pluginId	Match this plugin Id
	 * @param pluginRange	In this range.
	 * @return
	 */
	private Map<String, List< TargetConstraint<V, F> > > getMapOfPluginIdToFeatureConstraints(
			Map<String, List<TargetConstraint<V, F>> > pluginIdToPluginConstraints,
			Map<String, List<String>> pluginIdToFeatureIds) {

		Map<String, List< TargetConstraint<V, F>>> pluginIdToFeatureConstraints = newMap();
		
		// loop through each of the plugin constraints that I've got.
		for (Map.Entry<String, List<TargetConstraint<V, F> > > pluginEntry : pluginIdToPluginConstraints.entrySet() ) {
			String pluginId = pluginEntry.getKey();
			List<TargetConstraint<V, F>> constraints = pluginEntry.getValue();

			// make sure that this always generates at least an empty list for each plugin.
			List<TargetConstraint<V, F>> featureConstraints = getMapListValue(pluginIdToFeatureConstraints, pluginId);
			
			// now for each feature that has this plugin, compute a range for that feature.
			List<String> featureIds = pluginIdToFeatureIds.get(pluginId);
			if (featureIds != null) {
				for (String featureId : featureIds) {

					TargetConstraint<V, F> newConstraint = pluginConstraintsToConstraintOnFeature(
							pluginId, constraints, featureId);

					if (newConstraint != null) {
						
						m_logging.warning(INFERRED_CONSTRAINT,
								"To satisfy plugin requirement for " + pluginId
								+ " inferred a possible dependency on " + newConstraint.toString() );
						
						featureConstraints.add(newConstraint);
					}
				}
			}
		}
		
		return pluginIdToFeatureConstraints;
	}

	/**
	 * For a given plugin, and its constraints see what constraint might make sense for the
	 * given feature id.
	 * 
	 * @param pluginId
	 * @param constraints
	 * @param featureId
	 * @return
	 */
	private TargetConstraint<V, F> pluginConstraintsToConstraintOnFeature(
			String pluginId, List<TargetConstraint<V, F>> constraints,
			String featureId) {
		
		TargetConstraint<V, F> newConstraint = null;
		// keep low/high water marks.
		V low = null;
		V high = null;
		
		// walk through all versions of the feature.
		List< Target<V> > featureTargets = m_sites.getFeatureVersionSetById(featureId);
		for (Target<V> oneFeature: featureTargets) {
			
			// get the model for the feature.
			F feature = m_sites.getFeatureModel(oneFeature);
			V current = oneFeature.getVersion();
			
			// for each plugin in the feature, see if it satisfies the constraints.
			boolean satisfied = false; 
			for (PluginReference<V> pluginRef : feature.getProvidedPlugins() ) {
				// is this the plugin that we're looking for?
				// note that a feature might have MULTIPLE versions of a plugin, so we keep looking
				// if we fail the first time.
				Target<V> pluginTarget = pluginRef.getTarget();
				if (pluginTarget.getTargetId().equals(pluginId) ) {
					
					if (TargetConstraint.meetsAllConstraints(pluginTarget, constraints)) {
						satisfied = true;
						break;
					}
				}
			}
			
			// did we satisfy the constraints - update high & low marks.
			if (satisfied) {
				if (low == null || current.compareTo(low) < 0)
					low = current;
				if (high == null || current.compareTo(high) > 0)
					high = current;
			}
		}
		
		// did we set any mark?  If so, we can have a constraint on this feature.
		if (low != null) {
			Range<V> resultRange = new Range<V>(low, true, high, true);
			newConstraint = new TargetConstraint<V, F>(null, true, featureId, resultRange);
			
		}
		return newConstraint;
	}

	/**
	 * Match the constraints for a single feature....
	 * 
	 * @param featureId	Which feature are we attempting to match?
	 * @param constraints	What constraints do we have for the above feature?
	 * @throws ConstraintException
	 */
	private void matchOneSetOfConstraints(String featureId,
			List<TargetConstraint<V, SolverConstraintSource > > constraints) throws ConstraintException {
		
		Range<V> intersection = TargetConstraint.computeIntersection(constraints);
		
		if (intersection == null) {
			reportNoIntersection(featureId, constraints);
		}
		else if (m_nameToFixedContributions.containsKey(featureId) ) {
			handleConstraintsOnFixedFeature(featureId, intersection, constraints);
		}
		else if (m_nameToStartingContributions.containsKey(featureId) ) {
			checkForDonutHole(featureId, constraints);
		}
		else if (m_nameToChoiceDetails.containsKey(featureId) ) {
			handleNewConstraintOnExistingMatch(featureId, intersection, constraints);
		}
		else {
			handleConstraintsNewFeature(featureId, intersection, constraints);
		}
	}

	/**
	 * Reports no intersection by combining the current set of constraints we're examining
	 * in conjunction with the constraints already associated with a choice.
	 * 
	 * <p><b>This function always throws an exception.</b>
	 * </p>
	 * 
	 * <p>This can happen for a variety of reasons, including conflicting constraints discovered
	 * after a version has already been chosen, or future or generated constraints that
	 * exist when the search gets restarted.
	 * </p>
	 *  
	 * @param featureId	The feature for which we wish to report the error.
	 * @param constraints	The constraints that are currently being checked.
	 * 
	 * @throws ConstraintException
	 */
	private void reportNoIntersection(String featureId,
			List<TargetConstraint<V, SolverConstraintSource > > constraints) throws ConstraintException {
		
		Set<TargetConstraint<V, SolverConstraintSource>> allConstraints =
			new HashSet<TargetConstraint<V,SolverConstraintSource>>(constraints);
		
		ChoiceDetails<V, F> choice = m_nameToChoiceDetails.get(featureId);
		if (choice != null) {
			allConstraints.addAll(choice.getConstraints());
		}
		reportAndThrowConstraintFailure(NO_INTERSECTION, allConstraints, featureId);
	}
	
	/**
	 * Utility method to accumulate the list of plugin constraints and provided
	 * plugins from a collection of features.
	 */
	private void accumulatePluginInfo(Collection< PluginReference<V> > providedPlugins,
			Collection<TargetConstraint<V, F> > pluginConstraints, Collection<F> features) {

		for (F feature : features) {
			providedPlugins.addAll( feature.getProvidedPlugins() );
			pluginConstraints.addAll( feature.getPluginConstraints() );
		}
		
	}
	
	/**
	 * Utility method to gather up all the chosen features into a list....
	 */
	private List<F> getChosenFeatures() {
		List<F> results = newList();
		
		for (ChoiceDetails<V, F> item : m_nameToChoiceDetails.values()) {
			results.add(item.getFeature());
		}
		return results;
	}
	
	/**
	 * If this is a constraint on a new feature that we've not seen before, make sure we
	 * can pick at least one of the available versions, then put it in our record of what we've
	 * chosen and why.
	 * 
	 * @param featureId		The feature we're trying to satisfy
	 * @param intersection	The intersection of all the constraints.
	 * @param constraints	The constraints that created the intersection.
	 * @throws ConstraintException If the feature is not found or cannot be matched based on what is available.
	 */
	private void handleConstraintsNewFeature(String featureId,
			Range<V> intersection, List<TargetConstraint<V, SolverConstraintSource> > constraints) throws ConstraintException {

		F fd = chooseFeature(featureId, intersection, constraints);
		
		ChoiceDetails<V, F> cd = new ChoiceDetails<V, F>(fd.getTarget(), fd, false);
		cd.getConstraints().addAll(constraints);
		m_nameToChoiceDetails.put(featureId, cd);
		m_nextFeaturesToCheck.add(fd);
	}

	/**
	 * We've come across new constraints on an existing match - we need
	 * to validate whether the previous choice is still satisfied, and if not, we need
	 * to restart our search.
	 * 
	 * @param featureId		The ID of the feature we've already matched.
	 * @param intersection	The intersection of the constraints we're trying to match.
	 * @param constraints	The full set of constraints on the already matched item.
	 * @throws ConstraintException Thrown when the old constraints and new constraints don't intersect
	 */
	private void handleNewConstraintOnExistingMatch(String featureId,
			Range<V> intersection, List<TargetConstraint<V, SolverConstraintSource> > constraints) throws ConstraintException {
		
		ChoiceDetails<V, F> choice = m_nameToChoiceDetails.get(featureId);
		Target<V> currentChoice = choice.getTarget();
		// Is the choice we've already made one that will satisfy the new constraints we have?
		if (!intersection.isInRange( currentChoice.getVersion() ) ) {
			// nope, not a match, so we need to restart the search with a tighter range.
			// but first, we need to see whether the old constraints and the new constraints intersect!
			List<TargetConstraint<V, SolverConstraintSource>> combinedConstraints = newList();
			combinedConstraints.addAll(constraints);
			combinedConstraints.addAll(choice.getConstraints());
			Range<V> overallConstraint = TargetConstraint.computeIntersection(combinedConstraints);
			if (overallConstraint != null) {
				// yes - there is an overall constraint that works... 
				m_restartSearch = true;
				List<TargetConstraint<V, SolverConstraintSource> > futureConstraints =
					DataUtils.getMapListValue(m_nameToFutureConstraint, featureId);
				
				// now go through all the constraints that we have, and add the ones for which the
				// current choice falls outside its range.
				for (TargetConstraint<V, SolverConstraintSource> oneConstraint : constraints) {
					if (!oneConstraint.isMatchForTarget(currentChoice)) {
						m_logging.debug(ConstraintResult.ADDING_FUTURE_CONSTRAINT, "Restarting search with a future constraint of " + oneConstraint);
						futureConstraints.add(oneConstraint);
					}
				}
			}
			else {
				reportAndThrowConstraintFailure(NO_INTERSECTION, combinedConstraints, featureId);
			}
			
		}
		else {
			// add all of the new constraints for completeness.
			choice.getConstraints().addAll(constraints);
		}
	}

	/**
	 * This method checks for "donut holes" - essentially the idea that you cannot
	 * build a dependency chain that circles back on one of the original features
	 * we're trying to resolve.
	 * 
	 * <p>It is OK for initial features to have direct dependencies on each other, they just
	 * cannot have indirect dependencies on each other.
	 * </p>
	 * 
	 * @param featureId	The feature we're trying to resolve.
	 * @param constraints	The constraints for that feature.
	 * @throws ConstraintException If constraints cannot be satisfied, either because of a donut
	 * 	hole, or because an initial feature states a violated constraint on another starting
	 *  feature.
	 */
	private void checkForDonutHole(String featureId,
			List<TargetConstraint<V, SolverConstraintSource> > constraints) throws ConstraintException {
		
		List<TargetConstraint<V, SolverConstraintSource >> notInStartingSet = newList();

		// loop through every constraint, get its source, and see if the
		// source of the constraint is in the original set of features.
		for (TargetConstraint<V, SolverConstraintSource> oneConstraint : constraints) {
			String sourceFeatureId = oneConstraint.getSource().getSourceLogicalId(); 
			if ( ! m_nameToStartingContributions.containsKey(sourceFeatureId) ) {
				notInStartingSet.add(oneConstraint);
			}
		}
		
		// Did we find any dependency sources that weren't in our starting set?
		if (notInStartingSet.size() > 0) {
			reportAndThrowConstraintFailure(DONUT_HOLE, notInStartingSet, featureId);
		}

		// Now look for any features that have dependencies that could never be satisfied by the
		// other initial features.
		F startingFeature = m_nameToStartingContributions.get(featureId);
		List<TargetConstraint<V, SolverConstraintSource> > violatedConstraints = getViolatedConstraints(constraints, startingFeature.getTarget());
		if (violatedConstraints.size() > 0) {
			reportAndThrowConstraintFailure(INVALID_INITIAL_STATE, violatedConstraints,
					featureId, startingFeature.getTarget().getVersion().toString());
			throw new ConstraintException();
		}
	}

	private void reportAndThrowConstraintFailure(ConstraintResult code,
			Collection<TargetConstraint<V, SolverConstraintSource >> constraints, String ... parameters) throws ConstraintException {

		StringBuffer buf = new StringBuffer();
		String format = ConstraintResult.getResourceString( code );
		buf.append( MessageFormat.format(format, (Object[]) parameters) );
		buf.append(sm_linebreak);
		addConstraintsToErrorBuf(constraints, buf);
		buf.append("  ==== Dependency tree ====");
		buf.append(sm_linebreak);
		
		Set<String> featuresToReport = new HashSet<String>();
		Set<String> featuresReported = new HashSet<String>();
		accumulateFeaturesNotReported(constraints, featuresReported, featuresToReport);
		
		// loop through all the "from features, reporting those constraints....
		while ( featuresToReport.size() > 0) {
			List<String> toProcess = DataUtils.newList();
			toProcess.addAll(featuresToReport);
			featuresToReport.clear();
			
			for (String featureName : toProcess) {
				ChoiceDetails<V, F> cd = m_nameToChoiceDetails.get(featureName);
				// of course, choice details are null for originating features, and for constraint sources
				// that are not features!
				if (cd != null) {
					Collection< TargetConstraint<V, SolverConstraintSource> > someConstraints = cd.getConstraints();
					addConstraintsToErrorBuf(someConstraints, buf);
					
					accumulateFeaturesNotReported(someConstraints, featuresReported, featuresToReport);
				}
			}
		}
		
		m_logging.error(code, buf.toString());
		
		throw new ConstraintException();
	}

	private void addConstraintsToErrorBuf(
			Collection<TargetConstraint<V, SolverConstraintSource>> constraints,
			StringBuffer buf) {
		
		String featureId = null;
		for (TargetConstraint<V, SolverConstraintSource > oneConstraint : constraints) {
			if (featureId == null) {
				featureId = oneConstraint.getTargetName();
				buf.append("  ");
				buf.append(featureId);
				ChoiceDetails<V, F> cd = m_nameToChoiceDetails.get(featureId);
				if (cd != null) {
					buf.append(" (");
					buf.append( cd.getTarget().getVersion().toString() );
					buf.append(") ");
				}
				buf.append(" referenced from: ");
				buf.append(sm_linebreak);
			}
			buf.append("    ");
			SolverConstraintSource source = oneConstraint.getSource();
			buf.append( source.getSourceLogicalId() );
			buf.append(" using range " );
			buf.append(oneConstraint.getRange().toString() );
			buf.append(sm_linebreak);
		}
	}

	private void accumulateFeaturesNotReported(Collection< TargetConstraint<V, SolverConstraintSource> > constraints,
			Set<String> alreadyReported, Set<String> toReport) {

		for (TargetConstraint<V, SolverConstraintSource > oneConstraint : constraints) {
			String featureId = oneConstraint.getSource().getSourceLogicalId();
			if (featureId != null && !alreadyReported.contains(featureId)) {
				toReport.add(featureId);
				alreadyReported.add(featureId);
			}
		}
		
	}
	private void handleConstraintsOnFixedFeature(String featureId,
			Range<V> intersection, List<TargetConstraint<V, SolverConstraintSource > > constraints) throws ConstraintException {
		
		Target<V> target = m_nameToFixedContributions.get(featureId);
		if (intersection.isInRange( target.getVersion() ) ) {
			ChoiceDetails<V, F> choice = m_nameToChoiceDetails.get(featureId); 
			choice.getConstraints().addAll(constraints);

		}
		else {
			// Nope, the constraints on hand do not match the fixed contribution, so now
			// jump through some hoops to see whether we could retry with a different set of constraints.
			List< TargetConstraint<V, SolverConstraintSource> > possible = newList();
			List< TargetConstraint<V, SolverConstraintSource> > impossible = newList();
			
			// partition the constraints!
			m_chooser.partitionConstaintsAroundVersion(target, constraints, possible, impossible);
			
			if (impossible.size() > 0) {
				// we have "impossible" constraints, so no amount of retry will do anything.
				reportAndThrowConstraintFailure(FIXED_MISMATCH, constraints,
						featureId, target.getVersion().toString());
			}
			
			// For each version constraint, see if we can satisfy with an earlier version...
			for (TargetConstraint<V, SolverConstraintSource> constraint : possible) {

				String sourceFeatureId = constraint.getSource().getSourceLogicalId();
				
				// See if the constraint comes from a starting feature, if so, we must fail immediately.
				if (m_nameToStartingContributions.containsKey(sourceFeatureId) ) {
					List< TargetConstraint<V, SolverConstraintSource> > starting = newList();
					starting.add(constraint);
					reportAndThrowConstraintFailure(STARTING_MISMATCH_ON_FIXED, starting,
							featureId, target.getVersion().toString());
				}

				// in the case of manufactured constraints, there will not be an entry for that constraint.
				ChoiceDetails<V, F> cd = m_nameToChoiceDetails.get( sourceFeatureId );
				if (cd != null) {
					Range<V> sourceRange = TargetConstraint.computeIntersection(cd.getConstraints());
					List<Target<V> > sourceOptions = m_sites.getFeatureVersionSetById(sourceFeatureId);
					Range<V> newRange = m_chooser.pickNewSourceRange(
							sourceOptions, cd.getTarget(), sourceRange);
					
					// If there is no new constraint to satisfy, fail now.
					if (newRange == null) {
						reportAndThrowConstraintFailure(EXHAUSTED_OPTIONS_ON_FIXED, cd.getConstraints(),
								sourceFeatureId, featureId );
							
					}
					
					DefaultConstraintSource dcs = new DefaultConstraintSource(
							"derived:fixed-feature", "New constraint on feature using fixed feature.");
					TargetConstraint<V, DefaultConstraintSource> newConstraint =
						new TargetConstraint<V, DefaultConstraintSource>(dcs, true, sourceFeatureId, newRange);
					m_restartSearch = true;
					m_logging.debug(ASSUMING_CONSTRAINT, "Assuming constraint of " + newConstraint
							+ " because versions outside that range have a dependency on feature " + featureId + " that isn't satisfied by "
							+ target);
					m_nameToDiscoveredConstraint.put(sourceFeatureId, newConstraint);
				}
			}
		}
	}

	/**
	 * Run through a list of features, and get all of their dependencies on other features turned into a map
	 * of feature name to constraints.
	 * 
	 * @param featuresToCheck	The set of features from which to extract the resulting map.
	 * @param addDiscovered If <code>true</code>, then add the discovered constraints.
	 * 
	 * @return
	 */
	private Map<String, List<TargetConstraint<V, SolverConstraintSource> > >
	getConstraintsMap(List<F > featuresToCheck, boolean addDiscovered) {

		Map<String, List<TargetConstraint<V, SolverConstraintSource > > > result = newMap();
		
		// go through the features we have to check, get all their constraints, and put them in a map
		// of feature name to list of constraints.
		for (F fd : featuresToCheck) {
			for (TargetConstraint<V, F> constraint : fd.getFeatureConstraints() ) {
				String featureName = constraint.getTargetName();
				List< TargetConstraint<V, SolverConstraintSource > > constraintsForName
					= getMapListValue(result, featureName);
				TargetConstraint<V, SolverConstraintSource> downgradedConstraint
					= TargetConstraint.downcastSource(constraint);
				constraintsForName.add( downgradedConstraint );
			}
		}
		
		// Now go through all of the contingent constraints.
		for (Map.Entry<String, List<TargetConstraint<V, SolverConstraintSource>>> entry: result.entrySet() ) {
			String featureId = entry.getKey();
			List<TargetConstraint<V, SolverConstraintSource> > constraintsForName = entry.getValue();
			List<TargetConstraint<V, SolverConstraintSource>> perFeatureContingent =
				m_contingentConstraints.get(featureId);

			// do we have contingent constraints for this particular feature?
			if (perFeatureContingent != null) {
				// yes, add them in.
				constraintsForName.addAll(perFeatureContingent);
			}
		}
		
		for(Map.Entry<String, List<TargetConstraint<V, SolverConstraintSource>>> eachFutureConstraint : m_nameToFutureConstraint.entrySet()){
			String featureID = eachFutureConstraint.getKey();
			if (!m_nameToChoiceDetails.containsKey(featureID)) {
				List< TargetConstraint<V, SolverConstraintSource > > constraintsForName = getMapListValue(result, featureID);
				constraintsForName.addAll(eachFutureConstraint.getValue());
			}
		}

		if (addDiscovered) {
			// now, for other discovered constraints, such as plugin constraints, we actually need
			// to apply those at the beginning of the search, otherwise we'll loop forever
			// rediscovering plugin constraints.
			for (Map.Entry<String, TargetConstraint<V, DefaultConstraintSource>> entry : m_nameToDiscoveredConstraint.entrySet() ) {
				String featureId = entry.getKey();
				TargetConstraint<V, DefaultConstraintSource> constraint = entry.getValue();
				
				// do we already have choice information for this feature id?
				if (!m_nameToChoiceDetails.containsKey(featureId)) {
					// nope, so go add this constraint to the list of constraints we want to satisfy.
					List<TargetConstraint<V, SolverConstraintSource>> constraintsOnFeature =
						DataUtils.getMapListValue(result, featureId);
					
					TargetConstraint<V, SolverConstraintSource> downcast = TargetConstraint.downcastSource(constraint);
					constraintsOnFeature.add( downcast );
				}
			}
		}
		return result;
	}

	/**
	 * resets all of the default choices for features to include all of the fixed features that
	 * we've "chosen."
	 */
	private void resetNameToChoiceDetails() {
		
		// empty the current set of choices.
		m_nameToChoiceDetails.clear();
		
		// now re-add the fixed contributions....
		if (m_fixedContributions != null) {
			for (F fixedFeature : m_fixedContributions) {
				Target<V> fixedTarget = fixedFeature.getTarget();
				ChoiceDetails<V, F> cd =
					new ChoiceDetails<V, F>(fixedTarget, fixedFeature, true);
				m_nameToChoiceDetails.put(fixedTarget.getTargetId(), cd);
			}
		}
	}

	/**
	 * Filter through a list of constraints to find all of the constraints that are not
	 * satisfied by the given target.
	 * 
	 * @param constraints	The set of constraints to check
	 * @param target		The target that is supposed to match.
	 * 
	 * @return	An array (never null) of constraints for which target does not satisfy.
	 */
	private List<TargetConstraint<V, SolverConstraintSource > > getViolatedConstraints(
			List<TargetConstraint<V, SolverConstraintSource > > constraints, Target<V> target) {
		List<TargetConstraint<V, SolverConstraintSource > > results = newList();
		for (TargetConstraint<V, SolverConstraintSource > oneConstraint : constraints) {
			if (!oneConstraint.isMatchForTarget(target)) {
				results.add(oneConstraint);
			}
		}
		
		return results;
	}

	//=========================================================================
	//	Private member data.
	//=========================================================================

	/**
	 * Take as input a collection, and build a map that partitions the collection my some
	 * unique key from the entries in that collection.
	 * 
	 * @param toPartition Collection of items to partition. 
	 * @param extractor	The implementation of the "extractor" interface that will get the
	 * 	relevant value from each object.
	 * @return An appropriate Map.
	 */
	public static <V extends Comparable<V>, F extends Feature<V, F>> Map<String, F >
		uniquelyPartition(Collection< F> toPartition) {
	
	    Map<String, F > result = newMap();
	
	    for (F entry : toPartition) {
	        String featureId = entry.getTarget().getTargetId();
			F current = result.put(featureId, entry);
	        if (current != null) {
	        	throw new IllegalStateException("Found a second feature file with the same feature ID: <<" +
	        			featureId + ">>\nSources: " +   
	        			entry.getSourcePathString() + " and " + current.getSourcePathString() );
	        }
	    }
	
	    return result;
	}

	/**
	 * Access to the sites supplying the information for this search
	 */
	private FeatureSource<V, F> m_sites;
	
	private List< F > m_nextFeaturesToCheck;
	
	private List< F >	m_startingContributions;
	
	private Collection< F > m_fixedContributions;
	
	/**
	 * Maps a feature name to a set of "contingent" constraints that will only be used if other
	 * constraints arise on the feature in question.
	 */
	private Map<String, List< TargetConstraint<V, SolverConstraintSource> > > m_contingentConstraints =
		newMap();
	
	private Map< String, ChoiceDetails<V, F> > m_nameToChoiceDetails;
	
	private Map< String, ChoiceDetails<V, F> > m_unmodifiableNameToChoiceDetails;
	
	private Map< String, Target<V> > m_nameToFixedContributions;
	
	private Map< String, F > m_nameToStartingContributions;
	
	/**
	 * Keeps track of constraints that are "discovered", that is new constraints
	 * from resolved features that contradict earlier constraints.
	 */
	private Map<String, TargetConstraint<V, DefaultConstraintSource> > m_nameToDiscoveredConstraint;
	
	/**
	 * Keeps track of "future" constraints - constraints that aren't known the first time through
	 * the resolution process, but are discovered later, and are narrower than earlier constraints.
	 */
	private Map<String, List< TargetConstraint<V, SolverConstraintSource> > > m_nameToFutureConstraint;
	
	private ChoiceAlgorithm<V>	m_chooser;
	
	/**
	 * Keeps track of whether our search needs to be restarted....
	 */
	private boolean m_restartSearch;
	
	private LogOutput<ConstraintResult, String> m_logging;
	
	private static String sm_linebreak = System.getProperty("line.separator");
	
	private static LogOutput<ConstraintResult, String> sm_defaultLogging =
		new DefaultSystemOutLogging<ConstraintResult, String>();
}
