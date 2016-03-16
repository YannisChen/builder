package com.tibco.devtools.workspace.installer.standalone;

import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.w3c.dom.Document;

import com.tibco.devtools.workspace.installer.utils.DomTestUtilities;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.TargetConstraint;
import com.tibco.devtools.workspace.model.VersionInfo;

public class TestFeatureModel extends TestCase {

	public void testParseSimpleFeature() throws Exception {

		URL fileLoc = this.getClass().getResource("simpleFeature.xml");
		Document dom = DomTestUtilities.domFromClasspath(this.getClass(), "simpleFeature.xml");
		FeatureDescriptor model = FeatureDescriptor.fromDocument(dom);
		model.setUrlLocation(fileLoc);
		
		assertEquals("com.tibco.devtools.workspace", model.getTarget().getTargetId() );
		
		VersionInfo version = new VersionInfo(1, 1, 1, "qualifier");
		assertEquals(version, model.getTarget().getVersion());
		
		List<TargetConstraint<VersionInfo, FeatureDescriptor>> constraints = model.getFeatureConstraints();
		assertEquals(8, constraints.size());
		
		TargetConstraint<VersionInfo, FeatureDescriptor> firstConstraint = constraints.get(0);
		assertEquals("org.eclipse.platform", firstConstraint.getTargetName() );
		
		List<PluginReference<VersionInfo>> plugins = model.getProvidedPlugins();
		PluginReference<VersionInfo> firstPlugin = plugins.get(0);
		
		Target<VersionInfo> target = firstPlugin.getTarget();
		assertEquals("com.tibco.devtools.workspace.installer", target.getTargetId() );
		VersionInfo pluginVers = new VersionInfo(1, 1, 1, "qualifier");
		assertEquals(pluginVers, target.getVersion());
	}
	
	public void testFeatureInclude() throws Exception {
		
		URL fileLoc = this.getClass().getResource("includeFeature.xml");
		Document dom = DomTestUtilities.domFromClasspath(this.getClass(), "includeFeature.xml");
		FeatureDescriptor model = FeatureDescriptor.fromDocument(dom);
		model.setUrlLocation(fileLoc);
		
	}
}
