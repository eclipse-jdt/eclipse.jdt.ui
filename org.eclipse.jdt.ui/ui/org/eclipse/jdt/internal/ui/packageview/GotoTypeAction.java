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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class GotoTypeAction extends Action {
	
	private PackageExplorerPart fPackageExplorer;
	
	GotoTypeAction(PackageExplorerPart part) {
		super();
		setText(PackagesMessages.getString("GotoType.action.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("GotoType.action.description")); //$NON-NLS-1$
		fPackageExplorer= part;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GOTO_TYPE_ACTION);
	}


	public void run() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false);
		} catch (JavaModelException e) {
			String title= getDialogTitle();
			String message= PackagesMessages.getString("GotoType.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, title, message);
			return;
		}
	
		dialog.setTitle(getDialogTitle());
		dialog.setMessage(PackagesMessages.getString("GotoType.dialog.message")); //$NON-NLS-1$
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			gotoType((IType) types[0]);
		}
	}
	
	private void gotoType(IType type) {
		ICompilationUnit cu= (ICompilationUnit) type.getAncestor(IJavaElement.COMPILATION_UNIT);
		IJavaElement element= null;
		if (cu != null) {
			element= JavaModelUtil.toOriginal(cu);
		}
		else {
			element= type.getAncestor(IJavaElement.CLASS_FILE);
		}
		if (element != null) {
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				if (!element.equals(getSelectedElement(view))) {
					MessageDialog.openInformation(fPackageExplorer.getSite().getShell(), 
						getDialogTitle(), 
						PackagesMessages.getFormattedString("PackageExplorer.element_not_present", element.getElementName())); //$NON-NLS-1$
				}
			}
		}
	}
	
	private Object getSelectedElement(PackageExplorerPart view) {
		return ((IStructuredSelection)view.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}	
	
	private String getDialogTitle() {
		return PackagesMessages.getString("GotoType.dialog.title"); //$NON-NLS-1$
	}
}
