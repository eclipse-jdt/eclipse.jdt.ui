/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;


import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.surround.ISurroundWithTryCatchQuery;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringErrorDialogUtil;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to surround a set of statements with a try/catch block.
 * 
 * @since 2.0
 */
public class SurroundWithTryCatchAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String TITLE= RefactoringMessages.getString("SurroundWithTryCatchAction.title"); //$NON-NLS-1$

	private static class Query implements ISurroundWithTryCatchQuery {
		private Shell fParent;
		public Query(Shell shell) {
			fParent= shell;
		}
		public boolean catchRuntimeException() {
			return MessageDialog.openQuestion(fParent, TITLE,  RefactoringMessages.getString("SurroundWithTryCatchAction.no_exceptions")); //$NON-NLS-1$
		}
	}

	/**
	 * Creates a new <code>SurroundWithTryCatchAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public SurroundWithTryCatchAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(TITLE);
		fEditor= editor;
		setEnabled(!fEditor.isEditorInputReadOnly());
	}

	protected void run(ITextSelection selection) {
		SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(getCompilationUnit(), selection, 
			JavaPreferencesSettings.getCodeGenerationSettings(),
			new Query(getShell()));
		try {
			RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(TITLE, status);
				RefactoringStatusEntry entry= status.getFirstEntry(RefactoringStatus.FATAL);
				if (entry.getContext() instanceof JavaSourceContext && fEditor != null) {
					JavaSourceContext context= (JavaSourceContext)entry.getContext();
					ISourceRange range= context.getSourceRange();
					fEditor.setHighlightRange(range.getOffset(), range.getLength(), true);
				}
				return;
			}
			if (refactoring.stopExecution())
				return;
			PerformChangeOperation op= new PerformChangeOperation(refactoring.createChange(new NullProgressMonitor()));
			op.setChangeContext(new ChangeContext(new AbortChangeExceptionHandler()));
			new BusyIndicatorRunnableContext().run(false, false, op);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, TITLE, RefactoringMessages.getString("SurroundWithTryCatchAction.exception")); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, TITLE, RefactoringMessages.getString("SurroundWithTryCatchAction.exception")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// not cancelable
		}
	}

	/* package */ void editorStateChanged() {
		setEnabled(!fEditor.isEditorInputReadOnly());
	}
	
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0 && !fEditor.isEditorInputReadOnly());
	}
	
	private final ICompilationUnit getCompilationUnit() {
		Object editorInput= SelectionConverter.getInput(fEditor);
		if (editorInput instanceof ICompilationUnit)
			return (ICompilationUnit)editorInput;
		else
			return null;
	}
	
}
