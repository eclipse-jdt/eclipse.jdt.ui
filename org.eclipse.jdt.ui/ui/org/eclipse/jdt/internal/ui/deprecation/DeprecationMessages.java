/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.deprecation;

import org.eclipse.osgi.util.NLS;

/**
 * Description
 */
public class DeprecationMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.deprecation.DeprecationMessages"; //$NON-NLS-1$

	private DeprecationMessages() {
	}

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DeprecationMessages.class);
	}

	public static String FixDeprecationRefactoringWizard_caption;

	public static String FixDeprecationRefactoringWizard_title;

	public static String FixDeprecationRefactoringWizard_description;

	public static String FixDeprecationRefactoringWizard_project_pattern;

	public static String FixDeprecationRefactoringWizard_workspace_caption;
}
