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
 *     Microsoft Corporation - based this file on Corext
 *******************************************************************************/
package org.eclipse.jdt.internal.corext;

import org.eclipse.jdt.core.manipulation.JavaManipulation;


/**
 * Facade for JavaManipulationPlugin to not contaminate corext classes.
 */
public class CorextCore {

	public static String getPluginId() {
		return JavaManipulation.ID_PLUGIN;
	}

	private CorextCore() {
	}
}
