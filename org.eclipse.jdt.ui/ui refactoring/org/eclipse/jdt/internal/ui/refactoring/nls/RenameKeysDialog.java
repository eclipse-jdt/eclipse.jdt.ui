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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class RenameKeysDialog extends StatusDialog {

	private StringDialogField fNameField;
	private List<NLSSubstitution> fSelectedSubstitutions;
	private int fCommonPrefixLength;

	public RenameKeysDialog(Shell parent, List<NLSSubstitution> selectedSubstitutions) {
		super(parent);
		setTitle(NLSUIMessages.RenameKeysDialog_title);

		fSelectedSubstitutions= selectedSubstitutions;
		String prefix= getInitialPrefix(selectedSubstitutions);
		fCommonPrefixLength= prefix.length();

		fNameField= new StringDialogField();
		fNameField.setText(prefix);

		if (prefix.length() == 0) {
			fNameField.setLabelText(NLSUIMessages.RenameKeysDialog_description_noprefix);
		} else {
			fNameField.setLabelText(NLSUIMessages.RenameKeysDialog_description_withprefix + prefix + ':');
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		fNameField.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
		return composite;
	}

	@Override
	protected void okPressed() {
		String prefix= fNameField.getText();
		for (NLSSubstitution sub : fSelectedSubstitutions) {
			String newKey= prefix + sub.getKey().substring(fCommonPrefixLength);
			sub.setKey(newKey);
		}
		super.okPressed();
	}

	private String getInitialPrefix(List<NLSSubstitution> selectedSubstitutions) {
		String prefix= null;
		for (NLSSubstitution sub : selectedSubstitutions) {
			String curr= sub.getKey();
			if (prefix == null) {
				prefix= curr;
			} else if (!curr.startsWith(prefix)) {
				prefix= getCommonPrefix(prefix, curr);
				if (prefix.length() == 0) {
					return prefix;
				}
			}
		}
		return prefix;
	}

	private String getCommonPrefix(String a, String b) {
		String shorter= a.length() <= b.length() ? a : b;
		int len= shorter.length();
		for (int i= 0; i < len; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return shorter;
	}


}
