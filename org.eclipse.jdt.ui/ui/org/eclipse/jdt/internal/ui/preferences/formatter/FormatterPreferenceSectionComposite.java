/*******************************************************************************
 * Copyright (c) 2018 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.ui.forms.widgets.ExpandableComposite;

public class FormatterPreferenceSectionComposite extends ExpandableComposite {

	// don't expand/collapse when click on header label gives focus
	boolean fHasFocusBeforeClick= false;
	boolean fExpandLock= false;

	public FormatterPreferenceSectionComposite(Composite parent, int style, int expansionStyle) {
		super(parent, style, expansionStyle);
		textLabel.addListener(SWT.MouseEnter, e -> fHasFocusBeforeClick= toggle.isFocusControl());
		textLabel.addListener(SWT.MouseDown, e -> fExpandLock= !fHasFocusBeforeClick || e.button != 1);
	}

	@Override
	protected void internalSetExpanded(boolean expanded) {
		if (fExpandLock) {
			toggle.setExpanded(isExpanded());
			fHasFocusBeforeClick= true;
			fExpandLock= false;
		} else {
			super.internalSetExpanded(expanded);
		}
	}

	@Override
	public void setMenu(Menu menu) {
		// add only to header, not the rest of the composite
		textLabel.setMenu(menu);
	}
}
