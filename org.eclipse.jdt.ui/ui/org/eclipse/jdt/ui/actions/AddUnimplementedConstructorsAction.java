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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Evaluates constructors needed.
 * Will open an editor for the type. Changes are unsaved.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class AddUnimplementedConstructorsAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddUnimplementedConstructorsAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddUnimplementedConstructorsAction(UnifiedSite site) {
		super(site);
		setText(ActionMessages.getString("AddUnimplementedConstructorsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddUnimplementedConstructorsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddUnimplementedConstructorsAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		boolean enabled= false;
		try {
			enabled= getSelectedType(selection) != null;
		} catch (JavaModelException e) {
		}
		setEnabled(enabled);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
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
				MessageDialog.openError(shell, ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"), ActionMessages.getString("AddUnimplementedConstructorsAction.error.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(type, settings, false);
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
				dialog.run(false, true, new WorkbenchRunnableAdapter(op));
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					MessageDialog.openInformation(shell, ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-2$ //$NON-NLS-1$
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(shell, ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			ErrorDialog.openError(shell, ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"), null, e.getStatus()); //$NON-NLS-1$
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
}