/**
 *
 */
package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.FeatureOrderAnalyst;

/** Analyzes the features to be built and generates a feature order output file.
 *
 * @author Amelia A Lewis &lt;alewis@tibco.com>
 *
 */
public class CreateFeatureOrderFileTask extends WorkspaceInstallConfigBaseTask {

    @Override
    public void execute() throws BuildException {
        normalizeSettings(false);
        // feature order output is purely local, so we're never using remotes
        validate();

        try {
            FeatureOrderAnalyst analyst = new FeatureOrderAnalyst(getFeatureSearchPath(), m_featureOrderOutput );
            analyst.generateFeatureOrderOutput();

        } catch (IOException e) {
            throw new BuildException(e);
        } catch (SAXException f) {
            throw new BuildException(f);
        }

    }

    public void setFeatureorderoutput(File featureOrderOutput) {
        m_featureOrderOutput = AnyItem.checkNullVariable(featureOrderOutput);
    }

    
    @Override
	protected void validate() {
		super.validate();
		if (m_featureOrderOutput == null) {
            throw new BuildException("featureorderoutput property must be specified.");
		}
	}


	private File m_featureOrderOutput;
}
