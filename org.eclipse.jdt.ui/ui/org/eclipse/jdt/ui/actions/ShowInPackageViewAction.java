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

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
/**
 * This action reveals the currently selected Java element in the 
 * package explorer. 
 * <p>
 * The action is applicable to selections containing elements of type
 * <code>IJavaElement</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * @since 2.0
 */
public class ShowInPackageViewAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInPackageViewAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ShowInPackageViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("ShowInPackageViewAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("ShowInPackageViewAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ShowInPackageViewAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION);	
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ShowInPackageViewAction(JavaEditor editor) {
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
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaElement;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		try {
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null)
				run(element);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.getString("ShowInPackageViewAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}	
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run((IJavaElement)selection.getFirstElement());
	}
	
	private void run(IJavaElement element) {
		if (element == null)
			return;
			
		// reveal the top most element only
		IOpenable openable= element.getOpenable();
		if (openable instanceof IJavaElement)
			element= (IJavaElement)openable;

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		if (view != null) {
			if (reveal(view, element))
				return;
			element= getVisibleParent(element);
			if (element != null) {
				if (reveal(view, element))
					return;
				IResource resource= element.getResource();
				if (resource != null) {
					if (reveal(view, resource))
						return;
				}
			}
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("ShowInPackageViewAction.not_found")); //$NON-NLS-1$
		}
	}

	private boolean reveal(PackageExplorerPart view, Object element) {
		if (element == null)
			return false;
		view.selectReveal(new StructuredSelection(element));
		IElementComparer comparer= view.getTreeViewer().getComparer();
		Object selected= getSelectedElement(view);
		if (comparer != null ? comparer.equals(element, selected) : element.equals(selected))
			return true;
		return false;
	}

	private Object getSelectedElement(PackageExplorerPart view) {
		return ((IStructuredSelection)view.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}
	
	private IJavaElement getVisibleParent(IJavaElement element) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (element == null)
			return null;
		switch (element.getElementType()) {
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.TYPE:
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				// select parent cu/classfile
				element= (IJavaElement)element.getOpenable();
				break;
			case IJavaElement.JAVA_MODEL:
				element= null;
				break;
		}
		if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
			ICompilationUnit unit= (ICompilationUnit)element;
			if (unit.isWorkingCopy())
				element= unit.getOriginalElement();
		}
		return element;
	}
	
	private static String getDialogTitle() {
		return ActionMessages.getString("ShowInPackageViewAction.dialog.title"); //$NON-NLS-1$
	}
}