package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class SurroundWithTryCatchAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String TITLE= RefactoringMessages.getString("SurroundWithTryCatchAction.title"); //$NON-NLS-1$

	public SurroundWithTryCatchAction(CompilationUnitEditor editor) {
		super(UnifiedSite.create(editor.getEditorSite()));
		setText(TITLE);
		fEditor= editor;
	}

	protected void run(ITextSelection selection) {
		SurroundWithTryCatchRefactoring refactoring= 
			new SurroundWithTryCatchRefactoring(getCompilationUnit(), selection, CodeFormatterPreferencePage.getTabSize(),
			JavaPreferencesSettings.getCodeGenerationSettings());
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

	protected void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0);
	}
	
	private final ICompilationUnit getCompilationUnit() {
		Object editorInput= SelectionConverter.getInput(fEditor);
		if (editorInput instanceof ICompilationUnit)
			return (ICompilationUnit)editorInput;
		else
			return null;
	}
	
}
