/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.IntroduceFactoryWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action that encapsulates the a constructor call with a factory
 * method.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class IntroduceFactoryAction extends SelectionDispatchAction {
	private CompilationUnitEditor   fEditor;

	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("IntroduceFactoryAction.dialog_title");//$NON-NLS-1$

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public IntroduceFactoryAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Creates a new <code>IntroduceFactoryAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public IntroduceFactoryAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("IntroduceFactoryAction.label")); //$NON-NLS-1$
		setToolTipText(RefactoringMessages.getString("IntroduceFactoryAction.tooltipText")); //$NON-NLS-1$
		setDescription(RefactoringMessages.getString("IntroduceFactoryAction.description")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INTRODUCE_FACTORY_ACTION);
	}
	
	//---- structured selection --------------------------------------------------
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui here - happens on selection changes
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			//we have to call this here - no selection changed event is sent after a refactoring but it may still invalidate enablement
			if (canEnable(selection)) {
				IMethod	method= getSingleSelectedMethod(selection);
				ICompilationUnit unit= method.getCompilationUnit();
				ISourceRange nameRange= method.getNameRange();
				ITextSelection textSel= new TextSelection(nameRange.getOffset(), nameRange.getLength());
				IntroduceFactoryRefactoring refactoring= createRefactoring(unit, textSel);

				if (refactoring == null)
					return;
				new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("IntroduceFactoryAction.exception")); //$NON-NLS-1$
		}
	}

	private static boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		return IntroduceFactoryRefactoring.isAvailable(getSingleSelectedMethod(selection));
	}

	private static IMethod getSingleSelectedMethod(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		if (selection.getFirstElement() instanceof IMethod)
			return (IMethod)selection.getFirstElement();
		return null;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(canEnable(selection));
	}

	private boolean canEnable(ITextSelection selection) {
		return fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
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
		if (elements.length == 1 && elements[0] instanceof IMethod)
			return IntroduceFactoryRefactoring.isAvailable((IMethod)elements[0]);
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try{
			IntroduceFactoryRefactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (CoreException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("IntroduceFactoryAction.exception")); //$NON-NLS-1$
		}
	}

	private static IntroduceFactoryRefactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return IntroduceFactoryRefactoring.create(cunit, selection.getOffset(), selection.getLength());
	}

	private RefactoringWizard createWizard(IntroduceFactoryRefactoring refactoring) {
		String pageTitle= RefactoringMessages.getString("IntroduceFactoryAction.use_factory"); //$NON-NLS-1$

		return new IntroduceFactoryWizard(refactoring, pageTitle);
	}
}
