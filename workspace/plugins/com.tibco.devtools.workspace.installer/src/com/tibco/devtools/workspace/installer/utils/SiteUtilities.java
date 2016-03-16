package com.tibco.devtools.workspace.installer.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.PluginReference;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;
import com.tibco.devtools.workspace.util.DomUtilities;
import com.tibco.devtools.workspace.util.NoRemoveIterator;

/**
 * Utilities to help deal with update sites.
 *
 * <p>Note that the intent of this class is that it can be used both from within an Eclipse
 * environment, and outside of it, so this particular set of utilities should not have
 * Eclipse runtime dependencies.
 * </p>
 */
public class SiteUtilities {

	/**
	 * Iterate over all of the valid feature files in a directory.
	 * 
	 * @param dir	The directory to scan.
	 * 
	 * @return The iterator over the entries in the directory.
	 */
	public static Iterable<FeatureDescriptor> featuresInDir(File dir) {
		return new FeatureDescriptorIterator(dir);
	}
	

    /**
     * Rewrite site.xml to include all of the features now listed in the features folder.
     *
     * @param siteCache    The location of the site cache.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void rewriteSiteXml(File siteCache) throws FileNotFoundException, IOException {
    	
		String encoding = "UTF-8";
		File siteXmlDest = new File(siteCache, "site.xml");
		File siteXml = new File(siteCache, "site.xml.regen");
    	boolean success = false;
    	
        try {
			File siteFeatures = new File(siteCache, "features");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(siteXml), encoding));
			bw.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n<site>\n");

			File siteFeaturesList[] = siteFeatures.listFiles(sm_filterByName);
			for (File oneFeatureFile : siteFeaturesList) {
				FeatureDescriptor model = extractModelFromFeatureJar(oneFeatureFile);

				// now split before and after the underscore that separates the name from the vers id.
				StringBuilder featureLine = new StringBuilder("  ");
				featureLine.append("<feature url=\"features/");
				featureLine.append(oneFeatureFile.getName());
				featureLine.append("\" id=\"");
				Target<VersionInfo> target = model.getTarget();
				featureLine.append(target.getTargetId());
				featureLine.append("\" version=\"");
				featureLine.append(target.getVersion().toString());
				featureLine.append("\"/>\n");

				bw.write(featureLine.toString());
			}
			bw.write("</site>\n");
			bw.close();
			
			FileUtils.blindlyDelete(siteXmlDest);
			if (!siteXml.renameTo(siteXmlDest) ) {
				throw new IOException("Failed to rename " + siteXml.toString() + " to " + siteXmlDest.toString() );
			}
			success = true;
		} finally {
			// if we don't complete successfully, delete site.xml so that we don't fail the next time we run.
			if (!success) {
                FileUtils.blindlyDelete(siteXml);
			}
		}
    }
    
	/**
	 * Rewrite site.xml from XML document.
	 * 
	 * @param siteCache  The location of the site cache.
	 * 
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws IOException
	 */
    public static void rewriteSiteXml(File siteCache, Document doc) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, IOException {
    	rewriteSiteXml(siteCache, doc, "site.xml");
    }
    	   
    public static void rewriteSiteXml(File siteCache, Document doc, String fileName) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, IOException {
    	boolean success = false;
		File siteXmlDest = new File(siteCache, fileName);
		File siteXml = new File(siteCache, "site.xml.regen");
    	try {
			OutputStream out = new FileOutputStream(siteXml);
			DomUtilities.documentToStream(doc, out);
			out.close();
			FileUtils.blindlyDelete(siteXmlDest);
			if (!siteXml.renameTo(siteXmlDest) ) {
				throw new IOException("Failed to rename " + siteXml.toString() + " to " + siteXmlDest.toString() );
			}
		} finally {
			// if we don't complete successfully, delete site.xml so that we don't fail the next time we run.
			if (!success) {
				FileUtils.blindlyDelete(siteXml);
			}
		}
	}
	
