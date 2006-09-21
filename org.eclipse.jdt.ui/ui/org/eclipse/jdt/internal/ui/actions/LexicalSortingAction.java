/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.browsing.JavaBrowsingMessages;
import org.eclipse.jdt.internal.ui.viewsupport.SourcePositionSorter;

/*
 * XXX: This class should become part of the MemberFilterActionGroup
 *      which should be renamed to MemberActionsGroup
 */
public class LexicalSortingAction extends Action {
	private JavaElementSorter fSorter= new JavaElementSorter();
	private SourcePositionSorter fSourcePositonSorter= new SourcePositionSorter();
	private StructuredViewer fViewer;
	private String fPreferenceKey;

	public LexicalSortingAction(StructuredViewer viewer, String id) {
		super();
		fViewer= viewer;
		fPreferenceKey= "LexicalSortingAction." + id + ".isChecked"; //$NON-NLS-1$ //$NON-NLS-2$
		setText(JavaBrowsingMessages.LexicalSortingAction_label); 
		JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
		setToolTipText(JavaBrowsingMessages.LexicalSortingAction_tooltip); 
		setDescription(JavaBrowsingMessages.LexicalSortingAction_description); 
		boolean checked= JavaPlugin.getDefault().getPreferenceStore().getBoolean(fPreferenceKey); 
		valueChanged(checked, false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
	}

	public void run() {
		valueChanged(isChecked(), true);
	}

	private void valueChanged(final boolean on, boolean store) {
		setChecked(on);
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (on)
					fViewer.setComparator(fSorter);
				else
					fViewer.setComparator(fSourcePositonSorter);
			}
		});
		
		if (store)
			JavaPlugin.getDefault().getPreferenceStore().setValue(fPreferenceKey, on);
	}
}
