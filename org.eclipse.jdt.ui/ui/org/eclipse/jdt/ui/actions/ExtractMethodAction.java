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
package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Extracts the code selected inside a compilation unit editor into a new method.
 * Necessary arguments, exceptions and returns values are computed and an
 * appropriate method signature is generated.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */ 
public class ExtractMethodAction extends SelectionDispatchAction {

	private final CompilationUnitEditor fEditor;
	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("ExtractMethodAction.dialog.title");//$NON-NLS-1$

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ExtractMethodAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("ExtractMethodAction.label"));//$NON-NLS-1$
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.EXTRACT_METHOD_ACTION);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(canEnable(selection));
	}
	
	private boolean canEnable(ITextSelection selection) {
		if (selection.getLength() == 0)
			return false;
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		setEnabled(canEnable(selection));
	}
	
	private boolean canEnable(JavaTextSelection selection) {
		return (selection.resolveInMethodBody() || selection.resolveInStaticInitializer()) && 
			ExtractMethodRefactoring.isAvailable(selection.resolveSelectedNodes());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			ExtractMethodRefactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (CoreException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}
	}

	private static ExtractMethodRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) throws CoreException {
		return ExtractMethodRefactoring.create(
			cunit, 
			selection.getOffset(), selection.getLength(),
			JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private static RefactoringWizard createWizard(ExtractMethodRefactoring refactoring) {
		return new ExtractMethodWizard(refactoring);
	}
}
