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
import org.eclipse.jdt.internal.ui.fix.ControlFlowMergeCleanUp;
import org.eclipse.jdt.internal.ui.fix.MergeConditionalBlocksCleanUp;
import org.eclipse.jdt.internal.ui.fix.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp;
import org.eclipse.jdt.internal.ui.fix.OperandFactorizationCleanUp;
import org.eclipse.jdt.internal.ui.fix.PullOutIfFromIfElseCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantFallingThroughBlockEndCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantIfConditionCleanUp;
import org.eclipse.jdt.internal.ui.fix.StrictlyEqualOrDifferentCleanUp;
import org.eclipse.jdt.internal.ui.fix.TernaryOperatorCleanUp;

public final class DuplicateCodeTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.duplicate_code"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new OperandFactorizationCleanUp(values),
				new TernaryOperatorCleanUp(values),
				new StrictlyEqualOrDifferentCleanUp(values),
				new MergeConditionalBlocksCleanUp(values),
				new ControlFlowMergeCleanUp(values),
				new OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp(values),
				new RedundantFallingThroughBlockEndCleanUp(values),
				new RedundantIfConditionCleanUp(values),
				new PullOutIfFromIfElseCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group duplicateGroup= createGroup(numColumns, composite, CleanUpMessages.DuplicateCodeTabPage_GroupName_DuplicateCode);

		final CheckboxPreference operandFactorization= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_OperandFactorization, CleanUpConstants.OPERAND_FACTORIZATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(operandFactorization);

		final CheckboxPreference ternaryOperator= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_TernaryOperator, CleanUpConstants.TERNARY_OPERATOR, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(ternaryOperator);

		final CheckboxPreference strictlyEqualOrDifferent= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_StrictlyEqualOrDifferent, CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(strictlyEqualOrDifferent);

		final CheckboxPreference mergeConditionalBlocks= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_MergeConditionalBlocks, CleanUpConstants.MERGE_CONDITIONAL_BLOCKS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(mergeConditionalBlocks);

		final CheckboxPreference controlFlowMerge= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_ControlFlowMerge, CleanUpConstants.CONTROLFLOW_MERGE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(controlFlowMerge);

		final CheckboxPreference oneIfRatherThanDuplicateBlocksThatFallThrough= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_OneIfRatherThanDuplicateBlocksThatFallThrough, CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(oneIfRatherThanDuplicateBlocksThatFallThrough);

		final CheckboxPreference redundantFallingThroughBlockEnd= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_RedundantFallingThroughBlockEnd, CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(redundantFallingThroughBlockEnd);

		final CheckboxPreference redundantIfCondition= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_RedundantIfCondition, CleanUpConstants.REDUNDANT_IF_CONDITION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(redundantIfCondition);

		final CheckboxPreference pullOutIfFromIfElse= createCheckboxPref(duplicateGroup, numColumns, CleanUpMessages.DuplicateCodeTabPage_CheckboxName_PullOutIfFromIfElse, CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(pullOutIfFromIfElse);
	}
}
