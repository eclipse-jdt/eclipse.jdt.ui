/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial API
**********************************************************************/

package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.OpenNewWindowAction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
 * This class can be removed once the bug is fixed.
 * 
 * @since 2.0
 */
public class PatchedOpenNewWindowAction extends OpenNewWindowAction {
	
	private IWorkbenchWindow fWorkbenchWindow;
	
	public PatchedOpenNewWindowAction(IWorkbenchWindow window, IAdaptable input) {
		super(window, input);
		fWorkbenchWindow= window;
	}

	public void run() {
		JavaBrowsingPerspectiveFactory.setInputFromAction(getSelectedJavaElement());
		super.run();
		JavaBrowsingPerspectiveFactory.setInputFromAction(null);
	}

	private IJavaElement getSelectedJavaElement() {
		if (fWorkbenchWindow.getActivePage() != null) {
			ISelection selection= fWorkbenchWindow.getActivePage().getSelection();
			if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
				Object selectedElement= ((IStructuredSelection)selection).getFirstElement();
				if (selectedElement instanceof IJavaElement)
					return (IJavaElement)selectedElement;
				if (!(selectedElement instanceof IJavaElement) && selectedElement instanceof IAdaptable)
					return (IJavaElement)((IAdaptable)selectedElement).getAdapter(IJavaElement.class);
				else if (selectedElement instanceof IWorkspace)
						return JavaCore.create(((IWorkspace)selectedElement).getRoot());
			}
		}
		return null;
	}
}
