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

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.MoveInstanceMethodAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.MoveStaticMembersAction;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

/**
 * This action moves Java elements to a new location. The action prompts
 * the user for the new location.
 * <p>
 * The action is applicable to a homogenous selection containing either
 * projects, package fragment roots, package fragments, compilation units,
 * or static methods.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class MoveAction extends SelectionDispatchAction{
	
	private CompilationUnitEditor fEditor;
	private SelectionDispatchAction fMoveInstanceMethodAction;
	private SelectionDispatchAction fMoveStaticMembersAction;
	private SelectionDispatchAction fJdtMoveAction;
	
	/**
	 * Creates a new <code>MoveAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public MoveAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("MoveAction.text")); //$NON-NLS-1$
		fMoveStaticMembersAction= new MoveStaticMembersAction(site);
		fMoveInstanceMethodAction= new MoveInstanceMethodAction(site);
		fJdtMoveAction= new JdtMoveAction(site);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public MoveAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		setText(RefactoringMessages.getString("MoveAction.text")); //$NON-NLS-1$
		fMoveStaticMembersAction= new MoveStaticMembersAction(editor);
		fMoveInstanceMethodAction= new MoveInstanceMethodAction(editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}	

	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fMoveStaticMembersAction.selectionChanged(event);
		fMoveInstanceMethodAction.selectionChanged(event);
		if (fJdtMoveAction != null)
			fJdtMoveAction.selectionChanged(event);
		setEnabled(computeEnableState());	
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		assertThatExactlyOneIsEnabled();
		
		if (fJdtMoveAction != null && fJdtMoveAction.isEnabled())
			fJdtMoveAction.run();
		else if (fMoveInstanceMethodAction.isEnabled()) 
			fMoveInstanceMethodAction.run();	
		else if (fMoveStaticMembersAction.isEnabled())	
			fMoveStaticMembersAction.run();
	}

	private void assertThatExactlyOneIsEnabled() {
		Assert.isTrue(! fMoveInstanceMethodAction.isEnabled() || ! fMoveStaticMembersAction.isEnabled());
		if (fJdtMoveAction != null){
			Assert.isTrue(! fJdtMoveAction.isEnabled() || ! fMoveInstanceMethodAction.isEnabled());
			Assert.isTrue(! fJdtMoveAction.isEnabled() || ! fMoveStaticMembersAction.isEnabled());
			Assert.isTrue(fJdtMoveAction.isEnabled() || fMoveInstanceMethodAction.isEnabled() || fMoveStaticMembersAction.isEnabled());
		} else
			Assert.isTrue(fMoveInstanceMethodAction.isEnabled() || fMoveStaticMembersAction.isEnabled());
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	protected void run(ITextSelection selection) {
		if (fJdtMoveAction != null && fJdtMoveAction.isEnabled()){
			fJdtMoveAction.run();
			return;
		}
		if (fMoveInstanceMethodAction.isEnabled() && tryMoveInstanceMethod(selection))
			return;
			
		if (fMoveStaticMembersAction.isEnabled() && tryMoveStaticMembers(selection))
			return;
	
		MessageDialog.openInformation(getShell(), RefactoringMessages.getString("MoveAction.Move"), RefactoringMessages.getString("MoveAction.select")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private boolean tryMoveStaticMembers(ITextSelection selection) {
		try {
			IJavaElement[] elements= SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
			if (elements.length != 1)
				return false;
			
			if (! (elements[0] instanceof IMember))
				return false;
			
			MoveStaticMembersRefactoring refactoring= new MoveStaticMembersRefactoring(new IMember[]{(IMember)elements[0]}, JavaPreferencesSettings.getCodeGenerationSettings());
			if (! refactoring.checkPreactivation().isOK())
				return false;
			fMoveStaticMembersAction.run(selection);
			return true;
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	private boolean tryMoveInstanceMethod(ITextSelection selection) {
		try {
			MoveInstanceMethodRefactoring refactoring= MoveInstanceMethodRefactoring.create(
				getCompilationUnitForTextSelection(), selection.getOffset(), selection.getLength(),
				JavaPreferencesSettings.getCodeGenerationSettings());
			if (refactoring == null)
				return false;
			if (refactoring.checkActivation(new NullProgressMonitor()).hasFatalError())
				return false;
			fMoveInstanceMethodAction.run(selection);	
			return true;
		} catch (JavaModelException e) {
			return false;
		}
	}

	private ICompilationUnit getCompilationUnitForTextSelection() {
		Assert.isNotNull(fEditor);
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	/*
	 * @see SelectionDispatchAction#update(ISelection)
	 */
	public void update(ISelection selection) {
		fMoveStaticMembersAction.update(selection);
		fMoveInstanceMethodAction.update(selection);
		if (fJdtMoveAction != null)
			fJdtMoveAction.update(selection);
		setEnabled(computeEnableState());
	}
	
	private boolean computeEnableState(){
		if (fMoveStaticMembersAction.isEnabled() || fMoveInstanceMethodAction.isEnabled())
			return true;
		else if (fJdtMoveAction == null)
			return false;
		else  
			return fJdtMoveAction.isEnabled();
	}
}