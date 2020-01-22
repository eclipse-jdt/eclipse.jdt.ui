/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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

import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;


/**
 * Allows to set and retrieve clean up settings for given options keys.
 *
 * @since 3.5
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CleanUpOptions extends CleanUpOptionsCore {

	/**
	 * True value
	 */
	@SuppressWarnings("hiding")
	public static final String TRUE= CleanUpOptionsCore.TRUE;

	/**
	 * False value
	 */
	@SuppressWarnings("hiding")
	public static final String FALSE= CleanUpOptionsCore.FALSE;

	/**
	 * Creates a new CleanUpOptions instance with the given options.
	 *
	 * @param options map that maps clean ups keys (<code>String</code>) to a non-<code>null</code>
	 *            string value
	 */
	protected CleanUpOptions(Map<String, String> options) {
		super(options);
	}

	/**
	 * Creates a new instance.
	 */
	public CleanUpOptions() {
		super();
	}

	@Override
	public Set<String> getKeys() {
		return super.getKeys();
	}

	@Override
	public void setOption(String key, String value) {
		super.setOption(key, value);
	}

	@Override
	public String getValue(String key) {
		return super.getValue(key);
	}

	@Override
	public boolean isEnabled(String key) {
		return super.isEnabled(key);
	}

}
