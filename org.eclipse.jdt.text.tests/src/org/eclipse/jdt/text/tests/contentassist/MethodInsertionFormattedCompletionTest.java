/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.contentassist;

import java.util.Hashtable;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodInsertionFormattedCompletionTest extends AbstractCompletionTest {

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#configureCoreOptions(java.util.Hashtable)
	 */
	@Override
	protected void configureCoreOptions(Hashtable<String, String> options) {
		super.configureCoreOptions(options);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, JavaCore.INSERT);
	}

	@Test
	public void testThisMethod() throws Exception {
		assertMethodBodyProposal("this.|", "hashCode(", "this.hashCode ( )|");
	}

	@Test
	public void testMethod() throws Exception {
		assertMethodBodyProposal("h", "hashCode(", "hashCode ( )");
	}

	@Ignore("BUG_DISABLED_DUE_TO_FORMATTER_CONTEXT_INFO_INTERATION disabled due to formatter - context info interation")
	@Test
	public void testMethodWithParam() throws Exception {
		assertMethodBodyProposal("e", "equals(", "equals ( |)");
	}

	/* inserting */

	@Test
	public void testInsertThisMethod() throws Exception {
		assertMethodBodyProposal("this.|class", "hashCode(", "this.hashCode ( )|class");
	}

	@Test
	public void testInsertMethod() throws Exception {
		assertMethodBodyProposal("h|foobar", "hashCode(", "hashCode ( )|foobar");
	}

	@Ignore("BUG_DISABLED_DUE_TO_FORMATTER_CONTEXT_INFO_INTERATION disabled due to formatter - context info interation")
	@Test
	public void testInsertMethodWithParam() throws Exception {
		assertMethodBodyProposal("e|foobar", "equals(", "equals ( |)foobar");
	}

	@Test
	public void testFormattedMethodWithParameterFilling1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
		assertMethodBodyProposal("fList.", "add(O", "fList.add ( |arg0| )");
	}

	@Test
	public void testFormattedMethodWithParameterFilling2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
		assertMethodBodyProposal("fList.", "add(int", "fList.add ( |arg0|, arg1 );");
	}

	@Test
	public void testFormattedMethodWithParameterGuessing1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		addMembers("private java.util.List fList;");
		addLocalVariables("int foo= 3; Object obj= null;\n");

		assertMethodBodyProposal("fList.", "add(O", "fList.add ( |obj| )");
	}

	@Test
	public void testFormattedMethodWithParameterGuessing2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		addMembers("private java.util.List fList;");
		addLocalVariables("int foo= 3; Object obj= null;\n");

		assertMethodBodyProposal("fList.", "add(int", "fList.add ( |foo|, obj );");
	}
}
