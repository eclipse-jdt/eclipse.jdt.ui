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
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IReorgExceptionHandler;

/**
 * Class used to handle exceptions occurring during reorg actions.
 */
class ReorgExceptionHandler implements IReorgExceptionHandler{

	private MultiStatus fStatus;
	private boolean fForceOutOfSyncDelete;

	ReorgExceptionHandler() {
		String id= JavaPlugin.getDefault().getDescriptor().getUniqueIdentifier();
		fStatus= new MultiStatus(id, IStatus.OK, ReorgMessages.getString("ReorgExceptionHandler.see_details"), null); //$NON-NLS-1$
		fForceOutOfSyncDelete= false;
	}

	public void handle(ChangeContext context, IChange change, Exception e) {		
		if (e instanceof JavaModelException){
			JavaModelException jme= (JavaModelException)e;
			if (jme.getException() instanceof CoreException)
				fStatus.merge(((CoreException) jme.getException()).getStatus());
		} else if (e instanceof CoreException) {
			fStatus.merge(((CoreException) e).getStatus());
		} else if (e instanceof OperationCanceledException)	{
			throw (OperationCanceledException)e;
		}
			
	}

	public boolean forceDeletingResourceOutOfSynch(String name, CoreException e) {
		if (fForceOutOfSyncDelete)
			return true;
	
		int result= queryDeleteOutOfSync(name);
	
		if (result == IDialogConstants.YES_ID) 
			return true;
		else if (result == IDialogConstants.YES_TO_ALL_ID) {
			fForceOutOfSyncDelete = true;
			return true;			
		} else
			return false;
	}
	
	
	MultiStatus getStatus(){
		return fStatus;
	}	
	
	private static int queryDeleteOutOfSync(String name) {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		final MessageDialog dialog=
			new MessageDialog(
				shell,
				ReorgMessages.getString("ReorgExceptionHandler.error.title"), //$NON-NLS-1$
				null,
				ReorgMessages.getFormattedString("ReorgExceptionHandler.error.message", new String[] {name}),	 //$NON-NLS-1$
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.NO_LABEL,
					},
				0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		int result= dialog.getReturnCode();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.YES_TO_ALL_ID;
		return IDialogConstants.NO_ID;
	}
	
}
