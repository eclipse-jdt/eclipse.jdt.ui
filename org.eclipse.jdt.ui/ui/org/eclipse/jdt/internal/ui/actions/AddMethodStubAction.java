/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.codemanipulation.AddMethodStubOperation;
import org.eclipse.jdt.internal.ui.codemanipulation.AddMethodStubOperation.IRequestQuery;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;


/**
 * Creates Method Stubs in a type for selected methods.
 * The type has to be set before usage (setParentType)
 * Always forces the he type to be in an open editor. The result is unsaved,
 * so the user can decide if he wants to accept the changes
 */
public class AddMethodStubAction extends Action {

	private ISelection fSelection;
	private IType fParentType;

	public AddMethodStubAction() {
		super(JavaUIMessages.getString("AddMethodStubAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddMethodStubAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddMethodStubAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_METHODSTUB_ACTION });		
	}

	
	public boolean init(IType parentType, ISelection selection) {
		if (canActionBeAdded(parentType, selection)) {
			fParentType= parentType;
			fSelection= selection;
			if (parentType != null) {
				setText(JavaUIMessages.getFormattedString("AddMethodStubAction.detailedlabel", parentType.getElementName())); //$NON-NLS-1$
			} else {
				setText(JavaUIMessages.getString("AddMethodStubAction.label")); //$NON-NLS-1$
			}
			return true;
		} else {
			fParentType= null;
			fSelection= null;
		}
		return false;
	}	

	public void run() {
		if (fParentType == null || fParentType.getCompilationUnit() == null ||
				fSelection.isEmpty() || !(fSelection instanceof IStructuredSelection)) {
			return;
		}
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {		
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(fParentType);
			IType usedType= (IType)EditorUtility.getWorkingCopy(fParentType);
			if (usedType == null) {
				MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.ErrorDialog.title"), JavaUIMessages.getString("AddMethodStubAction.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
			
			ArrayList newMethods= new ArrayList(10);
			
			// remove the methods that already exist
			Iterator iter= ((IStructuredSelection)fSelection).iterator();
			while (iter.hasNext()) {
				Object obj= iter.next();
				if (obj instanceof IMethod) {
					newMethods.add(obj);
				}
			}			
			IMethod[] methods= (IMethod[]) newMethods.toArray(new IMethod[newMethods.size()]); 
			AddMethodStubOperation op= new AddMethodStubOperation(usedType, methods, createOverrideQuery(), createReplaceQuery(), false);
		
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(false, true, op);
			IMethod[] res= op.getCreatedMethods();
			if (res != null && res.length > 0 && editor != null) {
				//EditorUtility.openInEditor(res[0], true);
				EditorUtility.revealInEditor(editor, res[0]);
			}
		} catch (InvocationTargetException e) {
			MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.ActionFailed.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
			JavaPlugin.log(e.getTargetException());
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.ActionFailed.title"), null, e.getStatus()); //$NON-NLS-1$
			JavaPlugin.log(e.getStatus());
		} catch (PartInitException e) {
			MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.ActionFailed.title"), e.getMessage()); //$NON-NLS-1$
			JavaPlugin.log(e);
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}
	
	private IRequestQuery createOverrideQuery() {
		return new IRequestQuery() {
			public int doQuery(IMethod method) {
				JavaTextLabelProvider lprovider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS);
				String methodName= lprovider.getTextLabel(method);
				String declTypeName= lprovider.getTextLabel(method.getDeclaringType());
				String formattedMessage;
				try {
					if (Flags.isFinal(method.getFlags())) {
						formattedMessage= JavaUIMessages.getFormattedString("AddMethodStubAction.OverridesFinalDialog.message", new String[] { methodName, declTypeName }); //$NON-NLS-1$
					} else {
						formattedMessage= JavaUIMessages.getFormattedString("AddMethodStubAction.OverridesPrivateDialog.message", new String[] { methodName, declTypeName }); //$NON-NLS-1$
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
			public int doQuery(IMethod method) {
				JavaTextLabelProvider lprovider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS);
				String methodName= lprovider.getTextLabel(method);
				String formattedMessage= JavaUIMessages.getFormattedString("AddMethodStubAction.ReplaceExistingDialog.message", methodName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage);	
			}
		};
	}
	
	
	private int showQueryDialog(final String message) {
		int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.ALL, IRequestQuery.CANCEL};
		final Shell shell= JavaPlugin.getActiveWorkbenchShell();
		final int[] result= { MessageDialog.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= JavaUIMessages.getString("AddMethodStubAction.QueryDialog.title"); //$NON-NLS-1$
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, options, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	
	
	public static boolean canActionBeAdded(IType parentType, ISelection selection) {
		if (parentType == null || parentType.getCompilationUnit() == null ||
				selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
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