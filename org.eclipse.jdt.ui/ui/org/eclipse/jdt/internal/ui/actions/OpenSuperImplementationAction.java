
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


/**
 * On a selected method; opens the implementation in the super type (if existing)
 */
public class OpenSuperImplementationAction extends Action implements IUpdate, IObjectActionDelegate {

	private StructuredSelectionProvider fSelectionProvider;
	
	/**
	 * Use only for IWorkbenchWindowActionDelegates!
	 */ 
	public OpenSuperImplementationAction() {
		this(null);
	}
	
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
		if (fSelectionProvider == null) {
			return null;
		}
		
		IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_DO_ELEMENT_AT_OFFSET);
		if (selection.size() != 1)
			return null;
		
		try {
			Object element= selection.getFirstElement();
			if (element instanceof IMethod) {
				IMethod method= (IMethod) element;
				if (method.exists()) {
					int flags= method.getFlags();
					if (!Flags.isStatic(flags) && !Flags.isPrivate(flags)) {
						return method;
					}
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
			IMethod impl= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), method.isConstructor());
			if (impl == null) {
				// if no implementation found try to open a declaration
				impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), method.isConstructor());
			}
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

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		run();
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fSelectionProvider= StructuredSelectionProvider.createFrom(targetPart.getSite().getWorkbenchWindow().getSelectionService());
	}

}