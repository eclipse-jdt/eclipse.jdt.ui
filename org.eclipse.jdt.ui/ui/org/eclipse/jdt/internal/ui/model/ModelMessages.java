/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.osgi.util.NLS;

public class ModelMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.model.ModelMessages"; //$NON-NLS-1$

	public static String JavaModelLabelProvider_project_preferences_label;

	public static String JavaModelLabelProvider_refactorings_label;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ModelMessages.class);
	}

	private ModelMessages() {
	}
}
