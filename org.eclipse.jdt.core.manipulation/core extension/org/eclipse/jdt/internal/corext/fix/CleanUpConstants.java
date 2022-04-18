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
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;

public class CleanUpConstants {
	/**
	 * Constant for default options kind for clean up.
	 */
	public static final int DEFAULT_CLEAN_UP_OPTIONS= 1;

	/**
	 * Constant for default options kind for save actions.
	 */
	public static final int DEFAULT_SAVE_ACTION_OPTIONS= 2;

	/**
	 * Format Java Source Code <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String FORMAT_SOURCE_CODE= "cleanup.format_source_code"; //$NON-NLS-1$

	/**
	 * If true then only changed regions are formatted on save. Only has an effect if
	 * {@link #FORMAT_SOURCE_CODE} is TRUE <br>
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.4
	 */
	public static final String FORMAT_SOURCE_CODE_CHANGES_ONLY= "cleanup.format_source_code_changes_only"; //$NON-NLS-1$

	/**
	 * Format comments. Specify which comment with:<br> {@link #FORMAT_JAVADOC}<br>
	 * {@link #FORMAT_MULTI_LINE_COMMENT}<br> {@link #FORMAT_SINGLE_LINE_COMMENT} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated replaced by {@link #FORMAT_SOURCE_CODE}
	 */
	@Deprecated
	public static final String FORMAT_COMMENT= "cleanup.format_comment"; //$NON-NLS-1$

	/**
	 * Format single line comments. Only has an effect if {@link #FORMAT_COMMENT} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated replaced by
	 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_LINE_COMMENT}
	 */
	@Deprecated
	public static final String FORMAT_SINGLE_LINE_COMMENT= "cleanup.format_single_line_comment"; //$NON-NLS-1$

	/**
	 * Format multi line comments. Only has an effect if {@link #FORMAT_COMMENT} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated replaced by
	 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT}
	 */
	@Deprecated
	public static final String FORMAT_MULTI_LINE_COMMENT= "cleanup.format_multi_line_comment"; //$NON-NLS-1$

	/**
	 * Format javadoc comments. Only has an effect if {@link #FORMAT_COMMENT} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated replaced by
	 *             {@link DefaultCodeFormatterConstants#FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT}
	 */
	@Deprecated
	public static final String FORMAT_JAVADOC= "cleanup.format_javadoc"; //$NON-NLS-1$

	/**
	 * Removes trailing whitespace in compilation units<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES= "cleanup.remove_trailing_whitespaces"; //$NON-NLS-1$

	/**
	 * Removes trailing whitespace in compilation units on all lines<br>
	 * Only has an effect if {@link #FORMAT_REMOVE_TRAILING_WHITESPACES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES_ALL= "cleanup.remove_trailing_whitespaces_all"; //$NON-NLS-1$

	/**
	 * Removes trailing whitespace in compilation units on all lines which contain an other
	 * characters then whitespace<br>
	 * Only has an effect if {@link #FORMAT_REMOVE_TRAILING_WHITESPACES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY= "cleanup.remove_trailing_whitespaces_ignore_empty"; //$NON-NLS-1$

	/**
	 * Correct indentation in compilation units on all lines <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.4
	 */
	public static final String FORMAT_CORRECT_INDENTATION= "cleanup.correct_indentation"; //$NON-NLS-1$

