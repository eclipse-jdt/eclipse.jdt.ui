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

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
/**
 * This action reveals the currently selected Java element in the packages
 * view. The Java element can be represeented by either
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
public class ShowInPackageViewAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInPackageViewAction</code>.
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
	 * Creates a new <code>ShowInPackageViewAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public ShowInPackageViewAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null);
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
			IJavaElement element= SelectionConverter.elementAtOffset(fEditor);
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
		try {
			element= OpenActionUtil.getElementToOpen(element);
			if (element == null)
				return;
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				return;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.getString("ShowInPackageViewAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}
	}
	
	private static String getDialogTitle() {
		return ActionMessages.getString("ShowInPackageViewAction.dialog.title"); //$NON-NLS-1$
	}		
}