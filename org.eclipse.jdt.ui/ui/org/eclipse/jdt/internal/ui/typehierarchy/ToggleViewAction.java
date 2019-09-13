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

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.ITypeHierarchyViewPart;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action to switch between the different hierarchy views.
 */
public class ToggleViewAction extends Action {

	private ITypeHierarchyViewPart fViewPart;
	private int fViewerIndex;

	public ToggleViewAction(ITypeHierarchyViewPart v, int viewerIndex) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		String contextHelpId= null;
		switch (viewerIndex) {
		case ITypeHierarchyViewPart.HIERARCHY_MODE_SUPERTYPES:
			setText(TypeHierarchyMessages.ToggleViewAction_supertypes_label);
			contextHelpId= IJavaHelpContextIds.SHOW_SUPERTYPES;
			setDescription(TypeHierarchyMessages.ToggleViewAction_supertypes_description);
			setToolTipText(TypeHierarchyMessages.ToggleViewAction_supertypes_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "super_co.png"); //$NON-NLS-1$
			break;
		case ITypeHierarchyViewPart.HIERARCHY_MODE_SUBTYPES:
			setText(TypeHierarchyMessages.ToggleViewAction_subtypes_label);
			contextHelpId= IJavaHelpContextIds.SHOW_SUBTYPES;
			setDescription(TypeHierarchyMessages.ToggleViewAction_subtypes_description);
			setToolTipText(TypeHierarchyMessages.ToggleViewAction_subtypes_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "sub_co.png"); //$NON-NLS-1$
			break;
		case ITypeHierarchyViewPart.HIERARCHY_MODE_CLASSIC:
			setText(TypeHierarchyMessages.ToggleViewAction_vajhierarchy_label);
			contextHelpId= IJavaHelpContextIds.SHOW_HIERARCHY;
			setDescription(TypeHierarchyMessages.ToggleViewAction_vajhierarchy_description);
			setToolTipText(TypeHierarchyMessages.ToggleViewAction_vajhierarchy_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "hierarchy_co.png"); //$NON-NLS-1$
			break;
		default:
			Assert.isTrue(false);
			break;
		}

		fViewPart= v;
		fViewerIndex= viewerIndex;

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, contextHelpId);
	}

	public int getViewerIndex() {
		return fViewerIndex;
	}

	/*
	 * @see Action#actionPerformed
	 */
	@Override
	public void run() {
		fViewPart.setHierarchyMode(fViewerIndex);
	}
}
