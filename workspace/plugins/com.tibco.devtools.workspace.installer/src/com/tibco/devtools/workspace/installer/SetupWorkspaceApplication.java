package com.tibco.devtools.workspace.installer;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPlatformRunnable;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Eclipse "application" to set up workspaces.
 * 
 * TODO - I'm unsure whether or not I can follow the instructions of {@link IPlatformRunnable},
 * and switch to using IApplication, since, at least until now, I've tried to maintain backwards
 * compatibility with Eclipse 3.2.
 */
@SuppressWarnings("deprecation")
public class SetupWorkspaceApplication implements IPlatformRunnable {

	public static final Integer EXIT_ERROR = Integer.valueOf(1);

	public Object run(Object argsObj) throws Exception {
		boolean success = true; 
		try {

            String[] args = (String[]) argsObj;

            if (args == null || args.length < 1) {
                throw new IllegalArgumentException("Expecting a list of folders to be passed.");
            }

            List<File> searchPath = DataUtils.newList();
            for (String arg : (String[])argsObj) {
                File aPath = new File(arg);
                if (aPath.isDirectory()) {
                    searchPath.add(aPath);
                }
                else {
                    System.out.println("Warning: " + arg + " is not a directory - ignoring.");
                }
            }

			SetupWorkspaceCommand setupWorkspace = new SetupWorkspaceCommand(searchPath);
			success = setupWorkspace.run( new InstallProgressMonitor(10) );

            // make sure we save the workspace prior to exit...
            // note that this must be done outside a workspace "run" operation.
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.save(true, null);

		}
		catch (RuntimeException e) {
			return EXIT_ERROR;
		}

		// make sure there are arguments...
		return success ? EXIT_OK : EXIT_ERROR;
	}

}
