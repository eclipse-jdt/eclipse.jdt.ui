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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;

/**
 * Refocuses the type hierarchy on a type selection from a all types dialog.
 */
public class FocusOnTypeAction extends Action {
			
	private TypeHierarchyViewPart fViewPart;
	
	public FocusOnTypeAction(TypeHierarchyViewPart part) {
		super(TypeHierarchyMessages.getString("FocusOnTypeAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("FocusOnTypeAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("FocusOnTypeAction.tooltip")); //$NON-NLS-1$
		
		fViewPart= part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this,	IJavaHelpContextIds.FOCUS_ON_TYPE_ACTION);
	}

	/*
	 * @see Action#run
	 */
	public void run() {
		Shell parent= fViewPart.getSite().getShell();
		TypeSelectionDialog dialog= new TypeSelectionDialog(parent, new ProgressMonitorDialog(parent), 
			IJavaSearchConstants.TYPE, SearchEngine.createWorkspaceScope());
	
		dialog.setTitle(TypeHierarchyMessages.getString("FocusOnTypeAction.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(TypeHierarchyMessages.getString("FocusOnTypeAction.dialog.message")); //$NON-NLS-1$
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type= (IType)types[0];
			fViewPart.setInputElement(type);
		}
	}	
}
