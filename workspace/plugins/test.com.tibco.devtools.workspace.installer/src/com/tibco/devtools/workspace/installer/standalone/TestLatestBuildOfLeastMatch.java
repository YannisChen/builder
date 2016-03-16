package com.tibco.devtools.workspace.installer.standalone;

import java.util.List;

import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

public class TestLatestBuildOfLeastMatch extends TestCase {

	public void testDropNonLatest() {
		String [] toParse = {
				"foo 1.2.3.002",
				"foo 1.2.3.001",
				"foo 1.2.3.003",
				"bar 2.1.0.004",
				"bar 2.1.0.003",
				"bar 2.1.0.002",
				"bar 2.1.2.004",
				"bar 2.1.2.003"
		};
		
		List<Target<VersionInfo>> items = createTargetList(toParse);
		
		Target<VersionInfo> item0 = items.get(3);
		Target<VersionInfo> item1 = items.get(6);
		Target<VersionInfo> item2 = items.get(2);

		List<Target<VersionInfo>> result = LatestBuildOfLeastMatchAlgorithm.dropNonLatest(items);
		assertEquals(item0, result.get(0) );
		assertEquals(item1, result.get(1) );
		assertEquals(item2, result.get(2) );
	}

	/**
	 * Verifies that an exact constraint on something other than the latest build still works.
	 */
	public void testExactConstraint() {

		String [] toParse = {
				"foo 1.2.3.002",
				"foo 1.2.3.001",
				"foo 1.2.3.003",
		};
		LatestBuildOfLeastMatchAlgorithm<VersionInfo> algo =
			new LatestBuildOfLeastMatchAlgorithm<VersionInfo>();
		
		List< Target<VersionInfo> > options = createTargetList(toParse);
		
		VersionInfo exact = options.get(0).getVersion();
		Range<VersionInfo> range = new Range<VersionInfo>(exact, true, exact, true);
		Target<VersionInfo> result = algo.chooseTarget(options, range);
		assertEquals(options.get(0), result);
		
	}
	
	/**
	 * Produce a list of targets from a string array.
	 * @param toParse
	 * @return
	 */
	private static List<Target<VersionInfo>> createTargetList(String[] toParse) {
		List<Target<VersionInfo>> items = DataUtils.newList();
		for (String str : toParse) {
			items.add(TestVersionedTarget.parseTarget(str));
		}
		return items;
	}

}