	/**
	 * Controls access qualifiers for instance fields. For detailed settings use<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS}<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS= "cleanup.use_this_for_non_static_field_access"; //$NON-NLS-1$

	/**
	 * Adds a 'this' qualifier to field accesses.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     int fField;
	 *                     void foo() {fField= 10;} -&gt; void foo() {this.fField= 10;}
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS= "cleanup.always_use_this_for_non_static_field_access"; //$NON-NLS-1$

	/**
	 * Removes 'this' qualifier to field accesses.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     int fField;
	 *                     void foo() {this.fField= 10;} -&gt; void foo() {fField= 10;}
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY= "cleanup.use_this_for_non_static_field_access_only_if_necessary"; //$NON-NLS-1$

	/**
	 * Controls access qualifiers for instance methods. For detailed settings use<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS}<br>
	 * {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS= "cleanup.use_this_for_non_static_method_access"; //$NON-NLS-1$

	/**
	 * Adds a 'this' qualifier to method accesses.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     int method(){};
	 *                     void foo() {method()} -&gt; void foo() {this.method();}
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS= "cleanup.always_use_this_for_non_static_method_access"; //$NON-NLS-1$

	/**
	 * Removes 'this' qualifier from field accesses.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     int fField;
	 *                     void foo() {this.fField= 10;} -&gt; void foo() {fField= 10;}
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY= "cleanup.use_this_for_non_static_method_access_only_if_necessary"; //$NON-NLS-1$

	/**
	 * Controls access qualifiers for static members. For detailed settings use<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD}<br>
	 * {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS= "cleanup.qualify_static_member_accesses_with_declaring_class"; //$NON-NLS-1$

	/**
	 * Qualify static field accesses with declaring type.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   class E {
	 *                     public static int i;
	 *                     void foo() {i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </pre>
	 *
	 * <br>
	 * Only has an effect if {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD= "cleanup.qualify_static_field_accesses_with_declaring_class"; //$NON-NLS-1$

	/**
	 * Qualifies static method accesses with declaring type.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   class E {
	 *                     public static int m();
	 *                     void foo() {m();} -&gt; void foo() {E.m();}
	 *                   }
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD= "cleanup.qualify_static_method_accesses_with_declaring_class"; //$NON-NLS-1$

	/**
	 * Changes indirect accesses to static members to direct ones.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   class E {public static int i;}
	 *                   class ESub extends E {
	 *                     void foo() {ESub.i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS= "cleanup.qualify_static_member_accesses_through_subtypes_with_declaring_class"; //$NON-NLS-1$

	/**
	 * Changes non static accesses to static members to static accesses.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   class E {
	 *                     public static int i;
	 *                     void foo() {(new E()).i= 10;} -&gt; void foo() {E.i= 10;}
	 *                   }
	 * </pre>
	 *
	 * Only has an effect if {@link #MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS= "cleanup.qualify_static_member_accesses_through_instances_with_declaring_class"; //$NON-NLS-1$

	/**
	 * Controls the usage of blocks around single control statement bodies. For detailed settings
	 * use<br> {@link #CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS}<br> {@link #CONTROL_STATEMENTS_USE_BLOCKS_NEVER}<br>
	 * {@link #CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String CONTROL_STATEMENTS_USE_BLOCKS= "cleanup.use_blocks"; //$NON-NLS-1$

	/**
	 * Adds block to control statement body if the body is not a block.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   	 if (b) foo(); -&gt; if (b) {foo();}
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS= "cleanup.always_use_blocks"; //$NON-NLS-1$

	/**
	 * Adds block to control statement body if the body is not a block.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   	 if (b) foo(); -&gt; if (b) {foo();}
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated Use {@link #CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS} instead
	 */
	@Deprecated
	public static final String CONTROL_STATMENTS_USE_BLOCKS_ALWAYS= CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS;

	/**
	 * Remove unnecessary blocks in control statement bodies if they contain a single return or
	 * throw statement.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     if (b) {return;} -&gt; if (b) return;
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW= "cleanup.use_blocks_only_for_return_and_throw"; //$NON-NLS-1$


	/**
	 * Remove unnecessary blocks in control statement bodies if they contain a single return or
	 * throw statement.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     if (b) {return;} -&gt; if (b) return;
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated Use {@link #CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW} instead
	 */
	@Deprecated
	public static final String CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW= CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW;

	/**
	 * Remove unnecessary blocks in control statement bodies.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     if (b) {foo();} -&gt; if (b) foo();
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_USE_BLOCKS_NEVER= "cleanup.never_use_blocks"; //$NON-NLS-1$

	/**
	 * Remove unnecessary blocks in control statement bodies.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                     if (b) {foo();} -&gt; if (b) foo();
	 * </pre>
	 *
	 * Only has an effect if {@link #CONTROL_STATEMENTS_USE_BLOCKS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated Use {@link #CONTROL_STATEMENTS_USE_BLOCKS_NEVER} instead
	 */
	@Deprecated
	public static final String CONTROL_STATMENTS_USE_BLOCKS_NEVER= CONTROL_STATEMENTS_USE_BLOCKS_NEVER;

	/**
	 * Convert for loops to enhanced for loops.  For detailed setting use<br>
	 * {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED}<br>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   for (int i = 0; i &lt; array.length; i++) {} -&gt; for (int element : array) {}
	 * </pre>
	 *
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED= "cleanup.convert_to_enhanced_for_loop"; //$NON-NLS-1$

	/**
	 * Convert for loops to enhanced for loops.  For detailed setting use<br>
	 * {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED}<br>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   for (int i = 0; i &lt; array.length; i++) {} -&gt; for (int element : array) {}
	 * </pre>
	 *
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 * @deprecated Use {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED} instead
	 */
	@Deprecated
	public static final String CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED= CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED;

	/**
	 * Convert a for loop to enhanced for loop only if the loop variable will be used.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   for (int i = 0; i &lt; array.length; i++) {}; -&gt; will not be converted
	 * </pre>
	 * <br>
	 * Only has an effect if {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED= "cleanup.convert_to_enhanced_for_loop_if_loop_var_used"; //$NON-NLS-1$

	/**
	 * Convert a for loop to enhanced for loop only if the loop variable will be used.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   for (int i = 0; i &lt; array.length; i++) {}; -&gt; will not be converted
	 * </pre>
	 * <br>
	 * Only has an effect if {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.16
	 * @deprecated Use {@link #CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED}
	 */
	@Deprecated
	public static String CONTROL_STATMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED= CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED;

