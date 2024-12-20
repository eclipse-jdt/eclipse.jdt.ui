/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
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

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyCore;

/**
 *
 */
public class FiltersAction extends Action {

	private CallHierarchyViewPart fView;
	private IMember[] fMembers;
	String typeString;

	public FiltersAction(CallHierarchyViewPart viewPart, String type) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		fView = viewPart;
		typeString = type;
		switch(type) {
			case CallHierarchyCore.PREF_SHOW_ALL_CODE:
				setText(CallHierarchyMessages.FiltersDialog_ShowAllCode);

				break;
			case CallHierarchyCore.PREF_HIDE_TEST_CODE:
				setText(CallHierarchyMessages.FiltersDialog_HideTestCode);

				break;

			case CallHierarchyCore.PREF_SHOW_TEST_CODE_ONLY:
				setText(CallHierarchyMessages.FiltersDialog_TestCodeOnly);

				break;
		}

	}

	@Override
	public void run() {
		fView.setFilterMode(typeString);
	}

}
