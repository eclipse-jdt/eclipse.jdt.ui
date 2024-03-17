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
package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import java.util.Objects;

import org.eclipse.swt.events.MenuEvent;

import org.eclipse.jface.action.IAction;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer.IMenuVisibilityMenuItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxToggleInBrowserAction;

public class ToggleSignatureTypeParametersColoringAction extends CheckboxToggleInBrowserAction implements IMenuVisibilityMenuItemAction {
	final String preferenceKeyPrefix;

	public ToggleSignatureTypeParametersColoringAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(JavadocStylingMessages.JavadocStyling_typeParamsColoring, IAction.AS_CHECK_BOX, browserAccessor, JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING);
		this.preferenceKeyPrefix= Objects.requireNonNull(preferenceKeyPrefix);
		setId(ToggleSignatureTypeParametersColoringAction.class.getSimpleName());
		showCurentPreference();
	}

	private void showCurentPreference() {
		setChecked(JavaElementLinks.getPreferenceForTypeParamsReferencesColoring(preferenceKeyPrefix));
	}

	@Override
	public void menuShown(MenuEvent e) {
		showCurentPreference();
	}

	@Override
	public void run() {
		super.run();
		JavaElementLinks.setPreferenceForTypeParamsReferencesColoring(preferenceKeyPrefix, isChecked());
		toggleBrowserCheckbox(isChecked());
		checkboxToggler.getBrowserTextAccessor().applyChanges();
	}

}
