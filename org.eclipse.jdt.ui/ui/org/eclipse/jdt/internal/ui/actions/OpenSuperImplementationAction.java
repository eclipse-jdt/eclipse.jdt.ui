
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;


/**
 * On a selected method; opens the implementation in the super type (if existing)
 */
public class OpenSuperImplementationAction extends Action implements IUpdate {
	private int fFoo;
	private StructuredSelectionProvider fSelectionProvider;
	
	public OpenSuperImplementationAction(StructuredSelectionProvider provider) {
		super();
		setText(JavaUIMessages.getString("OpenSuperImplementationAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenSuperImplementationAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenSuperImplementationAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= provider;
	}
	
	public void update() {
		setEnabled(canOperateOn());
	}
	
	private boolean canOperateOn() {
		return getMethod() != null;	
	}
	
	private IMethod getMethod() {
		IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_DO_ELEMENT_AT_OFFSET);
		if (selection.size() != 1)
			return null;
		
		try {
			Object element= selection.getFirstElement();
			if (element instanceof IMethod) {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				if (!Flags.isStatic(flags) && !Flags.isPrivate(flags)) {
					return method;
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	public void run() {
		IMethod method= getMethod();
		if (method == null) {
			return;
		}
		
		try {
			IType declaringType= method.getDeclaringType();
			ICompilationUnit cu= declaringType.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy()) {
				declaringType= (IType) cu.getOriginal(declaringType);
				if (!declaringType.exists()) {
					return;
				}
			}
			ITypeHierarchy hierarchy= declaringType.newSupertypeHierarchy(null);
			IMethod impl= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, method.getElementName(), method.getParameterTypes(), method.isConstructor());
			if (impl != null) {
				IEditorPart part= EditorUtility.openInEditor(impl);
				EditorUtility.revealInEditor(part, impl);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			String title= JavaUIMessages.getString("OpenSuperImplementationAction.error.title");
			String message= JavaUIMessages.getString("OpenSuperImplementationAction.error.message");
			ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message, e.getStatus());
		}
	}
}