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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.PrimitiveType;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.ChangeTypeWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Action to generalize the type of a local or field declaration or the
 * return type of a method declaration.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class ChangeTypeAction extends SelectionDispatchAction {
	private CompilationUnitEditor fEditor;
	private String fDialogMessageTitle;

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 */
	public ChangeTypeAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Creates a new <code>ChangeTypeAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
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
	
	//---- structured selection ---------------------------------------------

	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(getMember(selection) != null);
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}

	public void run(IStructuredSelection selection) {
		try {
			IMember member= getMember(selection);
			if (member == null)
				return;
			ISourceRange range= member.getNameRange();
			ICompilationUnit icu= member.getCompilationUnit();
			ITextSelection textSelection= new TextSelection(range.getOffset(), range.getLength());
			ChangeTypeRefactoring refactoring= createRefactoring(icu, textSelection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
		}
	}

	private IMember getMember(IStructuredSelection selection) throws JavaModelException {
		if (selection.size() != 1)
			return null;
		
		Object element= selection.getFirstElement();
		if (!(element instanceof IMember))
			return null;
		
		if (element instanceof IMethod) {
			IMethod method= (IMethod)element;
			String returnType= method.getReturnType();
			if (PrimitiveType.toCode(Signature.toString(returnType)) != null)
				return null;
			return method;
		} else if (element instanceof IField) {
			return (IField)element;
		}
		return null;
	}

	//---- text selection ------------------------------------------------------------
	
	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(canEnable(selection));
	}
	
	private boolean canEnable(ITextSelection selection) {
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param selection the java text selection
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}
	
	private boolean canEnable(JavaTextSelection selection) throws JavaModelException {
		IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return ChangeTypeRefactoring.isAvailable(elements[0]);
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try {
			ChangeTypeRefactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
		}
	}

	private static ChangeTypeRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) throws CoreException {
		return ChangeTypeRefactoring.create(cunit, selection.getOffset(), selection.getLength());
	}

	private RefactoringWizard createWizard(ChangeTypeRefactoring refactoring) {
		return new ChangeTypeWizard(refactoring);
	}
}
