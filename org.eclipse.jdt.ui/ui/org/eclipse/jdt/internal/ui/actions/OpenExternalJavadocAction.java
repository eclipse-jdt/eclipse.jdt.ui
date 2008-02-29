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

import org.eclipse.swt.program.Program;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action to open the javadoc of the selected Java element in
 * a browser. If the javadoc is not external then the action
 * is disabled. Depending on the workbench preference settings
 * either the internal browser or the external browser is used.
 * 
 * @since 3.4
 */
public class OpenExternalJavadocAction extends SelectionDispatchAction {

	/**
	 * Create a new ShowExternalJavadocAction
	 * 
	 * @param site the site this action is working on 
	 */
	public OpenExternalJavadocAction(IWorkbenchSite site) {
		super(site);

		setText(ActionMessages.ShowExternalJavadocAction_label);
		setToolTipText(ActionMessages.ShowExternalJavadocAction_toolTip);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_JAVADOC_LOCATION_ATTRIB); //TODO: better image
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			URL url= JavaUI.getJavadocLocation((IJavaElement) selection.getFirstElement(), true);

			IWorkbenchBrowserSupport support= PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser;
			try {
				browser= support.createBrowser(null);
			} catch (PartInitException e) {
				JavaPlugin.log(e);
				Program.launch(url.toExternalForm());
				return;
			}

			try {
				browser.openURL(url);
			} catch (PartInitException e) {
				Program.launch(url.toExternalForm());
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
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