/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class SurroundWithTryCatchAction extends TextSelectionAction {

	private static final String TITLE= RefactoringMessages.getString("SurroundWithTryCatchAction.title"); //$NON-NLS-1$

	public SurroundWithTryCatchAction() {
		this(null);
	}
	
	public SurroundWithTryCatchAction(JavaEditor editor) {
		super(RefactoringMessages.getString("SurroundWithTryCatchAction.surround_trycatch"));  //$NON-NLS-1$
		setEditor(editor);
	}
	
	public void run() {
		SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(
			getCompilationUnit(), getTextSelection(),
			CodeFormatterPreferencePage.getTabSize(),
			JavaPreferencesSettings.getCodeGenerationSettings());
		try {
			RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(TITLE, status);
				RefactoringStatusEntry entry= status.getFirstEntry(RefactoringStatus.FATAL);
				AbstractTextEditor editor= getEditor();
				if (entry.getContext() instanceof JavaSourceContext && editor != null) {
					JavaSourceContext context= (JavaSourceContext)entry.getContext();
					ISourceRange range= context.getSourceRange();
					editor.setHighlightRange(range.getOffset(), range.getLength(), true);
				}
				return;
			}
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
}
