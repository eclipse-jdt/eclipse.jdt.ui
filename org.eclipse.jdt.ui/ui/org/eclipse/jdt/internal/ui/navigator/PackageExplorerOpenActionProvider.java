/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;

public class PackageExplorerOpenActionProvider extends CommonActionProvider {
	private OpenAndExpand fOpenAndExpand;
	private OpenEditorActionGroup fOpenGroup;

	private boolean fInViewPart;

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (fInViewPart) {
			fOpenGroup.fillActionBars(actionBars);

			if (fOpenAndExpand == null && fOpenGroup.getOpenAction().isEnabled()) {
				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenGroup.getOpenAction());
			} else if (fOpenAndExpand != null) {
				// Enabling will be updated on selection change however the fillActionBars can also be triggered
				// from selection change listener and might run before action enabling is matching the current selection
				fOpenAndExpand.update(fOpenAndExpand.getSelection());
				if (fOpenAndExpand.isEnabled()) {
					actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenAndExpand);
				}
			}
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (fInViewPart && fOpenGroup.getOpenAction().isEnabled()) {
			fOpenGroup.fillContextMenu(menu);
		}
	}

	@Override
	public void init(ICommonActionExtensionSite site) {
		ICommonViewerWorkbenchSite workbenchSite = null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();
		}

		if (workbenchSite != null && workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
			IViewPart viewPart = (IViewPart) workbenchSite.getPart();

			fOpenGroup = new OpenEditorActionGroup(viewPart);

			if (site.getStructuredViewer() instanceof TreeViewer) {
				fOpenAndExpand = new OpenAndExpand(workbenchSite.getSite(), (OpenAction) fOpenGroup.getOpenAction(), (TreeViewer) site.getStructuredViewer());
			}
			fInViewPart = true;
		}
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);

		if (fInViewPart) {
			fOpenGroup.setContext(context);
		}
	}

	/*
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 * @since 3.5
	 */
	@Override
	public void dispose() {
		if (fOpenGroup != null) {
			fOpenGroup.dispose();
		}

		super.dispose();
	}
}
