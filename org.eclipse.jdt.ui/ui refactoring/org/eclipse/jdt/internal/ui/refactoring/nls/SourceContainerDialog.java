/*****************************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

public class SourceContainerDialog extends ElementTreeSelectionDialog {

	private SourceContainerDialog(Shell shell) {
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
			new StandardJavaElementContentProvider());
		setValidator(new PackageAndProjectSelectionValidator());
		setSorter(new JavaElementSorter());
		setTitle(NewWizardMessages.getString("NewContainerWizardPage.ChooseSourceContainerDialog.title")); //$NON-NLS-1$
		setMessage(NewWizardMessages.getString("NewContainerWizardPage.ChooseSourceContainerDialog.description")); //$NON-NLS-1$
		addFilter(new JavaTypedViewerFilter());
	}

	public static IPackageFragmentRoot getSourceContainer(Shell shell, IWorkspaceRoot workspaceRoot, IJavaElement initElement) {
		SourceContainerDialog dialog= new SourceContainerDialog(shell);
		dialog.setInput(JavaCore.create(workspaceRoot));
		dialog.setInitialSelection(initElement);

		if (dialog.open() == Window.OK) {
			Object element= dialog.getFirstResult();
			if (element instanceof IJavaProject) {
				IJavaProject jproject= (IJavaProject)element;
				return jproject.getPackageFragmentRoot(jproject.getProject());
			} else if (element instanceof IPackageFragmentRoot) {
				return (IPackageFragmentRoot)element;
			}
			return null;
		}
		return null;
	}
}