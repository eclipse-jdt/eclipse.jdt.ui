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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PromoteTempWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to convert a local variable to a field.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class ConvertLocalToFieldAction extends SelectionDispatchAction {

	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("ConvertLocalToField.title"); //$NON-NLS-1$
	private final CompilationUnitEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ConvertLocalToFieldAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("ConvertLocalToField.label")); //$NON-NLS-1$
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.PROMOTE_TEMP_TO_FIELD_ACTION);
	}

	private static PromoteTempToFieldRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return PromoteTempToFieldRefactoring.create(cunit, selection.getOffset(), selection.getLength(), JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private static RefactoringWizard createWizard(PromoteTempToFieldRefactoring refactoring) {
		return new PromoteTempWizard(refactoring);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(canEnable(selection));
	}
	
	private boolean canEnable(ITextSelection selection) {
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}
	
	private boolean canEnable(JavaTextSelection selection) throws JavaModelException {
		IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && 
			PromoteTempToFieldRefactoring.isAvailable((ILocalVariable)elements[0]);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			PromoteTempToFieldRefactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}
}
