/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import org.eclipse.osgi.util.NLS;

public class JavaManipulationMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.core.manipulation.JavaManipulationMessages"; //$NON-NLS-1$

	private JavaManipulationMessages() {
	}

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, JavaManipulationMessages.class);
	}

	public static String JavaManipulationMessages_internalError;
	public static String UndoCompilationUnitChange_no_file;

	public static String OrganizeImportsOperation_description;
	public static String JavaModelUtil_applyedit_operation;
	public static String Resources_fileModified;
	public static String Resources_modifiedResources;

	public static String CodeAnalyzer_array_initializer;

	public static String CommentAnalyzer_starts_inside_comment;
	public static String CommentAnalyzer_ends_inside_comment;
	public static String CommentAnalyzer_internal_error;

	public static String StatementAnalyzer_end_of_selection;
	public static String StatementAnalyzer_beginning_of_selection;
	public static String StatementAnalyzer_do_body_expression;
	public static String StatementAnalyzer_for_initializer_expression;
	public static String StatementAnalyzer_for_expression_updater;
	public static String StatementAnalyzer_for_updater_body;
	public static String StatementAnalyzer_switch_statement;
	public static String StatementAnalyzer_synchronized_statement;
	public static String StatementAnalyzer_try_statement;
	public static String StatementAnalyzer_catch_argument;
	public static String StatementAnalyzer_while_expression_body;

	public static String SurroundWithTryCatchAnalyzer_compile_errors;
	public static String SurroundWithTryCatchAnalyzer_doesNotContain;
	public static String SurroundWithTryCatchAnalyzer_doesNotCover;
	public static String SurroundWithTryCatchAnalyzer_cannotHandleThis;
	public static String SurroundWithTryCatchAnalyzer_cannotHandleSuper;
	public static String SurroundWithTryCatchAnalyzer_onlyStatements;
}
