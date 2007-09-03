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

import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

/**
 * Allows to retrieve clean up settings from given
 * options keys.
 * <p>
 * Client should not extend this class.
 * </p>
 * 
 * @see CleanUpConstants
 * @since 3.4
 */
public class CleanUpOptions {
	
	private final Map fOptions;
	
	/**
	 * Create new CleanUpOptions instance. <code>options</code>
	 * maps named clean ups keys to {@link CleanUpConstants#TRUE} or
	 * {@link CleanUpConstants#FALSE}
	 * 
	 * @param options map from String to String
	 * @see CleanUpConstants
	 */
	public CleanUpOptions(Map options) {
		fOptions= options;
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
		return CleanUpConstants.TRUE == value || CleanUpConstants.TRUE.equals(value);
	}
}