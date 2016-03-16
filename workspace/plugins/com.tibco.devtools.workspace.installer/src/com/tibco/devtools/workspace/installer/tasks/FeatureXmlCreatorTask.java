package com.tibco.devtools.workspace.installer.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.util.DomUtilities;

public class FeatureXmlCreatorTask
    extends Task
{

        public void setSiteDirectory(File site)
        {
            m_site = checkNullProperty(site);
        }

        public void setFilename(String filename)
        {
            m_filename = checkNullProperty(filename);
        }

        public void setFeatureId(String featureId)
        {
            m_featureId = checkNullProperty(featureId);
        }

        public void setFeatureVersion(String featureVersion)
        {
            m_featureVersion = checkNullProperty(featureVersion);
        }

        public void setFeatureLabel(String featureLabel)
        {
            m_featureLabel = checkNullProperty(featureLabel);
        }

        @Override
        public void execute()
            throws BuildException
        {
            validate();
            generateFeatureXml();
        }

        private void validate()
            throws BuildException
        {
            if (m_site == null)
                throw new BuildException("The sitedirectory attribute must be specified.");
            if (!m_site.exists())
                throw new BuildException("The specified site: " + m_site + " does not exist.");
            if (!m_site.isDirectory())
                throw new BuildException("The specified site: " + m_site + " is not a directory.");
            if (m_filename == null)
                throw new BuildException("The filename attribute must be specified.");
            File features = new File(m_site, "features");
            File plugins = new File(m_site, "plugins");
            if (!features.exists() || !plugins.exists() || !features.isDirectory() || !plugins.isDirectory())
                throw new BuildException("The specified site: " + m_site + " does not contain the directories 'features' and 'plugins'.");
            for (File f : features.listFiles())
            {
                if (f.isDirectory() && !f.isHidden())
                {
                    try
                    {
                        m_candidates.add(f.getCanonicalFile());
                    }
                    catch (IOException ioe)
                    {
                        // if it's weird, ignore it.  or log the error.  but don't fail.
                    }
                }
            }
        }

        private <T>T checkNullProperty(T prop)
        {
            return AnyItem.checkNullVariable(prop);
        }

        private void generateFeatureXml()
            throws BuildException
        {
            try
            {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(m_site, m_filename)), UTF8));
                writer.println("<?xml version='1.0' encoding='" + UTF8 + "'?>");
                writer.println("<feature id=\"" + m_featureId + "\"");
                writer.println("         version=\"" + m_featureVersion + "\"");
                writer.println("         label=\"" + m_featureLabel + "\">");

                writer.println("    <requires>");

                for (File feature : m_candidates)
                    generateFeatureImport(writer, feature);

                writer.println("    </requires>");

                writer.println("</feature>");
                writer.flush();
                writer.close();
            }
            catch (IOException ioe)
            {
                throw new BuildException(ioe);
            }
        }

        private void generateFeatureImport(PrintWriter writer, File feature)
        {
            try
            {
                File featureXml = new File(feature, FEATURE_XML);
                DocumentBuilder builder = DomUtilities.getNamespaceAwareDocumentBuilder();
                Element doc = builder.parse(featureXml).getDocumentElement();
                Attr attr = doc.getAttributeNode(ID);

                String id = (attr != null) ? attr.getValue() : null;

                attr = doc.getAttributeNode(VERSION);
                String version = (attr != null) ? attr.getValue() : null;

                if ( (id != null) && (version != null) )
                {
                    writer.println("        <import feature=\"" + id + "\"");
                    writer.println("                version=\"" + version + "\"");
                    writer.println("                match=\"perfect\" />");
                }
            }
            catch (IOException e) {
                // if it's messed up, ignore it.
            } catch (SAXException e) {
                // if it's messed up, ignore it.
            }
        }

        // required
        private File m_site;
        private String m_filename;

        private List<File> m_candidates = new ArrayList<File>();

        // optional
        private String m_featureId = "com.tibco.devtools.dependencies.feature";
        private String m_featureVersion = "0.0.0";
        private String m_featureLabel = "Auto-generated Dependencies Feature";

        private static final String UTF8 = "UTF-8";
        private static final String FEATURE = "feature";
        private static final String FEATURE_XML = FEATURE + ".xml";
        private static final String ID = "id";
        private static final String VERSION = "version";
}
