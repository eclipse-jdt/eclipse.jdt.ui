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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringErrorDialogUtil;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Inlines the value of a local variable at all places where a read reference
 * is used.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class InlineTempAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String DIALOG_TITLE= RefactoringMessages.getString("InlineTempAction.inline_temp");//$NON-NLS-1$

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineTempAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		setEnabled(SelectionConverter.canOperateOn(fEditor));
		fEditor= editor;
		update(null);
	}

	public InlineTempAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("InlineTempAction.label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_TEMP_ACTION);
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
		InlineTempRefactoring refactoring= new InlineTempRefactoring(cu, selection.getOffset(), selection.getLength());
		
		try {
			RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(DIALOG_TITLE, status);
				return;
			}
			status= refactoring.checkInput(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringErrorDialogUtil.open(DIALOG_TITLE, status);
				return;
			}
			BusyIndicator.showWhile(getShell().getDisplay(), performChange(refactoring));
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");	
		}
	}
	
	private Runnable performChange(final InlineTempRefactoring refactoring){
		return new Runnable(){
			public void run() {
				IRewriteTarget target= null;
				try {
					IChange change= refactoring.createChange(new NullProgressMonitor());
					target= (IRewriteTarget) fEditor.getAdapter(IRewriteTarget.class);
					target.beginCompoundChange();
					ChangeContext context= new ChangeContext(new AbortChangeExceptionHandler());
					IProgressMonitor pm= new NullProgressMonitor();
					change.aboutToPerform(context, pm);
					change.perform(context, pm);
					change.performed();
				} catch (JavaModelException e) {
					ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");	
				} finally {
					if (target != null)
						target.endCompoundChange();
				}
			}
		};
	}
	
	private ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}	
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		//do nothing - it's a text-based action
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(false); // it's a text-based action
	}

}
