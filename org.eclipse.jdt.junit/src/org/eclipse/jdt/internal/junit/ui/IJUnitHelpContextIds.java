/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

/**
 * Help context ids for the JUnit UI.
 */
public interface IJUnitHelpContextIds {
	public static final String PREFIX= JUnitPlugin.PLUGIN_ID + '.';

	// Actions
	public static final String COPYTRACE_ACTION= PREFIX + "copy_trace_action_context"; //$NON-NLS-1$
	public static final String ENABLEFILTER_ACTION= PREFIX + "enable_filter_action_context"; //$NON-NLS-1$
	public static final String OPENEDITORATLINE_ACTION= PREFIX + "open_editor_atline_action_context"; //$NON-NLS-1$
	public static final String OPENTEST_ACTION= PREFIX + "open_test_action_context"; //$NON-NLS-1$
	public static final String RERUN_ACTION= PREFIX + "retrun_test_action_context"; //$NON-NLS-1$
	
	// view parts
	public static final String RESULTS_VIEW= PREFIX + "results_view_context"; //$NON-NLS-1$

	// Preference/Property pages
	public static final String JUNIT_PREFERENCE_PAGE= PREFIX + "junit_preference_page_context"; //$NON-NLS-1$

	// Wizard pages
	public static final String NEW_TESTCASE_WIZARD_PAGE= PREFIX + "new_testcase_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_TESTCASE_WIZARD_PAGE2= PREFIX + "new_testcase_wizard_page2_context"; //$NON-NLS-1$
	public static final String NEW_TESTSUITE_WIZARD_PAGE= PREFIX + "new_testsuite_wizard_page2_context"; //$NON-NLS-1$
	
	// Dialogs
	public static final String TEST_SELECTION_DIALOG= PREFIX + "test_selection_context"; //$NON-NLS-1$
}