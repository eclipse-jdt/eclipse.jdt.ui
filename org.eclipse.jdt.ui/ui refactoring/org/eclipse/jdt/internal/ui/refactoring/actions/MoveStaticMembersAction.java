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
package org.eclipse.jdt.internal.ui.refactoring.actions;

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

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class MoveStaticMembersAction extends SelectionDispatchAction{
	
	private MoveStaticMembersRefactoring fRefactoring;
	private CompilationUnitEditor fEditor;

	public MoveStaticMembersAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RefactoringGroup.move_label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);		
	}

	public MoveStaticMembersAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
		
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
		if (! isEnabled())
			fRefactoring= null;
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
	protected void run(ITextSelection selection) {
		if (canRun()){
			startRefactoring();	
		} else {
			String unavailable= RefactoringMessages.getString("MoveMembersAction.unavailable"); //$NON-NLS-1$;
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
		}
		fRefactoring= null;
		selectionChanged(selection);
	}
		
	private boolean canEnable(IStructuredSelection selection){
		if (selection.isEmpty())
			return false;
		
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (! (iter.next() instanceof IMember))
				return false;
		}
		return shouldAcceptElements(selection.toArray());
	}
		
	private boolean canRun(){
		try {
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element == null)
				return false;
			return (element instanceof IMember) && shouldAcceptElements(new IJavaElement[]{element});
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}
	}

	private MoveStaticMembersRefactoring createNewRefactoringInstance(Object[] elements){
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(elements));
		IMember[] methods= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		return new MoveStaticMembersRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private boolean shouldAcceptElements(Object[] elements) {
		try{
			fRefactoring= createNewRefactoringInstance(elements);
			return fRefactoring.checkPreactivation().isOK();
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}	
	}
		
	private RefactoringWizard createWizard(){
		String title= RefactoringMessages.getString("RefactoringGroup.move_Members"); //$NON-NLS-1$
		String helpId= IJavaHelpContextIds.MOVE_MEMBERS_ERROR_WIZARD_PAGE;
		return new MoveMembersWizard(fRefactoring, title, helpId);
	}
	
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), fRefactoring.getMovedMembers()[0]))
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
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("MoveMembersAction.error.title"), RefactoringMessages.getString("MoveMembersAction.error.message"));	 //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
