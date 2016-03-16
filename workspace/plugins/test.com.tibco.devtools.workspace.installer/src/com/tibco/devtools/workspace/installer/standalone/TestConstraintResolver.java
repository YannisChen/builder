package com.tibco.devtools.workspace.installer.standalone;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tibco.devtools.workspace.installer.tasks.ParsingUtils;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

/**
 * Tests the constraint resolver code.
 */
public class TestConstraintResolver extends TestCase {

	private static final LatestBuildOfLeastMatchAlgorithm<VersionInfo> LBLM_ALGORITHM = new LatestBuildOfLeastMatchAlgorithm<VersionInfo>();

	private static final LatestAlgorithm<VersionInfo> LATEST_ALGORITHM = new LatestAlgorithm<VersionInfo>();

	public void testChooseFeature() {
		
		ChoiceAlgorithm<VersionInfo> lblm = LBLM_ALGORITHM;
		
		MockFeatureSource site = createMockSite();
		
		CaptureLogger<ConstraintResult, String> logger = new CaptureLogger<ConstraintResult, String>();
		ConstraintSolver<VersionInfo, MockFeature> solver = ConstraintSolver.create(site, sm_emptyList, null, lblm);
		solver.setLogger(logger);
		
		List<TargetConstraint<VersionInfo, SolverConstraintSource>> emptyConstraints = Collections.emptyList();
		try {
			solver.chooseFeature("feature.z", parseRange("[3.2,3.3)"), emptyConstraints);
			fail("Should not have found the feature!");
		} catch (ConstraintException e) {
			assertNotNull( logger.matchMessage(logger.getErrors(), ConstraintResult.NOT_FOUND, "feature.z") );
		}
		
		try {
			solver.chooseFeature("feature.y", parseRange("[1.1,2)"), emptyConstraints);
			fail("Should not have found the feature!");
		} catch (ConstraintException e) {
			assertNotNull( logger.matchMessage(logger.getErrors(), ConstraintResult.UNMATCHED, "feature.y") );
		}
	}

	public void testFeatureSearch1() throws ConstraintException {
		ConstraintSolver<VersionInfo, MockFeature> solver = runSolverWithFeature("scenario1.a", "1.0");
		
		assertEquals(VersionInfo.parseVersion("1.1"), getChoiceVersion(solver, "scenario1.c"));
		assertEquals(VersionInfo.parseVersion("1.0"), getChoiceVersion(solver, "scenario1.b"));
		
	}

	public VersionInfo getChoiceVersion(ConstraintSolver<VersionInfo, MockFeature> solver, String featureId) {
		Map<String, ChoiceDetails<VersionInfo, MockFeature>> nameToChoice = solver.getNameToChoiceDetails();
		return nameToChoice.get(featureId).getTarget().getVersion();
	}
	
	public void testFeatureSearch2() throws ConstraintException {
		ConstraintSolver<VersionInfo, MockFeature> solver = runSolverWithFeature("scenario2.a", "1.0");
		
		assertEquals(VersionInfo.parseVersion("1.1"), getChoiceVersion(solver, "scenario2.e"));		
	}

	public void testLatestResolverWithFixedRollback() throws ConstraintException {
		
		MockFeatureSource site = createMockSite();
		
		List<MockFeature> starting = getListWithFeature(site, "scenario4.a", "1.0");
		List<MockFeature> fixed = getListWithFeature(site, "feature.y", "1.0");
		
		ConstraintSolver<VersionInfo, MockFeature> solver = 
			ConstraintSolver.create(site, starting, fixed, LATEST_ALGORITHM );
		
		assertTrue( solver.search() );

		assertEquals(VersionInfo.parseVersion("1.1"), getChoiceVersion(solver, "scenario4.b"));		
	}
	
