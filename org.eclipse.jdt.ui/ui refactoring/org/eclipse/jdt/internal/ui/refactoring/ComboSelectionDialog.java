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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jdt.internal.ui.util.SWTUtil;


class ComboSelectionDialog extends Dialog{

	private String fSelection= null;
	private final String fShellTitle;
	private final String fLabelText;
	private final String[] fAllowedStrings;
	private final int fInitialSelectionIndex;
	public ComboSelectionDialog(Shell parentShell, String shellTitle, String labelText, String[] comboStrings, int initialSelectionIndex) {
		super(parentShell);
		Assert.isNotNull(shellTitle);
		Assert.isNotNull(labelText);
		Assert.isTrue(comboStrings.length > 0);
		Assert.isTrue(initialSelectionIndex >= 0 && initialSelectionIndex < comboStrings.length);
		fShellTitle= shellTitle;
		fLabelText= labelText;
		fAllowedStrings= comboStrings;
		fInitialSelectionIndex= initialSelectionIndex;
	}

	String getSelectedString(){
		return fSelection;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(fShellTitle);

		Composite composite = (Composite)super.createDialogArea(parent);
		Composite innerComposite = new Composite(composite, SWT.NONE);
		innerComposite.setLayoutData(new GridData());
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		innerComposite.setLayout(gl);

		Label label= new Label(innerComposite, SWT.NONE);
		label.setText(fLabelText);
		label.setLayoutData(new GridData());

		final Combo combo= new Combo(innerComposite, SWT.READ_ONLY);
		SWTUtil.setDefaultVisibleItemCount(combo);
		for (String allowedString : fAllowedStrings) {
			combo.add(allowedString);
		}
		combo.select(fInitialSelectionIndex);
		fSelection= combo.getItem(combo.getSelectionIndex());
		GridData gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(getMaxStringLength());
		combo.setLayoutData(gd);
		combo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				fSelection= combo.getItem(combo.getSelectionIndex());
			}
		});
		applyDialogFont(composite);
		return composite;
	}

	private int getMaxStringLength() {
		int max= 0;
		for (String allowedString : fAllowedStrings) {
			max= Math.max(max, allowedString.length());
		}
		return max;
	}
}
