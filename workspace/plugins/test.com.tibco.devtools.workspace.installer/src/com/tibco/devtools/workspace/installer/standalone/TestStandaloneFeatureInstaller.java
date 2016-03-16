package com.tibco.devtools.workspace.installer.standalone;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.tibco.devtools.workspace.installer.StandaloneFeatureInstaller;
import com.tibco.devtools.workspace.util.DataUtils;

import junit.framework.TestCase;

/**
 * At the moment, this is a quite limited test of the "standalone" installer
 * class.
 */
public class TestStandaloneFeatureInstaller extends TestCase {

	public void testFilterRemoteUrls() throws MalformedURLException {
		
		List<URL> urls = DataUtils.newList();
		urls.add( new URL("file:///home/user/tibco-build/workspaceInstaller/local-site-cache"));
		urls.add( new URL("http://spin.tibco.com/milestones"));
		
		StandaloneFeatureInstaller.filterRemoteUrls(urls, true);
		assertEquals(2, urls.size());

		StandaloneFeatureInstaller.filterRemoteUrls(urls, false);
		assertEquals(1, urls.size());
	}
}
