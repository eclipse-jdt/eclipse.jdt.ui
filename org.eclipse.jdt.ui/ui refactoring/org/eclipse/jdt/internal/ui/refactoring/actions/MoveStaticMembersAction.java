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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class MoveStaticMembersAction extends SelectionDispatchAction{
	
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
		if (! (element instanceof IMember))
			return false;
		return MoveStaticMembersRefactoring.isAvailable(new IMember[] {(IMember)element});
	}
	
	public void run(IStructuredSelection selection) {
		try {
			IMember[] members= getSelectedMembers(selection);
			if (canEnable(members))
				startRefactoring(members);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(getShell(), fEditor))
				return;
			IMember member= getSelectedMember();
			IMember[] array= new IMember[]{member};
			if (member != null && canEnable(array)){
				startRefactoring(array);	
			} else {
				String unavailable= RefactoringMessages.getString("MoveMembersAction.unavailable"); //$NON-NLS-1$;
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private static IMember[] getSelectedMembers(IStructuredSelection selection){
		if (selection.isEmpty())
			return null;
		
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (! (iter.next() instanceof IMember))
				return null;
		}
		return convertToMemberArray(selection.toArray());
	}

	private static IMember[] convertToMemberArray(Object[] obj) {
		if (obj == null)
			return null;
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(obj));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}
	
	private static boolean canEnable(IMember[] members) throws JavaModelException {
		return MoveStaticMembersRefactoring.isAvailable(members);
	}
	
	private IMember getSelectedMember() throws JavaModelException{
		IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
		if (element == null || ! (element instanceof IMember))
			return null;
		return (IMember)element;
	}

	private static MoveStaticMembersRefactoring createNewRefactoringInstance(Object[] elements) throws JavaModelException{
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(elements));
		IMember[] methods= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		return MoveStaticMembersRefactoring.create(methods, JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private static RefactoringWizard createWizard(MoveStaticMembersRefactoring refactoring){
		return new MoveMembersWizard(refactoring);
	}
	
	private void startRefactoring(IMember[] members) throws JavaModelException {
		MoveStaticMembersRefactoring refactoring= createNewRefactoringInstance(members);
		Assert.isNotNull(refactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), refactoring.getMembersToMove()[0]))
			return;
		
		new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), 
			RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
	}
}
