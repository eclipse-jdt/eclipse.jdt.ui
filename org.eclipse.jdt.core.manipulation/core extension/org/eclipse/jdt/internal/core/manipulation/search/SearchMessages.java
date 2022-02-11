/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.manipulation.search;

import org.eclipse.osgi.util.NLS;

public final class SearchMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.core.manipulation.search.SearchMessages";//$NON-NLS-1$

	private SearchMessages() {
		// Do not instantiate
	}

	static {
		NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
	}

	public static String OccurrencesFinder_no_element;
	public static String OccurrencesFinder_no_binding;
	public static String OccurrencesFinder_searchfor;
	public static String OccurrencesFinder_label_singular;
	public static String OccurrencesFinder_label_plural;
	public static String OccurrencesFinder_occurrence_description;
	public static String OccurrencesFinder_occurrence_write_description;
	public static String OccurrencesSearchLabelProvider_line_number;

	public static String ExceptionOccurrencesFinder_no_exception;
	public static String ExceptionOccurrencesFinder_searchfor;
	public static String ExceptionOccurrencesFinder_label_singular;
	public static String ExceptionOccurrencesFinder_label_plural;
	public static String ExceptionOccurrencesFinder_occurrence_description;
	public static String ExceptionOccurrencesFinder_occurrence_implicit_close_description;

	public static String ImplementOccurrencesFinder_invalidTarget;
	public static String ImplementOccurrencesFinder_searchfor;
	public static String ImplementOccurrencesFinder_label_singular;
	public static String ImplementOccurrencesFinder_label_plural;
	public static String ImplementOccurrencesFinder_occurrence_description;

	public static String MethodExitsFinder_job_label;
	public static String MethodExitsFinder_label_plural;
	public static String MethodExitsFinder_label_singular;
	public static String MethodExitsFinder_no_return_type_selected;
	public static String MethodExitsFinder_occurrence_exit_description;
	public static String MethodExitsFinder_occurrence_exit_impclict_close_description;
	public static String MethodExitsFinder_occurrence_return_description;

	public static String BreakContinueTargetFinder_break_label_plural;
	public static String BreakContinueTargetFinder_break_label_singular;
	public static String BreakContinueTargetFinder_cannot_highlight;
	public static String BreakContinueTargetFinder_continue_label_plural;
	public static String BreakContinueTargetFinder_continue_label_singular;
	public static String BreakContinueTargetFinder_job_label;
	public static String BreakContinueTargetFinder_no_break_or_continue_selected;
	public static String BreakContinueTargetFinder_occurrence_description;

}
