/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This abstract action delegate offers base functionality used by
 * other JAR Package based action delegates.
 */
abstract class JarPackageActionDelegate implements IActionDelegate {

	private IStructuredSelection fSelection;

	/**
	 * Returns the active shell.
	 */
	protected Shell getShell() {
		return getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/*
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection)
			fSelection= (IStructuredSelection)selection;
		else
			fSelection= StructuredSelection.EMPTY;
	}

	/**
	 * Returns the description file for the first description file in
	 * the selection. Use this method if this action is only active if
	 * one single file is selected.
	 */
	protected IFile getDescriptionFile(IStructuredSelection selection) {
		return (IFile)selection.getFirstElement();
	}

	/**
	 * Returns a description file for each description file in
	 * the selection. Use this method if this action allows multiple
	 * selection.
	 */
	protected IFile[] getDescriptionFiles(IStructuredSelection selection) {
		IFile[] files= new IFile[selection.size()];
		Iterator iter= selection.iterator();
		int i= 0;
		while (iter.hasNext())
			files[i++]= (IFile)iter.next();
		return files;
	}

	protected IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	protected IStructuredSelection getSelection() {
		return fSelection;
	}
}
