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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ChangeSignatureWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui here - happens on selection changes
		}
	}

    /*
     * @see SelectionDispatchAction#selectionChanged(ITextSelection)
     */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
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
		if (elements.length != 1)
			return false;
		if (! (elements[0] instanceof IMethod))
			return false;
		return ChangeSignatureRefactoring.isAvailable((IMethod)elements[0]);
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			//we have to call this here - no selection changed event is sent after a refactoring but it may still invalidate enablement
			if (canEnable(selection)) 
				startRefactoring(getSingleSelectedMethod(selection));
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(getShell(), fEditor))
				return;
			IMethod singleSelectedMethod= getSingleSelectedMethod(selection);
			if (canRunOn(singleSelectedMethod)){
				startRefactoring(singleSelectedMethod);
			} else {
				String unavailable= RefactoringMessages.getString("ModifyParametersAction.unavailable"); //$NON-NLS-1$
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
		
	private static boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		return canRunOn(getSingleSelectedMethod(selection));
	}
	
	private static IMethod getSingleSelectedMethod(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		if (selection.getFirstElement() instanceof IMethod)
			return (IMethod)selection.getFirstElement();
		return null;
	}
	
	private IMethod getSingleSelectedMethod(ITextSelection selection) throws JavaModelException{
		IJavaElement[] elements= resolveElements();
		if (elements.length != 1)
			return null;
		if (elements[0] instanceof IMethod)
			return (IMethod)elements[0];
		IJavaElement elementAt= SelectionConverter.getInputAsCompilationUnit(fEditor).getElementAt(selection.getOffset());
		if (elementAt instanceof IMethod)
			return (IMethod)elementAt;
		return null;
	}
	
	private static boolean canRunOn(IMethod method) throws JavaModelException{
		return ChangeSignatureRefactoring.isAvailable(method);
	}
	
	private static ChangeSignatureRefactoring createRefactoring(IMethod method) throws JavaModelException{
		return ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
	}

	private static RefactoringWizard createWizard(ChangeSignatureRefactoring refactoring){
		return new ChangeSignatureWizard(refactoring);
	}
	
	private void startRefactoring(IMethod method) throws JavaModelException {
		ChangeSignatureRefactoring refactoring= createRefactoring(method); 
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
			MessageDialog.openInformation(getShell(), ActionMessages.getString("ModifyParameterAction.problem.title"), ActionMessages.getString("ModifyParameterAction.problem.message"));	 //$NON-NLS-1$ //$NON-NLS-2$
	}
}
