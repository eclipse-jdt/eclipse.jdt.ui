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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;

public class CutAction extends SelectionDispatchAction{

	private CopyToClipboardAction fCopyToClipboardAction;

	public CutAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction) {
		super(site);
		setText(ReorgMessages.getString("CutAction.text")); //$NON-NLS-1$
		fCopyToClipboardAction= new CopyToClipboardAction(site, clipboard, pasteAction);

		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

		update(getSelection());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CUT_ACTION);
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		try {
			/*
			 * cannot cut top-level types. this deletes the cu and then you cannot paste because the cu is gone. 
			 */
			if (! containsOnlyElementsInsideCompilationUnits(selection) || containsTopLevelTypes(selection)){
				setEnabled(false);
				return;
			}	
			fCopyToClipboardAction.selectionChanged(selection);
			setEnabled(fCopyToClipboardAction.isEnabled() && isDeleteEnabled(selection));
		} catch (CoreException e) {
			//no ui here - this happens on selection changes
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	private boolean isDeleteEnabled(IStructuredSelection selection) throws CoreException {
		Object[] elements= selection.toArray();
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		return processor.isApplicable();
	}

	private static boolean containsOnlyElementsInsideCompilationUnits(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object object= iter.next();
			if (! (object instanceof IJavaElement && ReorgUtils.isInsideCompilationUnit((IJavaElement)object)))
				return false;
		}
		return true;
	}

	private static boolean containsTopLevelTypes(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			if (isTopLevelType(iter.next()))
				return true;
		}
		return false;
	}

	private static boolean isTopLevelType(Object each) {
		return (each instanceof IType) && ((IType)each).getDeclaringType() == null;
	}

	public void run(IStructuredSelection selection) {
		try {
			selectionChanged(selection);
			if (isEnabled()) {
				fCopyToClipboardAction.run(selection);
				runDelete(selection);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			//OK
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}

	private void runDelete(IStructuredSelection selection) throws CoreException, InterruptedException, InvocationTargetException {
		Object[] elements= selection.toArray();
		DeleteRefactoring refactoring= createRefactoring(elements);
		Assert.isTrue(refactoring.isApplicable());
		IRunnableContext context= new ProgressMonitorDialog(getShell());
		JavaDeleteProcessor processor= (JavaDeleteProcessor)refactoring.getAdapter(JavaDeleteProcessor.class);
		if (processor != null)
			processor.setQueries(new ReorgQueries(getShell()));
		new RefactoringExecutionHelper(refactoring, RefactoringCore.getConditionCheckingFailedSeverity(), false, getShell(), context).perform();
	}

	private DeleteRefactoring createRefactoring(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring ref= new DeleteRefactoring(processor);
		processor.setSuggestGetterSetterDeletion(false);
		return ref;
	}
}
