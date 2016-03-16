package com.tibco.devtools.workspace.installer.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.tibco.devtools.workspace.util.DataUtils;

/**
 * Class to wrap up invocations of Subversion, when needed.
 */
public class SubversionInvoker {

	public SubversionInvoker() {
		m_cmds.add("svn");
		m_cmds.add("delete");
		m_cmds.add("");

		m_processBuilder = new ProcessBuilder( m_cmds );
	}
	
	/**
	 * Perform a local working copy delete of a file in Subversion.
	 * 
	 * @param toDelete	The file-system file that needs deleting...
	 * 
	 * @return	True if successful.
	 */
	public boolean deleteFile(File toDelete) {
		
		try {
			// set the file name to delete.
			m_cmds.set(2, toDelete.toString());
			
			// execute Subversion.
			Process proc = m_processBuilder.start();
			
			// wait...
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				throw new BuildException("Exit code " + proc.exitValue() + " from running Subversion delete.");
			}
		} catch (IOException e) {
			throw new BuildException(e);
		} catch (InterruptedException e) {
			throw new BuildException(e);
		}
		
		return true;
	}
	
	private ProcessBuilder m_processBuilder;
	
	private List<String> m_cmds = DataUtils.newList();
}
