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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * The action opens a Java editor on the selected method's super implementation.
 * <p>
 * The action is applicable to selections containing elements of type <code>
 * IMethod</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenSuperImplementationAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Creates a new <code>OpenSuperImplementationAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenSuperImplementationAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OpenSuperImplementationAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenSuperImplementationAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenSuperImplementationAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_SUPER_IMPLEMENTATION_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public OpenSuperImplementationAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getMethod(selection) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		IJavaElement element= elementAtOffset();
		IMethod method= null;
		if (element == null || !(element instanceof IMethod)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("OpenSuperImplementationAction.not_applicable")); //$NON-NLS-1$
			return;
		} else {
			method= checkMethod(element);
		}
		
		if (method == null) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), 
				ActionMessages.getFormattedString("OpenSuperImplementationAction.no_super_implementation", element.getElementName())); //$NON-NLS-1$
			return;
		}
		run(method);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		run(getMethod(selection));
	}
	
	private void run(IMethod method) {
		if (method == null)
			return;
		try {
			IType declaringType= method.getDeclaringType();
			IType workingCopyType= (IType) EditorUtility.getWorkingCopy(declaringType);
			if (workingCopyType != null) {
				declaringType= workingCopyType;
			}
			IMethod impl= findSuperImplementation(declaringType, method.getElementName(), method.getParameterTypes(), method.isConstructor());
			
			if (impl != null) {
				OpenActionUtil.open(impl);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.getString("OpenSuperImplementationAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}
	}
	
	private IMethod findSuperImplementation(IType declaringType, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
		IMethod impl= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, declaringType, name, paramTypes, isConstructor);
		if (impl == null) {
			// if no implementation found try to open a declaration
			impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, name, paramTypes, isConstructor);
		}
		return impl;
	}
	
	
	private IMethod getMethod(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		return checkMethod(element);
	}
	
	private IMethod getMethod(ITextSelection selection) {
		return checkMethod(elementAtOffset());
	}
	
	private IMethod checkMethod(Object element) {
		try {
			if (element instanceof IMethod) {
				IMethod method= (IMethod) element;
				if (method.exists()) {
					int flags= method.getFlags();
					if (!Flags.isStatic(flags) && !Flags.isPrivate(flags)) {
						IType declaringType= method.getDeclaringType();
						// if possible, make a check. don't care about working copies ect. In doubt, the action will be enabled.
						if (SuperTypeHierarchyCache.hasInCache(declaringType)) {
							if (findSuperImplementation(declaringType, method.getElementName(), method.getParameterTypes(), method.isConstructor()) == null) {
								return null;
							}
						}
						return method;
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	private IJavaElement elementAtOffset() {
		try {
			return SelectionConverter.getElementAtOffset(fEditor);
		} catch(JavaModelException e) {
		}
		return null;
	}
	
	private static String getDialogTitle() {
		return ActionMessages.getString("OpenSuperImplementationAction.error.title"); //$NON-NLS-1$
	}		
}