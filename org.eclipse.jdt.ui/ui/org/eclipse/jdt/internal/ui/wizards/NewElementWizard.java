/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public abstract class NewElementWizard extends Wizard implements INewWizard {

	public NewElementWizard() {
		setNeedsProgressMonitor(true);
	}
	
	protected IStructuredSelection getSelection() {
		IWorkbenchWindow window= JavaPlugin.getDefault().getActiveWorkbenchWindow();
		if (window != null) {
			ISelection sel= window.getSelectionService().getSelection();
			if (sel instanceof IStructuredSelection) {
				return (IStructuredSelection)sel;
			}
		}
		return null; 
	}
	

	protected void revealSelection(final Object toSelect) {
		IWorkbenchPage activePage= JavaPlugin.getDefault().getActivePage();
		if (activePage != null) {
			final IWorkbenchPart focusPart= activePage.getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				Display d= getShell().getDisplay();
				d.asyncExec(new Runnable() {
					public void run() {
						ISelection selection= new StructuredSelection(toSelect);
						((ISetSelectionTarget)focusPart).selectReveal(selection);
					}
				});
			}
		}
	}

	protected void openResource(final IResource resource) {
		if (resource.getType() == IResource.FILE) {
			final IWorkbenchPage activePage= JavaPlugin.getDefault().getActivePage();
			if (activePage != null) {
				final Display display= getShell().getDisplay();
				if (display != null) {
					display.asyncExec(new Runnable() {
						public void run() {
							try {
								activePage.openEditor((IFile)resource);
							} catch (PartInitException e) {
								MessageDialog.openError(getShell(), "Error", e.getMessage());
							}
						}
					});
				}
			}
		}
	}
	
	/**
	 * @see INewWizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {	
	}
	
}