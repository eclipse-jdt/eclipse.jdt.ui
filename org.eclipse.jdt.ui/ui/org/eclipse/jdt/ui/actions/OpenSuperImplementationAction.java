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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * This action opens a java editor on the super implementation of a element represented by either
 * <ul>
 * 	<li>a text selection inside a Java editor, or </li>
 * 	<li>a structured selection of a view part showing Java elements</li>
 * </ul>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenSuperImplementationAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/* (non-Javadoc)
	 * Class implements IObjectActionDelegate
	 */
	public static class ObjectDelegate implements IObjectActionDelegate {
		private OpenSuperImplementationAction fAction;
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			fAction= new OpenSuperImplementationAction(UnifiedSite.create(targetPart.getSite()));
		}
		public void run(IAction action) {
			fAction.run();
		}
		public void selectionChanged(IAction action, ISelection selection) {
			if (fAction == null)
				action.setEnabled(false);
		}
	}
	
	/**
	 * Creates a new <code>OpenSuperImplementationAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenSuperImplementationAction(UnifiedSite site) {
		super(site);
		setText(ActionMessages.getString("OpenSuperImplementationAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenSuperImplementationAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenSuperImplementationAction.tooltip")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new <code>OpenSuperImplementationAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenSuperImplementationAction(JavaEditor editor) {
		this(UnifiedSite.create(editor.getEditorSite()));
		fEditor= editor;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(ITextSelection selection) {
		try {
			return fEditor != null && SelectionConverter.elementAtOffset(fEditor) != null;
		} catch (JavaModelException e) {
		}
		return false;
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
		run(getMethod(selection));
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
				OpenActionUtil.open(impl);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.getString("OpenSuperImplementationAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}
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
						return method;
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	private Object elementAtOffset() {
		try {
			return SelectionConverter.elementAtOffset(fEditor);
		} catch(JavaModelException e) {
		}
		return null;
	}
	
	private static String getDialogTitle() {
		return ActionMessages.getString("OpenSuperImplementationAction.error.title"); //$NON-NLS-1$
	}		
}