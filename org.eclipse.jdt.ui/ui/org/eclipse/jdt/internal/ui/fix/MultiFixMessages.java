/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.osgi.util.NLS;

public class MultiFixMessages extends NLS {
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.fix.MultiFixMessages"; //$NON-NLS-1$

	private MultiFixMessages() {
	}

	public static String StringMultiFix_AddMissingNonNls_description;
	public static String StringMultiFix_RemoveUnnecessaryNonNls_description;
	public static String StringCleanUp_RemoveNLSTag_label;
	
	public static String UnusedCodeMultiFix_RemoveUnusedVariable_description;
	public static String UnusedCodeMultiFix_RemoveUnusedField_description;
	public static String UnusedCodeMultiFix_RemoveUnusedType_description;
	public static String UnusedCodeMultiFix_RemoveUnusedConstructor_description;
	public static String UnusedCodeMultiFix_RemoveUnusedMethod_description;
	public static String UnusedCodeMultiFix_RemoveUnusedImport_description;
	public static String UnusedCodeCleanUp_RemoveUnusedCasts_description;
	public static String UnusedCodeCleanUp_unusedImports_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedPrivateMembers_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedTypes_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedConstructors_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedMethods_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedFields_checkBoxLabel;
	public static String UnusedCodeCleanUp_unusedLocalVariables_checkBoxLabel;
	public static String UnusedCodeCleanUp_unnecessaryCasts_checkBoxLabel;
	
	public static String CodeStyleMultiFix_ChangeNonStaticAccess_description;
	public static String CodeStyleMultiFix_AddThisQualifier_description;
	public static String CodeStyleMultiFix_QualifyAccessToStaticField;
	public static String CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect;
	public static String CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description;
	public static String CodeStyleCleanUp_addDefaultSerialVersionId_description;
	public static String CodeStyleCleanUp_useThis_checkBoxLabel;
	public static String CodeStyleCleanUp_useDeclaring_checkBoxLabel;
	public static String CodeStyleCleanUp_changeNonStatic_checkBoxLabel;
	public static String CodeStyleCleanUp_changeIndirect_checkBoxLabel;
	public static String CodeStyleCleanUp_addStaticQualifier_checkBoxLabel;
	public static String CodeStyleCleanUp_QualifyNonStaticMethod_description;
	public static String CodeStyleCleanUp_QualifyStaticMethod_description;
	public static String CodeStyleCleanUp_removeFieldThis_description;
	public static String CodeStyleCleanUp_removeMethodThis_description;
	
	public static String Java50MultiFix_AddMissingDeprecated_description;
	public static String Java50MultiFix_AddMissingOverride_description;
	public static String Java50CleanUp_ConvertToEnhancedForLoop_description;
	public static String Java50CleanUp_AddTypeParameters_description;
	public static String Java50CleanUp_addMissingAnnotations_checkBoxLabel;
	public static String Java50CleanUp_override_checkBoxLabel;
	public static String Java50CleanUp_deprecated_checkBoxLabel;

	public static String SerialVersionCleanUp_Generated_description;
	
