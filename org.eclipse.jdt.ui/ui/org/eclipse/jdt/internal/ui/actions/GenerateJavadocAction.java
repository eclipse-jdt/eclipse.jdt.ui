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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.internal.ui.javadocexport.JavadocWizard;

public class GenerateJavadocAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	private IWorkbenchWindow fWindow;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	public void run(IAction action) {
		JavadocWizard wizard= new JavadocWizard();
		IStructuredSelection selection= null;
		if (fSelection instanceof IStructuredSelection) {
			selection= (IStructuredSelection)fSelection;
		} else {
			selection= new StructuredSelection();
		}
		
		wizard.init(fWindow.getWorkbench() , selection);
		WizardDialog dialog= new WizardDialog(fWindow.getShell(), wizard);
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}
}
