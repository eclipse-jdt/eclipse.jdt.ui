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

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ListDialog;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;

class PackageFragmentRootManipulationQuery implements IPackageFragmentRootManipulationQuery{
	
	private static final class ReferencingProjectListDialog extends ListDialog {
		private ReferencingProjectListDialog(Shell parent) {
			super(parent, SWT.TITLE | SWT.BORDER | SWT.RESIZE);
		}

		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
		}
	}
	
	private final Shell fShell;
	private final String fDialogTitle;
	private final String fMessagePattern;
	
	/*
	 * <code>messagePattern</code> should have exactly 1 placeholder {0}.
	 */
	PackageFragmentRootManipulationQuery(Shell shell, String dialogTitle, String messagePattern){
		Assert.isNotNull(shell);
		Assert.isNotNull(dialogTitle);
		Assert.isNotNull(messagePattern);
		fShell= shell;
		fDialogTitle= dialogTitle;
		fMessagePattern= messagePattern;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery#confirmManipulation(org.eclipse.jdt.core.IPackageFragmentRoot, org.eclipse.jdt.core.IJavaProject[])
	 */
	public boolean confirmManipulation(IPackageFragmentRoot root, IJavaProject[] referencingProjects) {
		String msg= MessageFormat.format(fMessagePattern, new Object[]{root.getElementName()});
		ListDialog dialog= new ReferencingProjectListDialog(fShell);
		dialog.setBlockOnOpen(true);
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setLabelProvider(new JavaElementLabelProvider());
		dialog.setInput(referencingProjects);
		dialog.setMessage(msg);
		dialog.setTitle(fDialogTitle);
		return dialog.open() == IDialogConstants.OK_ID;
	}
}