	/**
	 * Replaces <code>if</code>/<code>else if</code>/<code>else</code> blocks to use <code>switch</code> where possible.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String USE_SWITCH= "cleanup.switch"; //$NON-NLS-1$

	/**
	 * Convert switch statements to switch expressions.<br>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *      int i;
	 *      switch(j) {
	 *        case 1:
	 *          i = 2;
	 *          break;
	 *        default:
	 *          i = 3;
	 *        }
	 *
	 *        ->
	 *
	 *       int i = switch(j) {
	 *         case 1 -> 2;
	 *         default -> 3;
	 *       };
	 * </pre>
	 *
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS= "cleanup.convert_to_switch_expressions"; //$NON-NLS-1$

	/**
	 * Controls the usage of parentheses in expressions. For detailed settings use<br>
	 * {@link #EXPRESSIONS_USE_PARENTHESES_ALWAYS}<br> {@link #EXPRESSIONS_USE_PARENTHESES_NEVER}<br>
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES= "cleanup.use_parentheses_in_expressions"; //$NON-NLS-1$

	/**
	 * Add paranoiac parentheses around conditional expressions.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   boolean b= i &gt; 10 &amp;&amp; i &lt; 100 || i &gt; 20;
	 *                   -&gt;
	 *                   boolean b= ((i &gt; 10) &amp;&amp; (i &lt; 100)) || (i &gt; 20);
	 * </pre>
	 *
	 * Only has an effect if {@link #EXPRESSIONS_USE_PARENTHESES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES_ALWAYS= "cleanup.always_use_parentheses_in_expressions"; //$NON-NLS-1$

	/**
	 * Remove unnecessary parenthesis around conditional expressions.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   boolean b= ((i &gt; 10) &amp;&amp; (i &lt; 100)) || (i &gt; 20);
	 *                   -&gt;
	 *                   boolean b= i &gt; 10 &amp;&amp; i &lt; 100 || i &gt; 20;
	 * </pre>
	 *
	 * Only has an effect if {@link #EXPRESSIONS_USE_PARENTHESES} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String EXPRESSIONS_USE_PARENTHESES_NEVER= "cleanup.never_use_parentheses_in_expressions"; //$NON-NLS-1$

	/**
	 * Use lazy logical operator.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.15
	 */
	public static final String USE_LAZY_LOGICAL_OPERATOR= "cleanup.lazy_logical_operator"; //$NON-NLS-1$

	/**
	 * Replace unnecessary primitive wrappers instance creations by using static factory <code>valueOf()</code> method.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String VALUEOF_RATHER_THAN_INSTANTIATION= "cleanup.valueof_rather_than_instantiation"; //$NON-NLS-1$

	/**
	 * Replaces the <code>compareTo()</code> method by a comparison on primitive.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String PRIMITIVE_COMPARISON= "cleanup.primitive_comparison"; //$NON-NLS-1$

	/**
	 * Avoids to create primitive wrapper when parsing a string.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String PRIMITIVE_PARSING= "cleanup.primitive_parsing"; //$NON-NLS-1$

	/**
	 * Replaces a primitive boxing to serialize by a call to the static <code>toString()</code> method.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String PRIMITIVE_SERIALIZATION= "cleanup.primitive_serialization"; //$NON-NLS-1$

	/**
	 * Replace wrapper object by primitive type when an object is not necessary.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String PRIMITIVE_RATHER_THAN_WRAPPER= "cleanup.primitive_rather_than_wrapper"; //$NON-NLS-1$

	/**
	 * Controls the usage of 'final' modifier for variable declarations. For detailed settings
	 * use:<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES}<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS}<br>
	 * {@link #VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL= "cleanup.make_variable_declarations_final"; //$NON-NLS-1$

	/**
	 * Add a final modifier to private fields where possible i.e.:
	 *
	 * <pre>
	 *                   private int field= 0; -&gt; private final int field= 0;
	 * </pre>
	 *
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS= "cleanup.make_private_fields_final"; //$NON-NLS-1$

	/**
	 * Add a final modifier to method parameters where possible i.e.:
	 *
	 * <pre>
	 *                   void foo(int i) {} -&gt; void foo(final int i) {}
	 * </pre>
	 *
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS= "cleanup.make_parameters_final"; //$NON-NLS-1$

	/**
	 * Add a final modifier to local variables where possible i.e.:
	 *
	 * <pre>
	 *                   int i= 0; -&gt; final int i= 0;
	 * </pre>
	 *
	 * Only has an effect if {@link #VARIABLE_DECLARATIONS_USE_FINAL} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES= "cleanup.make_local_variable_final"; //$NON-NLS-1$

	/**
	 * Replace type declaration by local variable type inference.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.15
	 */
	public static final String USE_VAR= "cleanup.use_var"; //$NON-NLS-1$

	/**
	 * Uses pattern matching for the instanceof operator when possible.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.17
	 */
	public static final String USE_PATTERN_MATCHING_FOR_INSTANCEOF= "cleanup.instanceof"; //$NON-NLS-1$

	/**
	 * Controls conversion between lambda expressions and anonymous class creations. For detailed
	 * settings, use {@link #USE_LAMBDA} or {@link #USE_ANONYMOUS_CLASS_CREATION}
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String CONVERT_FUNCTIONAL_INTERFACES= "cleanup.convert_functional_interfaces"; //$NON-NLS-1$

	/**
	 * Replaces anonymous class creations with lambda expressions where possible in Java 8 source.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 * Only has an effect if {@link #CONVERT_FUNCTIONAL_INTERFACES} is TRUE.
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.10
	 */
	public static final String USE_LAMBDA= "cleanup.use_lambda"; //$NON-NLS-1$

