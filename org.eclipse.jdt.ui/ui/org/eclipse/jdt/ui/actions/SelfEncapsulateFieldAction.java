/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to run the self encapsulate field refactoring. 
 * <p>
 * Action is applicable to selections containing elements of type
 * <code>IField</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class SelfEncapsulateFieldAction extends SelectionDispatchAction {
	
	private CompilationUnitEditor fEditor;
	
	/**
	 * Creates a new <code>SelfEncapsulateFieldAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public SelfEncapsulateFieldAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("SelfEncapsulateFieldAction.label")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELF_ENCAPSULATE_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public SelfEncapsulateFieldAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	//---- text selection -------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		IJavaElement[] elements= SelectionConverter.codeResolveHandled(fEditor, getShell(), getDialogTitle());
		if (elements.length != 1 || !(elements[0] instanceof IField)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("SelfEncapsulateFieldAction.dialog.unavailable")); //$NON-NLS-1$
			return;
		}
		IField field= (IField)elements[0];
		try {
			if (!RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field)) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("SelfEncapsulateFieldAction.dialog.unavailable")); //$NON-NLS-1$
				return;
			}
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
			return;
		}
		run(field);
	}
	
	//---- structured selection -------------------------------------------------

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui
		}
	}
	
	/*
	 * (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		try {
			if (RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection))
				run((IField) selection.getFirstElement());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	//---- private helpers --------------------------------------------------------
	
	/*
	 * Should be private. But got shipped in this state in 2.0 so chaning this is a
	 * breaking API change.
	 */
	public void run(IField field) {
		if (!ActionUtil.isProcessable(getShell(), field))
			return;
		
		try  {	
			SelfEncapsulateFieldRefactoring refactoring= SelfEncapsulateFieldRefactoring.create(field);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(
				refactoring, 
				new SelfEncapsulateFieldWizard(refactoring), getShell(), getDialogTitle(), true);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getDialogTitle(),
				ActionMessages.getString("SelfEncapsulateFieldAction.dialog.cannot_perform")); //$NON-NLS-1$
		}
	}

	private String getDialogTitle() {
		return ActionMessages.getString("SelfEncapsulateFieldAction.dialog.title"); //$NON-NLS-1$
	}	
}