/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Page to create a new Java snippet file
 */
public class NewSnippetFileCreationPage extends WizardNewFileCreationPage {
	IWorkbench fWorkbench;
	static final String fgDefaultExtension= "jpage";

	public NewSnippetFileCreationPage(IWorkbench workbench, IStructuredSelection selection) {
		super("createScrapBookPage", selection);
		setTitle("Create Java Scrapbook Page");
		setDescription("Create a new Java scrapbook page.");
		fWorkbench= workbench;
	}

	public boolean finish() {
		// add extension if non is provided 
		IPath fileName= new Path(getFileName());
		String extension= fileName.getFileExtension();
		if (extension == null || !extension.equals(fgDefaultExtension)) {
			setFileName(fileName.toString() + "." + fgDefaultExtension);
		}

		boolean retValue= super.validatePage();

		final IFile file= createNewFile();
		if (retValue && file != null) {
			IWorkbenchWindow dw= fWorkbench.getActiveWorkbenchWindow();
			final IWorkbenchPart focusPart= dw.getActivePage().getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				Display d= getShell().getDisplay();
				d.asyncExec(new Runnable() {
					public void run() {
						ISelection selection= new StructuredSelection(file);
						((ISetSelectionTarget)focusPart).selectReveal(selection);
					}
				});
			}
			try {
				dw.getActivePage().openEditor(file);
				return true;
			} catch (PartInitException e) {
				MessageDialog.openError(getShell(), "Error in NewScrapbookPage",  e.getMessage());
			}
		}
		return false;
	}
	
	protected boolean validatePage() {
		// check whether file with extension doesn't exist
		boolean valid= super.validatePage();
		if (!valid)
			return valid;
			
		String fileName= getFileName();
		if (fileName != null && !fileName.endsWith("." + fgDefaultExtension)) {		
			fileName= fileName + "." + fgDefaultExtension;
			IPath path= getContainerFullPath();
			IWorkspace workspace= JavaPlugin.getWorkspace();
			if (workspace.getRoot().exists(path.append(fileName))) {
				setErrorMessage("A resource with the specified path already exists.");
				return false;
			}
		}
		return true;
	}
}


