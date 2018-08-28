/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class DialogsMessages extends NLS {
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.dialogs.DialogsMessages"; //$NON-NLS-1$
	private DialogsMessages() {
	}
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DialogsMessages.class);
	}
	public static String SortMembersMessageDialog_dialog_title;
	public static String SortMembersMessageDialog_description;
	public static String SortMembersMessageDialog_link_tooltip;
	public static String SortMembersMessageDialog_do_not_sort_fields_label;
	public static String SortMembersMessageDialog_sort_all_label;
	public static String SortMembersMessageDialog_sort_warning_label;
}
