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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.MoveMembersAction;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;

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
	
	private SelectionDispatchAction fMoveMembersAction;
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
		fMoveMembersAction= new MoveMembersAction(site);
		fJdtMoveAction= new JdtMoveAction(site);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public MoveAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("MoveAction.text")); //$NON-NLS-1$
		fMoveMembersAction= new MoveMembersAction(editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}	

	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fMoveMembersAction.selectionChanged(event);
		if (fJdtMoveAction != null)
			fJdtMoveAction.selectionChanged(event);
		setEnabled(computeEnableState());	
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		if (fJdtMoveAction != null && fJdtMoveAction.isEnabled())
			fJdtMoveAction.run();
		else if (fMoveMembersAction.isEnabled())	
			fMoveMembersAction.run();
	}
	
	/*
	 * @see SelectionDispatchAction#update(ISelection)
	 */
	public void update(ISelection selection) {
		fMoveMembersAction.update(selection);
		if (fJdtMoveAction != null)
			fJdtMoveAction.update(selection);
		setEnabled(computeEnableState());
	}
	
	private boolean computeEnableState(){
		if (fJdtMoveAction == null)	
			return fMoveMembersAction.isEnabled();
		else
			return fMoveMembersAction.isEnabled() || fJdtMoveAction.isEnabled();
	}
}