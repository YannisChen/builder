package com.tibco.devtools.workspace.installer.standalone;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.tibco.devtools.workspace.model.FeatureDescriptor;
import com.tibco.devtools.workspace.model.Target;
import com.tibco.devtools.workspace.model.VersionInfo;

/**
 * Represents a remote site - either an update site or a release unit site.
 * 
 * <p>The notion here is that clients will first retrieve the available features by
 * calling <code>getAvailableFeatures()</code>.  To determine any subsequent required
 * features, for each desired feature, the client can call <code>getFeatureModel</code>
 * and enumerate the dependencies.  And in turn, as those features require other features,
 * keep calling <code>getFeatureModel</code>.
 * </p>
 * 
 * <p>Having determined a full list of dependencies, a client can then call
 * <code>installTargetSet()</code> to install a particular set of features.
 * </p>
 *
 * @see FeatureDescriptor
 */
public interface SiteInformation extends FeatureSource<VersionInfo, FeatureDescriptor> {

	/**
	 * Get a feature model for a particular feature.
	 * 
	 * <p>Note that this method is expected to cache its results, so that subsequent
	 * invocations will not access the network again.</p>
	 * 
	 * @param target	The target feature version and ID to retrieve.
	 * 
	 * @return	An instance of a feature model.
	 */
	FeatureDescriptor getFeatureModel(Target<VersionInfo> target);

	/**
	 * Get the list of available features (by version number) on the remote site.
	 * 
	 * @return	The list of all of the available features on the site.
	 */
	Map<String, List< Target<VersionInfo> > > getAvailableFeatures();
	
	/**
	 * Install a set of versioned targets to the given destination directory.
	 * @param reporter Progress reported here.
	 * @param placer Controls where the files will be placed on the destination.
	 * @param toInstall	The set of feature targets to install.
	 * @param targetPlatform Which platform are we targeting with this install?
	 * @throws IOException If something goes wrong while accessing the sites...
	 */
	void installTargetSet(ProgressReport reporter, FilePlacer placer,
			Collection< Target<VersionInfo> > toInstall, TargetPlatformData targetPlatform) throws IOException;
}