	/**
	 * Replaces lambda expressions with anonymous class creations.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 * Only has an effect if {@link #CONVERT_FUNCTIONAL_INTERFACES} is TRUE.
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.10
	 */
	public static final String USE_ANONYMOUS_CLASS_CREATION= "cleanup.use_anonymous_class_creation"; //$NON-NLS-1$

	/**
	 * Removes useless parenthesis, return statements and brackets from lambda expressions and
	 * method references.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.15
	 */
	public static final String SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF= "cleanup.simplify_lambda_expression_and_method_ref"; //$NON-NLS-1$

	/**
	 * Replaces a plain comparator instance by a lambda expression passed to a <code>Comparator.comparing()</code> method.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String COMPARING_ON_CRITERIA= "cleanup.comparing_on_criteria"; //$NON-NLS-1$

	/**
	 * Precompiles the regular expressions.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.17
	 */
	public static final String PRECOMPILE_REGEX= "cleanup.precompile_regex"; //$NON-NLS-1$

	/**
	 * Invert calls to <code>Object.equals(Object)</code> and <code>String.equalsIgnoreCase(String)</code> when it is known that the second operand is not null and the first can be null.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String INVERT_EQUALS= "cleanup.invert_equals"; //$NON-NLS-1$

	/**
	 * Check for sign of bitwise operation.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CHECK_SIGN_OF_BITWISE_OPERATION= "cleanup.bitwise_conditional_expression"; //$NON-NLS-1$

	/**
	 * Fixes <code>Comparable.compareTo()</code> usage.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String STANDARD_COMPARISON= "cleanup.standard_comparison"; //$NON-NLS-1$

	/**
	 * Removes a String instance from a String literal.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String NO_STRING_CREATION= "cleanup.no_string_creation"; //$NON-NLS-1$

	/**
	 * Refactor access to system properties to use constants or methods
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY= "cleanup.system_property"; //$NON-NLS-1$

	/**
	 * Replace
	 * <code>System.getProperty("file.separator")</code>
	 * by
	 * <p>
	 * <code>File.separator</code><br> or<br>
	 * <code>FileSystems.getDefault().getSeparator()</code> (Java 7).
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR= "cleanup.system_property_file_separator"; //$NON-NLS-1$

	/**
	 * Replace <code>System.getProperty("file.encoding")</code> by<p>
	 * <code>Charset.defaultCharset().displayName()</code>
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING= "cleanup.system_property_file_encoding"; //$NON-NLS-1$

	/**
	 * Replace <code>System.getProperty("path.separator")</code> by<p>
	 * <code>File.pathSeparator</code>
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR= "cleanup.system_property_path_separator"; //$NON-NLS-1$

	/**
	 * Replace <code>System.getProperty("line.separator")</code> by<p>
	 * <code>System.lineSeparator()</code>
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR= "cleanup.system_property_line_separator"; //$NON-NLS-1$

	/**
	 * Replace Boolean/Long/Integer conversions using System properties to methods designed
	 * for the purpose.  For example, replace: <p><code>Boolean.parseBoolean(System.getProperty("arbitrarykey"))</code></p> by<p>
	 * <code>Boolean.getBoolean("arbitrarykey")</code>
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED= "cleanup.system_property_boolean"; //$NON-NLS-1$

	/**
	 * Replaces Boolean.TRUE/Boolean.FALSE by true/false when used as primitive.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String PREFER_BOOLEAN_LITERAL= "cleanup.boolean_literal"; //$NON-NLS-1$

	/**
	 * Adds type parameters to raw type references.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   List l; -&gt; List&lt;Object&gt; l;
	 * </pre>
	 *
	 * Possible values: {TRUE, FALSE}
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES= "cleanup.use_arguments_for_raw_type_references"; //$NON-NLS-1$

	/**
	 * Refactors a field into a local variable if its use is only local.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String SINGLE_USED_FIELD= "cleanup.single_used_field"; //$NON-NLS-1$

	/**
	 * Add a break to avoid passive for loop iterations.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String BREAK_LOOP= "cleanup.break_loop"; //$NON-NLS-1$

	/**
	 * Replace <code>while</code> by <code>do</code>/<code>while</code>.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String DO_WHILE_RATHER_THAN_WHILE= "cleanup.do_while_rather_than_while"; //$NON-NLS-1$

	/**
	 * Make inner <code>class</code> static.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String STATIC_INNER_CLASS= "cleanup.static_inner_class"; //$NON-NLS-1$

	/**
	 * Replaces String concatenation by StringBuilder when possible.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String STRINGBUILDER= "cleanup.stringbuilder"; //$NON-NLS-1$

	/**
	 * Replaces StringBuffer by StringBuilder.
	 *
	 * For detailed setting use<br>
	 * {@link #STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS}<br>
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.21
	 */
	public static final String STRINGBUFFER_TO_STRINGBUILDER= "cleanup.stringbuffer_to_stringbuilder"; //$NON-NLS-1$

	/**
	 * Replaces String concatenation by Text Block for Java 15 and higher.
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.22
	 */

	public static final String STRINGCONCAT_TO_TEXTBLOCK= "cleanup.stringconcat_to_textblock"; //$NON-NLS-1$