    public static FeatureDescriptor extractModelFromFeatureJar(File featureFile) throws IOException {
    	ZipFile zf = new ZipFile(featureFile);
    	ZipEntry ze = zf.getEntry("feature.xml");
    	FeatureDescriptor featureModel = null;
    	
    	if (ze != null) {
        	InputStream inStream = zf.getInputStream(ze);
        	
    		try {
    			featureModel = FeatureDescriptor.fromStream(inStream);
    		} catch (RuntimeException e) {
    			System.out.println("Unable to parse feature.xml model for " + featureFile.getCanonicalPath());
    			throw new RuntimeException("Unable to parse feature.xml model for " + featureFile.getCanonicalPath(), e);
    		}
    		
		} else {
			System.out.println("Can't find feature.xml from " + featureFile.getCanonicalPath());
			throw new RuntimeException("Can't find feature.xml from" + featureFile.getCanonicalPath());
		}
    	zf.close();
    	
		if (featureModel == null) {
			throw new RuntimeException("Can't parse feature.xml from " + featureFile.getCanonicalPath());
		}
    	return featureModel;
	}

	public static FeatureDescriptor getFeatureDescriptorFromZipStream(InputStream is,
			URL featureUrl) throws IOException, MalformedURLException {
		
		// scan the zip file for the feature.xml entry.
		FeatureDescriptor descriptor = null;
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry ze = zis.getNextEntry();
		boolean keepScanning = true;
		while (keepScanning && ze != null) {
		    if (ze.getName().equals("feature.xml") ) {
		        descriptor = FeatureDescriptor.fromStream(zis);
	
		        URL featureDescUrl = new URL("jar:" + featureUrl.toString() + "!/feature.xml");
		        descriptor.setUrlLocation(featureDescUrl);
		        
		        keepScanning = false;
		    }
		    else {
		        // skip to the next entry.
		        zis.closeEntry();
		        ze = zis.getNextEntry();
		    }
		}
	
		if (descriptor == null) {
		    System.out.println("WARNING: Unable to find feature.xml in feature JAR at " + featureUrl.toString());
		}
	
		zis.close();
		return descriptor;
	}
	/**
     * Checks to see whether "name" parameter starts with any string in the given
     * name filter collection.
     *
     * <p>
     * The idea here is that you can specify the name of a feature, like "com.tibco.tpcl.org.hibernate"
     * that will match the file name portion of a feature.  Note that you should only specify
     * the name of the feature, and not include the underscore character.  That way,
     * we can scan for only particular plugins in an extension folder, and add them to the cache site.
     * </p>
     *
     * @param nameFilter A list of prefixes to test against "name".  May be <code>null</code> or empty
     *     which implies any name matches.
     *
     * @param name    The name to check about matching.
     *
     * @return <code>true</code> if the "name" matches.
     */
    public static boolean isDesiredItemName(Collection<String> nameFilter, String name) {

        if (nameFilter != null && nameFilter.size() > 0) {
            for (String matchStr : nameFilter) {
                if (name.startsWith(matchStr) && name.charAt(matchStr.length()) == '_') {
                    return true;
                }
            }

            return false;
        }
        return true;
    }

    /**
     * Caches entries from an extension location back into a local site.
     *
     * @param siteCache The site into which files will be cached.
     * @param extensionLoc  The extension location to cache.
     * @param featuresFilter    A list of specific features we're interested in caching.  May be null
     *  which implies all features.
     * @throws IOException 
     * @throws IOException
     * @throws SAXException If something goes wrong parsing feature.xml
     * @throws ParserConfigurationException 
     */
    public static void cacheFeaturesFromExtensionLoc(File siteCache, File extensionLoc, Collection<String> featuresFilter) throws IOException {

    	Set<String> pluginSet = new HashSet<String>();
    	
        //System.out.println("Caching from " + extensionLoc + "/{features,plugins} to " + siteCache + "/{features,plugins}");
        File siteFeatures = new File(siteCache, "features");
        File sitePlugins = new File(siteCache, "plugins");

        FileUtils.createDirectories( siteFeatures );
        FileUtils.createDirectories( sitePlugins );

        File extLocPlugins = new File(extensionLoc, "plugins");

        File extensionLocFeatures = new File(extensionLoc, "features");
        // loop through all of the features in the extension location.
        File features[] = extensionLocFeatures.listFiles();
        for (File extLocFeatureFile : features) {

            // does a feature exist in the local site cache?
            String featureFileName = extLocFeatureFile.getName() + ".jar";
            File siteTargetFeature = new File(siteFeatures, featureFileName);
            if ( !siteTargetFeature.exists()
                    && isDesiredItemName(featuresFilter, extLocFeatureFile.getName() )) {

            	if (featuresFilter != null) {
            		addFeaturePluginsToSet(extLocFeatureFile, pluginSet);
            	}
            	
                // nope, so create a ZIP file with the contents of the extension loc.
                FileUtils.zipFolderToFile(extLocFeatureFile, siteTargetFeature);

            }
        }

        // loop through the plugins, but in this case, we need to see whether
        // the plugin is a folder or a file.
        File plugins[] = extLocPlugins.listFiles();
        for (File extLocPluginFile : plugins) {

        	boolean needsToBeCached = true;
        	if (featuresFilter != null) {
        		String name = extLocPluginFile.getName();
        		if (name.endsWith(".jar")) {
        			name = name.substring(0, name.length() - 4);
        		}
        		needsToBeCached = pluginSet.contains(name);
        	}
            if ( needsToBeCached ) {
                if (extLocPluginFile.isFile() ) {
                    // it is a file, so see if a file of the same name exists in the site cache.
                    File siteTargetPlugin = new File(sitePlugins, extLocPluginFile.getName());
                    if ( !siteTargetPlugin.exists() ) {
                        FileUtils.copyFile(extLocPluginFile, siteTargetPlugin);
                    }
                }
                else {
                    String pluginFileName = extLocPluginFile.getName() + ".jar";
                    File siteTargetPlugin = new File(sitePlugins, pluginFileName);
                    if ( !siteTargetPlugin.exists() ) {
                        FileUtils.zipFolderToFile(extLocPluginFile, siteTargetPlugin);
                    }
                }
            }
        }

        rewriteSiteXml(siteCache);
    }
    
