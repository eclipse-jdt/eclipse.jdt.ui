/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.AddMethodStubOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Creates method stubs in a type.
 * The type has to be set before usage (init)
 * Always forces the type to open in an editor. The result is unsaved,
 * so the user can decide if the changes are acceptable.
 */
public class AddMethodStubAction extends Action {

	private ISelection fSelection;
	private IType fParentType;

	public AddMethodStubAction() {
		super(JavaUIMessages.AddMethodStubAction_label); 
		setDescription(JavaUIMessages.AddMethodStubAction_description); 
		setToolTipText(JavaUIMessages.AddMethodStubAction_tooltip); 
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_METHODSTUB_ACTION);		
	}

	
	public boolean init(IType parentType, ISelection selection) {
		if (canActionBeAdded(parentType, selection)) {
			fParentType= parentType;
			fSelection= selection;
			if (parentType != null) {
				try {
					if (parentType.isInterface()) {
						setText(Messages.format(JavaUIMessages.AddMethodStubAction_detailed_implement, parentType.getElementName())); 
					} else {
						setText(Messages.format(JavaUIMessages.AddMethodStubAction_detailed_override, parentType.getElementName())); 
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			} else {
				setText(JavaUIMessages.AddMethodStubAction_label); 
			}
			return true;
		}
		fParentType= null;
		fSelection= null;
		return false;
	}	

	public void run() {
		if (!canActionBeAdded(fParentType, fSelection)) {
			return;
		}
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {		
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(fParentType);
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fParentType.getJavaProject());

			List list= ((IStructuredSelection)fSelection).toList();	
			IMethod[] methods= (IMethod[]) list.toArray(new IMethod[list.size()]); 
			AddMethodStubOperation op= new AddMethodStubOperation(fParentType, methods, settings, createOverrideQuery(), createReplaceQuery(), false);
		
			PlatformUI.getWorkbench().getProgressService().runInUI(
				PlatformUI.getWorkbench().getProgressService(),
				new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
				op.getScheduleRule());
			
			IMethod[] res= op.getCreatedMethods();
			if (res != null && res.length > 0 && editor != null) {
				EditorUtility.revealInEditor(editor, res[0]);
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, shell, JavaUIMessages.AddMethodStubAction_error_title, null); 
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, JavaUIMessages.AddMethodStubAction_error_title, null); 
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}
	
	private IRequestQuery createOverrideQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String declTypeName= JavaElementLabels.getElementLabel(method.getDeclaringType(), 0);
				String formattedMessage;
				try {
					if (Flags.isFinal(method.getFlags())) {
						formattedMessage= Messages.format(JavaUIMessages.AddMethodStubAction_OverridesFinalDialog_message, new String[] { methodName, declTypeName }); 
					} else {
						formattedMessage= Messages.format(JavaUIMessages.AddMethodStubAction_OverridesPrivateDialog_message, new String[] { methodName, declTypeName }); 
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
					return IRequestQuery.CANCEL;
				}
				return showQueryDialog(formattedMessage);	
			}
		};
	}
	
	private IRequestQuery createReplaceQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= Messages.format(JavaUIMessages.AddMethodStubAction_ReplaceExistingDialog_message, methodName); 
				return showQueryDialog(formattedMessage);	
			}
		};
	}
	
	
	private int showQueryDialog(final String message) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19367
		int[] returnCodes= {IRequestQuery.YES, IRequestQuery.YES_ALL, IRequestQuery.NO, IRequestQuery.CANCEL};
		final Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell == null) {
			JavaPlugin.logErrorMessage("AddMethodStubAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}
		final int[] result= { Window.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= JavaUIMessages.AddMethodStubAction_QueryDialog_title; 
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, options, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	
	
	/**
	  * Tests if the action can run with given arguments
	 */
	public static boolean canActionBeAdded(IType parentType, ISelection selection) {
		if (parentType == null || parentType.getCompilationUnit() == null || !JavaModelUtil.isEditable(parentType.getCompilationUnit()) ||
				!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return false;
		}

		Object[] elems= ((IStructuredSelection)selection).toArray();
		int nSelected= elems.length;
		if (nSelected > 0) {
			for (int i= 0; i < nSelected; i++) {
				Object elem= elems[i];
				if (!(elem instanceof IMethod)) {
					return false;
				}
				IMethod meth= (IMethod)elem;
				if (meth.getDeclaringType().equals(parentType)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	

}
