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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTempRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RenameTempAction extends SelectionDispatchAction {

	private final CompilationUnitEditor fEditor;
	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"); //$NON-NLS-1$
	
	public RenameTempAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"));//$NON-NLS-1$
		fEditor= editor;
		setEnabled(fEditor != null && getCompilationUnit() != null);
	}
	
	private static RenameTempRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return RenameTempRefactoring.create(cunit, selection.getOffset(), selection.getLength());
	}
	
	private static RefactoringWizard createWizard(RenameTempRefactoring refactoring) {
		String message= RefactoringMessages.getString("RenameTempAction.choose_new_name"); //$NON-NLS-1$
		String wizardPageHelp= IJavaHelpContextIds.RENAME_TEMP_WIZARD_PAGE; 
		String pageTitle= RefactoringMessages.getString("RenameTempAction.rename_Local_Variable");//$NON-NLS-1$
		RenameRefactoringWizard result= new RenameRefactoringWizard(refactoring, pageTitle, message, null, wizardPageHelp);
		return result;
	}
	
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			IJavaElement[] elements= selection.resolveElementAtOffset();
			if (elements.length != 1) {
				setEnabled(false);
			} else {
				setEnabled(RenameTempRefactoring.isAvailable(elements[0]));
			}
		} catch (CoreException e) {
			setEnabled(false);
		}
	}
	
	public boolean canRun(ITextSelection selection) {
		selectionChanged(selection);
		if (! isEnabled())
			return false;
		
		if (getCompilationUnit() == null)
			return false;	
		Refactoring renameTempRefactoring= createRefactoring(getCompilationUnit(), selection);
		if (renameTempRefactoring == null)
			return false;
		try {
			return (renameTempRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK());
		} catch(CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"), RefactoringMessages.getString("RenameTempAction.exception"));//$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}
	
	public void run(ITextSelection selection) {
		try{
			ICompilationUnit input= SelectionConverter.getInputAsCompilationUnit(fEditor);
			if (!ActionUtil.isProcessable(getShell(), input))
				return;
			RenameTempRefactoring refactoring= createRefactoring(input, selection);
			if (refactoring == null)
				return;		
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}
		
	private ICompilationUnit getCompilationUnit(){
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}	
}
