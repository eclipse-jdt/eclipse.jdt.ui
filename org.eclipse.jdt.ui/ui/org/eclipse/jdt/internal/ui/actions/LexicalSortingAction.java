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
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.browsing.JavaBrowsingMessages;

/*
 * XXX: This class should become part of the MemberFilterActionGroup
 *      which should be renamed to MemberActionsGroup
 */
public class LexicalSortingAction extends Action {
	private JavaElementSorter fSorter= new JavaElementSorter();
	private StructuredViewer fViewer;
	private String fPreferenceKey;

	public LexicalSortingAction(StructuredViewer viewer, String id) {
		super();
		fViewer= viewer;
		fPreferenceKey= "LexicalSortingAction." + id + ".isChecked"; //$NON-NLS-1$ //$NON-NLS-2$
		setText(JavaBrowsingMessages.getString("LexicalSortingAction.label")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
		setToolTipText(JavaBrowsingMessages.getString("LexicalSortingAction.tooltip")); //$NON-NLS-1$
		setDescription(JavaBrowsingMessages.getString("LexicalSortingAction.description")); //$NON-NLS-1$
		boolean checked= JavaPlugin.getDefault().getPreferenceStore().getBoolean(fPreferenceKey); //$NON-NLS-1$
		valueChanged(checked, false);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
	}

	public void run() {
		valueChanged(isChecked(), true);
	}

	private void valueChanged(final boolean on, boolean store) {
		setChecked(on);
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (on)
					fViewer.setSorter(fSorter);
				else
					fViewer.setSorter(null);
			}
		});
		
		if (store)
			JavaPlugin.getDefault().getPreferenceStore().setValue(fPreferenceKey, on);
	}
}
