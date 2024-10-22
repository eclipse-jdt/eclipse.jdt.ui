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

package org.eclipse.jdt.internal.corext.callhierarchy;

/**
 * These are the filter Options for the Call Hierarchy
 * When adding one, modify the getFilterOptions Method in FilterOptions
 */
public enum CallHierarchyFilterOptions {
	SHOW_ALL_CODE("PREF_SHOW_ALL_CODE", CallHierarchyMessages.FiltersDialog_ShowAllCode), //$NON-NLS-1$
	HIDE_TEST_CODE("PREF_HIDE_TEST_CODE", CallHierarchyMessages.FiltersDialog_HideTestCode), //$NON-NLS-1$
	SHOW_TEST_CODE_ONLY("PREF_SHOW_TEST_CODE_ONLY", CallHierarchyMessages.FiltersDialog_TestCodeOnly); //$NON-NLS-1$

	private final String identifyString;
	private final String text;

	CallHierarchyFilterOptions(String identifyString, String text) {
		this.identifyString = identifyString;
		this.text = text;
	}

    public String getId() {
        return identifyString;
    }

    public String getText() {
    	return text;
    }
}
