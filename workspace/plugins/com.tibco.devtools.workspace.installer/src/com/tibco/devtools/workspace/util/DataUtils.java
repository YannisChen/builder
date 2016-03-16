package com.tibco.devtools.workspace.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Miscellaneous utilities for dealing with data.
 */
public class DataUtils {
	
	/**
     * For a map that collects a list for each value, this method will construct a list
     * for a given key, if the given map entry doesn't exist.
     * @param <K>
     * @param <E>
     * @param map
     * @param key
     * @return
     */
    public static <K, E> List<E> getMapListValue(Map<K, List<E> > map, K key) {
    	List<E> result = map.get(key);
    	if (result == null) {
    		result = new ArrayList<E>();
    		map.put(key, result);
    	}
    	
    	return result;
    }
    
	/**
     * For a map that collects a list for each value, this method will construct a list
     * for a given key, if the given map entry doesn't exist.
     * @param <K>
     * @param <E>
     * @param map
     * @param key
     * @return
     */
    public static <K, E> Set<E> getMapSetValue(Map<K, Set<E> > map, K key) {
    	Set<E> result = map.get(key);
    	if (result == null) {
    		result = new HashSet<E>();
    		map.put(key, result);
    	}
    	
    	return result;
    }
    
    /**
     * Simple utility function to eliminate the amount you have to type to create a new
     * HashMap<K,V>().
     * 
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> newMap() {
    	return new HashMap<K, V>();
    }
    
    /**
     * Exists to reduce the amount of typing needed to create a new list.
     * 
     * <p>Should no longer be necessary in Java 1.7.</p>
     * @param <E>
     * @return
     */
    public static <E> List<E> newList() {
    	return new ArrayList<E>();
    }
    
}