	/**
	 * Only replace local var StringBuffer uses with StringBuilder.
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.21
	 */
	public static final String STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS= "cleanup.stringbuilder_for_local_vars"; //$NON-NLS-1$

	/**
	 * Replaces <code>String.replaceAll()</code> by <code>String.replace()</code>.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String PLAIN_REPLACEMENT= "cleanup.plain_replacement"; //$NON-NLS-1$

	/**
	 * Replace
	 * <code>s.strip().length() == 0</code>
	 * by
	 * <code>s.isBlank()</code> (Java 11).
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String USE_STRING_IS_BLANK= "cleanup.use_string_is_blank"; //$NON-NLS-1$

	/**
	 * Removes unused imports. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_IMPORTS= "cleanup.remove_unused_imports"; //$NON-NLS-1$

	/**
	 * Controls the removal of unused private members. For detailed settings use:<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS}<br> {@link #REMOVE_UNUSED_CODE_PRIVATE_FELDS}<br>
	 * {@link #REMOVE_UNUSED_CODE_PRIVATE_METHODS}<br> {@link #REMOVE_UNUSED_CODE_PRIVATE_TYPES} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_MEMBERS= "cleanup.remove_unused_private_members"; //$NON-NLS-1$

	/**
	 * Removes unused private types. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_TYPES= "cleanup.remove_unused_private_types"; //$NON-NLS-1$

	/**
	 * Removes unused private constructors. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS= "cleanup.remove_private_constructors"; //$NON-NLS-1$

	/**
	 * Removes unused private fields. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_FELDS= "cleanup.remove_unused_private_fields"; //$NON-NLS-1$

	/**
	 * Removes unused private methods. <br>
	 * Only has an effect if {@link #REMOVE_UNUSED_CODE_PRIVATE_MEMBERS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_PRIVATE_METHODS= "cleanup.remove_unused_private_methods"; //$NON-NLS-1$

	/**
	 * Removes unused parameters for private methods. <br>
	 *
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_METHOD_PARAMETERS= "cleanup.remove_unused_method_parameters"; //$NON-NLS-1$


	/**
	 * Removes unused local variables. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNUSED_CODE_LOCAL_VARIABLES= "cleanup.remove_unused_local_variables"; //$NON-NLS-1$

	/**
	 * Removes unused casts. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNNECESSARY_CASTS= "cleanup.remove_unnecessary_casts"; //$NON-NLS-1$

	/**
	 * Remove unnecessary '$NON-NLS$' tags.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * String s; //$NON-NLS-1$ -&gt; String s;
	 * </pre>
	 *
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String REMOVE_UNNECESSARY_NLS_TAGS= "cleanup.remove_unnecessary_nls_tags"; //$NON-NLS-1$

	/**
	 * Insert inferred type arguments for diamonds.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.10
	 */
	public static final String INSERT_INFERRED_TYPE_ARGUMENTS= "cleanup.insert_inferred_type_arguments"; //$NON-NLS-1$

	/**
	 * Removes redundant type arguments from class instance creations and creates a diamond operator.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.10
	 */
	public static final String REMOVE_REDUNDANT_TYPE_ARGUMENTS= "cleanup.remove_redundant_type_arguments"; //$NON-NLS-1$

	/**
	 * Rewrites Eclipse-autogenerated hashcode method by Eclipse-autogenerated hashcode method for Java 7.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String MODERNIZE_HASH= "cleanup.hash"; //$NON-NLS-1$

	/**
	 * Removes redundant modifiers.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.14
	 */
	public static final String REMOVE_REDUNDANT_MODIFIERS= "cleanup.remove_redundant_modifiers"; //$NON-NLS-1$

	/**
	 * Removes the second <code>substring()</code> parameter if this parameter is the length of the string.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String SUBSTRING= "cleanup.substring"; //$NON-NLS-1$

	/**
	 * Replaces for loops to use String.join() where possible.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String JOIN= "cleanup.join"; //$NON-NLS-1$

	/**
	 * Replaces a for loop on an array that assigns the same value by a call to Arrays.fill().
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String ARRAYS_FILL= "cleanup.arrays_fill"; //$NON-NLS-1$

	/**
	 * Removes redundant null checks.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String EVALUATE_NULLABLE= "cleanup.evaluate_nullable"; //$NON-NLS-1$

	/**
	 * Raises embedded if into parent if.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String RAISE_EMBEDDED_IF= "cleanup.embedded_if"; //$NON-NLS-1$

	/**
	 * Uses Autoboxing.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.13
	 */
	public static final String USE_AUTOBOXING= "cleanup.use_autoboxing"; //$NON-NLS-1$

	/**
	 * Uses unboxing.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.13
	 */
	public static final String USE_UNBOXING= "cleanup.use_unboxing"; //$NON-NLS-1$

	/**
	 * Push down negation.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.13
	 */
	public static final String PUSH_DOWN_NEGATION= "cleanup.push_down_negation"; //$NON-NLS-1$

