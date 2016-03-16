package com.tibco.devtools.workspace.installer.standalone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.tibco.devtools.workspace.installer.tasks.ParsingUtils;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DataUtils;

/**
 * A mock FeatureSource for testing purposes.
 */
public class MockFeatureSource implements FeatureSource<VersionInfo, MockFeature> {

	/**
	 * Get the feature model for an individual target.
	 */
	public MockFeature getFeatureModel(Target<VersionInfo> target) {
		
		return m_targetToFeature.get(target);
	}

	/**
	 * Get the feature version set for a given feature ID.
	 */
	public List<Target<VersionInfo>> getFeatureVersionSetById(String featureId) {
		
		List<Target<VersionInfo> > unwrappedResult = m_nameToTargetList.get(featureId); 
		return unwrappedResult != null ? Collections.unmodifiableList(unwrappedResult) : null;
	}

	/**
	 * Return the list of all the features....
	 */
	public Collection<String> getAvailableFeatureIds() {
		return m_nameToTargetList.keySet();
	}
	
	/**
	 * Add a feature to the mock feature source.
	 * 
	 * @param feature	The feature to add.
	 */
	public void add(MockFeature feature) {
		Target<VersionInfo> target = feature.getTarget();
		m_targetToFeature.put(target, feature);

		List<Target<VersionInfo> > targets = DataUtils.getMapListValue(m_nameToTargetList, target.getTargetId() );
		targets.add(target);
	}
	
	/**
	 * Given a reader, create a mock site from a line by line format.
	 * 
	 * <p>The format of the file looks like
	 * <pre>
	 * feature.name X.Y.Z
	 *   dependency.1 [A,B)
	 *   dependency.2 [C,D)
	 * another.feature.name P.Q.R
	 *   dependency.3 ....
	 * </pre>
	 * </p>
	 * 
	 * @param reader
	 * @return
	 */
	public static MockFeatureSource createMockSiteFromReader(BufferedReader reader) {
		
		MockFeatureSource mfs = new MockFeatureSource();
		MockFeature currentFeature = null;
		
		try {
			String line;
			while ( ( line = reader.readLine() ) != null) {
				if (sm_blankLine.matcher(line).matches() ) {
					// ignore line
				}
				else if (sm_startsWithSpaces.matcher(line).find() ) {
					line = line.trim();
					if (line.startsWith("RF") || line.startsWith("RP")) {
						// either a "requires feature" or "requires plugin".
						boolean isFeature = line.startsWith("RF");
						line = line.substring(2).trim();
						TargetConstraint<VersionInfo, MockFeature> newConstraint = ParsingUtils.constraintFromString(
								currentFeature, line);
						
						if (isFeature) {
							currentFeature.addFeatureConstraint(newConstraint);
						}
						else {
							currentFeature.addPluginConstraint(newConstraint);
						}
					}
					else if (line.startsWith("PP")) {
						// Provides plugin...
						line = line.substring(2).trim();
						Target<VersionInfo> target = TestVersionedTarget.parseTarget(line);
						PluginReference<VersionInfo> plugRef
							= new PluginReference<VersionInfo>(target);
						currentFeature.getProvidedPlugins().add(plugRef);
					}
				}
				else {
					line = line.trim();
					Target<VersionInfo> target = TestVersionedTarget.parseTarget(line);
					currentFeature = new MockFeature(target, target.toString() );
					mfs.add(currentFeature);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error reading mock update site. " + e, e);
		}
		
		return mfs;
	}

	public static MockFeatureSource createMockSiteFromStream(InputStream stream) {
		InputStreamReader isr = new InputStreamReader(stream);
		BufferedReader br = new BufferedReader(isr);
		return createMockSiteFromReader(br);
	}
	
	private Map<String, List<Target<VersionInfo>>> m_nameToTargetList = new HashMap<String, List<Target<VersionInfo> >>();
	private Map<Target<VersionInfo>, MockFeature> m_targetToFeature = DataUtils.newMap();
	
	private static Pattern sm_blankLine = Pattern.compile("^\\s*$");
	private static Pattern sm_startsWithSpaces = Pattern.compile("^\\s");
}
