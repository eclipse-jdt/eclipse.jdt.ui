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
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.ConvertAnonymousToNestedWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to convert an anonymous inner class to a nested class
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class ConvertAnonymousToNestedAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("ConvertAnonymousToNestedAction.dialog_title"); //$NON-NLS-1$
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ConvertAnonymousToNestedAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("ConvertAnonymousToNestedAction.Convert_Anonymous")); //$NON-NLS-1$
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_ACTION);
	}

	private Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ConvertAnonymousToNestedRefactoring(cunit, selection.getOffset(), selection.getLength());
	}

	private RefactoringWizard createWizard(Refactoring refactoring) {
		String helpId= IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_ERROR_WIZARD_PAGE;
		String pageTitle= RefactoringMessages.getString("ConvertAnonymousToNestedAction.wizard_title"); //$NON-NLS-1$
		return new ConvertAnonymousToNestedWizard((ConvertAnonymousToNestedRefactoring)refactoring, pageTitle, helpId);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			Refactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(ITextSelection selection) {
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}
	
}
