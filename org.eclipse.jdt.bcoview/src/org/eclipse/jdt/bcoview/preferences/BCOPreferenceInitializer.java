/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.preferences;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initalizer of default values for BCO preferences
 */
public class BCOPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = BytecodeOutlinePlugin.getDefault().getPreferenceStore();
		store.setDefault(BCOConstants.LINK_VIEW_TO_EDITOR, true);
		store.setDefault(BCOConstants.LINK_REF_VIEW_TO_EDITOR, true);

		store.setDefault(BCOConstants.SHOW_ONLY_SELECTED_ELEMENT, true);

		store.setDefault(BCOConstants.SHOW_RAW_BYTECODE, false);

		store.setDefault(BCOConstants.SHOW_ASMIFIER_CODE, false);
		store.setDefault(BCOConstants.DIFF_SHOW_ASMIFIER_CODE, false);

		store.setDefault(BCOConstants.SHOW_ANALYZER, false);

		store.setDefault(BCOConstants.SHOW_VARIABLES, true);
		store.setDefault(BCOConstants.DIFF_SHOW_VARIABLES, true);

		store.setDefault(BCOConstants.SHOW_LINE_INFO, true);
		store.setDefault(BCOConstants.DIFF_SHOW_LINE_INFO, true);

		store.setDefault(BCOConstants.SHOW_STACKMAP, true);
		store.setDefault(BCOConstants.DIFF_SHOW_STACKMAP, true);

		store.setDefault(BCOConstants.EXPAND_STACKMAP, false);
		store.setDefault(BCOConstants.DIFF_EXPAND_STACKMAP, false);

		store.setDefault(BCOConstants.RECALCULATE_STACKMAP, false);
		store.setDefault(BCOConstants.SHOW_HEX_VALUES, false);
	}

}
