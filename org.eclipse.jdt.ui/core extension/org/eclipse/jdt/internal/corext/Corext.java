/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Facade for JavaPlugin to not contaminate corext classes.
 */
public class Corext {

	public static String getPluginId() {
		return JavaPlugin.getPluginId();
	}

	private Corext() {
	}
}
