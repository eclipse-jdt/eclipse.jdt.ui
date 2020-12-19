/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.AddAllCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ElseIfCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExtractIncrementCleanUp;
import org.eclipse.jdt.internal.ui.fix.InstanceofCleanUp;
import org.eclipse.jdt.internal.ui.fix.LambdaExpressionAndMethodRefCleanUp;
import org.eclipse.jdt.internal.ui.fix.NumberSuffixCleanUp;
import org.eclipse.jdt.internal.ui.fix.PullUpAssignmentCleanUp;
import org.eclipse.jdt.internal.ui.fix.ReduceIndentationCleanUp;
import org.eclipse.jdt.internal.ui.fix.SwitchCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;

public final class CodeStyleTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.code_style"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new ControlStatementsCleanUp(values),
				new SwitchCleanUp(values),
				new AddAllCleanUp(values),
				new ElseIfCleanUp(values),
				new ReduceIndentationCleanUp(values),
				new ExpressionsCleanUp(values),
				new ExtractIncrementCleanUp(values),
				new PullUpAssignmentCleanUp(values),
				new NumberSuffixCleanUp(values),
				new InstanceofCleanUp(values),
				new VariableDeclarationCleanUp(values),
				new LambdaExpressionAndMethodRefCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group controlGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_ControlStatements);

		final CheckboxPreference useBlockPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseBlocks, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockAlwaysPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseBlocks, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockJDTStylePref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_UseBlocksSpecial, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, CleanUpModifyDialog.FALSE_TRUE);
		intent(controlGroup);
		final RadioPreference useBlockNeverPref= createRadioPref(controlGroup, numColumns - 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseBlocks, CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useBlockPref, new RadioPreference[] {useBlockAlwaysPref, useBlockJDTStylePref, useBlockNeverPref});

		CheckboxPreference elseIf= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ElseIf, CleanUpConstants.ELSE_IF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(elseIf);

		final CheckboxPreference reduceIndentationPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ReduceIndentation, CleanUpConstants.REDUCE_INDENTATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(reduceIndentationPref);

		final CheckboxPreference switchPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_Switch, CleanUpConstants.USE_SWITCH, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(switchPref);

		final CheckboxPreference addAllPref= createCheckboxPref(controlGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseAddAllRemoveAll, CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(addAllPref);

		Group expressionsGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_Expressions);

		final CheckboxPreference useParenthesesPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseParentheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpModifyDialog.FALSE_TRUE);
		intent(expressionsGroup);
		final RadioPreference useParenthesesAlwaysPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_AlwaysUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpModifyDialog.FALSE_TRUE);
		final RadioPreference useParenthesesNeverPref= createRadioPref(expressionsGroup, 1, CleanUpMessages.CodeStyleTabPage_RadioName_NeverUseParantheses, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useParenthesesPref, new RadioPreference[] {useParenthesesAlwaysPref, useParenthesesNeverPref});

		final CheckboxPreference extractIncrementPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_ExtractIncrement, CleanUpConstants.EXTRACT_INCREMENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(extractIncrementPref);

		final CheckboxPreference pullUpAssignmentPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_PullUpAssignment, CleanUpConstants.PULL_UP_ASSIGNMENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(pullUpAssignmentPref);

		final CheckboxPreference instanceofPref= createCheckboxPref(expressionsGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_Instanceof, CleanUpConstants.INSTANCEOF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(instanceofPref);

		Group numberSuffixGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_NumberLiteral);

		final CheckboxPreference numberSuffixPref= createCheckboxPref(numberSuffixGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_NumberSuffix, CleanUpConstants.NUMBER_SUFFIX,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(numberSuffixPref);

		Group variableGroup= createGroup(numColumns, composite, CleanUpMessages.CodeStyleTabPage_GroupName_VariableDeclarations);

		final CheckboxPreference useFinalPref= createCheckboxPref(variableGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinal, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpModifyDialog.FALSE_TRUE);
		intent(variableGroup);
		final CheckboxPreference useFinalFieldsPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForFields, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalParametersPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForParameters, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference useFinalVariablesPref= createCheckboxPref(variableGroup, 1, CleanUpMessages.CodeStyleTabPage_CheckboxName_UseFinalForLocals, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(useFinalPref, new CheckboxPreference[] {useFinalFieldsPref, useFinalParametersPref, useFinalVariablesPref});

		Group functionalInterfacesGroup= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_FunctionalInterfaces);

		CheckboxPreference simplifyLambdaExpressionAndMethodRef= createCheckboxPref(functionalInterfacesGroup, numColumns, CleanUpMessages.CodeStyleTabPage_CheckboxName_SimplifyLambdaExpressionAndMethodRefSyntax, CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(simplifyLambdaExpressionAndMethodRef);
	}
}
