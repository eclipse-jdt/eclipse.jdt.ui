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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.UserInterfaceStarter;

import org.eclipse.ltk.core.refactoring.Refactoring;


public class RenameUserInterfaceStarter extends UserInterfaceStarter {
	
	private static class SelectionState {
		private Display fDisplay;
		private Object fElement;
		private List fParts;
		private List fSelections;
		public SelectionState(Object element) {
			fElement= element;
			fParts= new ArrayList();
			fSelections= new ArrayList();
			init();
		}
		private void init() {
			IWorkbenchWindow dw = JavaPlugin.getActiveWorkbenchWindow();
			if (dw ==  null)
				return;
			fDisplay= dw.getShell().getDisplay();
			IWorkbenchPage page = dw.getActivePage();
			if (page == null)
				return;
			IViewReference vrefs[]= page.getViewReferences();
			for(int i= 0; i < vrefs.length; i++) {
				consider(vrefs[i].getPart(false));
			}
			IEditorReference refs[]= page.getEditorReferences();
			for(int i= 0; i < refs.length; i++) {
				consider(refs[i].getPart(false));
			}
		}
		private void consider(IWorkbenchPart part) {
			if (part == null)
				return;
			ISetSelectionTarget target= null;
			if (!(part instanceof ISetSelectionTarget)) {
				target= (ISetSelectionTarget)part.getAdapter(ISetSelectionTarget.class);
				if (target == null)
					return;
			} else {
				target= (ISetSelectionTarget)part;
			}
			ISelection s= part.getSite().getSelectionProvider().getSelection();
			if (!(s instanceof IStructuredSelection))
				return;
			IStructuredSelection selection= (IStructuredSelection)s;
			if (!selection.toList().contains(fElement))
				return;
			fParts.add(part);
			fSelections.add(selection);
		}
		public void restore(Object newElement) {
			if (fDisplay == null)
				return;
			for (int i= 0; i < fParts.size(); i++) {
				final IStructuredSelection selection= (IStructuredSelection)fSelections.get(i);
				final ISetSelectionTarget target= (ISetSelectionTarget)fParts.get(i);
				List l= selection.toList();
				int index= l.indexOf(fElement);
				if (index != -1) { 
					l.set(index, newElement);
					fDisplay.asyncExec(new Runnable() {
						public void run() {
							target.selectReveal(selection);
						}
					});
				}
			}
		}
	}
	
	public void activate(Refactoring refactoring, Shell parent, boolean save) throws CoreException {
		IRenameProcessor processor= (IRenameProcessor)refactoring.getAdapter(IRenameProcessor.class);
		SelectionState state= new SelectionState(processor.getElements());
		super.activate(refactoring, parent, save);
		state.restore(processor.getNewElement());
	}
}
