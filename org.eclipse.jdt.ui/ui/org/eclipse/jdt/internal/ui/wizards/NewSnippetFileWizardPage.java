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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Page to create a new Java snippet file.
 */
public class NewSnippetFileWizardPage extends WizardNewFileCreationPage {
	
	private static final String fgDefaultExtension= ".jpage"; //$NON-NLS-1$
	
	public NewSnippetFileWizardPage(IStructuredSelection selection) {
		super("createScrapBookPage", selection); //$NON-NLS-1$
		setTitle(NewWizardMessages.getString("NewSnippetFileWizardPage.title")); //$NON-NLS-1$
	}

	public boolean finish() {
		// add extension if non is provided 
		String fileName= getFileName();
		if (fileName != null && !fileName.endsWith(fgDefaultExtension)) {
			setFileName(fileName + fgDefaultExtension);
		}

		boolean retValue= super.validatePage();

		final IFile file= createNewFile();
		if (retValue && file != null) {
			Shell shell= getShell();
			IWorkbenchPage page= JavaPlugin.getActivePage();
			if (shell == null || page == null) {
				return true;
			}
			final IWorkbenchPart focusPart= page.getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						ISelection selection= new StructuredSelection(file);
						((ISetSelectionTarget) focusPart).selectReveal(selection);
					}
				});
			}
			try {
				IDE.openEditor(page, file, true);
				return true;
			} catch (PartInitException e) {
				ExceptionHandler.handle(e, shell, NewWizardMessages.getString("NewSnippetFileWizardPage.open_error.message"),  e.getMessage()); //$NON-NLS-1$
			}
		}
		return false;
	}
	
	/**
	 * @see WizardNewFileCreationPage#validatePage
	 */
	protected boolean validatePage() {
		// check whether file with extension doesn't exist
		boolean valid= super.validatePage();
		if (!valid)
			return false;
		
		IWorkspaceRoot workspaceRoot= JavaPlugin.getWorkspace().getRoot();
		IPath containerPath= getContainerFullPath();
		if (containerPath != null && containerPath.segmentCount() > 0) {
			IProject project= workspaceRoot.getProject(containerPath.segment(0));
			try {
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					setErrorMessage(NewWizardMessages.getString("NewSnippetFileWizardPage.error.OnlyInJavaProject")); //$NON-NLS-1$
					return false;
				}
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	
		String fileName= getFileName();
		if (fileName != null && !fileName.endsWith(fgDefaultExtension)) {		
			fileName= fileName + fgDefaultExtension;
			IPath path= getContainerFullPath();
			
			if (path != null && workspaceRoot.exists(path.append(fileName))) {
				setErrorMessage(NewWizardMessages.getString("NewSnippetFileWizardPage.error.AlreadyExists")); //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}
	
	/*
	 * @see WizardNewFileCreationPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.NEW_SNIPPET_WIZARD_PAGE);		
	}

}


