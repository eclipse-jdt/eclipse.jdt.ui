/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
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
public class AddMethodStubAction extends JavaUIAction {

	private static final String PREFIX= "AddMethodStubAction.";
	private static final String NOTHINGADDED_PREFIX= PREFIX + "NothingAddedDialog.";
	private static final String OVERRIDESFINAL_PREFIX= PREFIX + "OverridesFinalDialog.";
	private static final String NOTINWORKINGCOPY_PREFIX= PREFIX + "NotInWorkingCopyDialog.";
	
	
	private ISelectionProvider fSelectionProvider;
	private IType fParentType;

	public AddMethodStubAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= selProvider;
	}

	
	public void setParentType(IType parentType) {
		fParentType= parentType;
		if (parentType != null) {
			setText(JavaPlugin.getFormattedString(PREFIX + "detailedlabel", parentType.getElementName()));
		} else {
			setText(JavaPlugin.getResourceString(PREFIX + "label"));
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
					showSimpleDialog(shell, NOTINWORKINGCOPY_PREFIX);
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
				showSimpleDialog(shell, NOTHINGADDED_PREFIX);
				return;
			}
			
			IMethod[] methods= new IMethod[nMethods];
			newMethods.toArray(methods);
			
			if (usedType == fParentType) {
				// not yet open
				editor= EditorUtility.openInEditor(fParentType);
				usedType= EditorUtility.getWorkingCopy(fParentType);
				if (usedType == null) {
					showSimpleDialog(shell, NOTINWORKINGCOPY_PREFIX);
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
			MessageDialog.openError(shell, "AddMethodStubAction failed", e.getTargetException().getMessage());
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, "AddMethodStubAction failed", null, e.getStatus());
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		} catch (PartInitException e) {
			MessageDialog.openError(shell, "AddMethodStubAction failed", e.getMessage());
		}
	}
			
	private void showSimpleDialog(Shell shell, String resourcePrefix) {
		JavaPlugin plugin= JavaPlugin.getDefault();
		String okLabel= plugin.getResourceString(IUIConstants.KEY_OK);
	
		String title= plugin.getResourceString(resourcePrefix + "title");
		String message= plugin.getResourceString(resourcePrefix + "message");
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { okLabel }, 0);
	 	dialog.open();
	}
	
	
	private boolean showOverridesFinalDialog(Shell shell, IMethod overridden) {
		JavaPlugin plugin= JavaPlugin.getDefault();
		String okLabel= plugin.getResourceString(IUIConstants.KEY_OK);
		String cancelLabel= plugin.getResourceString(IUIConstants.KEY_CANCEL);
	
	 	JavaTextLabelProvider provider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS); 	
		String methodName= provider.getTextLabel(overridden);
		String title= plugin.getResourceString(OVERRIDESFINAL_PREFIX + "title");
		String message= plugin.getFormattedString(OVERRIDESFINAL_PREFIX + "message", methodName);
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { okLabel, cancelLabel }, 0);
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