    /**
     * For a given feature folder, inspect the feature model, and list all of the plugins.
     * 
     * @param featureDir	The feature folder.
     * @param items	The set of plugin file names to look for.
     */
    private static void addFeaturePluginsToSet(File featureDir, Set<String> items) {
    	File featureFile = new File(featureDir, "feature.xml");
    	FeatureDescriptor model;
		try {
			model = featureModelFromFile(featureFile);
		} catch (Exception e) {
			// we catch this exception here assuming that parser configuration problems are
			// essentially a programming error.
			throw new RuntimeException(e);
		}
    	
    	for (PluginReference<VersionInfo> plugRef : model.getProvidedPlugins()) {
    		Target<VersionInfo> target = plugRef.getTarget();
			items.add(target.getTargetId() + "_" + target.getVersion().toString() );
    	}
    }
    
    /**
     * Extract a {@link FeatureDescriptor} from a file on disk.
     * 
     * @param featureFile	The file to parse.
     * 
     * @return	The model of the feature.
     */
    private static FeatureDescriptor featureModelFromFile(File featureFile) throws ParserConfigurationException, IOException, SAXException {
    	DocumentBuilder db = DomUtilities.getNamespaceAwareDocumentBuilder();
    	
    	Document dom = db.parse(featureFile);
    	FeatureDescriptor descriptor = FeatureDescriptor.fromDocument(dom);
    	descriptor.setFileLocation(featureFile);
    	return descriptor;
    }
    
	/**
	 * Quick filter to eliminate all of the non-feature files in a folder -
	 * at least based on file name.
	 */
	private static final FilenameFilter sm_filterByName = new FilenameFilter() {

		public boolean accept(File dir, String name) {
		    if (name.startsWith(".") || name.startsWith("_")) {
		        return false;
		    }

		    // assume, for the moment, that the file name ends in ".jar"
		    // adjustment, by amy, 12 April: *skip* it if it isn't a jar.
		    if (!name.endsWith(".jar")) {
		        return false;
		    }
			return true;
		}
		
	};

    private static class FeatureDescriptorIterator extends NoRemoveIterator<FeatureDescriptor> {
	
		public FeatureDescriptorIterator(File featuresDir) {
			m_possibles = featuresDir.listFiles(sm_filterByName);
			m_index = 0;
		}
		
		@Override
		public Iterator<FeatureDescriptor> iterator() {
			return this;
		}
	
		public boolean hasNext() {
			return m_index < m_possibles.length;
		}
	
		public FeatureDescriptor next() {
			try {
				return SiteUtilities.extractModelFromFeatureJar(m_possibles[m_index++]);
			} catch (IOException e) {
				throw new RuntimeException("Problem parsing JAR file.", e);
			}
		}
	
		private File[] m_possibles;
		private int m_index;
		
	}

}
