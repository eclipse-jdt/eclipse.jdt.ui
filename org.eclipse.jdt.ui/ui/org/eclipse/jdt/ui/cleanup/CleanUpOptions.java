/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - modified to supply logic through internal class
 *******************************************************************************/
package org.eclipse.jdt.ui.cleanup;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;


/**
 * Allows to set and retrieve clean up settings for given options keys.
 * 
 * @since 3.5
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CleanUpOptions {
	
	// Use internal class to supply logic
	private final org.eclipse.jdt.internal.corext.fix.CleanUpOptions fOptions;

	/**
	 * True value
	 */
	public static final String TRUE= "true"; //$NON-NLS-1$

	/**
	 * False value
	 */
	public static final String FALSE= "false"; //$NON-NLS-1$

	/**
	 * Creates a new CleanUpOptions instance with the given options.
	 * 
	 * @param options map that maps clean ups keys (<code>String</code>) to a non-<code>null</code>
	 *            string value
	 */
	protected CleanUpOptions(Map<String, String> options) {
		fOptions= new org.eclipse.jdt.internal.corext.fix.CleanUpOptions(options);
	}

	/**
	 * Creates a new instance.
	 */
	public CleanUpOptions() {
		fOptions= new org.eclipse.jdt.internal.corext.fix.CleanUpOptions();
	}

	/**
	 * Tells whether the option with the given <code>key</code> is enabled.
	 * 
	 * @param key the name of the option
	 * @return <code>true</code> if enabled, <code>false</code> if not enabled or unknown key
	 * @throws IllegalArgumentException if the key is <code>null</code>
	 * @see CleanUpConstants
	 */
	public boolean isEnabled(String key) {
		return fOptions.isEnabled(key);
	}

	/**
	 * Returns the value for the given key.
	 * 
	 * @param key the key of the value
	 * @return the value associated with the key
	 * @throws IllegalArgumentException if the key is null or unknown
	 */
	public String getValue(String key) {
		return fOptions.getValue(key);
	}

	/**
	 * Sets the option for the given key to the given value.
	 * 
	 * @param key the name of the option to set
	 * @param value the value of the option
	 * @throws IllegalArgumentException if the key is <code>null</code>
	 * @see CleanUpOptions#TRUE
	 * @see CleanUpOptions#FALSE
	 */
	public void setOption(String key, String value) {
		fOptions.setOption(key, value);
	}

	/**
	 * Returns an unmodifiable set of all known keys.
	 * 
	 * @return an unmodifiable set of all keys
	 */
	public Set<String> getKeys() {
		return fOptions.getKeys();
	}
}
