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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import org.eclipse.osgi.util.NLS;

public class CleanUpMessages extends NLS {
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpMessages"; //$NON-NLS-1$

	public static String CleanUpConfigurationBlock_SelectedCleanUps_label;
	public static String CleanUpConfigurationBlock_ShowCleanUpWizard_checkBoxLabel;

	public static String CleanUpModifyDialog_SelectOne_Error;
	public static String CleanUpModifyDialog_XofYSelected_Label;

	public static String CleanUpProfileManager_ProfileName_EclipseBuildIn;

	public static String CodeFormatingTabPage_CheckboxName_FormatSourceCode;

	public static String CodeFormatingTabPage_correctIndentation_checkbox_text;
	public static String CodeFormatingTabPage_FormatterSettings_Description;
	public static String CodeFormatingTabPage_GroupName_Formatter;
	public static String CodeFormatingTabPage_Imports_GroupName;
	public static String CodeFormatingTabPage_OrganizeImports_CheckBoxLable;
	public static String CodeFormatingTabPage_OrganizeImportsSettings_Description;
	public static String CodeFormatingTabPage_SortMembers_GroupName;
	public static String CodeFormatingTabPage_SortMembers_CheckBoxLabel;
	public static String CodeFormatingTabPage_SortMembers_Description;

	public static String CodeFormatingTabPage_SortMembersExclusive_radio0;
	public static String CodeFormatingTabPage_SortMembersFields_CheckBoxLabel;

	public static String CodeFormatingTabPage_RemoveTrailingWhitespace_all_radio;

	public static String CodeFormatingTabPage_RemoveTrailingWhitespace_checkbox_text;

	public static String CodeFormatingTabPage_RemoveTrailingWhitespace_ignoreEmpty_radio;

	public static String CodeFormatingTabPage_SortMembersSemanticChange_warning;

	public static String CodeStyleTabPage_CheckboxName_Switch;
	public static String CodeStyleTabPage_CheckboxName_UseAddAllRemoveAll;
	public static String CodeStyleTabPage_CheckboxName_UseBlocks;
	public static String CodeStyleTabPage_CheckboxName_UseFinal;
	public static String CodeStyleTabPage_CheckboxName_UseFinalForFields;
	public static String CodeStyleTabPage_CheckboxName_UseFinalForLocals;
	public static String CodeStyleTabPage_CheckboxName_UseFinalForParameters;

	public static String CodeStyleTabPage_CheckboxName_UseParentheses;
	public static String CodeStyleTabPage_GroupName_ControlStatements;

	public static String CodeStyleTabPage_GroupName_Expressions;
	public static String CodeStyleTabPage_CheckboxName_ExtractIncrement;
	public static String CodeStyleTabPage_CheckboxName_PullUpAssignment;
	public static String CodeStyleTabPage_CheckboxName_ElseIf;
	public static String CodeStyleTabPage_CheckboxName_ReduceIndentation;
	public static String CodeStyleTabPage_CheckboxName_Instanceof;

	public static String CodeStyleTabPage_GroupName_NumberLiteral;
	public static String CodeStyleTabPage_CheckboxName_NumberSuffix;

	public static String CodeStyleTabPage_GroupName_VariableDeclarations;
	public static String CodeStyleTabPage_RadioName_AlwaysUseBlocks;
	public static String CodeStyleTabPage_RadioName_AlwaysUseParantheses;
	public static String CodeStyleTabPage_RadioName_NeverUseBlocks;
	public static String CodeStyleTabPage_RadioName_NeverUseParantheses;

	public static String CodeStyleTabPage_RadioName_UseBlocksSpecial;

	public static String CodeStyleTabPage_CheckboxName_SimplifyLambdaExpressionAndMethodRefSyntax;

	public static String PerformanceTabPage_GroupName_Performance;

