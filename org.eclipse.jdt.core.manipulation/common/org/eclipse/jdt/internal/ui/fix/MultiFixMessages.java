/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.osgi.util.NLS;

/**
 * @since 1.11
 */
public class MultiFixMessages extends NLS {
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.fix.MultiFixMessages"; //$NON-NLS-1$

	private MultiFixMessages() {
	}

	public static String CleanUpRefactoringWizard_CleaningUp11_Title;
	public static String CleanUpRefactoringWizard_CleaningUpN1_Title;
	public static String CleanUpRefactoringWizard_CleaningUpNN_Title;
	public static String CleanUpRefactoringWizard_CleanUpConfigurationPage_title;
	public static String CleanUpRefactoringWizard_Configure_Button;
	public static String CleanUpRefactoringWizard_ConfigureCustomProfile_button;
	public static String CleanUpRefactoringWizard_CustomCleanUpsDialog_title;
	public static String CleanUpRefactoringWizard_EmptySelection_message;
	public static String CleanUpRefactoringWizard_HideWizard_Link;
	public static String CleanUpRefactoringWizard_Profile_TableHeader;
	public static String CleanUpRefactoringWizard_Project_TableHeader;
	public static String CleanUpRefactoringWizard_unknownProfile_Name;
	public static String CleanUpRefactoringWizard_UnmanagedProfileWithName_Name;
	public static String CleanUpRefactoringWizard_use_configured_radio;
	public static String CleanUpRefactoringWizard_use_custom_radio;
	public static String CleanUpRefactoringWizard_XofYCleanUpsSelected_message;
	public static String CodeFormatCleanUp_correctIndentation_description;
	public static String CodeFormatCleanUp_RemoveTrailingAll_description;
	public static String CodeFormatCleanUp_RemoveTrailingNoEmpty_description;
	public static String CodeFormatFix_correctIndentation_changeGroupLabel;
	public static String CodeFormatFix_RemoveTrailingWhitespace_changeDescription;
	public static String ImportsCleanUp_OrganizeImports_Description;
	public static String SortMembersCleanUp_AllMembers_description;
	public static String SortMembersCleanUp_Excluding_description;
	public static String SortMembersCleanUp_RemoveMarkersWarning0;
	public static String StringMultiFix_AddMissingNonNls_description;
	public static String StringMultiFix_RemoveUnnecessaryNonNls_description;

	public static String UnusedCodeMultiFix_RemoveUnusedVariable_description;
	public static String UnusedCodeMultiFix_RemoveUnusedField_description;
	public static String UnusedCodeMultiFix_RemoveUnusedType_description;
	public static String UnusedCodeMultiFix_RemoveUnusedConstructor_description;
	public static String UnusedCodeMultiFix_RemoveUnusedMethod_description;
	public static String UnusedCodeMultiFix_RemoveUnusedImport_description;
	public static String UnusedCodeCleanUp_RemoveUnusedCasts_description;

	public static String CodeStyleMultiFix_ChangeNonStaticAccess_description;
	public static String CodeStyleMultiFix_AddThisQualifier_description;
	public static String CodeStyleMultiFix_QualifyAccessToStaticField;
	public static String CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect;
	public static String CodeStyleMultiFix_ConvertSingleStatementInControlBodyToBlock_description;
	public static String CodeStyleCleanUp_addDefaultSerialVersionId_description;

	public static String CodeStyleCleanUp_ExtractIncrement_description;
	public static String CodeStyleCleanUp_PullUpAssignment_description;
	public static String CodeStyleCleanUp_ElseIf_description;
	public static String CodeStyleCleanUp_ReduceIndentation_description;
	public static String CodeStyleCleanUp_Instanceof_description;
	public static String CodeStyleCleanUp_numberSuffix_description;
	public static String CodeStyleCleanUp_QualifyNonStaticMethod_description;
	public static String CodeStyleCleanUp_QualifyStaticMethod_description;
	public static String CodeStyleCleanUp_removeFieldThis_description;
	public static String CodeStyleCleanUp_removeMethodThis_description;
	public static String CodeStyleCleanUp_Switch_description;

	public static String Java50MultiFix_AddMissingDeprecated_description;
	public static String Java50MultiFix_AddMissingOverride_description;
	public static String Java50MultiFix_AddMissingOverride_description2;
	public static String Java50CleanUp_ConvertLoopOnlyIfLoopVarUsed_description;
	public static String Java50CleanUp_ConvertToEnhancedForLoop_description;
	public static String Java50CleanUp_AddTypeParameters_description;

	public static String SerialVersionCleanUp_Generated_description;

	public static String CleanUpRefactoringWizard_WindowTitle;
	public static String CleanUpRefactoringWizard_PageTitle;
	public static String CleanUpRefactoringWizard_formatterException_errorMessage;

	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocks_description;
	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocksWithReturnOrThrow_description;

	public static String SwitchExpressionsCleanUp_ConvertToSwitchExpressions_description;

	public static String UnimplementedCodeCleanUp_AddUnimplementedMethods_description;
	public static String UnimplementedCodeCleanUp_MakeAbstract_description;

	public static String ExpressionsCleanUp_addParanoiac_description;
	public static String ExpressionsCleanUp_removeUnnecessary_description;

