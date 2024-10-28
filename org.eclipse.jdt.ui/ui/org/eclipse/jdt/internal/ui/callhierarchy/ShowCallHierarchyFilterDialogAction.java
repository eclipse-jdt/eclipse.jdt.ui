/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

class ShowCallHierarchyFilterDialogAction extends Action {
	private CallHierarchyViewPart fPart;

	public ShowCallHierarchyFilterDialogAction(CallHierarchyViewPart view, String tooltipText) {
		super();
		fPart= view;

		setToolTipText(tooltipText);

		setText(CallHierarchyMessages.ShowFilterDialogAction_text);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_FILTER);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_FILTER);
	}

	@Override
	public void run() {
		openFiltersDialog();
	}

	private void openFiltersDialog() {
		FiltersDialog dialog= new FiltersDialog(
				fPart.getViewSite().getShell());

		if (Window.OK == dialog.open()) {
			fPart.refresh();
		}
	}
}
