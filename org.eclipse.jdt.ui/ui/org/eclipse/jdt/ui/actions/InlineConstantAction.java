/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.InlineConstantWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Inlines a constant.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class InlineConstantAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	private static final String DIALOG_TITLE= "Inline Constant";

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineConstantAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public InlineConstantAction(IWorkbenchSite site) {
		super(site);
		setText("Inline &Constant...");
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_CONSTANT_ACTION);		
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

    /*
     * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
     */
    protected void selectionChanged(ITextSelection selection) {
       //do nothing
    }

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		if (selection.isEmpty() || selection.size() != 1) 
			return false;
		
		Object first= selection.getFirstElement();
		return (first instanceof IField) && shouldAcceptElement((IField)first);
	}

	private boolean shouldAcceptElement(IField field) throws JavaModelException {
		return (! field.isBinary() && JdtFlags.isStatic(field) && JdtFlags.isFinal(field));
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {		
		try {
			Assert.isTrue(canEnable(selection));
			
			Object first= selection.getFirstElement();
			Assert.isTrue(first instanceof IField);
			
			IField field= (IField) first;
			run(field.getNameRange().getOffset(), field.getNameRange().getLength(), field.getCompilationUnit());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");	
		}
 	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		run(selection.getOffset(), selection.getLength(), getCompilationUnitForTextSelection());
	}
	
	private void run(int selectionOffset, int selectionLength, ICompilationUnit cu) {
		Assert.isNotNull(cu);
		Assert.isTrue(selectionOffset >= 0);
		Assert.isTrue(selectionLength >= 0);
		
		InlineConstantRefactoring refactoring= InlineConstantRefactoring.create(
			cu, selectionOffset, selectionLength,
			JavaPreferencesSettings.getCodeGenerationSettings());
		if (refactoring == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, "No constant reference or declaration selected.");
			return;
		}
		try {
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), DIALOG_TITLE, true);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");	
		}		
	}
	
	private ICompilationUnit getCompilationUnitForTextSelection() {
		Assert.isNotNull(fEditor);
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private RefactoringWizard createWizard(InlineConstantRefactoring refactoring) {
		return new InlineConstantWizard(refactoring);
	}
}