	public static String VariableDeclarationCleanUp_AddFinalField_description;
	public static String VariableDeclarationCleanUp_AddFinalParameters_description;
	public static String VariableDeclarationCleanUp_AddFinalLocals_description;

	public static String CodeFormatCleanUp_description;
	public static String CodeFormatFix_description;

	public static String VarCleanUp_description;
	public static String PatternMatchingForInstanceofCleanup_description;
	public static String LambdaExpressionsCleanUp_use_lambda_where_possible;
	public static String LambdaExpressionsCleanUp_use_anonymous;
	public static String LambdaExpressionAndMethodRefCleanUp_description;
	public static String PatternCleanup_description;
	public static String NoStringCreationCleanUp_description;
	public static String BooleanLiteralCleanup_description;

	public static String NullAnnotationsCleanUp_add_nullable_annotation;
	public static String NullAnnotationsCleanUp_add_nonnull_annotation;
	public static String NullAnnotationsCleanUp_add_nonnullbydefault_annotation;
	public static String NullAnnotationsCleanUp_remove_redundant_nullness_annotation;

	public static String SingleUsedFieldCleanUp_description;
	public static String SingleUsedFieldCleanUp_description_old_field_declaration;
	public static String SingleUsedFieldCleanUp_description_new_local_var_declaration;
	public static String SingleUsedFieldCleanUp_description_uses_of_the_var;
	public static String BreakLoopCleanUp_description;
	public static String DoWhileRatherThanWhileCleanUp_description;
	public static String StaticInnerClassCleanUp_description;
	public static String StringBuilderCleanUp_description;
	public static String PlainReplacementCleanUp_description;
	public static String CodeStyleCleanUp_LazyLogical_description;
	public static String ValueOfRatherThanInstantiationCleanup_description;
	public static String ValueOfRatherThanInstantiationCleanup_description_float_with_valueof;
	public static String ValueOfRatherThanInstantiationCleanup_description_float_with_float_value;
	public static String ValueOfRatherThanInstantiationCleanup_description_single_argument;
	public static String ValueOfRatherThanInstantiationCleanup_description_valueof;
	public static String PrimitiveComparisonCleanUp_description;
	public static String PrimitiveParsingCleanUp_description;
	public static String PrimitiveSerializationCleanUp_description;
	public static String PrimitiveRatherThanWrapperCleanUp_description;

	public static String TypeParametersCleanUp_InsertInferredTypeArguments_description;
	public static String TypeParametersCleanUp_RemoveUnnecessaryTypeArguments_description;
	public static String HashCleanup_description;

	public static String RedundantModifiersCleanup_description;
	public static String SubstringCleanUp_description;
	public static String JoinCleanup_description;
	public static String ArraysFillCleanUp_description;
	public static String EvaluateNullableCleanUp_description;
	public static String EmbeddedIfCleanup_description;
	public static String AutoboxingCleanup_description;
	public static String UnboxingCleanup_description;
	public static String PushDownNegationCleanup_description;
	public static String BooleanValueRatherThanComparisonCleanUp_description;
	public static String DoubleNegationCleanUp_description;
	public static String RedundantComparisonStatementCleanup_description;
	public static String RedundantSuperCallCleanup_description;
	public static String UnreachableBlockCleanUp_description;
	public static String TernaryOperatorCleanUp_description;
	public static String StrictlyEqualOrDifferentCleanUp_description;
	public static String MergeConditionalBlocksCleanup_description;
	public static String MergeConditionalBlocksCleanup_description_inner_if;
	public static String MergeConditionalBlocksCleanup_description_if_suite;
	public static String ControlFlowMergeCleanUp_description;
	public static String RedundantFallingThroughBlockEndCleanup_description;
	public static String RedundantIfConditionCleanup_description;
	public static String UseDirectlyMapMethodCleanup_description;
	public static String CollectionCloningCleanUp_description;
	public static String MapCloningCleanUp_description;
	public static String OverriddenAssignmentCleanUp_description;
	public static String RedundantSemicolonsCleanup_description;
	public static String RedundantComparatorCleanUp_description;
	public static String UnnecessaryArrayCreationCleanup_description;
	public static String ArrayWithCurlyCleanup_description;
	public static String ReturnExpressionCleanUp_description;
	public static String UselessReturnCleanUp_description;
	public static String UselessContinueCleanUp_description;
	public static String UnloopedWhileCleanUp_description;
	public static String AddAllCleanup_description;
	public static String ObjectsEqualsCleanup_description;

	public static String OperandFactorizationCleanUp_description;
	public static String OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp_description;
	public static String PullOutIfFromIfElseCleanUp_description;

	public static String InvertEqualsCleanUp_description;
	public static String CheckSignOfBitwiseOperation_description;
	public static String StandardComparisonCleanUp_description;

	public static String ComparingOnCriteriaCleanUp_description;
	public static String TryWithResourceCleanup_description;
	public static String MultiCatchCleanUp_description;
	public static String ConstantsCleanUp_description;
	public static String StringBufferToStringBuilderCleanUp_description;
	public static String StringBuilderForLocalVarsOnlyCleanUp_description;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MultiFixMessages.class);
	}

}
