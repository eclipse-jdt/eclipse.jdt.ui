/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *     Microsoft Corporation - based this file on ProfileVersioner
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.PreferenceConstantsCore;

public class ProfileVersionerCore {

	private static final int CURRENT_VERSION= 21;

	public static int getFirstVersion() {
		return 1;
	}

	public static int getCurrentVersion() {
		return CURRENT_VERSION;
	}

	public static Map<String, String> updateAndComplete(Map<String, String> oldSettings, int version) {
		final Map<String, String> newSettings= DefaultCodeFormatterConstants.getEclipseDefaultSettings();

		switch (version) {

		case 1:
			version1to2(oldSettings);
			//$FALL-THROUGH$
		case 2:
			version2to3(oldSettings);
			//$FALL-THROUGH$
		case 3:
		    version3to4(oldSettings);
			//$FALL-THROUGH$
		case 4:
			version4to5(oldSettings);
			//$FALL-THROUGH$
		case 5:
		    version5to6(oldSettings);
		    //$FALL-THROUGH$
		case 6:
		    version6to7(oldSettings);
		    //$FALL-THROUGH$
		case 7:
		case 8:
		case 9:
		    version9to10(oldSettings);
			//$FALL-THROUGH$
		case 10 :
			version10to11(oldSettings);
			//$FALL-THROUGH$
		case 11 :
			version11to12(oldSettings);
			//$FALL-THROUGH$
		case 12 :
			version12to13(oldSettings);
			//$FALL-THROUGH$
		case 13 :
			version13to14(oldSettings);
			//$FALL-THROUGH$
		case 14:
			version14to15(oldSettings);
			//$FALL-THROUGH$
		case 15:
			version15to16(oldSettings);
			//$FALL-THROUGH$
		case 16:
			version16to17(oldSettings);
			//$FALL-THROUGH$
		case 17:
			version17to18(oldSettings);
			//$FALL-THROUGH$
		case 18:
			version18to19(oldSettings);
			//$FALL-THROUGH$
		case 19:
			version19to20(oldSettings);
			//$FALL-THROUGH$
		case 20:
			version20to21(oldSettings);
			//$FALL-THROUGH$
		default:
			for (Map.Entry<String, String> entry : oldSettings.entrySet()) {
				final String key= entry.getKey();
				if (!newSettings.containsKey(key))
					continue;

				final String value= entry.getValue();
				if (value != null) {
					newSettings.put(key, value);
				}
			}
		    // copy over profile options (not formatter settings)
		    if (oldSettings.containsKey(JavaCore.JAVA_FORMATTER)) {
		    	newSettings.put(JavaCore.JAVA_FORMATTER, oldSettings.get(JavaCore.JAVA_FORMATTER));
		    }
		}
		return newSettings;
	}

