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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ExtractInterfaceWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Extract a new interface from a class and tries to use the interface instead 
 * of the concrete class where possible. 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class ExtractInterfaceAction extends SelectionDispatchAction {

	private ExtractInterfaceRefactoring fRefactoring;
	private CompilationUnitEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ExtractInterfaceAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Creates a new <code>ExtractInterfaceAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ExtractInterfaceAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("ExtractInterfaceAction.Extract_Interface")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.EXTRACT_INTERFACE_ACTION);
	}
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
		if (! isEnabled())
			fRefactoring= null;
	}

    /*
     * @see SelectionDispatchAction#selectionChanged(ITextSelection)
     */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		if (fRefactoring == null)
			selectionChanged(selection);
		if (isEnabled())
			startRefactoring();
		fRefactoring= null;	
		selectionChanged(selection);
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		if (canRun()){
			startRefactoring();
		} else {
			String unavailable= RefactoringMessages.getString("ExtractInterfaceAction.To_activate"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
		}
		fRefactoring= null;
		selectionChanged(selection);
	}
		
	private boolean canEnable(IStructuredSelection selection){
		try {
			if (selection.isEmpty() || selection.size() != 1) 
				return false;
			
			Object first= selection.getFirstElement();
			if (first instanceof IType)
				return shouldAcceptElement((IType)first);
			if (first instanceof ICompilationUnit)	
				return shouldAcceptElement(JavaElementUtil.getMainType((ICompilationUnit)first));
			return false;
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;	
		}
	}
		
	private boolean canRun(){
		IJavaElement[] elements= resolveElements();
		if (elements.length != 1)
			return false;

		return (elements[0] instanceof IType) && shouldAcceptElement((IType)elements[0]);
	}

	private boolean shouldAcceptElement(IType type) {
		if (type == null || ! type.exists())
			return false;
		try{
			fRefactoring= new ExtractInterfaceRefactoring(type, JavaPreferencesSettings.getCodeGenerationSettings());
			return fRefactoring.checkPreactivation().isOK();
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}	
	}
		
	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
	}

	private RefactoringWizard createWizard(){
		return new ExtractInterfaceWizard(fRefactoring);
	}
	
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), fRefactoring.getInputType()))
			return;
		try{
			Object newElementToProcess= new RefactoringStarter().activate(fRefactoring, createWizard(), getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
			if (newElementToProcess == null)
				return;
			IStructuredSelection mockSelection= new StructuredSelection(newElementToProcess);
			selectionChanged(mockSelection);
			if (isEnabled())
				run(mockSelection);
			else
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("ExtractInterfaceAction.Refactoring"), RefactoringMessages.getString("ExtractInterfaceAction.not_possible")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}}
