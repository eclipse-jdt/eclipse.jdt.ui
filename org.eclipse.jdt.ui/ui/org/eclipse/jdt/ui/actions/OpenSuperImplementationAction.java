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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

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
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
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
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		IMethod method= getMethod(selection);
		
		setEnabled(method != null && checkMethod(method));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		IJavaElement element= elementAtOffset();
		if (element == null || !(element instanceof IMethod)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("OpenSuperImplementationAction.not_applicable")); //$NON-NLS-1$
			return;
		}
		run((IMethod) element);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		run(getMethod(selection));
	}
	
	public void run(IMethod method) {
		if (method == null)
			return;		
		if (!ActionUtil.isProcessable(getShell(), method))
			return;
		
		if (!checkMethod(method)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), 
				ActionMessages.getFormattedString("OpenSuperImplementationAction.no_super_implementation", method.getElementName())); //$NON-NLS-1$
			return;
		}		

		try {
			IType type= method.getDeclaringType();

			IMethod impl= findSuperImplementation(type, method.getElementName(), method.getParameterTypes(), method.isConstructor());
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
		if (element instanceof IMethod) {
			return (IMethod) element;
		}
		return null;
	}
	
	private boolean checkMethod(IMethod method) {
		try {
			int flags= method.getFlags();
			if (!Flags.isStatic(flags) && !Flags.isPrivate(flags)) {
				IType declaringType= method.getDeclaringType();
				// if possible, make a check. don't care about working copies ect. In doubt, the action will be enabled.
				if (SuperTypeHierarchyCache.hasInCache(declaringType)) {
					if (findSuperImplementation(declaringType, method.getElementName(), method.getParameterTypes(), method.isConstructor()) == null) {
						return false;
					}
				}
				return true;
			}
		} catch (JavaModelException e) {
			if (!e.isDoesNotExist()) {
				JavaPlugin.log(e);
			}
		}
		return false;
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
