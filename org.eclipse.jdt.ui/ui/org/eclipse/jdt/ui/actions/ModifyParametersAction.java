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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ChangeSignatureWizard;
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
		//do nothing, this happens too often
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		//we have to call this here - no selection changed event is sent after a refactoring but it may still invalidate enablement
		if (canEnable(selection)) 
			startRefactoring(getSingleSelectedElement(selection));
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	protected void run(ITextSelection selection) {
		try {
			IJavaElement singleSelectedElement= getSingleSelectedElement(selection);
			if (canRunOn(singleSelectedElement)){
				startRefactoring(singleSelectedElement);
			} else {
				String unavailable= RefactoringMessages.getString("ModifyParametersAction.unavailable"); //$NON-NLS-1$
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
		
	private boolean canEnable(IStructuredSelection selection){
		try{
			if (selection.isEmpty() || selection.size() != 1) 
				return false;
		
			return canRunOn(selection.getFirstElement());
		} catch (JavaModelException e){
			return false;//no ui here - happens on selection changes
		}
	}
	
	private static Object getSingleSelectedElement(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
	
		return selection.getFirstElement();
	}
	
	private IJavaElement getSingleSelectedElement(ITextSelection selection) throws JavaModelException{
		IJavaElement[] elements= resolveElements();
		if (elements.length > 1)
			return null;
		if (elements.length == 1)
			return elements[0];
		return SelectionConverter.getInputAsCompilationUnit(fEditor).getElementAt(selection.getOffset());
	}
	
	private boolean canRunOn(Object element) throws JavaModelException{
		return (element instanceof IMethod) && ChangeSignatureRefactoring.isAvailable((IMethod)element);
	}
	
	private static ChangeSignatureRefactoring createRefactoring(IMethod method) throws JavaModelException{
		return ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
	}

	private static RefactoringWizard createWizard(ChangeSignatureRefactoring refactoring){
		String title= RefactoringMessages.getString("RefactoringGroup.modify_method_parameters"); //$NON-NLS-1$
		String helpId= IJavaHelpContextIds.MODIFY_PARAMETERS_ERROR_WIZARD_PAGE;
		return new ChangeSignatureWizard(refactoring, title, helpId);
	}
	
	private void startRefactoring(Object element) {
		try{
			Assert.isTrue(element instanceof IMethod); //we're enabled so there's no other way
			ChangeSignatureRefactoring refactoring= createRefactoring((IMethod) element); 
			Assert.isNotNull(refactoring);
			// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
			if (!ActionUtil.isProcessable(getShell(), refactoring.getMethod()))
				return;
			Object newElementToProcess= new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
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
