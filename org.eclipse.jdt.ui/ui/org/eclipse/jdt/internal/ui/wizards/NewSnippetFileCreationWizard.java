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
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Creates a new snippet page
 */
public class NewSnippetFileCreationWizard extends Wizard implements INewWizard {

	private NewSnippetFileWizardPage fPage;
	private IStructuredSelection fSelection;
	
	public NewSnippetFileCreationWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(NewWizardMessages.getString("NewSnippetFileCreationWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		if (fSelection == null) {
			IJavaElement elem= EditorUtility.getActiveEditorJavaInput();
			if (elem != null) {
				fSelection= new StructuredSelection(elem);
			} else {
				fSelection= StructuredSelection.EMPTY;
			}
		}
		fPage= new NewSnippetFileWizardPage(fSelection);
		addPage(fPage);
	}

	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		return fPage.finish();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchWizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fSelection= selection;
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWSCRAPPAGE);
	}
}
