/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport.browser;

/**
 * Base class for actions that toggle specific styling inside browser HTML content controlled by specified checkbox and support
 * mouse hovering over corresponding GUI widget (e.g. toolbar or menu item) and {@link StylingPreference} states
 * persisted in preference store.
 */
public abstract class HoverPreferenceStylingInBrowserAction extends CheckboxToggleInBrowserAction {

	protected StylingPreference currentPreference;

	public HoverPreferenceStylingInBrowserAction(String text, int style, BrowserTextAccessor browserAccessor, String checkboxId) {
		super(text, style, browserAccessor, checkboxId);
	}

	/**
	 * Enumeration of possible preferences for toggling specific type of additional styling inside browser viewer.
	 */
	public enum StylingPreference {
		OFF,
		ALWAYS,
		HOVER
	}

	protected boolean isCurrentPreferenceAlways() {
		return currentPreference == StylingPreference.ALWAYS;
	}

	protected abstract StylingPreference getPreferenceFromStore();

	protected abstract void putPreferenceToStore(StylingPreference preference);

	protected abstract StylingPreference changeStylingPreference(StylingPreference oldPreference);

	public void loadCurentPreference() {
		currentPreference= getPreferenceFromStore();
	}

	protected boolean mouseEnter() {
		if (!isCurrentPreferenceAlways()) {
			toggleBrowserCheckbox(true);
			return true;
		}
		return false;
	}

	protected boolean mouseExit() {
		if (!isCurrentPreferenceAlways()) {
			toggleBrowserCheckbox(false);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		super.run();
		currentPreference= changeStylingPreference(currentPreference);
		putPreferenceToStore(currentPreference);
	}

}