package com.tibco.devtools.workspace.installer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.installer.utils.FeatureUtils;
import com.tibco.devtools.workspace.model.FeatureDescriptor;

public class FeatureOrderAnalyst {

    public FeatureOrderAnalyst(List<File> searchPath, File outputFile) {
        m_searchPath = searchPath;
        m_outputFile = outputFile;
    }

    public void generateFeatureOrderOutput() throws IOException, SAXException {
        FileOutputStream fos = new FileOutputStream(m_outputFile);
        Writer fw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<featureOrder>\n");

        StringBuffer featureLine = new StringBuffer();
        List<FeatureDescriptor> features = FeatureUtils.sortFeaturesByDependency(FeatureUtils.getBuildFeatures(m_searchPath));
        File currentReleaseUnit = null;
        File featureReleaseUnit = null;
        boolean opened = false;
        for (FeatureDescriptor oneFeature : features) {

            featureLine.delete(0, featureLine.length() );

            // useful invariant: feature.xml is found inside the feature directory,
            // which is inside the features directory, which is inside the release
            // unit directory.
            featureReleaseUnit = oneFeature.getFileLocation().getParentFile().getParentFile().getParentFile();
            if (!featureReleaseUnit.equals(currentReleaseUnit)) {
                if (opened) {
                    featureLine.append("  </release-unit>\n");
                }
                featureLine.append("  <release-unit location=\"");
                featureLine.append(featureReleaseUnit);
                featureLine.append("\">\n");
                opened = true;
            }
            String featureId = oneFeature.getTarget().getTargetId();

            featureLine.append("    <feature id=\"");
            featureLine.append(featureId);
            featureLine.append("\"/>\n");

            String strLine = featureLine.toString();
            bw.write(strLine);

            currentReleaseUnit = featureReleaseUnit;
        }
        if (opened)
            bw.write("  </release-unit>\n");
        bw.write("</featureOrder>\n");

        bw.close();
    }

    private File		m_outputFile;
    
    private List<File>	m_searchPath;
    
}
