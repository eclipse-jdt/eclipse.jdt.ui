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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.surround.ISurroundWithTryCatchQuery;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
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
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class SurroundWithTryCatchAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	private static class Query implements ISurroundWithTryCatchQuery {
		private Shell fParent;
		public Query(Shell shell) {
			fParent= shell;
		}
		public boolean catchRuntimeException() {
			MessageDialog dialog = new MessageDialog(
				fParent, getDialogTitle(),  null,	// accept the default window icon
				RefactoringMessages.getString("SurroundWithTryCatchAction.no_exceptions"), //$NON-NLS-1$
				MessageDialog.QUESTION, 
				new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
				1); 
			return dialog.open() == 0; // yes selected
		}
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public SurroundWithTryCatchAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("SurroundWithTryCatchAction.label")); //$NON-NLS-1$);
		fEditor= editor;
		setEnabled(checkEditor());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SURROUND_WITH_TRY_CATCH_ACTION);
	}

	protected void run(ITextSelection selection) {
		SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(getCompilationUnit(), selection, 
			JavaPreferencesSettings.getCodeGenerationSettings(),
			new Query(getShell()));
		IRewriteTarget target= null;
		try {
			target= (IRewriteTarget)fEditor.getAdapter(IRewriteTarget.class);
			if (target != null)
				target.beginCompoundChange();
			RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(getDialogTitle(), status);
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
			ExceptionHandler.handle(e, getDialogTitle(), RefactoringMessages.getString("SurroundWithTryCatchAction.exception")); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getDialogTitle(), RefactoringMessages.getString("SurroundWithTryCatchAction.exception")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// not cancelable
		} finally {
			if (target != null)
				target.endCompoundChange();
		}
	}

	/* package */ void editorStateChanged() {
		setEnabled(fEditor != null && !fEditor.isEditorInputReadOnly() && getCompilationUnit() != null);
	}
	
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0 && checkEditor());
	}
	
	private final ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private boolean checkEditor() {
		return fEditor != null && !fEditor.isEditorInputReadOnly() && getCompilationUnit() != null;
	}
	
	private static String getDialogTitle() {
		return RefactoringMessages.getString("SurroundWithTryCatchAction.dialog.title"); //$NON-NLS-1$
	}
}