	public static String CleanUpRefactoringWizard_SelectCleanUpsPage_message;
	public static String CleanUpRefactoringWizard_SelectCleanUpsPage_preSingleSelect_message;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_message;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_preSingleSelect_message;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_preSelect_message;
	public static String CleanUpRefactoringWizard_SelectCleanUpsPage_name;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_name;
	public static String CleanUpRefactoringWizard_WindowTitle;
	public static String CleanUpRefactoringWizard_PageTitle;
	public static String CleanUpRefactoringWizard_CodeStyleSection_description;
	public static String CleanUpRefactoringWizard_UnusedCodeSection_description;
	public static String CleanUpRefactoringWizard_PotentialProgrammingProblems_description;
	public static String CleanUpRefactoringWizard_Annotations_sectionName;
	public static String CleanUpRefactoringWizard_Remove_sectionTitle;
	public static String CleanUpRefactoringWizard_memberAccesses_sectionDescription;
	public static String CleanUpRefactoringWizard_controlStatements_sectionDescription;
	public static String CleanUpRefactoringWizard_UnnecessaryCode_tabLabel;
	public static String CleanUpRefactoringWizard_MissingCode_tabLabel;
	public static String CleanUpRefactoringWizard_UnnecessaryCode_section;
	public static String CleanUpRefactoringWizard_EnableAllButton_label;
	public static String CleanUpRefactoringWizard_DisableAllButton_label;
	public static String CleanUpRefactoringWizard_EnableDefaultsButton_label;
	public static String CleanUpRefactoringWizard_expressions_sectionDescription;
	public static String CleanUpRefactoringWizard_statusLineText;
	public static String CleanUpRefactoringWizard_formatterException_errorMessage;
	public static String CleanUpRefactoringWizard_previewLabel_text;
	public static String CleanUpRefactoringWizard_qualifyNonStaticField_checkBoxLabel;
	public static String CleanUpRefactoringWizard_qualifyNonStaticMethod_checkBoxLabel;
	public static String CleanUpRefactoringWizard_qualifyStaticMethod_checkBoxLabel;
	public static String CleanUpRefactoringWizard_removeThis_checkBoxLabel;
	public static String CleanUpRefactoringWizard_removeMethodThis_checkBoxLabel;
	public static String CleanUpRefactoringWizard_addMethodThis_checkBoxLabel;
	public static String CleanUpRefactoringWizard_variableDeclaration_groupDescription;
	public static String CleanUpRefactoringWizard_addFinalFields_checkBoxLabel;
	public static String CleanUpRefactoringWizard_addFinalParameters_checkBoxLabel;
	public static String CleanUpRefactoringWizard_addFinalLocals_checkBoxLabel;
	public static String CleanUpRefactoringWizard_changeToFinal_checkBoxLabel;
	public static String CleanUpRefactoringWizard_NonStaticAccesses_groupDescription;
	public static String CleanUpRefactoringWizard_StaticAccesses_groupDescription;
	public static String CleanUpRefactoringWizard_NoBlockForReturnOrThrow_checkBoxLabel;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatCodeCheckBox;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatCommentsCheckBox;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatGroupName;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatJavadocCheckBox;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatMultiLineCommentCheckBox;
	public static String CleanUpRefactoringWizard_FormatingGroup_formatSingleLineCommentCheckBox;
	public static String CleanUpRefactoringWizard_FormatingGroup_goToFormatPreferenceLink;
	public static String CleanUpRefactoringWizard_FormatingGroup_goToFormatPreferenceToolTip;
	public static String CleanUpRefactoringWizard_FormatingGroup_tabName;

	public static String PotentialProgrammingProblemsCleanUp_AddSerialId_section_name;
	public static String PotentialProgrammingProblemsCleanUp_Generated_radioButton_name;
	public static String PotentialProgrammingProblemsCleanUp_RandomSerialId_description;
	public static String PotentialProgrammingProblemsCleanUp_Default_radioButton_name;
	
	public static String ControlStatementsCleanUp_useBlocks_checkBoxLabel;
	public static String ControlStatementsCleanUp_convertLoops_checkBoxLabel;
	public static String ControlStatementsCleanUp_always_checkBoxLabel;
	public static String ControlStatementsCleanUp_removeIfPossible_checkBoxLabel;
	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocks_description;
	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocksWithReturnOrThrow_description;

	public static String ExpressionsCleanUp_parenthesisAroundConditions_checkBoxLabel;
	public static String ExpressionsCleanUp_addParanoiac_checkBoxLabel;
	public static String ExpressionsCleanUp_removeUnnecessary_checkBoxLabel;
	public static String ExpressionsCleanUp_addParanoiac_description;
	public static String ExpressionsCleanUp_removeUnnecessary_description;
	
	public static String VariableDeclarationCleanUp_AddFinalField_description;
	public static String VariableDeclarationCleanUp_AddFinalParameters_description;
	public static String VariableDeclarationCleanUp_AddFinalLocals_description;
	
	public static String CodeFormatCleanUp_description;
	public static String CodeFormatFix_description;
	
	public static String CommentFormatCleanUp_javadocComments;
	public static String CommentFormatCleanUp_multiLineComments;
	public static String CommentFormatCleanUp_singleLineComments;
	public static String CommentFormatFix_description;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MultiFixMessages.class);
	}

}