	/**
	 * Directly checks boolean values instead of comparing them with <code>true</code>/<code>false</code>.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String BOOLEAN_VALUE_RATHER_THAN_COMPARISON= "cleanup.boolean_value_rather_than_comparison"; //$NON-NLS-1$

	/**
	 * Reduces double negation in boolean expression.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String DOUBLE_NEGATION= "cleanup.double_negation"; //$NON-NLS-1$

	/**
	 * Removes useless bad value checks before assignments or return statements.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REMOVE_REDUNDANT_COMPARISON_STATEMENT= "cleanup.comparison_statement"; //$NON-NLS-1$

	/**
	 * Remove super() call in constructor.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REDUNDANT_SUPER_CALL= "cleanup.no_super"; //$NON-NLS-1$

	/**
	 * Detect two successive <code>if</code> conditions that are identical and remove the second one.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String UNREACHABLE_BLOCK= "cleanup.unreachable_block"; //$NON-NLS-1$

	/**
	 * Replaces (X && Y) || (X && Z) by (X && (Y || Z)).
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String OPERAND_FACTORIZATION= "cleanup.operand_factorization"; //$NON-NLS-1$

	/**
	 * Replaces (X && Y) || (!X && Z) by X ? Y : Z.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String TERNARY_OPERATOR= "cleanup.ternary_operator"; //$NON-NLS-1$

	/**
	 * Replaces <code>(X && !Y) || (!X && Y)</code> by <code>X ^ Y</code>.
	 * Replaces also <code>(X && Y) || (!X && !Y)</code> by <code>X == Y</code>.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String STRICTLY_EQUAL_OR_DIFFERENT= "cleanup.strictly_equal_or_different"; //$NON-NLS-1$

	/**
	 * Merge conditions of if/else if/else that have the same blocks.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.16
	 */
	public static final String MERGE_CONDITIONAL_BLOCKS= "cleanup.merge_conditional_blocks"; //$NON-NLS-1$

	/**
	 * Factorizes common code in all if / else if / else statements at the end of each blocks.
	 * Ultimately it removes the empty and passive if conditions.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String CONTROLFLOW_MERGE= "cleanup.controlflow_merge"; //$NON-NLS-1$

	/**
	 * Merge consecutive <code>if</code> statements with same code block that end with a jump statement.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH= "cleanup.one_if_rather_than_duplicate_blocks_that_fall_through"; //$NON-NLS-1$

	/**
	 * Moves an inner <code>if</code> statement around the outer <code>if</code> condition.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.21
	 */
	public static final String PULL_OUT_IF_FROM_IF_ELSE= "cleanup.pull_out_if_from_if_else"; //$NON-NLS-1$

	/**
	 * Merges blocks that end with a jump statement into the following same code.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REDUNDANT_FALLING_THROUGH_BLOCK_END= "cleanup.redundant_falling_through_block_end"; //$NON-NLS-1$

	/**
	 * Remove a condition on an else that is negative to the condition of the previous if.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REDUNDANT_IF_CONDITION= "cleanup.if_condition"; //$NON-NLS-1$

	/**
	 * Use directly map method.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.14
	 */
	public static final String USE_DIRECTLY_MAP_METHOD= "cleanup.use_directly_map_method"; //$NON-NLS-1$

	/**
	 * Replaces creating a new Collection, then invoking Collection.addAll() on it, by creating the new Collection with the other Collection as parameter.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String COLLECTION_CLONING= "cleanup.collection_cloning"; //$NON-NLS-1$

	/**
	 * Replaces creating a new Map, then invoking Map.putAll() on it, by creating the new Map with the other Map as parameter.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String MAP_CLONING= "cleanup.map_cloning"; //$NON-NLS-1$

	/**
	 * Remove passive assignment when the variable is reassigned before being read.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String OVERRIDDEN_ASSIGNMENT= "cleanup.overridden_assignment"; //$NON-NLS-1$

	/**
	 * Move the declaration of the variable to the location of the overriding assignment
	 * if necessary.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String OVERRIDDEN_ASSIGNMENT_MOVE_DECL= "cleanup.overridden_assignment_move_decl"; //$NON-NLS-1$

	/**
	 * Removes redundant semicolons.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.14
	 */
	public static final String REMOVE_REDUNDANT_SEMICOLONS= "cleanup.remove_redundant_semicolons"; //$NON-NLS-1$

	/**
	 * Remove the comparator declaration if it is the default one.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String REDUNDANT_COMPARATOR= "cleanup.redundant_comparator"; //$NON-NLS-1$

	/**
	 * Removes unnecessary array creation for varargs.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.19
	 */
	public static final String REMOVE_UNNECESSARY_ARRAY_CREATION= "cleanup.remove_unnecessary_array_creation"; //$NON-NLS-1$

	/**
	 * Replace the new instance syntax by curly brackets to create an array.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String ARRAY_WITH_CURLY= "cleanup.array_with_curly"; //$NON-NLS-1$

	/**
	 * Removes unnecessary local variable declaration or unnecessary variable assignment before a return statement.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.20
	 */
	public static final String RETURN_EXPRESSION= "cleanup.return_expression"; //$NON-NLS-1$

