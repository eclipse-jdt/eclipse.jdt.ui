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
package org.eclipse.jdt.internal.junit.wizards;

import org.eclipse.osgi.util.NLS;

public final class WizardMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.junit.wizards.WizardMessages";//$NON-NLS-1$

	private WizardMessages() {
		// Do not instantiate
	}

	public static String NewTestCaseWizardPageOne__error_junit4NotOnbuildpath;
	public static String NewTestCaseWizardPageOne_error_java5required;
	public static String NewTestCaseWizardPageOne_junit3_radio_label;
	public static String NewTestCaseWizardPageOne_junit4_radio_label;
	public static String NewTestCaseWizardPageOne_linkedtext_java5required;
	public static String NewTestCaseWizardPageOne_linkedtext_junit3_notonbuildpath;
	public static String NewTestCaseWizardPageOne_linkedtext_junit4_notonbuildpath;
	public static String NewTestCaseWizardPageOne_methodStub_setUpBeforeClass;
	public static String NewTestCaseWizardPageOne_methodStub_tearDownAfterClass;
	public static String NewTestCaseWizardPageOne_not_yet_implemented_string;
	public static String Wizard_title_new_testcase;
	public static String Wizard_title_new_testsuite;
	public static String CheckedTableSelectionDialog_emptyListMessage;
	public static String CheckedTableSelectionDialog_selectAll;
	public static String CheckedTableSelectionDialog_deselectAll;
	public static String NewTestCaseWizardPageOne_title;
	public static String NewTestCaseWizardPageOne_description;
	public static String NewTestCaseWizardPageOne_methodStub_setUp;
	public static String NewTestCaseWizardPageOne_methodStub_tearDown;
	public static String NewTestCaseWizardPageOne_methodStub_constructor;
	public static String NewTestCaseWizardPageOne_method_Stub_label;
	public static String NewTestCaseWizardPageOne_class_to_test_label;
	public static String NewTestCaseWizardPageOne_class_to_test_browse;
	public static String NewTestCaseWizardPageOne_class_to_test_dialog_title;
	public static String NewTestCaseWizardPageOne_class_to_test_dialog_message;
	public static String NewTestCaseWizardPageOne_error_superclass_not_exist;
	public static String NewTestCaseWizardPageOne_error_superclass_is_interface;
	public static String NewTestCaseWizardPageOne_error_superclass_not_implementing_test_interface;
	public static String NewTestCaseWizardPageOne_error_superclass_empty;
	public static String NewTestCaseWizardPageOne_error_class_to_test_not_valid;
	public static String NewTestCaseWizardPageOne_error_class_to_test_not_exist;
	public static String NewTestCaseWizardPageOne_warning_class_to_test_is_interface;
	public static String NewTestCaseCreationWizard_fix_selection_junit3_description;
	public static String NewTestCaseCreationWizard_fix_selection_junit4_description;
	public static String NewTestCaseCreationWizard_fix_selection_open_build_path_dialog;
	public static String NewTestCaseCreationWizard_fix_selection_problem_updating_classpath;
	public static String NewTestCaseCreationWizard_fix_selection_invoke_fix;
	public static String NewTestCaseCreationWizard_create_progress;
	public static String NewTestCaseCreationWizard_fix_selection_not_now;
	public static String NewTestCaseWizardPageOne_warning_class_to_test_not_visible;
	public static String NewTestCaseWizardPageOne_comment_class_to_test;
	public static String NewTestCaseWizardPageOne_error_junitNotOnbuildpath;
	public static String NewTestCaseWizardPageTwo_selected_methods_label_one;
	public static String NewTestCaseWizardPageTwo_selected_methods_label_many;
	public static String NewTestCaseWizardPageTwo_title;
	public static String NewTestCaseWizardPageTwo_description;
	public static String NewTestCaseWizardPageTwo_create_tasks_text;
	public static String NewTestCaseWizardPageTwo_create_final_method_stubs_text;
	public static String NewTestCaseWizardPageTwo_methods_tree_label;
	public static String NewTestCaseWizardPageTwo_selectAll;
	public static String NewTestCaseWizardPageTwo_deselectAll;
	public static String NewTestSuiteWiz_unsavedchangesDialog_title;
	public static String NewTestSuiteWiz_unsavedchangesDialog_message;
	public static String NewTestSuiteWizPage_title;
	public static String NewTestSuiteWizPage_description;
	public static String NewTestSuiteWizPage_classes_in_suite_label;
	public static String NewTestSuiteWizPage_selectAll;
	public static String NewTestSuiteWizPage_deselectAll;
	public static String NewTestSuiteWizPage_createType_beginTask;
	public static String NewTestSuiteWizPage_createType_updating_suite_method;
	public static String NewTestSuiteWizPage_createType_updateErrorDialog_title;
	public static String NewTestSuiteWizPage_createType_updateErrorDialog_message;
	public static String NewTestSuiteWizPage_classes_in_suite_error_no_testclasses_selected;
	public static String NewTestSuiteWizPage_typeName_error_name_empty;
	public static String NewTestSuiteWizPage_typeName_error_name_qualified;
	public static String NewTestSuiteWizPage_typeName_error_name_not_valid;
	public static String NewTestSuiteWizPage_typeName_error_name_name_discouraged;
	public static String NewTestSuiteWizPage_typeName_warning_already_exists;
	public static String NewTestSuiteWizPage_cannotUpdateDialog_title;
	public static String NewTestSuiteWizPage_cannotUpdateDialog_message;
	public static String NewTestClassWizPage_treeCaption_classSelected;
	public static String NewTestClassWizPage_treeCaption_classesSelected;
	public static String NewTestSuiteCreationWizardPage_infinite_recursion;
	public static String UpdateAllTests_selected_methods_label_one;
	public static String UpdateAllTests_selected_methods_label_many;
	public static String UpdateAllTests_title;
	public static String UpdateAllTests_message;
	public static String UpdateAllTests_beginTask;
	public static String UpdateAllTests_cannotUpdate_errorDialog_title;
	public static String UpdateAllTests_cannotUpdate_errorDialog_message;
	public static String UpdateAllTests_cannotFind_errorDialog_title;
	public static String UpdateAllTests_cannotFind_errorDialog_message;
	public static String NewJUnitWizard_op_error_title;
	public static String NewJUnitWizard_op_error_message;
	public static String ExceptionDialog_seeErrorLogMessage;
	public static String UpdateTestSuite_infinite_recursion;
	public static String UpdateTestSuite_error;
	public static String UpdateTestSuite_update;
	public static String UpdateTestSuite_could_not_update;

	static {
		NLS.initializeMessages(BUNDLE_NAME, WizardMessages.class);
	}
}