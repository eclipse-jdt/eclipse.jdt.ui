/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - modified and renamed for use in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

public class MapCleanUpOptionsCore extends CleanUpOptionsCore {

	private final Map<String, String> fOptions;

	/**
	 * Create new CleanUpOptions instance. <code>options</code>
	 * maps named clean ups keys to {@link CleanUpOptionsCore#TRUE},
	 * {@link CleanUpOptionsCore#FALSE} or any String value
	 *
	 * @param options map from String to String
	 * @see CleanUpConstants
	 */
	public MapCleanUpOptionsCore(Map<String, String> options) {
		super(options);
		fOptions= options;
	}

	public MapCleanUpOptionsCore() {
		this(new Hashtable<String, String>());
	}

	/**
	 * @return all options as map, modifying the map modifies this object
	 */
	public Map<String, String> getMap() {
		return fOptions;
	}

	/**
	 * @param options the options to add to this options
	 */
	public void addAll(CleanUpOptionsCore options) {
		if (options instanceof MapCleanUpOptionsCore) {
			fOptions.putAll(((MapCleanUpOptionsCore)options).getMap());
		} else {
			for (String key : options.getKeys()) {
				fOptions.put(key, options.getValue(key));
			}
		}
	}

}
