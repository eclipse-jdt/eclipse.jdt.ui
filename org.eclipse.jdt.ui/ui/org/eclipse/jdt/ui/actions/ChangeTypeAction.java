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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.ChangeTypeWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * @author tip
 */
public class ChangeTypeAction extends SelectionDispatchAction {
	private CompilationUnitEditor   fEditor;
	private String                  fDialogMessageTitle;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ChangeTypeAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Creates a new <code>ChangeTypeAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ChangeTypeAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("ChangeTypeAction.label")); //$NON-NLS-1$
		setToolTipText(RefactoringMessages.getString("ChangeTypeAction.tooltipText")); //$NON-NLS-1$
		setDescription(RefactoringMessages.getString("ChangeTypeAction.description")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CHANGE_TYPE_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			ChangeTypeRefactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
		} catch (CoreException e){
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		if (canEnable(selection)){
			setEnabled(true);
		}
	}
	
	private boolean canEnable(IStructuredSelection selection) {
		if (selection.size() == 1){
			Object element= selection.getFirstElement();
			if (element instanceof IMember){
				return true;
			}
		}
		return false;
	}

	public void run(IStructuredSelection selection) {
		if (canEnable(selection)){
			Object element= selection.getFirstElement();
			if (element instanceof IMember){
				IMember member= (IMember)element;
				try {
					ISourceRange range= member.getNameRange();
					ICompilationUnit icu= member.getCompilationUnit();
					ITextSelection textSelection= new TextSelection(range.getOffset(), range.getLength());
					ChangeTypeRefactoring refactoring= createRefactoring(icu, textSelection);
					if (refactoring == null)
						return;
					new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
				} catch (CoreException e){
					ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
				}
			}
		}
	}
	
	private static ChangeTypeRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) throws CoreException {
		return ChangeTypeRefactoring.create(cunit, 
				selection.getOffset(), selection.getLength());
	}

	private RefactoringWizard createWizard(ChangeTypeRefactoring refactoring) {
		String pageTitle= RefactoringMessages.getString("ChangeTypeWizard.title"); //$NON-NLS-1$

		return new ChangeTypeWizard(refactoring);
	}

	private boolean checkEnabled(ITextSelection selection) {
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}

	private RefactoringWizard createWizard(Refactoring refactoring) {
		return new ChangeTypeWizard((ChangeTypeRefactoring)refactoring);
	}
}
