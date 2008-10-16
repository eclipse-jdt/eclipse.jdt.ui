/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

public class MapCleanUpOptions extends CleanUpOptions {

	private final Map fOptions;

	/**
	 * Create new CleanUpOptions instance. <code>options</code>
	 * maps named clean ups keys to {@link CleanUpOptions#TRUE},
	 * {@link CleanUpOptions#FALSE} or any String value
	 *
	 * @param options map from String to String
	 * @see CleanUpConstants
	 */
	public MapCleanUpOptions(Map options) {
		super(options);
		fOptions= options;
	}

	public MapCleanUpOptions() {
		this(new Hashtable());
	}

	/**
	 * @return all options as map, modifying the map modifies this object
	 */
	public Map getMap() {
		return fOptions;
	}

	/**
	 * @param options the options to add to this options
	 */
	public void addAll(CleanUpOptions options) {
		if (options instanceof MapCleanUpOptions) {
			fOptions.putAll(((MapCleanUpOptions)options).getMap());
		} else {
			Set keys= options.getKeys();
			for (Iterator iterator= keys.iterator(); iterator.hasNext();) {
				String key= (String) iterator.next();
				fOptions.put(key, options.getValue(key));
			}
		}
	}

}
