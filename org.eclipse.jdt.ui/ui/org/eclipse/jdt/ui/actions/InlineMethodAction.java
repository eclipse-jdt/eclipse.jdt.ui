/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.code.InlineMethodWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Inlines the body of a method for a single call.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class InlineMethodAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	private static final String DIALOG_TITLE= "Inline Method";

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineMethodAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		setText("Inline Method...");
		update(null);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void update(ISelection selection) {
		setEnabled(getCompilationUnit() != null);		
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return;
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(
			cu, selection.getOffset(), selection.getLength(),
			JavaPreferencesSettings.getCodeGenerationSettings());
		if (refactoring == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, "No method invocation or declaration selected.");
			return;
		}
		try {
			IRewriteTarget target= (IRewriteTarget) fEditor.getAdapter(IRewriteTarget.class);
			try {
				target.beginCompoundChange();
				new RefactoringStarter().activate(refactoring, createWizard(refactoring), DIALOG_TITLE, false);
			} finally {
				if (target != null)
					target.endCompoundChange();
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");	
		}
	}
	
	private ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private RefactoringWizard createWizard(InlineMethodRefactoring refactoring) {
		return new InlineMethodWizard(refactoring);
	}
}