	public static String PerformanceTabPage_CheckboxName_SingleUsedField;
	public static String PerformanceTabPage_CheckboxName_BreakLoop;
	public static String PerformanceTabPage_CheckboxName_StaticInnerClass;
	public static String PerformanceTabPage_CheckboxName_StringBuilder;
	public static String PerformanceTabPage_CheckboxName_PlainReplacement;
	public static String PerformanceTabPage_CheckboxName_UseStringIsBlank;
	public static String PerformanceTabPage_CheckboxName_UseLazyLogicalOperator;
	public static String PerformanceTabPage_CheckboxName_ValueOfRatherThanInstantiation;
	public static String PerformanceTabPage_CheckboxName_PrimitiveComparison;
	public static String PerformanceTabPage_CheckboxName_PrimitiveParsing;
	public static String PerformanceTabPage_CheckboxName_PrimitiveSerialization;
	public static String PerformanceTabPage_CheckboxName_PrimitiveRatherThanWrapper;
	public static String PerformanceTabPage_CheckboxName_PrecompileRegEx;
	public static String PerformanceTabPage_CheckboxName_StringBufferToStringBuilder;
	public static String PerformanceTabPage_CheckboxName_StringBufferToStringBuilderLocalsOnly;
	public static String PerformanceTabPage_CheckboxName_NoStringCreation;
	public static String PerformanceTabPage_CheckboxName_BooleanLiteral;

	public static String ContributedCleanUpTabPage_ErrorPage_message;

	public static String MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughInstances;
	public static String MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughSubtypes;
	public static String MemberAccessesTabPage_CheckboxName_FieldQualifier;
	public static String MemberAccessesTabPage_CheckboxName_MethodQualifier;
	public static String MemberAccessesTabPage_CheckboxName_QualifyFieldWithDeclaringClass;
	public static String MemberAccessesTabPage_CheckboxName_QualifyMethodWithDeclaringClass;
	public static String MemberAccessesTabPage_CheckboxName_QualifyWithDeclaringClass;
	public static String MemberAccessesTabPage_GroupName_NonStaticAccesses;
	public static String MemberAccessesTabPage_GroupName_StaticAccesses;
	public static String MemberAccessesTabPage_RadioName_AlwaysThisForFields;
	public static String MemberAccessesTabPage_RadioName_AlwaysThisForMethods;
	public static String MemberAccessesTabPage_RadioName_NeverThisForFields;
	public static String MemberAccessesTabPage_RadioName_NeverThisForMethods;


	public static String MissingCodeTabPage_CheckboxName_AddMissingAnnotations;
	public static String MissingCodeTabPage_CheckboxName_AddMissingDeprecatedAnnotations;
	public static String MissingCodeTabPage_CheckboxName_AddMissingOverrideAnnotations;

	public static String MissingCodeTabPage_CheckboxName_AddMissingOverrideInterfaceAnnotations;
	public static String MissingCodeTabPage_CheckboxName_AddMethods;
	public static String MissingCodeTabPage_CheckboxName_AddSUID;
	public static String MissingCodeTabPage_GroupName_Annotations;
	public static String MissingCodeTabPage_GroupName_UnimplementedCode;
	public static String MissingCodeTabPage_GroupName_PotentialProgrammingProblems;
	public static String MissingCodeTabPage_Label_CodeTemplatePreferencePage;
	public static String MissingCodeTabPage_RadioName_AddDefaultSUID;
	public static String MissingCodeTabPage_RadioName_AddGeneratedSUID;

	public static String UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags;
	public static String UnnecessaryCodeTabPage_CheckboxName_Substring;
	public static String UnnecessaryCodeTabPage_CheckboxName_ArraysFill;
	public static String UnnecessaryCodeTabPage_CheckboxName_EvaluateNullable;
	public static String UnnecessaryCodeTabPage_CheckboxName_PushDownNegation;
	public static String UnnecessaryCodeTabPage_CheckboxName_BooleanValueRatherThanComparison;
	public static String UnnecessaryCodeTabPage_CheckboxName_DoubleNegation;
	public static String UnnecessaryCodeTabPage_CheckboxName_ComparisonStatement;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantSuperCall;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnreachableBlock;
	public static String UnnecessaryCodeTabPage_CheckboxName_UseDirectlyMapMethod;
	public static String UnnecessaryCodeTabPage_CheckboxName_CollectionCloning;
	public static String UnnecessaryCodeTabPage_CheckboxName_MapCloning;
	public static String UnnecessaryCodeTabPage_CheckboxName_OverriddenAssignment;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantModifiers;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantModifiers_description;
	public static String UnnecessaryCodeTabPage_CheckboxName_EmbeddedIf;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantSemicolons;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantSemicolons_description;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantComparator;
	public static String UnnecessaryCodeTabPage_CheckboxName_RedundantArrayCreation_description;
	public static String UnnecessaryCodeTabPage_CheckboxName_ArrayWithCurly;
	public static String UnnecessaryCodeTabPage_CheckboxName_ReturnExpression;
	public static String UnnecessaryCodeTabPage_CheckboxName_UselessReturn;
	public static String UnnecessaryCodeTabPage_CheckboxName_UselessContinue;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnloopedWhile;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnnecessaryVarargsArrayCreation;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedFields;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedImports;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedMembers;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedMethods;
	public static String UnnecessaryCodeTabPage_CheckboxName_UnusedTypes;
	public static String UnnecessaryCodeTabPage_GroupName_UnnecessaryCode;
	public static String UnnecessaryCodeTabPage_GroupName_UnusedCode;