	private static void version1to2(final Map<String, String> oldSettings) {
		checkAndReplace(oldSettings,
			FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,
			FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
			FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);

		checkAndReplace(oldSettings,
			FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);

		checkAndReplace(oldSettings,
			JavaCore.PLUGIN_ID + ".formatter.inset_space_between_empty_arguments", //$NON-NLS-1$
			FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS);

		checkAndReplace(oldSettings,
			FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN,
			FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN);

		checkAndReplace(oldSettings,
			FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
	}

	private static void mapOldValueRangeToNew(Map<String, String> settings, String oldKey, String [] oldValues,
			String newKey, String [] newValues) {

			if (!settings.containsKey(oldKey))
				return;

			final String value= settings.get(oldKey);

			if (value == null)
				return;

			for (int i = 0; i < oldValues.length; i++) {
				if (value.equals(oldValues[i])) {
					settings.put(newKey, newValues[i]);
				}
			}

		}

		private static void checkAndReplace(Map<String, String> settings, String oldKey, String newKey) {
			checkAndReplace(settings, oldKey, new String [] {newKey});
		}

		private static void checkAndReplace(Map<String, String> settings, String oldKey, String newKey1, String newKey2) {
			checkAndReplace(settings, oldKey, new String [] {newKey1, newKey2});
		}

		private static void checkAndReplace(Map<String, String> settings, String oldKey, String [] newKeys) {
			if (!settings.containsKey(oldKey))
				return;

			final String value= settings.get(oldKey);

			if (value == null)
				return;

			for (String newKey : newKeys) {
				settings.put(newKey, value);
			}
		}

		private static void checkAndReplaceBooleanWithINSERT(Map<String, String> settings, String oldKey, String newKey) {
			if (!settings.containsKey(oldKey))
				return;

			String value= settings.get(oldKey);

			if (value == null)
				return;

			if (DefaultCodeFormatterConstants.TRUE.equals(value))
				value= JavaCore.INSERT;
			else
				value= JavaCore.DO_NOT_INSERT;

			settings.put(newKey, value);
		}


		private static void version2to3(Map<String, String> oldSettings) {

			checkAndReplace(oldSettings,
				FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION,
				DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_FOR_PARENS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_IF_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);

			checkAndReplace(oldSettings,
			    FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
			    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);

			checkAndReplace(oldSettings,
			    FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
			    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);


			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_MESSAGESEND_ARGUMENTS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE,
				DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR);

			checkAndReplace(oldSettings,
				FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR);

			checkAndReplace(oldSettings,
				FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION);

			checkAndReplace(oldSettings,
				FORMATTER_COMPACT_IF_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);

			checkAndReplace(oldSettings,
				FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT ,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT ,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION);

			checkAndReplace(oldSettings,
				FORMATTER_BINARY_EXPRESSION_ALIGNMENT,
				FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION);

			checkAndReplace(oldSettings,
				FORMATTER_COMPACT_IF_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);

			checkAndReplace(oldSettings,
				FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION);

			checkAndReplace(oldSettings,
				FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER);

			checkAndReplace(oldSettings,
				FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION);

			checkAndReplace(oldSettings,
				FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT,
				DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL);


			mapOldValueRangeToNew(oldSettings,
				FORMATTER_TYPE_MEMBER_ALIGNMENT, new String [] {FORMATTER_NO_ALIGNMENT,	FORMATTER_MULTICOLUMN},
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, new String [] {DefaultCodeFormatterConstants.FALSE, DefaultCodeFormatterConstants.TRUE});


			checkAndReplace(oldSettings,
				FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER);

			checkAndReplace(oldSettings,
				FORMATTER_BLOCK_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);

			checkAndReplace(oldSettings,
				FORMATTER_METHOD_DECLARATION_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_TYPE_DECLARATION_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION);

			checkAndReplace(oldSettings,
				FORMATTER_SWITCH_BRACE_POSITION,
				DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH);

		}

