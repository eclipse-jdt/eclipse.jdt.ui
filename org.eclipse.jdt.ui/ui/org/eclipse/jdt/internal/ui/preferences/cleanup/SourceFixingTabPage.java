/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.BitwiseConditionalExpressionCleanup;
import org.eclipse.jdt.internal.ui.fix.InvertEqualsCleanUp;
import org.eclipse.jdt.internal.ui.fix.StandardComparisonCleanUp;

public final class SourceFixingTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.source_fixing"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(final Map<String, String> values) {
		return new AbstractCleanUp[] {
				new InvertEqualsCleanUp(values),
				new StandardComparisonCleanUp(values),
				new BitwiseConditionalExpressionCleanup(values)
		};
	}

	@Override
	protected void doCreatePreferences(final Composite composite, final int numColumns) {
		final Label warningImage= new Label(composite, SWT.LEFT | SWT.WRAP);
		warningImage.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
		warningImage.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		createLabel(numColumns - 1, composite, CleanUpMessages.SourceFixingTabPage_warning);

		Group standardCodeGroup= createGroup(numColumns, composite, CleanUpMessages.SourceFixingTabPage_GroupName_standardCode);

		final CheckboxPreference invertEqualsPref= createCheckboxPref(standardCodeGroup, numColumns, CleanUpMessages.SourceFixingTabPage_CheckboxName_InvertEquals, CleanUpConstants.INVERT_EQUALS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(invertEqualsPref);

		final CheckboxPreference standardComparisonPref= createCheckboxPref(standardCodeGroup, numColumns, CleanUpMessages.SourceFixingTabPage_CheckboxName_StandardComparison, CleanUpConstants.STANDARD_COMPARISON, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(standardComparisonPref);

		final CheckboxPreference bitwiseComparisonPref= createCheckboxPref(standardCodeGroup, numColumns, CleanUpMessages.SourceFixingTabPage_CheckboxName_CheckSignOfBitwiseOperation, CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(bitwiseComparisonPref);
	}
}
