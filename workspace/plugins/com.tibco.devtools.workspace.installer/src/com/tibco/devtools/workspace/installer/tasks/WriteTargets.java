package com.tibco.devtools.workspace.installer.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * Writes out a {@link TargetList} object to a file
 */
public class WriteTargets extends Task {

	//=========================================================================
	// Ant setters
	//=========================================================================
	
	/**
	 * Set the "file" attribute.
	 */
	public void setFile(File target) {
		m_file = target;
	}
	
	/**
	 * Set the "refid" attribute.
	 * @param refid
	 */
	public void setRefid(String refid) {
		m_refid = refid;
	}
	
	/**
	 * Set the "overwrite" attribute.
	 */
	public void setOverWrite(boolean overwrite) {
		m_overwrite = overwrite;
	}
	
	public void setFormat(String format) {
		m_format = format;
	}
	
	//=========================================================================
	// Ant execution
	//=========================================================================
	
	/**
	 * Actually perform the write of the target information.
	 */
	@Override
	public void execute() throws BuildException {
		if (m_file == null) {
			throw new BuildException("Attribute 'file' required on 'write.targets' task.");
		}
		
		if (m_refid == null) {
			throw new BuildException("Attribute 'refid' required on 'write.targets' task.");
		}
		
        Object genericTargets = getProject().getReference(m_refid);
        if (genericTargets == null) {
        	throw new BuildException("No object with reference id of " + m_refid + " as specified by the refid attribute.");
        }
        
        if (!(genericTargets instanceof TargetList) ) {
        	throw new BuildException("Object at reference name " + m_refid + " is of the wrong type.  Must be created by evaluate.dependencies task.");
		}
        
        if (!m_overwrite && m_file.exists()) {
        	throw new BuildException("Target " + m_file + " exists, and overwrite is false.");
        }
        
        TargetList targets = (TargetList) genericTargets;
        
        try {
    		FileOutputStream fos = new FileOutputStream(m_file);
    		BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(fos, "utf-8") );

    		if (m_format.equalsIgnoreCase("text")) {
            	writeTextFile(bw, targets);
            }
            else if (m_format.equalsIgnoreCase("xml")) {
            	writeXmlFile(bw, targets);
            }
            else {
            	bw.close();
            	throw new BuildException("Attribute 'format' must be either 'text' or 'xml'.");
            }

    		bw.close();
        }
        catch (IOException e) {
        	throw new BuildException(e);
        }
	}
	
	/**
	 * Hmmm - this takes the debatable approach of assuming that only legal characters
	 * will show up in version qualifiers and feature names.
	 * 
	 * @param bw
	 * @param targets
	 * @throws IOException
	 */
	private void writeXmlFile(BufferedWriter bw, TargetList targets) throws IOException {
		bw.write("<?xml version=\"1.0\" ?>");
		bw.newLine();
		bw.write("<targets>");
		bw.newLine();
		
		// write out the XML elements....
		for (Target<VersionInfo> target : targets.getTargets() ) {
			bw.write("  ");
			bw.write("<target feature=\"");
			bw.write(target.getTargetId());
			bw.write("\" version=\"");
			bw.write(target.getVersion().toString());
			bw.write("\" />");
			bw.newLine();
		}
		
		bw.write("</targets>");
		bw.newLine();
	}

	/**
	 * Note that we don't use a standard properties file here, because
	 * the general case allows for multiple versions of the same target ID,
	 * something we cannot allow with a property file.
	 * 
	 * @param targets
	 * @throws IOException
	 */
	private void writeTextFile(BufferedWriter bw, TargetList targets) throws IOException {
		
		bw.write("# Text format listing of feature targets.");
		bw.newLine();
		for (Target<VersionInfo> target : targets.getTargets() ) {
			bw.write(target.getTargetId());
			bw.write(" ");
			bw.write(target.getVersion().toString());
			bw.newLine();
		}
	}

	private String m_format = "text";
	
	private File m_file;

	private String m_refid;
	
	private boolean m_overwrite = false;
}
