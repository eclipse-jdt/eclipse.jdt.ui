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
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.refactoring.reorg.IRefactoringRenameSupport;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public class RenameResourceAction extends SelectionDispatchAction {

	public RenameResourceAction(IWorkbenchSite site) {
		super(site);
	}
	
	public void run(IStructuredSelection selection) {
		IResource element= getResource(selection);
		if (element == null)
			return;
		run((IResource)element);
	}

	public void run(IResource element) {
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104		
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
//		try{
//			IRefactoringRenameSupport support= new RefactoringSupport.Resource(element);
//			if (! canRename(support, element))
//				return;
//			support.rename(getShell(), element);
//		} catch (JavaModelException e){
//			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.exception"));  //$NON-NLS-1$ //$NON-NLS-2$
//		}	
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		try {
			IResource element= getResource(selection);
			if (element == null)
				setEnabled(false);
			else
				setEnabled(canRename(element) );
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	private static IResource getResource(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IResource))
			return null;
		return (IResource)first;
	}

	private static boolean canRename(IResource element) throws JavaModelException {
		return false;
		// return canRename(new RefactoringSupport.Resource(element), element);	
	}

	private static boolean canRename(IRefactoringRenameSupport support, IResource element) {
		if (support == null)
			return false;
		try{
			return support.canRename(element);
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			return false;
		}	
	}
}
