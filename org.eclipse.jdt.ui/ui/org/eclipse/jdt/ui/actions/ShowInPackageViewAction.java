/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;
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
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
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
	public void run(ITextSelection selection) {
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
	public void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run((IJavaElement)selection.getFirstElement());
	}
	
	public void run(IJavaElement element) {
		if (element == null)
			return;
			
		// reveal the top most element only
		IOpenable openable= element.getOpenable();
		if (openable instanceof IJavaElement)
			element= (IJavaElement)openable;

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		view.tryToReveal(element);
	}

	private static String getDialogTitle() {
		return ActionMessages.getString("ShowInPackageViewAction.dialog.title"); //$NON-NLS-1$
	}
}
