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

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Reveals the selected element in the resource navigator view. 
 * <p>
 * Action is applicable to structured selections containing Java element 
 * or resources.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class ShowInNavigatorViewAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInNavigatorViewAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ShowInNavigatorViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("ShowInNavigatorView.label")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SHOW_IN_NAVIGATOR_VIEW_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ShowInNavigatorViewAction(JavaEditor editor) {
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
		try {
			setEnabled(getResource(selection) != null); 
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		IJavaElement element= SelectionConverter.codeResolveOrInputHandled(fEditor, 
			getShell(), getDialogTitle(), ActionMessages.getString("ShowInNavigatorView.dialog.message")); //$NON-NLS-1$
		if (element == null)
			return;
		try {
			run(getResource(element));
		} catch(JavaModelException e) {
			// This shouldn't happen. If we can't convert the selection the
			// action is disabled.
			JavaPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		try {
			run(getResource(selection));
		} catch (JavaModelException e) {
			// This shouldn't happen. If we can't convert the selection the
			// action is disabled.
			JavaPlugin.log(e);
		}
	}
	
	private void run(IResource resource) {
		if (resource == null)
			return;
		try {
			IWorkbenchPage page= getSite().getWorkbenchWindow().getActivePage();	
			IViewPart view= page.showView(IPageLayout.ID_RES_NAV);
			if (view instanceof ISetSelectionTarget) {
				ISelection selection= new StructuredSelection(resource);
				((ISetSelectionTarget)view).selectReveal(selection);
			}
		} catch(PartInitException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("ShowInNavigatorView.error.activation_failed")); //$NON-NLS-1$
		}
	}
	
	private IResource getResource(IStructuredSelection selection) throws JavaModelException {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		if (element instanceof IResource)
			return (IResource)element;
		if (element instanceof IJavaElement)
			return getResource((IJavaElement)element);
		return null;
	}
	
	private IResource getResource(IJavaElement element) throws JavaModelException {
		if (element == null)
			return null;
		if (element instanceof IMember) {
			IOpenable openable= ((IMember)element).getOpenable();
			if (openable instanceof IJavaElement)
				element= (IJavaElement)openable;
			else
				element= null;
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit)element;
			if (unit.isWorkingCopy())
				element= unit.getOriginalElement();
		}
		if (element == null)
			return null;
		IResource result= element.getCorrespondingResource();
		if (result == null)
			result= element.getUnderlyingResource();
		return result;
	}
	
	private static String getDialogTitle() {
		return ActionMessages.getString("ShowInNavigatorView.dialog.title"); //$NON-NLS-1$
	}	
}
