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

	public static String CleanUpRefactoringWizard_CleaningUp11_Title;
	public static String CleanUpRefactoringWizard_CleaningUpN1_Title;
	public static String CleanUpRefactoringWizard_CleaningUpNN_Title;
	public static String CleanUpRefactoringWizard_CleanUpConfigurationPage_title;
	public static String CleanUpRefactoringWizard_Configure_Button;
	public static String CleanUpRefactoringWizard_ConfiguredProfiles_Label;
	public static String CleanUpRefactoringWizard_Profile_TableHeader;
	public static String CleanUpRefactoringWizard_Project_TableHeader;
	public static String CleanUpRefactoringWizard_unknownProfile_Name;
	public static String ImportsCleanUp_OrganizeImports_Description;
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
	public static String CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description;
	public static String CodeStyleCleanUp_addDefaultSerialVersionId_description;
	public static String CodeStyleCleanUp_QualifyNonStaticMethod_description;
	public static String CodeStyleCleanUp_QualifyStaticMethod_description;
	public static String CodeStyleCleanUp_removeFieldThis_description;
	public static String CodeStyleCleanUp_removeMethodThis_description;
	
	public static String Java50MultiFix_AddMissingDeprecated_description;
	public static String Java50MultiFix_AddMissingOverride_description;
	public static String Java50CleanUp_ConvertToEnhancedForLoop_description;
	public static String Java50CleanUp_AddTypeParameters_description;

	public static String SerialVersionCleanUp_Generated_description;
	
	public static String CleanUpRefactoringWizard_WindowTitle;
	public static String CleanUpRefactoringWizard_PageTitle;
	public static String CleanUpRefactoringWizard_formatterException_errorMessage;
	
	public static String PotentialProgrammingProblemsCleanUp_RandomSerialId_description;
	
	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocks_description;
	public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocksWithReturnOrThrow_description;

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
