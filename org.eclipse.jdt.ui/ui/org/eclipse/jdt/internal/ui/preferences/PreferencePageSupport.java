/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 *
 */
public class PreferencePageSupport {
	/**
	 * 
	 */
	private PreferencePageSupport() {
		super();
	}
	
	/**
	 * Open the given preference page in a preference dialog.
	 * @param shell The shell to open on
	 * @param id The id of the preference page as in the plugin.xml 
	 * @param page An instance of the page. Note that such a page should also set its own
	 * title to correctly show up.
	 * @return Returns <code>true</code> if the user ended the page by pressing OK.
	 */
	public static boolean showPreferencePage(Shell shell, String id, IPreferencePage page) {
		// inline when PreferencesUtil is finalized (see bug 63656) 
		return PreferencesUtil.createPreferenceDialogOn(id, new String[] { id }, null).open() == Window.OK;
	}	
}
