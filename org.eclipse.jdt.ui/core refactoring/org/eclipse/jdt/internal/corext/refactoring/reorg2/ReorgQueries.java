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
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.Assert;

class ReorgQueries implements IReorgQueries{
	
	private final Shell fShell;
	
	ReorgQueries(Shell parent){
		Assert.isNotNull(parent);
		fShell= parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgQueries#createYesYesToAllNoNoToAllQuery(java.lang.String)
	 */
	public IConfirmQuery createYesYesToAllNoNoToAllQuery(String dialogTitle, int queryID) {
		return new YesYesToAllNoNoToAllQuery(fShell, dialogTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgQueries#createYesNoQuery(java.lang.String)
	 */
	public IConfirmQuery createYesNoQuery(String dialogTitle, int queryID) {
		return new YesNoQuery(fShell, dialogTitle);
	}

	private static class YesYesToAllNoNoToAllQuery implements IConfirmQuery{
		private boolean fYesToAll= false;
		private boolean fNoToAll= false;
		private final Shell fShell;
		private final String fDialogTitle;
		
		YesYesToAllNoNoToAllQuery(Shell parent, String dialogTitle){
			fShell= parent;
			fDialogTitle= dialogTitle;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.IConfirmQuery#confirm(java.lang.String)
		 */
		public boolean confirm(final String question) throws OperationCanceledException {
			if (fYesToAll) 
				return true;

			if (fNoToAll) 
				return false;

			final int[] result= new int[1];
			fShell.getDisplay().syncExec(createQueryRunnable(question, result));
			return getResult(result);
		}

		private boolean getResult(int[] result) throws OperationCanceledException {
			switch(result[0]){
				case IDialogConstants.YES_TO_ALL_ID: 
					fYesToAll= true;
					return true;
				case IDialogConstants.YES_ID:
					return true;
				case IDialogConstants.CANCEL_ID:
					throw new OperationCanceledException();
				case IDialogConstants.NO_ID:
					return false;
				case IDialogConstants.NO_TO_ALL_ID:
					fNoToAll= true;
					return false;
				default:
					Assert.isTrue(false);
					return false;
			}
		}

		private Runnable createQueryRunnable(final String question, final int[] result) {
			return new Runnable() {
				public void run() {
					int resultId[]= {
						IDialogConstants.YES_ID,
						IDialogConstants.YES_TO_ALL_ID,
						IDialogConstants.NO_ID,
						IDialogConstants.NO_TO_ALL_ID,
						IDialogConstants.CANCEL_ID};
 
					MessageDialog dialog= new MessageDialog(
						fShell, 
						fDialogTitle, 
						null,
						question,
						MessageDialog.QUESTION,
						new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.YES_TO_ALL_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.NO_TO_ALL_LABEL,
							IDialogConstants.CANCEL_LABEL },
						0);
					dialog.open();
					result[0]= resultId[dialog.getReturnCode()];
				}
			};
		}
	}
	
	private static class YesNoQuery implements IConfirmQuery{

		private final Shell fShell;
		private final String fDialogTitle;

		YesNoQuery(Shell parent, String dialogTitle){
			fShell= parent;
			fDialogTitle= dialogTitle;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.IConfirmQuery#confirm(java.lang.String)
		 */
		public boolean confirm(String question) throws OperationCanceledException {
			final int[] result= new int[1];
			fShell.getDisplay().syncExec(createQueryRunnable(question, result));
			return getResult(result);
		}

		private Runnable createQueryRunnable(final String question, final int[] result) {
			return new Runnable() {
				public void run() {
					int resultId[]= {
						IDialogConstants.YES_ID,
						IDialogConstants.NO_ID,
						IDialogConstants.CANCEL_ID};
 
					MessageDialog dialog= new MessageDialog(
						fShell, 
						fDialogTitle, 
						null,
						question,
						MessageDialog.QUESTION,
						new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.CANCEL_LABEL },
						0);
					dialog.open();
					result[0]= resultId[dialog.getReturnCode()];
				}
			};
		}
		
		private boolean getResult(int[] result) throws OperationCanceledException {
			switch(result[0]){
				case IDialogConstants.YES_ID:
					return true;
				case IDialogConstants.CANCEL_ID:
					throw new OperationCanceledException();
				case IDialogConstants.NO_ID:
					return false;
				default:
					Assert.isTrue(false);
					return false;
			}
		}
	}
}