	/**
	 * Removes useless lone return at the end of a method.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REMOVE_USELESS_RETURN= "cleanup.useless_return"; //$NON-NLS-1$

	/**
	 * Removes useless lone continue at the end of a loop.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String REMOVE_USELESS_CONTINUE= "cleanup.useless_continue"; //$NON-NLS-1$

	/**
	 * Replaces a <code>while</code> loop that always terminates during the first iteration by an <code>if</code>.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String UNLOOPED_WHILE= "cleanup.unlooped_while"; //$NON-NLS-1$

	/**
	 * Replaces a loop on elements by Collection.addAll(), Collection.addAll(Arrays.asList()) or Collections.addAll().
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String CONTROL_STATEMENTS_USE_ADD_ALL= "cleanup.add_all"; //$NON-NLS-1$

	/**
	 * Reduces the code of the equals method implementation by using Objects.equals().
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.17
	 */
	public static final String USE_OBJECTS_EQUALS= "cleanup.objects_equals"; //$NON-NLS-1$

	/**
	 * Controls whether missing annotations should be added to the code. For detailed settings use:<br>
	 * {@link #ADD_MISSING_ANNOTATIONS_DEPRECATED}<br> {@value #ADD_MISSING_ANNOTATIONS_OVERRIDE} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_ANNOTATIONS= "cleanup.add_missing_annotations"; //$NON-NLS-1$

	/**
	 * Add '@Override' annotation in front of overriding methods.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   class E1 {void foo();}
	 *                   class E2 extends E1 {
	 *                   	 void foo(); -&gt;  @Override void foo();
	 *                   }
	 * </pre>
	 *
	 * Only has an effect if {@link #ADD_MISSING_ANNOTATIONS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_ANNOTATIONS_OVERRIDE= "cleanup.add_missing_override_annotations"; //$NON-NLS-1$

	/**
	 * Add '@Override' annotation in front of methods that override or implement a superinterface method.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   interface I {void foo();}
	 *                   class E implements I {
	 *                   	 void foo(); -&gt;  @Override void foo();
	 *                   }
	 * </pre>
	 *
	 * Only has an effect if {@link #ADD_MISSING_ANNOTATIONS} and {@link #ADD_MISSING_ANNOTATIONS_OVERRIDE} are TRUE and
	 * the compiler compliance is 1.6 or higher.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.6
	 */
	public static final String ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION= "cleanup.add_missing_override_annotations_interface_methods"; //$NON-NLS-1$

	/**
	 * Add '@Deprecated' annotation in front of deprecated members.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                         /**@deprecated* /
	 *                        int i;
	 *                    -&gt;
	 *                         /**@deprecated* /
	 *                         &#064;Deprecated
	 *                        int i;
	 * </pre>
	 *
	 * Only has an effect if {@link #ADD_MISSING_ANNOTATIONS} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_ANNOTATIONS_DEPRECATED= "cleanup.add_missing_deprecated_annotations"; //$NON-NLS-1$

	/**
	 * Controls whether missing serial version ids should be added to the code. For detailed
	 * settings use:<br> {@link #ADD_MISSING_SERIAL_VERSION_ID_DEFAULT}<br>
	 * {@link #ADD_MISSING_SERIAL_VERSION_ID_GENERATED} <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID= "cleanup.add_serial_version_id"; //$NON-NLS-1$

	/**
	 * Adds a generated serial version id to subtypes of java.io.Serializable and
	 * java.io.Externalizable
	 *
	 * public class E implements Serializable {} -> public class E implements Serializable { private
	 * static final long serialVersionUID = 4381024239L; } <br>
	 * Only has an effect if {@link #ADD_MISSING_SERIAL_VERSION_ID} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID_GENERATED= "cleanup.add_generated_serial_version_id"; //$NON-NLS-1$

	/**
	 * Adds a default serial version it to subtypes of java.io.Serializable and
	 * java.io.Externalizable
	 *
	 * public class E implements Serializable {} -> public class E implements Serializable { private
	 * static final long serialVersionUID = 1L; } <br>
	 * Only has an effect if {@link #ADD_MISSING_SERIAL_VERSION_ID} is TRUE <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_SERIAL_VERSION_ID_DEFAULT= "cleanup.add_default_serial_version_id"; //$NON-NLS-1$

	/**
	 * Moves increment or decrement outside an expression when possible.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String EXTRACT_INCREMENT= "cleanup.extract_increment"; //$NON-NLS-1$

	/**
	 * Moves assignments inside an if condition above the if node.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String PULL_UP_ASSIGNMENT= "cleanup.pull_up_assignment"; //$NON-NLS-1$

	/**
	 * Uses the <code>else if</code> pseudo keyword.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String ELSE_IF= "cleanup.else_if"; //$NON-NLS-1$

	/**
	 * Removes useless indentation when the opposite workflow falls through.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String REDUCE_INDENTATION= "cleanup.reduce_indentation"; //$NON-NLS-1$

	/**
	 * Uses an <code>instanceof</code> expression to check an object against a hardcoded class.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String INSTANCEOF= "cleanup.instanceof_keyword"; //$NON-NLS-1$

	/**
	 * Controls whether long literal suffix should be rewritten in uppercase.<br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.13
	 */
	public static final String NUMBER_SUFFIX= "cleanup.number_suffix"; //$NON-NLS-1$

