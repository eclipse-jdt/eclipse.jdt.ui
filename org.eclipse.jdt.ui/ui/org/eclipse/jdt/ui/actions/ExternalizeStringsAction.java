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

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Externalizes the strings of a compilation unit. Opens a wizard that
 * gathers additional information to externalize the strings.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>ICompilationUnit</code>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class ExternalizeStringsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Creates a new <code>ExternalizeStringsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ExternalizeStringsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("ExternalizeStringsAction.label")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.EXTERNALIZE_STRINGS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ExternalizeStringsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getCompilationUnit(selection) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		IJavaElement element= SelectionConverter.getInput(fEditor);
		if (!(element instanceof ICompilationUnit))
			return;
		run((ICompilationUnit)element);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		run(getCompilationUnit(selection));
	}	
	
	private void run(ICompilationUnit unit) {
		if (!ActionUtil.isProcessable(getShell(), unit))
			return;
		try {
			openExternalizeStringsWizard(unit);
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("ExternalizeStringsAction.dialog.message")); //$NON-NLS-1$
		}
	}

	private ICompilationUnit getCompilationUnit(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (first instanceof ICompilationUnit) 
			return (ICompilationUnit) first;
		if (first instanceof IType)
			return ((IType) first).getCompilationUnit();
		return null;
	}
	
	private static Refactoring createNewRefactoringInstance(ICompilationUnit cu) {
		return NLSRefactoring.create(cu, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	/* package */ static void openExternalizeStringsWizard(ICompilationUnit unit) throws JavaModelException {
		if (unit == null)
			return;
		
		Refactoring refactoring= createNewRefactoringInstance(unit);
		if (refactoring == null)
			return;
		ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
		new RefactoringStarter().activate(refactoring, wizard, JavaPlugin.getActiveWorkbenchShell(), getDialogTitle(), true); 
	}	
	
	private static String getDialogTitle() {
		return ActionMessages.getString("ExternalizeStringsAction.dialog.title"); //$NON-NLS-1$
	}		
}
