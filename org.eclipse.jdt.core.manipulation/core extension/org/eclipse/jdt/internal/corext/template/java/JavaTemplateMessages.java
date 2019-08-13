/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public final class JavaTemplateMessages extends NLS {

	private static final String BUNDLE_NAME= JavaTemplateMessages.class.getName();

	private JavaTemplateMessages() {
		// Do not instantiate
	}

	public static String ContextType_error_multiple_cursor_variables;
	public static String CompilationUnitContextType_variable_description_file;
	public static String CompilationUnitContextType_variable_description_primary_type_name;
	public static String CompilationUnitContextType_variable_description_enclosing_method;
	public static String CompilationUnitContextType_variable_description_enclosing_type;
	public static String CompilationUnitContextType_variable_description_enclosing_package;
	public static String CompilationUnitContextType_variable_description_enclosing_project;
	public static String CompilationUnitContextType_variable_description_enclosing_method_arguments;
	public static String CompilationUnitContextType_variable_description_line_selection;
	public static String CompilationUnitContextType_variable_description_return_type;
	public static String JavaContextType_variable_description_array;
	public static String JavaContextType_variable_description_array_type;
	public static String JavaContextType_variable_description_array_element;
	public static String JavaContextType_variable_description_index;
	public static String JavaContextType_variable_description_collection;
	public static String JavaContextType_variable_description_iterable;
	public static String JavaContextType_variable_description_iterable_type;
	public static String JavaContextType_variable_description_iterable_element;
	public static String JavaContextType_variable_description_iterator;
	public static String JavaContextType_variable_description_todo;
	public static String JavaContext_error_title;
	public static String JavaContext_error_message;
	public static String JavaContext_unexpected_error_message;
	public static String JavaDocContextType_variable_description_word_selection;
	public static String TemplateSet_error_missing_attribute;
	public static String TemplateSet_error_read;
	public static String TemplateSet_error_write;
	public static String Context_error_cannot_evaluate;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaTemplateMessages.class);
	}
}