	/**
	 * Add '$NON-NLS$' tags to non externalized strings.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *                   	 String s= &quot;&quot;; -&gt; String s= &quot;&quot;; //$NON-NLS-1$
	 * </pre>
	 *
	 * <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ADD_MISSING_NLS_TAGS= "cleanup.add_missing_nls_tags"; //$NON-NLS-1$

	/**
	 * If true the imports are organized while cleaning up code.
	 *
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String ORGANIZE_IMPORTS= "cleanup.organize_imports"; //$NON-NLS-1$

	/**
	 * Should members be sorted? <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see #SORT_MEMBERS_ALL
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String SORT_MEMBERS= "cleanup.sort_members"; //$NON-NLS-1$

	/**
	 * If sorting members, should fields, enum constants and initializers also be sorted? <br>
	 * This has only an effect if {@link #SORT_MEMBERS} is also enabled. <br>
	 * <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see #SORT_MEMBERS
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.3
	 */
	public static final String SORT_MEMBERS_ALL= "cleanup.sort_members_all"; //$NON-NLS-1$

	/**
	 * If enabled method stubs are added to all non abstract classes which require to implement some
	 * methods. <br>
	 * Possible values: {TRUE, FALSE}<br>
	 *
	 * <br>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 3.4
	 */
	public static final String ADD_MISSING_METHODES= "cleanup.add_missing_methods"; //$NON-NLS-1$

	/**
	 * Changes code to make use of Java 7 try-with-resources feature. In particular, it removes now useless finally clauses.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.18
	 */
	public static final String TRY_WITH_RESOURCE= "cleanup.try_with_resource"; //$NON-NLS-1$

	/**
	 * Refactors <code>catch</code> clauses with the same body to use Java 7's multi-catch.
	 * <p>
	 * Possible values: {TRUE, FALSE}
	 * <p>
	 *
	 * @see CleanUpOptionsCore#TRUE
	 * @see CleanUpOptionsCore#FALSE
	 * @since 4.19
	 */
	public static final String MULTI_CATCH= "cleanup.multi_catch"; //$NON-NLS-1$

	/**
	 * Should the Clean Up Wizard be shown when executing the Clean Up Action? <br>
	 * <br>
	 * Possible values: {<code><b>true</b></code>, <code><b>false</b></code>} <br>
	 * Default value: <code><b>true</b></code><br>
	 * <br>
	 *
	 * @since 3.3
	 */
	public static final String SHOW_CLEAN_UP_WIZARD= "cleanup.showwizard"; //$NON-NLS-1$

	/**
	 * A key to a serialized string in the <code>InstanceScope</code> containing all the profiles.<br>
	 * Following code snippet can load the profiles:
	 *
	 * <pre>
	 * List profiles= new ProfileStore(CLEANUP_PROFILES, new CleanUpVersioner()).readProfiles(InstanceScope.INSTANCE);
	 * </pre>
	 *
	 * @since 3.3
	 */
	public static final String CLEANUP_PROFILES= "org.eclipse.jdt.ui.cleanupprofiles"; //$NON-NLS-1$

	/**
	 * Stores the id of the clean up profile used when executing clean up.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #DEFAULT_PROFILE} <br>
	 *
	 * @since 3.3
	 */
	public final static String CLEANUP_PROFILE= "cleanup_profile"; //$NON-NLS-1$$

	/**
	 * Stores the id of the clean up profile used when executing clean up on save.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #DEFAULT_SAVE_PARTICIPANT_PROFILE} <br>
	 *
	 * @since 3.3
	 */
	public static final String CLEANUP_ON_SAVE_PROFILE= "cleanup.on_save_profile_id"; //$NON-NLS-1$

	/**
	 * A key to the version of the profile stored in the preferences.<br>
	 * <br>
	 * Possible values: Integer value<br>
	 * Default value: CleanUpProfileVersioner#CURRENT_VERSION <br>
	 *
	 * @since 3.3
	 */
	public final static String CLEANUP_SETTINGS_VERSION_KEY= "cleanup_settings_version"; //$NON-NLS-1$

	/**
	 * Id of the 'Eclipse [built-in]' profile.<br>
	 * <br>
	 *
	 * @since 3.3
	 */
	public final static String ECLIPSE_PROFILE= "org.eclipse.jdt.ui.default.eclipse_clean_up_profile"; //$NON-NLS-1$

	/**
	 * Id of the 'Save Participant [built-in]' profile.<br>
	 * <br>
	 *
	 * @since 3.3
	 */
	public final static String SAVE_PARTICIPANT_PROFILE= "org.eclipse.jdt.ui.default.save_participant_clean_up_profile"; //$NON-NLS-1$

	public static final String CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS= "cleanup.on_save_use_additional_actions"; //$NON-NLS-1$

	/**
	 * The id of the profile used as a default profile when executing clean up.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #ECLIPSE_PROFILE} <br>
	 *
	 * @since 3.3
	 */
	public final static String DEFAULT_PROFILE= ECLIPSE_PROFILE;

	/**
	 * The id of the profile used as a default profile when executing clean up on save.<br>
	 * <br>
	 * Possible values: String value<br>
	 * Default value: {@link #SAVE_PARTICIPANT_PROFILE} <br>
	 *
	 * @since 3.3
	 */
	public final static String DEFAULT_SAVE_PARTICIPANT_PROFILE= SAVE_PARTICIPANT_PROFILE;
}
