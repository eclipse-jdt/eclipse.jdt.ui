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
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public final class JavaBrowsingMessages extends NLS {

	private static final String BUNDLE_NAME= JavaBrowsingMessages.class.getName();

	private JavaBrowsingMessages() {
		// Do not instantiate
	}

	public static String JavaBrowsingPart_toolTip;
	public static String JavaBrowsingPart_toolTip2;
	public static String LexicalSortingAction_label;
	public static String LexicalSortingAction_tooltip;
	public static String LexicalSortingAction_description;
	public static String PackagesView_flatLayoutAction_label;
	public static String PackagesView_HierarchicalLayoutAction_label;
	public static String PackagesView_LayoutActionGroup_layout_label;
	public static String StatusBar_concat;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaBrowsingMessages.class);
	}
}