	public void testScanningForPlugins() {
		
		MockFeatureSource site = createMockSite();
		
	    List< MockFeature > starting = getListWithFeature(site, "scenario9.a", "1.0");
	
		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		Map<String, List<TargetConstraint<VersionInfo, MockFeature>>> pluginIdToConstraint = DataUtils.newMap();
		TargetConstraint<VersionInfo, MockFeature> cnstrt1 = new TargetConstraint<VersionInfo, MockFeature>(null, true, "scenario13.pb", parseRange("[1.1,2.0)"));
		List<TargetConstraint<VersionInfo, MockFeature>> list = DataUtils.newList();
		list.add(cnstrt1);
		pluginIdToConstraint.put("scenario13.pb", list);
		Map<String, List<TargetConstraint<VersionInfo, MockFeature>>> results = solver.scanForFeaturesWithPlugins(pluginIdToConstraint);
		assertEquals(1, results.size() );
		
		List<TargetConstraint<VersionInfo, MockFeature>> resultList1 = results.get("scenario13.pb"); 
		assertEquals(1, resultList1.size());
		assertEquals("scenario13.a", resultList1.get(0).getTargetName());
	}

	public void testPluginResolutionSuccess() {
		
		MockFeatureSource site = createMockSite();
		List<MockFeature> starting = getListWithFeature(site, "scenario10.a", "1.0");
		
		// this looks like any of the above scenarios, except that we've added a plugin dependency.
		ConstraintSolver<VersionInfo, MockFeature> solver = 
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		// just verify no errors on the search.
		assertTrue( solver.search() );
	}
	
	public void testPluginResolutionFailure() {
		
		MockFeatureSource site = createMockSite();
		List<MockFeature> starting = getListWithFeature(site, "scenario11.a", "1.0");
		
		// this looks like any of the above scenarios, except that we've added a plugin dependency.
		ConstraintSolver<VersionInfo, MockFeature> solver = 
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		CaptureLogger<ConstraintResult, String> logger = new CaptureLogger<ConstraintResult, String>();
		solver.setLogger( logger );
		// just verify no errors on the search.
		assertFalse("Solver should have failed.", solver.search());
		
		assertNotNull( logger.matchMessage( logger.getErrors(), ConstraintResult.UNMATCHED_PLUGINS, "scenario11.pc") );
		assertNotNull( logger.matchMessage( logger.getErrors(), ConstraintResult.UNMATCHED_PLUGINS, "scenario11.pd") );
		
	}
	
	public void testSearchFailures() {

		MockFeatureSource site = createMockSite();
		
		List<MockFeature> fixed = getListWithFeature(site, "feature.y", "1.0");
		List<MockFeature> starting = getListWithFeature(site, "scenario3.a", "1.0");
		ConstraintSolver<VersionInfo, MockFeature> solver = ConstraintSolver.create(site, starting, fixed, LBLM_ALGORITHM);
		
		assertConstraintFailure(solver, ConstraintResult.FIXED_MISMATCH, "feature.y");
		
		// Verify that the constraints from initial features on fixed features can ever be
		// satisfied.
		solver = ConstraintSolver.create(site, starting, fixed, LATEST_ALGORITHM);
		assertConstraintFailure(solver, ConstraintResult.STARTING_MISMATCH_ON_FIXED, "feature.y");
		
	    // verify that situations in which no intersection is discovered are flagged.
	    starting = getListWithFeature(site, "scenario5.a", "1.0");
	    solver = ConstraintSolver.create(site, starting, sm_emptyList, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.NO_INTERSECTION, "feature.y");
	    
	    // verify that when features do not exist on the update site, we get a "not found" error.
	    starting = getListWithFeature(site, "scenario6.a", "1.0");
	    solver = ConstraintSolver.create(site, starting, sm_emptyList, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.NOT_FOUND, "feature.z");
	    
	    // verify that when features do not exist on the update site, we get a "not found" error.
	    starting = getListWithFeature(site, "scenario7.a", "1.0");
	    solver = ConstraintSolver.create(site, starting, sm_emptyList, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.UNMATCHED, "feature.y",
	    		"Available versions include [1, 1.0.1]");

	    // verify that we disallow "donut holes" - indirect dependencies on features that are part of the originating set.
	    starting = getListWithFeature(site, "scenario8.a", "1.0");
	    starting.add(getFeature(site, "scenario8.c", "1.1"));
	    solver = ConstraintSolver.create(site, starting, sm_emptyList, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.DONUT_HOLE, "scenario8.c");

	    // verify that constraints on original features to each other are consistent!
	    starting.add(getFeature(site, "scenario8.d", "1.0") );
	    solver = ConstraintSolver.create(site, starting, sm_emptyList, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.INVALID_INITIAL_STATE, "scenario8.c");

	    // verify that constraints on original features to each other are consistent!
	    starting = getListWithFeature(site, "scenario9.a", "1.0");
		fixed = getListWithFeature(site, "feature.y", "1.0");
	    solver = ConstraintSolver.create(site, starting, fixed, LATEST_ALGORITHM );
	    assertConstraintFailure(solver, ConstraintResult.EXHAUSTED_OPTIONS_ON_FIXED, "scenario9.b");

	}

