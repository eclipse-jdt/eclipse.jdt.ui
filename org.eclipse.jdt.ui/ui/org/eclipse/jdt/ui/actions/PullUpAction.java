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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Action to pull up method and fields into a superclass.
 * <p>
 * Action is applicable to selections containing elements of
 * type <code>IField</code> and <code>IMethod</code>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class PullUpAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	/**
	 * Creates a new <code>PullUpAction</code>. The action requires that the selection 
	 * provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public PullUpAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RefactoringGroup.pull_Up_label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.PULL_UP_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public PullUpAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	//---- structured selection -----------------------------------------------

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(getSelectedMembers(selection)));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			IMember[] members= getSelectedMembers(selection);
			if (canEnable(members))
				startRefactoring(members);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	//---- text selection -----------------------------------------------------
	
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
		IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return PullUpRefactoring.isAvailable(new IMember[] {(IMember)element});
	}
	
	/*
	 * @see SelectionDispatchAction#run(ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(getShell(), fEditor))
				return;
			IMember member= getSelectedMember();
			IMember[] array= new IMember[]{member};
			if (member != null && canEnable(array)){
				startRefactoring(array);	
			} else {
				String unavailable= RefactoringMessages.getString("PullUpAction.unavailable"); //$NON-NLS-1$
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	//---- private helper methods ------------------------------------------------
		
	private static IMember[] getSelectedMembers(IStructuredSelection selection) {
		if (selection.isEmpty())
			return null;
		
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (! (iter.next() instanceof IMember))
				return null;
		}
		return convertToMemberArray(selection.toArray());
	}
		
	private static boolean canEnable(IMember[] members) throws JavaModelException {
		return PullUpRefactoring.isAvailable(members);
	}
			
	private IMember getSelectedMember() throws JavaModelException {
		IJavaElement element= SelectionConverter.resolveEnclosingElement(
			fEditor, (ITextSelection)fEditor.getSelectionProvider().getSelection());
		if (element == null || ! (element instanceof IMember))
			return null;
		return (IMember)element;
	}

	private static PullUpRefactoring createNewRefactoringInstance(IMember[] members) throws JavaModelException {
		return PullUpRefactoring.create(members, JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private static IMember[] convertToMemberArray(Object[] obj) {
		if (obj == null)
			return null;
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(obj));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}

	private static RefactoringWizard createWizard(PullUpRefactoring refactoring) {
		return new PullUpWizard(refactoring);
	}
		
	private void startRefactoring(IMember[] members) throws JavaModelException {
		PullUpRefactoring refactoring= createNewRefactoringInstance(members);
		Assert.isNotNull(refactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), refactoring.getDeclaringType()))
			return;
	
		Object newElementToProcess= new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
		if (newElementToProcess == null)
			return;
		IStructuredSelection mockSelection= new StructuredSelection(newElementToProcess);
		selectionChanged(mockSelection);
		if (isEnabled())
			run(mockSelection);
		else
			MessageDialog.openInformation(getShell(), ActionMessages.getString("PullUpAction.problem.title"), ActionMessages.getString("PullUpAction.problem.message"));	 //$NON-NLS-1$ //$NON-NLS-2$
	}	
}
