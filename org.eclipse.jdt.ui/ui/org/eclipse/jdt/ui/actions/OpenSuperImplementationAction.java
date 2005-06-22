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
package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
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
		setText(ActionMessages.OpenSuperImplementationAction_label); 
		setDescription(ActionMessages.OpenSuperImplementationAction_description); 
		setToolTipText(ActionMessages.OpenSuperImplementationAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_SUPER_IMPLEMENTATION_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
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
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.OpenSuperImplementationAction_not_applicable); 
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
	
	/*
	 * No Javadoc since the method isn't meant to be public but is
	 * since the beginning
	 */
	public void run(IMethod method) {
		if (method == null)
			return;		
		if (!ActionUtil.isProcessable(getShell(), method))
			return;
		
		if (!checkMethod(method)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), 
				Messages.format(ActionMessages.OpenSuperImplementationAction_no_super_implementation, method.getElementName())); 
			return;
		}		

		try {
			IType type= method.getDeclaringType();

			IMethod impl= findSuperImplementation(type, method);
			if (impl != null) {
				OpenActionUtil.open(impl);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.OpenSuperImplementationAction_error_message; 
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}
	}
	
	private IMethod findSuperImplementation(IType declaringType, IMethod method) throws JavaModelException {
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
		IMethod impl= JavaModelUtil.findMethodImplementationInHierarchy2(hierarchy, declaringType, method);
		if (impl == null) {
			// if no implementation found try to open a declaration
			impl= JavaModelUtil.findMethodDeclarationInHierarchy2(hierarchy, declaringType, method);
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
				// if possible, make a check. don't care about working copies etc. In doubt, the action will be enabled.
				if (SuperTypeHierarchyCache.hasInCache(declaringType)) {
					if (findSuperImplementation(declaringType, method) == null) {
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
		return ActionMessages.OpenSuperImplementationAction_error_title; 
	}		
}