	public void testConstraintMismatches() {
		MockFeatureSource site = createMockSite();
		
		List<MockFeature> starting = getListWithFeature(site, "scenario12.a", "1.0");
		starting.add( getFeature(site, "scenario12.b", "1.0") );
		starting.add( getFeature(site, "scenario12.f", "1.1") );
		
		List<MockFeature> fixed = DataUtils.newList();
		fixed.add( new MockFeature( TestVersionedTarget.parseTarget("scenario12.z 1.1"), "<<GENERATED>>") );

		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, fixed, LBLM_ALGORITHM );
		
		assertTrue( solver.search() );
		List<MismatchedConstraintsResult<VersionInfo, SolverConstraintSource>> mcrs = solver.computeMismatchedInitialConstraints();
		assertEquals(5, mcrs.size() );
		
		MismatchedConstraintsResult<VersionInfo, SolverConstraintSource> err1 = mcrs.get(0);
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > starting1 = err1.getStartingConstraints();
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > narrower1 = err1.getNarrowerConstraints();
		assertEquals("scenario12.c", err1.getFeatureId() );
		assertEquals(1, starting1.size() );
		assertEquals("scenario12.a", starting1.get(0).getSource().getSourceLogicalId() );
		assertEquals(1, narrower1.size() );
		assertEquals("scenario12.b", narrower1.get(0).getSource().getSourceLogicalId() );

		MismatchedConstraintsResult<VersionInfo, SolverConstraintSource> err2 = mcrs.get(1);
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > starting2 = err2.getStartingConstraints();
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > narrower2 = err2.getNarrowerConstraints();
		
		assertEquals("scenario12.d", err2.getFeatureId() );
		assertEquals(1, starting2.size() );
		assertEquals("scenario12.a", starting2.get(0).getSource().getSourceLogicalId() );
		assertEquals(0, narrower2.size() );

		MismatchedConstraintsResult<VersionInfo, SolverConstraintSource> err3 = mcrs.get(2);
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > starting3 = err3.getStartingConstraints();
		List< TargetConstraint<VersionInfo, SolverConstraintSource> > narrower3 = err3.getNarrowerConstraints();
		
		assertEquals("scenario12.e", err3.getFeatureId() );
		assertEquals(1, starting3.size() );
		assertEquals("scenario12.b", starting3.get(0).getSource().getSourceLogicalId() );
		
		// note that there is only one constraints here - a "future" constraint.
		assertEquals(1, narrower3.size() );
		assertEquals("scenario12.d", narrower3.get(0).getSource().getSourceLogicalId() );
		
		MismatchedConstraintsResult<VersionInfo, SolverConstraintSource> err4 = mcrs.get(3);
		assertEquals("scenario12.f", err4.getFeatureId() );
		
