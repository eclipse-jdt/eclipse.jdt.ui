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

import java.util.Objects;

import org.eclipse.jface.action.Action;

/**
 * Base class for actions that toggle <code>checked</code> "pre-selected" state of particular checkbox inside browser HTML content.
 */
public abstract class CheckboxToggleInBrowserAction extends Action {
	protected final CheckboxInBrowserToggler checkboxToggler;

	public CheckboxToggleInBrowserAction(String text, int style, BrowserTextAccessor browserTextAccessor, String checkboxId) {
		super(text, style);
		checkboxToggler= new CheckboxInBrowserToggler(Objects.requireNonNull(browserTextAccessor), Objects.requireNonNull(checkboxId));
	}

	protected void toggleBrowserCheckbox(boolean enabled) {
		checkboxToggler.toggleCheckboxInBrowser(enabled);
	}

}