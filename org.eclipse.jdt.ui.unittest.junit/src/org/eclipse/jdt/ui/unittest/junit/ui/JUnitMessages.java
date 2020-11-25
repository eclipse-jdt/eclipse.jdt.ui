/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.ui;

import org.eclipse.osgi.util.NLS;

public final class JUnitMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.jdt.ui.unittest.junit.ui.JUnitMessages";//$NON-NLS-1$

	public static String JUnitCantRunMultipleTests;

	public static String OpenEditorAction_action_label;
	public static String OpenEditorAction_error_cannotopen_message;
	public static String OpenEditorAction_error_cannotopen_title;
	public static String OpenEditorAction_error_dialog_message;
	public static String OpenEditorAction_error_dialog_title;

	public static String OpenTestAction_error_methodNoFound;
	public static String OpenTestAction_dialog_title;
	public static String OpenTestAction_select_element;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JUnitMessages.class);
	}

	private JUnitMessages() {
		// Do not instantiate
	}
}
