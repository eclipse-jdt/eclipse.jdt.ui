/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import org.eclipse.osgi.util.NLS;

/**
 * @since 3.4
 */
public class BreadcrumbMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbMessages"; //$NON-NLS-1$

	public static String BreadcrumbItemDropDown_showDropDownMenu_action_toolTip;

	public static String FilteredTable_accessible_listener_text;
	public static String FilteredTable_clear_button_tooltip;
	public static String FilteredTable_go_left_action_tooltip;
	public static String FilteredTable_go_right_action_tooltip;
	public static String FilteredTable_initial_filter_text;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BreadcrumbMessages.class);
	}

	private BreadcrumbMessages() {
	}
}