	public static String SourceFixingTabPage_warning;

	public static String SourceFixingTabPage_GroupName_standardCode;

	public static String SourceFixingTabPage_CheckboxName_InvertEquals;
	public static String SourceFixingTabPage_CheckboxName_CheckSignOfBitwiseOperation;
	public static String SourceFixingTabPage_CheckboxName_StandardComparison;

	public static String DuplicateCodeTabPage_GroupName_DuplicateCode;

	public static String DuplicateCodeTabPage_CheckboxName_OperandFactorization;
	public static String DuplicateCodeTabPage_CheckboxName_TernaryOperator;
	public static String DuplicateCodeTabPage_CheckboxName_StrictlyEqualOrDifferent;
	public static String DuplicateCodeTabPage_CheckboxName_MergeConditionalBlocks;
	public static String DuplicateCodeTabPage_CheckboxName_ControlFlowMerge;
	public static String DuplicateCodeTabPage_CheckboxName_OneIfRatherThanDuplicateBlocksThatFallThrough;
	public static String DuplicateCodeTabPage_CheckboxName_RedundantFallingThroughBlockEnd;
	public static String DuplicateCodeTabPage_CheckboxName_RedundantIfCondition;
	public static String DuplicateCodeTabPage_CheckboxName_PullOutIfFromIfElse;

	public static String JavaFeatureTabPage_GroupName_Java16;
	public static String JavaFeatureTabPage_CheckboxName_PatternMatchingForInstanceof;

	public static String JavaFeatureTabPage_GroupName_Java14;
	public static String JavaFeatureTabPage_CheckboxName_ConvertToSwitchExpressions;

	public static String JavaFeatureTabPage_GroupName_Java10;

	public static String JavaFeatureTabPage_CheckboxName_UseVar;

	public static String JavaFeatureTabPage_GroupName_Java1d8;

	public static String JavaFeatureTabPage_GroupName_FunctionalInterfaces;
	public static String JavaFeatureTabPage_CheckboxName_ConvertFunctionalInterfaces;
	public static String JavaFeatureTabPage_RadioName_UseLambdaWherePossible;
	public static String JavaFeatureTabPage_RadioName_UseAnonymous;
	public static String JavaFeatureTabPage_CheckboxName_ComparingOnCriteria;
	public static String JavaFeatureTabPage_CheckboxName_Join;

	public static String JavaFeatureTabPage_GroupName_Java1d7;

	public static String JavaFeatureTabPage_CheckboxName_TryWithResource;
	public static String JavaFeatureTabPage_CheckboxName_MultiCatch;
	public static String JavaFeatureTabPage_CheckboxName_RedundantTypeArguments;
	public static String JavaFeatureTabPage_CheckboxName_Hash;
	public static String JavaFeatureTabPage_CheckboxName_ObjectsEquals;

	public static String JavaFeatureTabPage_GroupName_Java1d5;

	public static String JavaFeatureTabPage_CheckboxName_ConvertForLoopToEnhanced;
	public static String JavaFeatureTabPage_CheckboxName_ConvertLoopOnlyIfLoopVarUsed;
	public static String JavaFeatureTabPage_CheckboxName_Autoboxing;
	public static String JavaFeatureTabPage_CheckboxName_Unboxing;

	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty;
	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_FileSeparator;
	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_PathSeparator;
	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_LineSeparator;
	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_FileEncoding;
	public static String JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_BooleanProperty;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CleanUpMessages.class);
	}

	private CleanUpMessages() {
	}
}
