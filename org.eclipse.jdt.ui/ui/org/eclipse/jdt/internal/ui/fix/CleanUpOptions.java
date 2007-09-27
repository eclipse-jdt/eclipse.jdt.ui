/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

/**
 * Allows to retrieve clean up settings from given
 * options keys.
 * <p>
 * Client should not extend this class.
 * </p>
 * 
 * @since 3.4
 */
public class CleanUpOptions {
	
	private final Map fOptions;
	
	/**
	 * True value
	 * 
	 * @since 3.3
	 */
	public static final String TRUE= "true"; //$NON-NLS-1$
	
	/**
	 * False value
	 * 
	 * @since 3.3
	 */
	public static final String FALSE= "false"; //$NON-NLS-1$
	
	/**
	 * Create new CleanUpOptions instance. <code>options</code>
	 * maps named clean ups keys to {@link CleanUpOptions#TRUE}, 
	 * {@link CleanUpOptions#FALSE} or any String value
	 * 
	 * @param options map from String to String
	 */
	protected CleanUpOptions(Map options) {
		fOptions= options;
	}
	
	/**
	 * Create new CleanUpOptions instance.
	 */
	public CleanUpOptions() {
		fOptions= new Hashtable();
	}

	/**
	 * Is the option with the given <code>key</code> enabled?
	 * 
	 * @param key the name of the option
	 * @return true if enabled, false if not enabled or unknown key
	 * @see CleanUpConstants
	 */
	public boolean isEnabled(String key) {
		Assert.isNotNull(key);
		
		Object value= fOptions.get(key);
		return CleanUpOptions.TRUE == value || CleanUpOptions.TRUE.equals(value);
	}

	/**
	 * @param key to key of the value
	 * @return the value associated with the key, <b>null</b> if key is unknown
	 */
	public String getValue(String key) {
		Assert.isNotNull(key);
		return (String) fOptions.get(key);
	}

	/**
	 * @param key the name of the option to set
	 * @param value the value of the option
	 * 
	 * @see CleanUpOptions#TRUE
	 * @see CleanUpOptions#FALSE
	 */
	public void setOption(String key, String value) {
		Assert.isNotNull(key);
		Assert.isNotNull(value);
		
		fOptions.put(key, value);
	}
	
	/**
	 * @return an unmodifiable set of all keys
	 */
	public Set getKeys() {
		return Collections.unmodifiableSet(fOptions.keySet());
	}
}