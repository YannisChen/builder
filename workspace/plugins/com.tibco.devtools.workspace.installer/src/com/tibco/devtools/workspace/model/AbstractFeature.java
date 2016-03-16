package com.tibco.devtools.workspace.model;

import java.util.Collection;
import java.util.List;

import static com.tibco.devtools.workspace.util.DataUtils.newList;

/**
 * Abstract base class for feature implementation.
 *
 * @param <V>
 * @param <F>
 */
public abstract class AbstractFeature<V extends Comparable<V>, F extends Feature<V, F>>
 implements Feature<V, F> {

	/**
	 * Construct an abstract feature....
	 * 
	 * @param target
	 */
	public AbstractFeature(Target<V> target) {
		m_target = target;
	}
	
	// ========================================================================
	//	Feature methods.
	// ========================================================================
	
	
	/**
	 * Get the constraints on other features.
	 */
	public List<TargetConstraint<V, F>> getFeatureConstraints() {
		return m_featureConstraints;
	}

	public String getSourceLogicalId() {
		return m_target.getTargetId();
	}

	public Collection<TargetConstraint<V, F>> getPluginConstraints() {
		return m_pluginConstraints;
	}

	public List<PluginReference<V>> getProvidedPlugins() {
		return m_pluginRefs;
	}

	public Target<V> getTarget() {
		return m_target;
	}

	// ========================================================================
	//	Private data
	// ========================================================================
	
	// Feature id and version model
	private Target<V> m_target;
	// Imported features in feature.xml
	private List< TargetConstraint<V, F > > m_featureConstraints = newList();
	// Imported plugins in feature.xml
	private List< TargetConstraint<V, F > > m_pluginConstraints = newList();
	// Provided plugins in feature.xml
	private List<PluginReference<V>> m_pluginRefs = newList();
	
}
