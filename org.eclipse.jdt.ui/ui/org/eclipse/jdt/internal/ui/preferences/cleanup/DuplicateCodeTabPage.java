/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.MergeConditionalBlocksCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantFallingThroughBlockEndCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantIfConditionCleanUp;

public final class DuplicateCodeTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.duplicate_code"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new MergeConditionalBlocksCleanUp(values),
				new RedundantFallingThroughBlockEndCleanUp(values),
				new RedundantIfConditionCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group optimizationGroup= createGroup(numColumns, composite, CleanUpMessages.DuplicateCodeTabPage_GroupName_DuplicateCode);

		final CheckboxPreference mergeConditionalBlocks= createCheckboxPref(optimizationGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_MergeConditionalBlocks, CleanUpConstants.MERGE_CONDITIONAL_BLOCKS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(mergeConditionalBlocks);

		final CheckboxPreference redundantFallingThroughBlockEnd= createCheckboxPref(optimizationGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_RedundantFallingThroughBlockEnd, CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(redundantFallingThroughBlockEnd);

		final CheckboxPreference redundantIfCondition= createCheckboxPref(optimizationGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_RedundantIfCondition, CleanUpConstants.REDUNDANT_IF_CONDITION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(redundantIfCondition);
	}
}