		MismatchedConstraintsResult<VersionInfo, SolverConstraintSource> err5 = mcrs.get(4);
		assertEquals("scenario12.z", err5.getFeatureId() );
		
	}
	
	public void testExactMatch() {
		MockFeatureSource site = createMockSite();
		
		List<MockFeature> starting = getListWithFeature(site, "scenario14.a", "1.0");
		
		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		assertTrue("Should have resolved.", solver.search());
	}
	
	/**
	 * When given two starting features with the same feature ID, the constraint solver
	 * fails.  This test verifies that it fails in a way that tells what is going on....
	 */
	public void testUniquePartitionFailure() {
		
		List<MockFeature > toPartition = DataUtils.newList();
		Target<VersionInfo> target1 = createTarget("some.feature.foo", "1.0");
		MockFeature mf1 = new MockFeature(target1, "Source#1");
		toPartition.add(mf1);
		
		Target<VersionInfo> target2 = createTarget("some.feature.foo", "2.0");
		MockFeature mf2 = new MockFeature(target2, "Source#2");
		
		toPartition.add(mf2);
		try {
			ConstraintSolver.uniquelyPartition(toPartition);
			fail("Expected to get an IllegalStateException!");
		}
		catch (IllegalStateException ise) {
			String message = ise.getMessage();
			assertTrue( message.contains("Source#1"));
			assertTrue( message.contains("Source#2"));
		}
		
	}
	
	/**
	 * This test exists because non-intersecting constraints on already chosen
	 * targets would initiate an infinite loop, wherein the new constraints
	 * would be used to generate a new filter, discarding the old constraints.  
	 */
	public void testNonIntersectingConstraintsOnAlreadyChosenTargets() {
		MockFeatureSource site = createMockSite();

		List<MockFeature> starting = getListWithFeature(site, "scenario16.a", "1.0");
		
		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		assertFalse("Should have failed.", solver.search());

	}
	
	/**
	 * Initial implementation generated constraints when discovering a new constraint
	 * that invalidated an old choice, but that meant gratuitously introducing
	 * extra constraints.
	 * 
	 * <p>New solution saves all constraints that triggered a conflict.
	 * </p>
	 */
	public void testUsingExistingConstraintsWheneverPossible() {
		MockFeatureSource site = createMockSite();

		List<MockFeature> starting = getListWithFeature(site, "scenario17.a", "1.0");
		
		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		// capture the output so that we can check that the right number of items appear in the output.
		CaptureLogger<ConstraintResult, String> logger = new CaptureLogger<ConstraintResult, String>();
		solver.setLogger( logger );
		
		solver.search();
		
		// Get the message that we've got all the constraints that we're supposed to have.
		String matched = logger.matchMessage(logger.getErrors(),
				ConstraintResult.NO_INTERSECTION,
				"scenario17.b using range [1.1,2)",
				"scenario17.e using range [1.2,2)",
				"scenario17.g using range [2.1,3)");
		
		assertNotNull(matched);
	}
	
	public void testContingentConstraints() {
		
		MockFeatureSource site = createMockSite();

		List<MockFeature> starting = getListWithFeature(site, "scenario18.a", "1.0");
		
		ConstraintSolver<VersionInfo, MockFeature> solver =
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		List<TargetConstraint<VersionInfo, DefaultConstraintSource>> contingents = DataUtils.newList();
		
		// a tighter constraint
		contingents.add(newTestConstraint("scenarion 18", "scenario18.b [1.1,2)") );
		
		// constraint should not be used, but feature exists.
		contingents.add(newTestConstraint("scenarion 18", "scenario18.c [1,2)") );
		
		// constraint should not be used, feature doesn't exist.
		contingents.add(newTestConstraint("scenarion 18", "scenario18.d [1,2)") );
		
		solver.addContingentConstraints(contingents);
		assertTrue(solver.search() );
		
		Map<String, ChoiceDetails<VersionInfo, MockFeature>> results = solver.getNameToChoiceDetails();
		
		ChoiceDetails<VersionInfo, MockFeature> cd1 = results.get("scenario18.b");
		assertEquals(cd1.getTarget(), createTarget("scenario18.b", "1.1"));
		
		assertFalse(results.containsKey("scenario18.c"));
	}
	
	public void testPluginConstraintLoopingForever() {
		
		MockFeatureSource site = createMockSite();
		List<MockFeature> starting = getListWithFeature(site, "scenario19.a", "1.0");
		
		// this looks like any of the above scenarios, except that we've added a plugin dependency.
		ConstraintSolver<VersionInfo, MockFeature> solver = 
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		// just verify no errors on the search.
		assertTrue( solver.search() );
	}
	
	private TargetConstraint<VersionInfo, DefaultConstraintSource> newTestConstraint(String description, String constraintStr) {
		
		DefaultConstraintSource source = new DefaultConstraintSource(null, description);
		TargetConstraint<VersionInfo, DefaultConstraintSource> constraint =
			ParsingUtils.constraintFromString(source, constraintStr);
		
		return constraint;
	}
	
	private void assertConstraintFailure(ConstraintSolver<?, ?> solver,
			ConstraintResult expected, String ... matches) {
		
		CaptureLogger<ConstraintResult, String> logger = new CaptureLogger<ConstraintResult, String>();
		solver.setLogger(logger);
		assertFalse("Search should have failed.", solver.search());
		
		// make sure there is a message that contains the feature ID and whatever other contained string
		// I'm looking for.
		String msg = logger.matchMessage(logger.getErrors(), expected, matches);
		assertNotNull(msg);
	}
	
	public static Range<VersionInfo> parseRange(String str) {
		Matcher matcher = sm_rangeExpr.matcher(str);
		if (matcher.matches()) {
			return new Range<VersionInfo>( VersionInfo.parseVersion(matcher.group(2)),
					matcher.group(1).equals("["), VersionInfo.parseVersion(matcher.group(3)),
					matcher.group(4).equals("]"));
		}
		throw new IllegalArgumentException("Unrecognized version range.");
	}
	
	private ConstraintSolver<VersionInfo, MockFeature> runSolverWithFeature(String featureName,
			String version) throws ConstraintException {
		
		MockFeatureSource site = createMockSite();
		
		List<MockFeature> starting = getListWithFeature(site, featureName, version);
		
		ConstraintSolver<VersionInfo, MockFeature> solver = 
			ConstraintSolver.create(site, starting, sm_emptyList, LBLM_ALGORITHM );
		
		solver.search();
		return solver;
	}

	/**
	 * Get a list of one element with the desired feature in it.
	 * 
	 * @param site	The site from which to fetch the feature model.
	 * @param featureName	The name of the feature to fetch.
	 * @param version	The version (as a string)
	 * @return	A list containing the one feature.
	 */
	private List<MockFeature> getListWithFeature(MockFeatureSource site,
			String featureName, String version) {
		MockFeature feature = getFeature(site, featureName, version);
		List<MockFeature> starting = DataUtils.newList();
		starting.add(feature);
		return starting;
	}

	private MockFeature getFeature(MockFeatureSource site, String featureName,
			String versionStr) {
		Target<VersionInfo> target = createTarget(featureName, versionStr);
		return site.getFeatureModel(target );
	}

	private Target<VersionInfo> createTarget(String featureName, String versionStr) {
		VersionInfo version = VersionInfo.parseVersion(versionStr);
		Target<VersionInfo> target = new Target<VersionInfo>(featureName, version );
		return target;
	}
	
	private MockFeatureSource createMockSite() {
		InputStream stream = getClass().getResourceAsStream("ConstraintResolverScenarios.txt");
		MockFeatureSource site = MockFeatureSource.createMockSiteFromStream(stream);
		return site;
	}
	
	private static List<MockFeature> sm_emptyList = Collections.emptyList();
	
	private static Pattern sm_rangeExpr = Pattern.compile("^(\\[|\\()([^,]*)\\,([^\\]\\)]*)(\\)|\\])$");
}
