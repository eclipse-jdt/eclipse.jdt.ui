/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IMethod;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDescriptor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;


public class RenameRefactoringAction extends SelectionDispatchAction {

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
	
	public RenameRefactoringAction(IWorkbenchSite site) {
		super(site);
		setText("Rename with Participants...");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(selection.size() == 1);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		Object element= selection.getFirstElement();
		try {
			RenameRefactoring refactoring= new RenameRefactoring(element);
			run(refactoring, getShell());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), "Rename Refactoring", "Unexpected Exception occured");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(true);
	}
	
	public static void run(RenameRefactoring refactoring, Shell parent) throws CoreException {
		if (refactoring.isAvailable()) {
			// TODO we need to find a better home for this
			IRenameProcessor processor= refactoring.getProcessor();
			if (processor instanceof RenameVirtualMethodProcessor) {
				RenameVirtualMethodProcessor virtual= (RenameVirtualMethodProcessor)processor;
				RefactoringStatus status= virtual.checkActivation();
				if (!status.hasFatalError()) {
					IMethod method= virtual.getMethod();
					if (!method.equals(virtual.getOriginalMethod())) {
						String message= null;
						if (method.getDeclaringType().isInterface()) {
							message= RefactoringCoreMessages.getFormattedString(
								"MethodChecks.implements", //$NON-NLS-1$
								new String[]{
									JavaElementUtil.createMethodSignature(method), 
									JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
						} else {
							message= RefactoringCoreMessages.getFormattedString(
								"MethodChecks.overrides", //$NON-NLS-1$
								new String[]{
									JavaElementUtil.createMethodSignature(method), 
									JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
						}						
						message= message + "\n\nOK to perform the operation on this method?";
						if (!MessageDialog.openQuestion(parent, "Rename Refactoring", message)) {
							return;
						}
					}
				}
			}
			RefactoringWizardDescriptor descriptor= RefactoringWizardDescriptor.get(refactoring.getProcessor());
			if (descriptor != null) {
				RefactoringWizard wizard= descriptor.createWizard();	
				wizard.initialize(refactoring);	
				SelectionState state= new SelectionState(processor.getElement());
				new RefactoringStarter().activate(refactoring, wizard, parent, "Rename", true);
				state.restore(processor.getNewElement());
				return;
			}
		}
		MessageDialog.openInformation(parent, 
			"Rename Refactoring", 
			"No refactoring available to process the selected element.");
		
	}
}
