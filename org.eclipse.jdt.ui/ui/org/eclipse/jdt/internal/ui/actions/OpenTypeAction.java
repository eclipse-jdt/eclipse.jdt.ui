/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class OpenTypeAction extends Action implements IWorkbenchWindowActionDelegate {
	
	public OpenTypeAction() {
		super();
		setText(JavaUIMessages.getString("OpenTypeAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenTypeAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenTypeAction.tooltip")); //$NON-NLS-1$
		setImageDescriptor(JavaPluginImages.DESC_TOOL_OPENTYPE);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_TYPE_ACTION);
	}

	public void run() {
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		// begin fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=66436
		OpenTypeSelectionDialog dialog;
		try {
			dialog= new OpenTypeSelectionDialog(parent, PlatformUI.getWorkbench().getProgressService(), 
				IJavaSearchConstants.TYPE, SearchEngine.createWorkspaceScope());
		} catch (OperationCanceledException e) {
			// action got canceled
			return;
		}
		// end fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=66436
		
		dialog.setMatchEmptyString(true);	
		dialog.setTitle(JavaUIMessages.getString("OpenTypeAction.dialogTitle")); //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("OpenTypeAction.dialogMessage")); //$NON-NLS-1$
		int result= dialog.open();
		if (result != IDialogConstants.OK_ID)
			return;
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type= (IType)types[0];
			try {
				IEditorPart part= EditorUtility.openInEditor(type, true);
				EditorUtility.revealInEditor(part, type);
			} catch (CoreException x) {
				String title= JavaUIMessages.getString("OpenTypeAction.errorTitle"); //$NON-NLS-1$
				String message= JavaUIMessages.getString("OpenTypeAction.errorMessage"); //$NON-NLS-1$
				ExceptionHandler.handle(x, title, message);
			}
		}
	}

	//---- IWorkbenchWindowActionDelegate ------------------------------------------------

	public void run(IAction action) {
		run();
	}
	
	public void dispose() {
		// do nothing.
	}
	
	public void init(IWorkbenchWindow window) {
		// do nothing.
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}
}
