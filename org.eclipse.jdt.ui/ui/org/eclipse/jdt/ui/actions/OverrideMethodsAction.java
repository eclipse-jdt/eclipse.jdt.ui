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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Adds unimplemented methods of a type. The action opens a dialog from which the user can
 * choose the methods to be added.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is unsaved, so the
 * user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * {@link org.eclipse.jdt.core.IType}.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OverrideMethodsAction extends SelectionDispatchAction {

	/** The dialog title */
	private static final String DIALOG_TITLE= ActionMessages.getString("OverrideMethodsAction.error.title"); //$NON-NLS-1$

	/** The compilation unit editor */
	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 */
	public OverrideMethodsAction(final CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	/**
	 * Creates a new override method action.
	 * <p>
	 * The action requires that the selection provided by the site's selection provider is
	 * of type {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site the workbench site providing context information for this action
	 */
	public OverrideMethodsAction(final IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OverrideMethodsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OverrideMethodsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OverrideMethodsAction.tooltip")); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION);
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			final IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && !type.isAnnotation() && !type.isInterface();
		}
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;
		return false;
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	private String getDialogTitle() {
		return DIALOG_TITLE;
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		final Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			final IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && !type.isAnnotation() && !type.isInterface()) {
				return type;
			}
		} else if (elements[0] instanceof ICompilationUnit) {
			final IType type= ((ICompilationUnit) elements[0]).findPrimaryType();
			if (!type.isAnnotation() && !type.isInterface())
				return type;
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			final IType type= getSelectedType(selection);
			if (type == null) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.not_applicable")); //$NON-NLS-1$
				return;
			}
			if (!ElementValidator.check(type, getShell(), getDialogTitle(), false) || !ActionUtil.isProcessable(getShell(), type)) {
				return;
			}
			if (type == null) {
				MessageDialog.openError(getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.type_removed_in_editor")); //$NON-NLS-1$
				return;
			}
			run(getShell(), type);
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			final IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null) {
				if (!ElementValidator.check(type, getShell(), getDialogTitle(), false) || !ActionUtil.isProcessable(getShell(), type)) {
					return;
				}
				if (type.isInterface() || type.isAnnotation()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.not_applicable")); //$NON-NLS-1$
					return;
				}
				run(getShell(), type);
			} else {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.not_applicable")); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	private void run(Shell shell, IType type) throws JavaModelException, CoreException {
		final OverrideMethodDialog dialog= new OverrideMethodDialog(shell, fEditor, type, false);
		String[] keys= null;
		if (!dialog.hasMethodsToOverride()) {
			MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}
		if (dialog.open() == Window.OK) {
			final Object[] selected= dialog.getResult();
			if (selected == null)
				return;
			final List list= new ArrayList(selected.length);
			for (int index= 0; index < selected.length; index++) {
				if (selected[index] instanceof IMethodBinding)
					list.add(((IBinding) selected[index]).getKey());
			}
			keys= (String[]) list.toArray(new String[list.size()]);
			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
			settings.createComments= dialog.getGenerateComment();
			final IEditorPart editor= EditorUtility.openInEditor(type.getCompilationUnit());
			final IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null)
				target.beginCompoundChange();
			try {
				final AddUnimplementedMethodsOperation operation= new AddUnimplementedMethodsOperation(type, dialog.getElementPosition(), keys, settings, dialog.getGenerateAnnotation(), false);
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null)
					context= new BusyIndicatorRunnableContext();
				PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
				final String[] created= operation.getCreatedMethods();
				if (created == null || created.length == 0)
					MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.nothing_found")); //$NON-NLS-1$
			} catch (InvocationTargetException exception) {
				ExceptionHandler.handle(exception, shell, getDialogTitle(), null);
			} catch (InterruptedException exception) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null)
					target.endCompoundChange();
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException exception) {
			if (JavaModelUtil.filterNotPresentException(exception))
				JavaPlugin.log(exception);
			setEnabled(false);
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		// Do nothing
	}
}