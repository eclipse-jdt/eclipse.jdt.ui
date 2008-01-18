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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Action to set the focus into the editor breadcrumb.
 * The breadcrumb is made visible if it is hidden.
 * 
 * @since 3.4
 */
public class ShowInBreadcrumbAction extends Action {

	public ShowInBreadcrumbAction() {
		setEnabled(true);
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_SHOW_BREADCRUMB, true);
	}

}
