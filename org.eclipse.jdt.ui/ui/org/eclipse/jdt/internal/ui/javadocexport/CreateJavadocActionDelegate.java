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
package org.eclipse.jdt.internal.ui.javadocexport;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CreateJavadocActionDelegate implements IObjectActionDelegate {

	private ISelection fCurrentSelection;

	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {

		if (fCurrentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) fCurrentSelection;
			Iterator iter= structuredSelection.iterator();
			if (iter.hasNext()) {
				Object res= iter.next();
				if (res instanceof IFile) {
					IFile file= (IFile) res;
					JavadocWizard wizard= new JavadocWizard(file);

					wizard.init(JavaPlugin.getActiveWorkbenchWindow().getWorkbench(),structuredSelection);
					WizardDialog dialog= new WizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					//					dialog.getShell().setText(WorkbenchMessages.getString("CreateFileAction.title")); //$NON-NLS-1$
					dialog.open();
				}
			}
		}
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fCurrentSelection= selection;
	}

}
