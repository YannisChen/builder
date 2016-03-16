package com.tibco.devtools.workspace.installer.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.tibco.devtools.workspace.installer.utils.SiteUtilities;
import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

class SiteFeatureInfo {

	/**
	 * Clients must provide an implementation of this method to read the contents of a URL.
	 */
	public static interface FetchUrl {
		
		InputStream openStream(URL url) throws IOException;
		
	}
	
    public SiteFeatureInfo(FetchUrl retriever, Target<VersionInfo> target, URL siteUrl, URL url) {
    	m_reader = retriever;
        m_target = target;
        m_siteUrl = siteUrl;
        m_url = url;
    }

    public Target<VersionInfo> getTarget() {
        return m_target;
    }

    public URL getSiteUrl() {
        return m_siteUrl;
    }

    public void setSiteUrl(URL newUrl) {
    	m_siteUrl = newUrl;
    }
    
    public URL getUrl() {
    	return m_url;
    }
    
    public void setUrl(URL newUrl) {
    	m_url = newUrl;
    }
    /**
     * Get plugin's url from feature's url
     * Note sometime m_siteUrl and m_url are not same host, or are not general directory structure.
     **/
    public URL getPluginUrl(PluginReference<VersionInfo> pr) throws MalformedURLException {
        Target<VersionInfo> target = pr.getTarget();
		String pluginName = target.getTargetId() + "_" + target.getVersion().toString(true) + ".jar";
        String srcPath = "plugins/" + pluginName;

        // now go get the stream.
        String headOfRightURL = m_url.toString().split("features/" + m_target.getTargetId())[0];
        return new URL(headOfRightURL + srcPath);
    }
    
    public FeatureDescriptor getModel() {

        if (m_descriptor == null) {
            try {
                // Get the model contents
                InputStream contents = getFeatureContents();
                FeatureDescriptor descriptor = SiteUtilities.getFeatureDescriptorFromZipStream(contents, m_url);
                
                m_descriptor = descriptor;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return m_descriptor;
    }

	public InputStream getFeatureContents() {
		
        try {
            return m_reader.openStream(m_url);
        } catch (IOException e) {
            // capture and rethrow here, for the moment don't expect clients
            // to deal with this error.
            throw new RuntimeException(e);
        }

    }

    private Target<VersionInfo> m_target;

    /**
     * The URL for the site that this came from.
     */
    private URL m_siteUrl;

    /**
     * The URL for the feature JAR.
     */
    private URL m_url;

    private FeatureDescriptor    m_descriptor;

    private FetchUrl m_reader;
}
