/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Creates unimplemented constructors for a type.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class AddUnimplementedConstructorsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Creates a new <code>AddUnimplementedConstructorsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddUnimplementedConstructorsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddUnimplementedConstructorsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddUnimplementedConstructorsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddUnimplementedConstructorsAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddUnimplementedConstructorsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		boolean enabled= false;
		try {
			enabled= getSelectedType(selection) != null;
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);			
		}
		setEnabled(enabled);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(IStructuredSelection selection) {
		Shell shell= getShell();
		try {
			IType type= getSelectedType(selection);
			if (type == null) {
				return;
			}		
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(type);
			type= (IType)EditorUtility.getWorkingCopy(type);
			
			if (type == null) {
				MessageDialog.openError(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.type_removed_in_editor")); //$NON-NLS-1$
				return;
			}
			
			run(shell, type, editor, false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null);
		}			
	}

	//---- Java Editior --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(ITextSelection selection) {
		Shell shell= getShell();
		try {
			IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null)
				run(shell, type, fEditor, true);
			else
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.not_applicable")); //$NON-NLS-1$
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	//---- Helpers -------------------------------------------------------------------
	
	private void run(Shell shell, IType type, IEditorPart editor, boolean activatedFromEditor) {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;		
		}

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(type, settings, false);

		IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
		if (target != null) {
			target.beginCompoundChange();
		}
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(false, true, new WorkbenchRunnableAdapter(op));
			IMethod[] res= op.getCreatedMethods();
			if (res == null || res.length == 0) {
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-1$
			} else if (editor != null) {
				EditorUtility.revealInEditor(editor, res[0]);
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null);
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		} finally {
			if (target != null) {
				target.endCompoundChange();
			}
		}
	}
		
	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		}
		return null;
	}	
	
	private String getDialogTitle() {
		return ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"); //$NON-NLS-1$
	}	
}