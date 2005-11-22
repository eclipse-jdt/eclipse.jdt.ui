/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
	
	public static String UnusedCodeMultiFix_RemoveUnusedVariable_description;
	public static String UnusedCodeMultiFix_RemoveUnusedField_description;
	public static String UnusedCodeMultiFix_RemoveUnusedType_description;
	public static String UnusedCodeMultiFix_RemoveUnusedConstructor_description;
	public static String UnusedCodeMultiFix_RemoveUnusedMethod_description;
	public static String UnusedCodeMultiFix_RemoveUnusedImport_description;
	
	public static String CodeStyleMultiFix_ChangeNonStaticAccess_description;
	public static String CodeStyleMultiFix_AddThisQualifier_description;
	public static String CodeStyleMultiFix_QualifyAccessToStaticField;
	public static String CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect;
	public static String CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description;
	
	public static String Java50MultiFix_AddMissingDeprecated_description;
	public static String Java50MultiFix_AddMissingOverride_description;
	
	public static String CleanUpRefactoringWizard_SelectCleanUpsPage_message;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_message;
	public static String CleanUpRefactoringWizard_SelectCleanUpsPage_name;
	public static String CleanUpRefactoringWizard_SelectCompilationUnitsPage_name;
	public static String CleanUpRefactoringWizard_WindowTitle;
	public static String CleanUpRefactoringWizard_PageTitle;
	public static String CleanUpRefactoringWizard_CodeStyleSection_description;
	public static String CleanUpRefactoringWizard_J2SE50Section_description;
	public static String CleanUpRefactoringWizard_UnusedCodeSection_description;
	public static String CleanUpRefactoringWizard_StringExternalization_description;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MultiFixMessages.class);
	}
}
