/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that replaces <code>System.getProperty(xxx)</code> by Java methods:
 * <ul>
 * <li>Beware! The code doesn't any longer take the system property into account!</li>
 *
 * System.getProperties() can be overridden by calls to System.setProperty(String key, String value)
 * or with command line parameters -Dfile.separator=/
 *
 * File.separator gets the separator for the default filesystem.
 *
 * FileSystems.getDefault() gets you the default filesystem.
 * </ul>
 */
public class ConstantsForSystemPropertyCleanUp extends AbstractCleanUpCoreWrapper<ConstantsForSystemPropertiesCleanUpCore> {
	public ConstantsForSystemPropertyCleanUp(final Map<String, String> options) {
		super(options, new ConstantsForSystemPropertiesCleanUpCore());
	}

	public ConstantsForSystemPropertyCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
