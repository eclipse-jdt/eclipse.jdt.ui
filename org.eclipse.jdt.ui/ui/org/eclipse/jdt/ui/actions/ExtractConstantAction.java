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
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ExtractConstantWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Extracts an expression into a new local variable and replaces all occurrences of
 * the expression with the local variable.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class ExtractConstantAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private String fDialogMessageTitle;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ExtractConstantAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("ExtractConstantAction.label")); //$NON-NLS-1$
		fEditor= editor;
		fDialogMessageTitle= RefactoringMessages.getString("ExtractConstantAction.extract_constant"); //$NON-NLS-1$
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.EXTRACT_CONSTANT_ACTION);
	}

	private Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractConstantRefactoring(cunit, selection.getOffset(), selection.getLength(), 
																 JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private RefactoringWizard createWizard(Refactoring refactoring) {
		String helpId= IJavaHelpContextIds.EXTRACT_CONSTANT_ERROR_WIZARD_PAGE;
		String pageTitle= RefactoringMessages.getString("ExtractConstantAction.extract_constant"); //$NON-NLS-1$
		return new ExtractConstantWizard((ExtractConstantRefactoring)refactoring, pageTitle, helpId);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			Refactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(ITextSelection selection) {
		if (selection.getLength() == 0)
			return false;
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}
	
}
