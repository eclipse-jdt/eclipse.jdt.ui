/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.InlineConstantAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.InlineMethodAction;

/**
 * Inlines a method, local variable or a static final field.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class InlineAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private final InlineTempAction fInlineTemp;
	private final InlineMethodAction fInlineMethod;
	private final InlineConstantAction fInlineConstant;

	/**
	 * Creates a new <code>InlineAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public InlineAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("InlineAction.Inline")); //$NON-NLS-1$
		fInlineTemp		= new InlineTempAction(site);
		fInlineConstant	= new InlineConstantAction(site);
		fInlineMethod	= new InlineMethodAction(site);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineAction(CompilationUnitEditor editor) {
		//don't want to call 'this' here - it'd create useless action objects
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("InlineAction.Inline")); //$NON-NLS-1$
		fEditor= editor;
		fInlineTemp		= new InlineTempAction(editor);
		fInlineConstant	= new InlineConstantAction(editor);
		fInlineMethod	= new InlineMethodAction(editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
		setEnabled(getCompilationUnit() != null);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(ISelection selection) {
		fInlineConstant.update(selection);
		fInlineMethod.update(selection);
		fInlineTemp.update(selection);
		setEnabled(computeEnablementState());
	}

	private boolean computeEnablementState() {
		return fInlineTemp.isEnabled() || fInlineConstant.isEnabled() || fInlineMethod.isEnabled();
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;

		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) 
			return;
		
		if (fInlineTemp.isEnabled() && tryInlineTemp(cu, selection))
			return;

		if (fInlineConstant.isEnabled() && tryInlineConstant(cu, selection))
			return;
		
		//InlineMethod is last (also tries enclosing element):
		if (fInlineMethod.isEnabled() && tryInlineMethod(cu, selection))
			return;
		
		MessageDialog.openInformation(getShell(), RefactoringMessages.getString("InlineAction.dialog_title"), RefactoringMessages.getString("InlineAction.select")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private boolean tryInlineTemp(ICompilationUnit cu, ITextSelection selection){
		InlineTempRefactoring inlineTemp= InlineTempRefactoring.create(cu, selection.getOffset(), selection.getLength());
		if (inlineTemp == null)
			return false;
		fInlineTemp.run(selection);
		return true;
	}
	
	private boolean tryInlineConstant(ICompilationUnit cu, ITextSelection selection) {
		InlineConstantRefactoring inlineConstantRef= InlineConstantRefactoring.create(cu, selection.getOffset(), selection.getLength());
		if (inlineConstantRef == null)
			return false;
		fInlineConstant.run(selection);
		return true;
	}
	
	private boolean tryInlineMethod(ICompilationUnit cu, ITextSelection selection){
		InlineMethodRefactoring inlineMethodRef= InlineMethodRefactoring.create(
			cu, selection.getOffset(), selection.getLength());
		if (inlineMethodRef == null)	
			return false;
		fInlineMethod.run(selection);
		return true;
	}
	
	private ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		if (fInlineConstant.isEnabled())
			fInlineConstant.run(selection);
		else if (fInlineMethod.isEnabled())
			fInlineMethod.run(selection);
		else	
			//inline temp will never be enabled on IStructuredSelection
			//don't bother running it
			Assert.isTrue(! fInlineTemp.isEnabled());
	}
}
