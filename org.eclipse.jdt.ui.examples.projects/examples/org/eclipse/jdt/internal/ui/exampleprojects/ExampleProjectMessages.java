/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.exampleprojects;

import org.eclipse.osgi.util.NLS;

public final class ExampleProjectMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.exampleprojects.ExampleProjectMessages";//$NON-NLS-1$

	private ExampleProjectMessages() {
		// Do not instantiate
	}

	public static String ExampleProjectCreationWizard_title;
	public static String ExampleProjectCreationWizard_op_error_title;
	public static String ExampleProjectCreationWizard_op_error_message;
	public static String ExampleProjectCreationWizard_overwritequery_title;
	public static String ExampleProjectCreationWizard_overwritequery_message;
	public static String ExampleProjectCreationOperation_op_desc;
	public static String ExampleProjectCreationOperation_op_desc_proj;
	public static String ExampleProjectCreationWizardPage_error_alreadyexists;

	static {
		NLS.initializeMessages(BUNDLE_NAME, ExampleProjectMessages.class);
	}
}