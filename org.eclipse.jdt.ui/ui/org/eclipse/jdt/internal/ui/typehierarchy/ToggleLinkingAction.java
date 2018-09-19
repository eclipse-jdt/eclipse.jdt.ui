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

import org.eclipse.jdt.ui.ITypeHierarchyViewPart;

import org.eclipse.jdt.internal.ui.actions.AbstractToggleLinkingAction;


/**
 * This action toggles whether the type hierarchy links its selection to the active
 * editor.
 *
 * @since 2.1
 */
public class ToggleLinkingAction extends AbstractToggleLinkingAction {

	private ITypeHierarchyViewPart fHierarchyViewPart;

	/**
	 * Constructs a new action.
	 * @param part the type hierarchy part
	 */
	public ToggleLinkingAction(ITypeHierarchyViewPart part) {
		setChecked(part.isLinkingEnabled());
		fHierarchyViewPart= part;
	}

	/**
	 * Runs the action.
	 */
	@Override
	public void run() {
		fHierarchyViewPart.setLinkingEnabled(isChecked());
	}

}
