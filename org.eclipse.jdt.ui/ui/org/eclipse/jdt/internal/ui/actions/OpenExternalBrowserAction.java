/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.net.URL;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action to open the selection in an external browser. If the
 * selection is a java element its corresponding javadoc is shown
 * if possible. If it is an URL the urls content is shown.
 * 
 * The action is disabled if the selection can not be opened.
 * 
 * @since 3.4
 */
public class OpenExternalBrowserAction extends SelectionDispatchAction {

	/**
	 * Create a new ShowExternalJavadocAction
	 * 
	 * @param site the site this action is working on 
	 */
	public OpenExternalBrowserAction(IWorkbenchSite site) {
		super(site);

		setText(ActionMessages.OpenExternalBrowserAction_javadoc_label); 
		setToolTipText(ActionMessages.OpenExternalBrowserAction_javadoc_toolTip);
		
		setImageDescriptor(JavaPluginImages.DESC_ELCL_EXTERNAL_BROWSER);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_EXTERNAL_BROWSER);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		if (canEnable(selection)) {
			setEnabled(true);

			Object element= selection.getFirstElement();
			if (element instanceof URL) {
				setText(ActionMessages.OpenExternalBrowserAction_url_label);
				setToolTipText(ActionMessages.OpenExternalBrowserAction_url_toolTip);
			} else {
				setText(ActionMessages.OpenExternalBrowserAction_javadoc_label);
				setToolTipText(ActionMessages.OpenExternalBrowserAction_javadoc_toolTip);
			}
		} else {
			setEnabled(false);
			
			setText(ActionMessages.OpenExternalBrowserAction_javadoc_label);
			setToolTipText(ActionMessages.OpenExternalBrowserAction_javadoc_toolTip);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		Object element= selection.getFirstElement();
		URL url;
		if (element instanceof IJavaElement) {
			try {
				url= JavaUI.getJavadocLocation((IJavaElement) element, true);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return;
			}
		} else {
			url= (URL) element;
		}

		OpenBrowserUtil.open(url, getShell().getDisplay(), null);
	}

	/**
	 * True if this action can operate on the given selection
	 * 
	 * @param selection the selection to inspect
	 * @return true if this action can operate on the selection
	 */
	private boolean canEnable(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;

		Object element= selection.getFirstElement();
		if (element instanceof URL)
			return true;
		
		if (!(element instanceof IJavaElement))
			return false;

		IJavaElement input= (IJavaElement) element;

		try {
			URL url= JavaUI.getJavadocLocation(input, true);
			if (url == null)
				return false;

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}

		return true;
	}

}