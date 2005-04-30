/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Externalizes the strings of a compilation unit. Opens a wizard that
 * gathers additional information to externalize the strings.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>ICompilationUnit</code>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class ExternalizeStringsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Creates a new <code>ExternalizeStringsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ExternalizeStringsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ExternalizeStringsAction_label); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.EXTERNALIZE_STRINGS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public ExternalizeStringsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(fEditor != null && SelectionConverter.canOperateOn(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(RefactoringAvailabilityTester.isExternalizeStringsAvailable(selection));
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		IJavaElement element= SelectionConverter.getInput(fEditor);
		if (!(element instanceof ICompilationUnit))
			return;
		run((ICompilationUnit)element);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (RefactoringAvailabilityTester.isExternalizeStringsAvailable(selection)) 
			run(getCompilationUnit(selection));
	}	
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	public void run(final ICompilationUnit unit) {
		if (!ActionUtil.isProcessable(getShell(), unit))
			return;
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					if (unit != null && unit.exists()) {
						NLSRefactoring refactoring= NLSRefactoring.create(unit);
						if (refactoring != null)
							new RefactoringStarter().activate(refactoring, new ExternalizeWizard(refactoring), getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, true); 
					}
				} catch(JavaModelException e) {
					ExceptionHandler.handle(e, getShell(), ActionMessages.ExternalizeStringsAction_dialog_title, ActionMessages.ExternalizeStringsAction_dialog_message); 
				}
			}
		});
	}

	private static ICompilationUnit getCompilationUnit(IStructuredSelection selection) {
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (first instanceof ICompilationUnit) 
			return (ICompilationUnit) first;
		if (first instanceof IType)
			return ((IType) first).getCompilationUnit();
		return null;
	}		
}