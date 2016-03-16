package com.tibco.devtools.workspace.installer.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Range;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;

import junit.framework.TestCase;

/**
 * Tests the functionality of the {@link FeatureUtils} class.
 */
public class TestFeatureUtils extends TestCase {

	/**
	 * This test resolves the exact order of the resulting list by assuming
	 * a depth-first analysis, starting from the first item encountered.
	 * 
	 * <p>Other orderings are obviously possible, so long as the dependencies
	 * are enumerated in the correct order.  If need be, the example tests
	 * could be expanded so that the resulting order is unambiguous, regardless
	 * of the order in which the features are passed.</p>
	 *
	 */
	public void testFeatureDependencySort() {
		
		File fileA = new File("a");
		File fileB = new File("b");
		File fileC = new File("c");
		File fileD = new File("d");
		FeatureDescriptor a = createFeatureModel(fileA, "com.tibco.feature.A",
				new String[] {"com.tibco.feature.B", "com.tibco.feature.D" } );
		
		FeatureDescriptor b = createFeatureModel(fileB, "com.tibco.feature.B",
				new String[] {} );
		
		FeatureDescriptor c = createFeatureModel(fileC, "com.tibco.feature.C",
				new String[] {"com.tibco.feature.D", "org.example.feature.X" } );
		
		FeatureDescriptor d = createFeatureModel(fileD, "com.tibco.feature.D",
				new String[] {} );
		
		// first test ======================
		FeatureDescriptor[] test1Arr = new FeatureDescriptor[] {
				a, b, c, d
		};
		List<FeatureDescriptor> test1 = Arrays.asList(test1Arr);
		List<FeatureDescriptor> result1 = FeatureUtils.sortFeaturesByDependency(test1);
		
		assertTrue("incorrect size of result", result1.size() == 4);
		assertEquals( result1.get(0), b);
		assertEquals( result1.get(1), d);
		assertEquals( result1.get(2), a);
		assertEquals( result1.get(3), c);
		
		// second test ======================
		
		FeatureDescriptor[] test2Arr = new FeatureDescriptor[] { b, a, d };
		List<FeatureDescriptor> test2 = Arrays.asList(test2Arr);
		List<FeatureDescriptor> result2 = FeatureUtils.sortFeaturesByDependency(test2);
		
		assertTrue("incorrect size of result", result2.size() == 3);
		assertEquals( result2.get(0), b);
		assertEquals( result2.get(1), d);
		assertEquals( result2.get(2), a);
		
		// third test ======================
		
		FeatureDescriptor[] test3Arr = new FeatureDescriptor[] { c, b, a, d };
		List<FeatureDescriptor> test3 = Arrays.asList(test3Arr);
		List<FeatureDescriptor> result3 = FeatureUtils.sortFeaturesByDependency(test3);
		
		assertTrue("incorrect size of result", result3.size() == 4);
		assertEquals( result3.get(0), d);
		assertEquals( result3.get(1), c);
		assertEquals( result3.get(2), b);
		assertEquals( result3.get(3), a);
		
	}
	
	public static FeatureDescriptor createFeatureModel(File fileLoc,
			String featureName, String[] dependsOn) {
		
		Target<VersionInfo> target = new Target<VersionInfo>(featureName, new VersionInfo(0, 0, 0, ""));
		FeatureDescriptor desc = new FeatureDescriptor(target);
		desc.setFileLocation(fileLoc);
		
		VersionInfo minVersion = VersionInfo.parseVersion("0.0.0");
		for (String dependsId : dependsOn) {
			Range<VersionInfo> range = new Range<VersionInfo>(minVersion, true, VersionInfo.UNBOUNDED, true);
			TargetConstraint<VersionInfo, FeatureDescriptor> tc =
				new TargetConstraint<VersionInfo, FeatureDescriptor>(null, true, dependsId, range);
			desc.getFeatureConstraints().add(tc);
		}
		
		return desc;
		
	}
}
