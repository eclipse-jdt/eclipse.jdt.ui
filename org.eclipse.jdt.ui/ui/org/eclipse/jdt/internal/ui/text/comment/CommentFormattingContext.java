/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.formatter.FormattingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.text.comment.CommentFormatterPreferenceConstants;

/**
 * Formatting context for the comment formatter.
 * 
 * @since 3.0
 */
public class CommentFormattingContext extends FormattingContext {
	
	/**
	 * A Map that translates keys.
	 * 
	 * @since 3.1
	 */
	private static class KeyMappingMap implements Map {
		
		/** Mapping visible keys to actual keys */
		private Map fKeyMap;
		
		/** Backing Map */
		private Map fBackingMap;
		
		/**
		 * Initialize with the given mapping for keys and backing store.
		 * 
		 * @param keyMap the mapping for keys
		 * @param backingMap the backing store
		 */
		public KeyMappingMap(Map keyMap, Map backingMap) {
			fKeyMap= keyMap;
			fBackingMap= backingMap;
		}

		/**
		 * Map the given key according to the key map.
		 * 
		 * @param key the key
		 * @return the mapped key or the original if it is not mapped
		 */
		private Object map(Object key) {
			if (fKeyMap.containsKey(key))
				return fKeyMap.get(key);
			return key;
		}

		/*
		 * @see java.util.Map#size()
		 */
		public int size() {
			return fBackingMap.size();
		}

		/*
		 * @see java.util.Map#clear()
		 */
		public void clear() {
			fBackingMap.clear();
		}

		/*
		 * @see java.util.Map#isEmpty()
		 */
		public boolean isEmpty() {
			return fBackingMap.isEmpty();
		}

		/*
		 * @see java.util.Map#containsKey(java.lang.Object)
		 */
		public boolean containsKey(Object key) {
			return fBackingMap.containsKey(map(key));
		}

		/*
		 * @see java.util.Map#containsValue(java.lang.Object)
		 */
		public boolean containsValue(Object value) {
			return fBackingMap.containsValue(value);
		}

		/*
		 * @see java.util.Map#values()
		 */
		public Collection values() {
			return fBackingMap.values();
		}

		/*
		 * @see java.util.Map#putAll(java.util.Map)
		 */
		public void putAll(Map t) {
			throw new UnsupportedOperationException();
		}

		/*
		 * @see java.util.Map#entrySet()
		 */
		public Set entrySet() {
			throw new UnsupportedOperationException();
		}

		/*
		 * @see java.util.Map#keySet()
		 */
		public Set keySet() {
			throw new UnsupportedOperationException();
		}

		/*
		 * @see java.util.Map#get(java.lang.Object)
		 */
		public Object get(Object key) {
			return fBackingMap.get(map(key));
		}

		/*
		 * @see java.util.Map#remove(java.lang.Object)
		 */
		public Object remove(Object key) {
			return fBackingMap.remove(map(key));
		}

		/*
		 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
		 */
		public Object put(Object key, Object value) {
			return fBackingMap.put(map(key), value);
		}
	}
	
	/**
	 * Preference keys of this context's preferences.
	 * @since 3.1
	 */
	private static final String[] PREFERENCE_KEYS= new String[] { 
		    PreferenceConstants.FORMATTER_COMMENT_FORMAT, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE, 
		    PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, 
		    PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS, 
		    PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER, 
		    PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS, 
		    PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 
		    PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATHTML };
	
	/**
	 * Mapped preference keys of this context's preferences.
	 * @since 3.1
	 */
	private static final String[] MAPPED_PREFERENCE_KEYS= new String[] { 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMAT, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATHTML };

	/**
	 * Map from JDT/UI preference keys to JDT/Core preference keys.
	 * @since 3.1
	 */
	private static Map fPreferenceConstantsMap;

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#getPreferenceKeys()
	 */
	public String[] getPreferenceKeys() {
		return PREFERENCE_KEYS;
	}

	
	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#isBooleanPreference(java.lang.String)
	 */
	public boolean isBooleanPreference(String key) {
		return !key.equals(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH);
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#isIntegerPreference(java.lang.String)
	 */
	public boolean isIntegerPreference(String key) {
		return key.equals(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH);
	}

	/**
	 * Map from JDT/Text preference keys to JDT/Core preference keys.
	 * <p>
	 * TODO: remove after migrating comment formatter preferences to
	 * JDT/Core preference store
	 * </p><p>
	 * NOTE: the returned Map might not support <code>entrySet()</code>,
	 * <code>keySet()</code> and <code>putAll(Map)</code>
	 * </p>
	 * 
	 * @param preferences the JDT/Text preferences
	 * @return the JDT/Core preferences
	 * @since 3.1
	 */
	public static Map mapOptions(Map preferences) {
		Map preferenceConstantsMap= getPreferenceConstantsMap();
		if (preferenceConstantsMap.size() == 0)
			return preferences;
		return new KeyMappingMap(preferenceConstantsMap, preferences);
	}


	/**
	 * Returns a Map between JDT/UI and JDT/Core preference keys.
	 * 
	 * @return the Map between JDT/UI and JDT/Core preference keys
	 * @since 3.1
	 */
	private static Map getPreferenceConstantsMap() {
		if (fPreferenceConstantsMap == null) {
			String[] keys= PREFERENCE_KEYS;
			String[] mapedKeys= MAPPED_PREFERENCE_KEYS;
			fPreferenceConstantsMap= new HashMap(keys.length);
			for (int i= 0, n= keys.length; i < n; i++)
				if (!mapedKeys[i].equals(keys[i])) {
					fPreferenceConstantsMap.put(mapedKeys[i], keys[i]);
					fPreferenceConstantsMap.put(keys[i], mapedKeys[i]);
				}
		}
		return fPreferenceConstantsMap;
	}
}
