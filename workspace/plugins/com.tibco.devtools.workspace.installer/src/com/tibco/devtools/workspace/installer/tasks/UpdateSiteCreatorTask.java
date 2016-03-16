package com.tibco.devtools.workspace.installer.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tibco.devtools.workspace.installer.standalone.UpdateSiteUtils;
import com.tibco.devtools.workspace.installer.utils.SiteUtilities;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.FeatureLine;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

public class UpdateSiteCreatorTask
    extends Task
{

    public void setSiteDirectory(File site)
    {
        m_site = checkNullFile(site);
    }

    public void setRemoveList(File removeList) {
		this.m_removeList = removeList;
	}
    
	public void setFastUpdateSiteXml(boolean fastUpdateSiteXml) {
		this.m_fastUpdateSiteXml = fastUpdateSiteXml;
	}

	@Override
	public void execute() throws BuildException {
		validate();
		Set<FeatureLine> toRemove = new HashSet<FeatureLine>();
		Set<FeatureLine> toAdd = new HashSet<FeatureLine>();
		try {
			Document doc = null;
			File target = new File(m_site, "site.xml");
			if (m_fastUpdateSiteXml && (target.exists() && (doc = UpdateSiteUtils.parseSiteDocument(new FileInputStream(target), "rewrite site.xml")) != null)) {
				analyzeChangedFeatures(doc, toRemove, toAdd);
				// must call method removeFeatures() before calling method addFeatures()
				removeFeatures(doc, toRemove);
				addFeatures(doc, toAdd);
				SiteUtilities.rewriteSiteXml(m_site, doc);
			} else {
				SiteUtilities.rewriteSiteXml(m_site);
			}
			
			if (m_removeList != null && m_removeList.exists()) {
				doc = UpdateSiteUtils.parseSiteDocument(new FileInputStream(target), "rewrite site.xml");
				generateSiteXmlWithoutRemoveListItems(doc);
			}
		} catch (Exception e) {
			throw new BuildException(e.getMessage() 
									+ " UpdateSiteCreatorTask arguments: 'm_site'=" + m_site 
									+ ",'m_removeList'=" + m_removeList 
									+ ",'m_fastUpdateSiteXml'=" + m_fastUpdateSiteXml, e);
		}
	}
    
	private void analyzeChangedFeatures(Document doc, final Set<FeatureLine> toRemove, final Set<FeatureLine> toAdd) throws FileNotFoundException, IOException {
		for (File jar : new File(m_site, "features").listFiles()) {
			toAdd.add(new FeatureLine(jar.getName()));
		}
		final String baseUrl = m_site.toURI().toURL().toString();
		UpdateSiteUtils.SiteFeaturesHandler handler = new UpdateSiteUtils.SiteFeaturesHandler() {
			public void processFeature(Target<VersionInfo> target, URL url) {
				String oUrl = url.toString().replace(baseUrl, "");
				FeatureLine eachLine = new FeatureLine(target, oUrl);
				if(toAdd.contains(eachLine)){
					// all the new features will be left in toAdd list.
					toAdd.remove(eachLine);
				}else{
					// all the to remove features will be in toRemove list.
					toRemove.add(eachLine);
				}
			}
		};
		UpdateSiteUtils.parseSiteXml(doc, m_site.toURI().toURL(), handler);
	}
	
	private void removeFeatures(Document doc, Set<FeatureLine> featureLines){
		Element site = doc.getDocumentElement();
		for(FeatureLine toRemoveLine : featureLines){
			NodeList nodes = site.getChildNodes();
			for(int i = 0; i < nodes.getLength(); i++){
				Node eachNode = nodes.item(i);
				if(eachNode.getNodeType() != Node.TEXT_NODE){
					NamedNodeMap atts = eachNode.getAttributes();
					int flag = 0; // 0000
					for(int j = 0; j < atts.getLength(); j++){
						Attr attr = (Attr)atts.item(j);
						String name = attr.getName();
						String value = attr.getValue();
						if("id".equals(name)){
							if(toRemoveLine.target.getTargetId().equals(value)){
								flag = flag | 1; // 0001
							}
						}else if("version".equals(name)) {
							if(toRemoveLine.target.getVersion().toString().equals(value)){
								flag = flag | 2; // 0010
							}
						}
					}
					// 0011 there are two flags
					if(flag == 3){
						site.removeChild(eachNode);
						break;
					}
				}
			}
		}
	}
	
	private void addFeatures(Document doc, Set<FeatureLine> featureLines) throws IOException{
		Element site = doc.getDocumentElement();
		for(FeatureLine each : featureLines){
			FeatureDescriptor model = SiteUtilities.extractModelFromFeatureJar(new File(m_site, each.url));
			
			Element element = doc.createElement("feature");
			element.setAttribute("url", each.url);
			element.setAttribute("id", model.getTarget().getTargetId());
			element.setAttribute("version", model.getTarget().getVersion().toString());
			site.appendChild(element);
		}
	}
	
	private void generateSiteXmlWithoutRemoveListItems(Document doc){
		Set<FeatureLine> toRemove = new HashSet<FeatureLine>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(m_removeList));
			String line;
			while ((line = br.readLine()) != null) {
				if("features".equals(new File(line).getParentFile().getName())){
					Target<VersionInfo> target = SiteUtilities.extractModelFromFeatureJar(new File(line)).getTarget();
					toRemove.add(new FeatureLine(target));
				}
			}
			removeFeatures(doc, toRemove);
			SiteUtilities.rewriteSiteXml(m_site, doc, "site-xml-without-features-in-remove-list.xml");
		} catch (Exception e) {
			throw new BuildException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new BuildException("Failed to parse " + m_removeList.getAbsolutePath(), e);
				}
			}
		}
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
        File features = new File(m_site, "features");
        File plugins = new File(m_site, "plugins");
        if (!features.exists() || !plugins.exists() || !features.isDirectory() || !plugins.isDirectory())
            throw new BuildException("The specified site: " + m_site + " does not contain the directories 'features' and 'plugins'.");
    }

    private File checkNullFile(File file)
    {
        return AnyItem.checkNullVariable(file);
    }

    private File m_site;
    private File m_removeList;
    private boolean m_fastUpdateSiteXml;
}
