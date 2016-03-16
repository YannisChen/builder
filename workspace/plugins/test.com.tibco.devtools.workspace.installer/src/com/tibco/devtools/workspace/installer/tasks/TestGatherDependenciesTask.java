package com.tibco.devtools.workspace.installer.tasks;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;

import junit.framework.TestCase;

/**
 * Test the gather-dependencies task to verify that it does the right thing.
 * 
 */
public class TestGatherDependenciesTask extends TestCase {

	private AntProjectRunner m_projRunner;
	
	@Override
	protected void setUp() throws Exception {
		
		m_projRunner = new AntProjectRunner("test-gather.xml");
	}

	@Override
	protected void tearDown() throws Exception {
		m_projRunner.teardown();
	}

	/**
	 * The following test works in a quite hacked way - normally, you cannot
	 * "maybeConfigure" an Ant task unless you've executed all of the tasks
	 * that come before it.  In this case, since we know our target has no
	 * prerequisites, and we know our task comes first, we can simply test
	 * how we do on parsing the task.
	 * 
	 * @throws IOException
	 */
	public void testParseUpdateSites() throws IOException {
		
		Target myTarget = m_projRunner.getTargetFromProject("gather");
		
		// second, get the first task, and make sure it is configured - this makes sure
		// we're parsed.
		myTarget.getTasks()[0].maybeConfigure();
		
		// now get the actual task.
		Task[] tasks = myTarget.getTasks();
		UnknownElement ue = (UnknownElement) tasks[0];
		GatherDependenciesTask gdt = (GatherDependenciesTask) ue.getRealThing();
		//gdt.normalizeSettings(true);
		List<URL> urls = gdt.getUpdateSites();
		assertEquals(3, urls.size() );
	}

}
