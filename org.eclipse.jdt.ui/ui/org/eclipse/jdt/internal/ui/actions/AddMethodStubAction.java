/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.codemanipulation.AddMethodStubOperation;
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;


/**
 * Creates Method Stubs in a type for selected methods.
 * The type has to be set before usage (setParentType)
 * Always forces the he type to be in an open editor. The result is unsaved,
 * so the user can decide if he wants to accept the changes
 */
public class AddMethodStubAction extends Action {

	private ISelectionProvider fSelectionProvider;
	private IType fParentType;

	public AddMethodStubAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddMethodStubAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddMethodStubAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddMethodStubAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;

		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_METHODSTUB_ACTION });		
	}

	
	public void setParentType(IType parentType) {
		fParentType= parentType;
		if (parentType != null) {
			setText(JavaUIMessages.getFormattedString("AddMethodStubAction.detailedlabel", parentType.getElementName())); //$NON-NLS-1$
		} else {
			setText(JavaUIMessages.getString("AddMethodStubAction.label")); //$NON-NLS-1$
		}
	} 

	public void run() {
		if (fParentType == null || fParentType.getCompilationUnit() == null) {
			return;
		}
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		try {
			IType usedType= null;
			
			IEditorPart editor= EditorUtility.isOpenInEditor(fParentType.getCompilationUnit());
			if (editor != null) {
				// work on the working copy
				usedType= EditorUtility.getWorkingCopy(fParentType);
				if (usedType == null) {
					MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.dialogTitle"), JavaUIMessages.getString("AddMethodStubAction.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
					return;
				}
			} else {
				usedType= fParentType;
			}	
	
			ISelection selection= fSelectionProvider.getSelection();
			Iterator iter= ((IStructuredSelection)selection).iterator();
		
			ArrayList newMethods= new ArrayList(10);	

			boolean overrideFinalMethod= false;
			ITypeHierarchy hierarchy= usedType.newSupertypeHierarchy(null);
			while (iter.hasNext()) {
				Object obj= iter.next();
				if (obj instanceof IMethod) {
					IMethod method= (IMethod)obj;
					if (StubUtility.findMethod(method, usedType) != null) {
						// do not add: exists already
					} else {
						if (!overrideFinalMethod) {
							IMethod overridden= StubUtility.findInHierarchy(hierarchy, method);
							if (overridden != null && Flags.isFinal(overridden.getFlags())) {
								if (showOverridesFinalDialog(shell, overridden)) {
									overrideFinalMethod= true;
								} else {
									return;
								}
							}
						}
						newMethods.add(method);
					}
				}
			}
				
			int nMethods= newMethods.size();
			if (nMethods == 0) {
				MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.dialogTitle"), JavaUIMessages.getString("AddMethodStubAction.method_exists")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
			
			IMethod[] methods= new IMethod[nMethods];
			newMethods.toArray(methods);
			
			if (usedType == fParentType) {
				// not yet open
				editor= EditorUtility.openInEditor(fParentType);
				usedType= EditorUtility.getWorkingCopy(fParentType);
				if (usedType == null) {
					MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.dialogTitle"), JavaUIMessages.getString("AddMethodStubAction.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
					return;
				}				
			}
		
		
			AddMethodStubOperation op= new AddMethodStubOperation(usedType, methods, false);
		
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(false, true, op);
			IMethod[] res= op.getCreatedMethods();
			if (res != null && res.length > 0 && editor != null) {
				EditorUtility.revealInEditor(editor, res[0]);
			}
		} catch (InvocationTargetException e) {
			MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.actionFailed"), e.getTargetException().getMessage()); //$NON-NLS-1$
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.actionFailed"), null, e.getStatus()); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		} catch (PartInitException e) {
			MessageDialog.openError(shell, JavaUIMessages.getString("AddMethodStubAction.actionFailed"), e.getMessage()); //$NON-NLS-1$
		}
	}
	
	private boolean showOverridesFinalDialog(Shell shell, IMethod overridden) {
		JavaTextLabelProvider provider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS); 	
		String methodName= provider.getTextLabel(overridden);
		String title= JavaUIMessages.getString("AddMethodStubAction.warning"); //$NON-NLS-1$
		String message= JavaUIMessages.getFormattedString("AddMethodStubAction.OverridesFinalDialog.message", methodName); //$NON-NLS-1$
		
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0); //$NON-NLS-2$ //$NON-NLS-1$
	 	return (dialog.open() == dialog.OK);
	}
	
	public boolean canActionBeAdded() {
		if (fParentType == null || fParentType.getCompilationUnit() == null) {
			return false;
		}
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Object[] vec= ((IStructuredSelection)sel).toArray();
			int nSelected= vec.length;
			if (nSelected > 0) {
				for (int i= 0; i < nSelected; i++) {
					Object elem= vec[i];
					if (!(elem instanceof IMethod)) {
						return false;
					}
					IMethod meth= (IMethod)elem;
					try {
						int flags= meth.getFlags();
						if (meth.isConstructor() || Flags.isStatic(flags) || Flags.isPrivate(flags)) {
							return false;
						}
					} catch (JavaModelException e) {
						// ignore
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	

}