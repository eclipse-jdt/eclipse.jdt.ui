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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.ModifyParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to start the modify parameters refactoring. The refactoring supports 
 * swapping and renaming of arguments.
 * <p>
 * This action is applicable to selections containing a method with one or
 * more arguments.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class ModifyParametersAction extends SelectionDispatchAction {
	
	private ModifyParametersRefactoring fRefactoring;
	private CompilationUnitEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ModifyParametersAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Creates a new <code>ModifyParametersAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ModifyParametersAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RefactoringGroup.modify_Parameters_label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MODIFY_PARAMETERS_ACTION);
	}
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

    /*
     * @see SelectionDispatchAction#selectionChanged(ITextSelection)
     */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		startRefactoring();
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	protected void run(ITextSelection selection) {
		if (! canRun(selection)){
			String unavailable= RefactoringMessages.getString("ModifyParametersAction.unavailable"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			fRefactoring= null;
			return;
		}
		startRefactoring();	
	}
		
	private boolean canEnable(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1) 
			return false;
		
		Object first= selection.getFirstElement();
		return (first instanceof IMethod) && shouldAcceptElement((IMethod)first);
	}
		
	private boolean canRun(ITextSelection selection){
		IJavaElement[] elements= resolveElements();
		if (elements.length > 1)
			return false;
		if (elements.length == 0){
			try {
				IJavaElement selected= SelectionConverter.getInputAsCompilationUnit(fEditor).getElementAt(selection.getOffset());
				return (selected instanceof IMethod && shouldAcceptElement((IMethod)selected));
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
		}		

		return (elements[0] instanceof IMethod) && shouldAcceptElement((IMethod)elements[0]);
	}

	private boolean shouldAcceptElement(IMethod method) {
		try{
			fRefactoring= new ModifyParametersRefactoring(method);
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
		String title= RefactoringMessages.getString("RefactoringGroup.modify_method_parameters"); //$NON-NLS-1$
		String helpId= IJavaHelpContextIds.MODIFY_PARAMETERS_ERROR_WIZARD_PAGE;
		return new ModifyParametersWizard(fRefactoring, title, helpId);
	}
	
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), fRefactoring.getMethod()))
			return;
		try{
			Object newElementToProcess= new RefactoringStarter().activate(fRefactoring, createWizard(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
			if (newElementToProcess == null)
				return;
			IStructuredSelection mockSelection= new StructuredSelection(newElementToProcess);
			selectionChanged(mockSelection);
			if (isEnabled())
				run(mockSelection);
			else
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), ActionMessages.getString("ModifyParameterAction.problem.title"), ActionMessages.getString("ModifyParameterAction.problem.message"));	 //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	

}
