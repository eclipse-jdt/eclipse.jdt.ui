/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - Fixed bug 25898
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.surround.ISurroundWithTryCatchQuery;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringErrorDialogUtil;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
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
				1) {
					// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=18303
					protected void createButtonsForButtonBar(Composite parent) {
						super.createButtonsForButtonBar(parent);
						Button button= getButton(1);
						if (button != null)
							button.setFocus();
					}
			};
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

	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null || !ElementValidator.checkValidateEdit(cu, getShell(), getDialogTitle()))
			return;
		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(getCompilationUnit(), selection, 
			JavaPreferencesSettings.getCodeGenerationSettings(),
			new Query(getShell()));
		
		if (refactoring == null)
			return;
		try {
			RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(getDialogTitle(), status, getShell());
				RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
				if (entry.getContext() instanceof JavaStatusContext && fEditor != null) {
					JavaStatusContext context= (JavaStatusContext)entry.getContext();
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
		}
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0 && checkEditor());
	}
	
	private final ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private boolean checkEditor() {
		return fEditor != null && getCompilationUnit() != null;
	}
	
	private static String getDialogTitle() {
		return RefactoringMessages.getString("SurroundWithTryCatchAction.dialog.title"); //$NON-NLS-1$
	}
}
