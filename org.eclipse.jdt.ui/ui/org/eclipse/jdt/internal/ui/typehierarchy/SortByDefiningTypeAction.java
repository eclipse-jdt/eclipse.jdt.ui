/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action to let the label provider show the defining type of the method
 */
public class SortByDefiningTypeAction extends Action {

	private MethodsViewer fMethodsViewer;

	/**
	 * Creates the action.
	 * @param viewer the viewer
	 * @param initValue the initial state
	 */
	public SortByDefiningTypeAction(MethodsViewer viewer, boolean initValue) {
		super(TypeHierarchyMessages.SortByDefiningTypeAction_label);
		setDescription(TypeHierarchyMessages.SortByDefiningTypeAction_description);
		setToolTipText(TypeHierarchyMessages.SortByDefiningTypeAction_tooltip);

		JavaPluginImages.setLocalImageDescriptors(this, "definingtype_sort_co.png"); //$NON-NLS-1$

		fMethodsViewer= viewer;

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SORT_BY_DEFINING_TYPE_ACTION);

		setChecked(initValue);
	}

	/*
	 * @see Action#actionPerformed
	 */
	@Override
	public void run() {
		BusyIndicator.showWhile(fMethodsViewer.getControl().getDisplay(), () -> fMethodsViewer.sortByDefiningType(isChecked()));
	}
}
