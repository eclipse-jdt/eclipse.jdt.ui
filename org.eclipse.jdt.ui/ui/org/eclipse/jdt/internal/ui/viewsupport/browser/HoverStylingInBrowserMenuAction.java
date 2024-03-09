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

import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Event;

import org.eclipse.jface.action.IAction;

import org.eclipse.jdt.internal.ui.viewsupport.ArmListeningMenuItemsConfigurer.IArmListeningMenuItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer.IMenuVisibilityMenuItemAction;

/**
 * Base class for actions that toggle specific styling inside browser HTML content controlled by specified checkbox and support
 * mouse hovering over corresponding menu item widget and
 * {@link org.eclipse.jdt.internal.ui.viewsupport.browser.HoverPreferenceStylingInBrowserAction.StylingPreference} states
 * persisted in preference store.
 */
public abstract class HoverStylingInBrowserMenuAction extends HoverPreferenceStylingInBrowserAction
		implements IArmListeningMenuItemAction, IMenuVisibilityMenuItemAction {


	public HoverStylingInBrowserMenuAction(String text, BrowserTextAccessor browserAccessor, String checkboxId) {
		super(text, IAction.AS_PUSH_BUTTON, browserAccessor, checkboxId);
	}

	@Override
	protected StylingPreference changeStylingPreference(StylingPreference oldPreference) {
		return switch (oldPreference) {
			case OFF -> StylingPreference.ALWAYS;
			case HOVER ->  StylingPreference.OFF;
			case ALWAYS -> StylingPreference.HOVER;
		};
	}

	@Override
	public boolean itemArmed(ArmEvent event) {
		return mouseEnter();
	}

	@Override
	public boolean itemUnarmed(ArmEvent event) {
		return mouseExit();
	}

	@SuppressWarnings("unused")
	public boolean menuButtonMouseEnter(Event event) {
		if (currentPreference == StylingPreference.HOVER) {
			toggleBrowserCheckbox(true);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	public boolean menuButtonMouseExit(Event event) {
		if (currentPreference == StylingPreference.HOVER) {
			toggleBrowserCheckbox(false);
			return true;
		}
		return false;
	}

	protected abstract void presentCurrentPreference();

	@Override
	public void loadCurentPreference() {
		super.loadCurentPreference();
		presentCurrentPreference();
	}

	@Override
	public boolean menuShown(MenuEvent e) {
		loadCurentPreference();
		return false;
	}

	@Override
	public boolean menuHidden(MenuEvent e) {
		return mouseExit();
	}

	@Override
	public void run() {
		super.run();
		presentCurrentPreference();

		if (currentPreference == StylingPreference.OFF) {
			// we just switched styling off so temporarily we want styling not applied
			toggleBrowserCheckbox(false);
		} else {
			toggleBrowserCheckbox(true); // menu will be displayed again with mouse cursor again being on this item, thus same as being armed
		}
		checkboxToggler.getBrowserTextAccessor().applyChanges();
	}

}