		private static void version3to4(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.align_type_members_on_columns", //$NON-NLS-1$
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS);

			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.formatter.insert_space_after_comma__in_superinterfaces", //$NON-NLS-1$
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES);

			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.formatter.insert_space_before_comma__in_superinterfaces", //$NON-NLS-1$
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES);

			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.formatter.insert_space_between_empty_arguments_in_method_invocation", //$NON-NLS-1$
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
		}

		private static void version4to5(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.formatter.indent_block_statements", //$NON-NLS-1$
				new String[] { DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY, DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK });
		}

		private static void version5to6(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
				"org.eclipse.jdt.core.formatter.insert_new_line_in_control_statements", //$NON-NLS-1$
				new String[] {
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT,
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT,
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT,
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT
					});
		}

		private static void version6to7(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMAT, FORMATTER_COMMENT_FORMAT2);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATHEADER, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATSOURCE, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_INDENTROOTTAGS, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_LINELENGTH, DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_CLEARBLANKLINES, FORMATTER_COMMENT_CLEAR_BLANK_LINES);
			checkAndReplace(oldSettings, FORMATTER_COMMENT_FORMATHTML, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML);

			checkAndReplaceBooleanWithINSERT(oldSettings, FORMATTER_COMMENT_NEWLINEFORPARAMETER, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER);
			checkAndReplaceBooleanWithINSERT(oldSettings, FORMATTER_COMMENT_SEPARATEROOTTAGS, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS);
		}

		private static void version9to10(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
					FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION,
					FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION);
			checkAndReplace(oldSettings,
					DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER,
					DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER);
		}

		private static void version10to11(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
					FORMATTER_COMMENT_FORMAT2,
					new String[] {
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT
					});
			checkAndReplace(oldSettings, FORMATTER_COMMENT_CLEAR_BLANK_LINES,
					new String[] {
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
					});
		}

		private static void version11to12(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings,
					FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION,
					new String[] {
							FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_MEMBER,
							DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE,
							DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER
			});
			checkAndReplace(oldSettings,
					FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_MEMBER,
					new String[] {
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE
			});
		}

		private static void version12to13(Map<String, String> oldSettings) {
			oldSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION,
					DefaultCodeFormatterConstants.FALSE);
		}

		private static void version13to14(Map<String, String> oldSettings) {
			if (DefaultCodeFormatterConstants.FALSE.equals(oldSettings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS)))
				oldSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION, DefaultCodeFormatterConstants.FALSE);

			oldSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_DESCREIPTIONS_GROUPED, DefaultCodeFormatterConstants.FALSE);
		}

		private static void version14to15(Map<String, String> oldSettings) {
			String[][] transitions= {
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY },
					{ DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE, FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION },
			};
			for (String[] transition : transitions) {
				String value= oldSettings.get(transition[1]);
				if (JavaCore.DO_NOT_INSERT.equals(value))
					oldSettings.put(transition[0], DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY);
			}
		}

		private static void version15to16(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings, FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION, new String[] {
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR,
			});
			checkAndReplace(oldSettings, FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, new String[] {
					DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION,
					DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR,
			});
			checkAndReplace(oldSettings, FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR, new String[] {
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR,
			});
			checkAndReplace(oldSettings, FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR, new String[] {
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR,
			});
		}

		private static void version16to17(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD,
					DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD);
		}

		private static void version17to18(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR);
		}

		private static void version18to19(Map<String, String> oldSettings) {
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER,
					DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_RECORD_HEADER);

			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR);

			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_RECORD_DECLARATION);

			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_RECORD_DECLARATION);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_RECORD_DECLARATION);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_RECORD_DECLARATION);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_RECORD_COMPONENTS);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_RECORD_COMPONENTS);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_DECLARATION);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_CONSTRUCTOR);

			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE,
					DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE,
					DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE);

			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RECORD_COMPONENTS);
			checkAndReplace(oldSettings, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION,
					DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_RECORD_DECLARATION);
		}

		private static void version19to20(Map<String, String> oldSettings) {
			oldSettings.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE,
					DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, DefaultCodeFormatterConstants.INDENT_DEFAULT));
		}

		private static void version20to21(Map<String, String> oldSettings) {
			String[][] derivations= {
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_ENUM_CONSTANT, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_ENUM_CONSTANT },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_FIELD },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_METHOD },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PACKAGE },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_TYPE },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_TYPE_ANNOTATION, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ANNOTATIONS },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PARAMETER },
					{ DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE },
			};
			for (String[] derivation : derivations) {
				boolean splitAnnotations = JavaCore.INSERT.equals(oldSettings.get(derivation[0]));
				String wrapSetting= DefaultCodeFormatterConstants.createAlignmentValue(splitAnnotations,
						splitAnnotations ? DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE : DefaultCodeFormatterConstants.WRAP_NO_SPLIT);
				oldSettings.put(derivation[1], wrapSetting);
			}
		}

		/* old format constant values */

	    private static final String FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.method_declaration_arguments_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.message_send_arguments_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.message_send_selector_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.qualified_allocation_expression_arguments_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.type_declaration_superclass_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.type_declaration_superinterfaces_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.method_throws_clause_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.conditional_expression_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.allocation_expression_arguments_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_COMPACT_IF_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.compact_if_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.array_initializer_expressions_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_BINARY_EXPRESSION_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.binary_expression_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.explicit_constructor_arguments_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.anonymous_type_declaration_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.array_initializer_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_BLOCK_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.block_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_METHOD_DECLARATION_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.method_declaration_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_TYPE_DECLARATION_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.type_declaration_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_SWITCH_BRACE_POSITION = JavaCore.PLUGIN_ID + ".formatter.switch_brace_position"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_constructor_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_messagesend_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_open_paren_in_parenthesized_expression"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_opening_paren_in_message_send"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_closing_paren_in_message_send"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_constructor_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_messagesend_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_message_send"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_closing_paren"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_first_argument"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_between_empty_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND = JavaCore.PLUGIN_ID + ".formatter.insert_space_within_message_send"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_block_close_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_anonymous_type_open_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_block_open_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_catch_expression"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_constructor_declaration_open_paren"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_first_initializer"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_for_paren"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_if_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_method_declaration_open_paren"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_method_open_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_open_paren_in_parenthesized_expression"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_switch_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_switch_open_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_synchronized_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_type_open_brace"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_while_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE = JavaCore.PLUGIN_ID + ".formatter.insert_space_between_brackets_in_array_reference";//$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER = JavaCore.PLUGIN_ID + ".formatter.insert_space_between_empty_array_initializer"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_MESSAGESEND_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_between_empty_messagesend_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_catch_expression"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_FOR_PARENS = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_for_parens"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_IF_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_if_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_switch_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_synchronized_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION = JavaCore.PLUGIN_ID + ".formatter.insert_space_in_while_condition"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_bracket_in_array_reference";//$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_bracket_in_array_type_reference"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_assignment_operators"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_assignment_operators"; //$NON-NLS-1$
	    private static final String FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE = JavaCore.PLUGIN_ID + ".formatter.format_guardian_clause_on_one_line"; //$NON-NLS-1$
	    private static final String FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION = JavaCore.PLUGIN_ID + ".formatter.array_initializer_continuation_indentation"; //$NON-NLS-1$
	    private static final String FORMATTER_TYPE_MEMBER_ALIGNMENT = JavaCore.PLUGIN_ID + ".formatter.type_member_alignment"; //$NON-NLS-1$
	    private static final String FORMATTER_MULTICOLUMN = "256"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_method_throws"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_method_throws"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_method_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_method_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS = JavaCore.PLUGIN_ID + ".formatter.insert_space_after_comma_in_constructor_throws"; //$NON-NLS-1$
	    private static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS = JavaCore.PLUGIN_ID + ".formatter.insert_space_before_comma_in_constructor_throws"; //$NON-NLS-1$
	    private static final String FORMATTER_NO_ALIGNMENT = "0";//$NON-NLS-1$

		/**
		 * @deprecated Use multiple settings for each kind of comments. See
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT}
		 *             and {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_LINE_COMMENT}.
		 */
		@Deprecated
		private static final String FORMATTER_COMMENT_FORMAT2= DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT;

		/**
		 * @deprecated Use
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT}
		 *             and
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT}
		 */
		@Deprecated
		private static final String FORMATTER_COMMENT_CLEAR_BLANK_LINES= DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES;

		/**
		 * @deprecated see https://bugs.eclipse.org/318010
		 * @since 3.7
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_MEMBER= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_MEMBER;

		/**
		 * @deprecated see https://bugs.eclipse.org/318010
		 * @since 3.7
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION;

		// Old comment formatter constants
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_SOURCE} */
		@Deprecated
		private static final String FORMATTER_COMMENT_FORMATSOURCE= PreferenceConstantsCore.FORMATTER_COMMENT_FORMATSOURCE;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION} */
		@Deprecated
		private static final String FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION= PreferenceConstantsCore.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_HEADER} */
		@Deprecated
		private static final String FORMATTER_COMMENT_FORMATHEADER= PreferenceConstantsCore.FORMATTER_COMMENT_FORMATHEADER;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_INDENT_ROOT_TAGS} */
		@Deprecated
		private static final String FORMATTER_COMMENT_INDENTROOTTAGS= PreferenceConstantsCore.FORMATTER_COMMENT_INDENTROOTTAGS;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT} */
		@Deprecated
		private static final String FORMATTER_COMMENT_FORMAT= PreferenceConstantsCore.FORMATTER_COMMENT_FORMAT;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER} */
		@Deprecated
		private static final String FORMATTER_COMMENT_NEWLINEFORPARAMETER= PreferenceConstantsCore.FORMATTER_COMMENT_NEWLINEFORPARAMETER;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS} */
		@Deprecated
		private static final String FORMATTER_COMMENT_SEPARATEROOTTAGS= PreferenceConstantsCore.FORMATTER_COMMENT_SEPARATEROOTTAGS;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_CLEAR_BLANK_LINES} */
		@Deprecated
		private static final String FORMATTER_COMMENT_CLEARBLANKLINES= PreferenceConstantsCore.FORMATTER_COMMENT_CLEARBLANKLINES;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_LINE_LENGTH} */
		@Deprecated
		private static final String FORMATTER_COMMENT_LINELENGTH= PreferenceConstantsCore.FORMATTER_COMMENT_LINELENGTH;
		/** @deprecated As of 3.1, replaced by {@link org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_HTML} */
		@Deprecated
		private static final String FORMATTER_COMMENT_FORMATHTML= PreferenceConstantsCore.FORMATTER_COMMENT_FORMATHTML;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE},
		 *             and {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY;

		/**
		 * @deprecated As of 3.16 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION= DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION;

		/**
		 * @deprecated As of 3.17 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR},
		 *             and
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR}
		 */
		@Deprecated
		private static final String FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION= DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION;

		/**
		 * @deprecated As of 3.17 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_WRAP_BEFORE_STRING_CONCATENATION},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR}, and
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR}
		 */
		@Deprecated
		private static final String FORMATTER_WRAP_BEFORE_BINARY_OPERATOR= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR;

		/**
		 * @deprecated As of 3.17 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR}, and
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR;

		/**
		 * @deprecated As of 3.17 replaced by
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR},
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR}, and
		 *             {@link DefaultCodeFormatterConstants#FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR}
		 */
		@Deprecated
		private static final String FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR;

}
