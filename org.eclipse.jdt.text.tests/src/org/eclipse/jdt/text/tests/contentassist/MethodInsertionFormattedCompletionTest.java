/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodInsertionFormattedCompletionTest extends AbstractCompletionTest {
	private static final boolean BUG_DISABLED_DUE_TO_FORMATTER_CONTEXT_INFO_INTERATION= true;
	private static final Class THIS= MethodInsertionFormattedCompletionTest.class;

	public static Test setUpTest(Test test) {
		return new CompletionTestSetup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS, suiteName(THIS)));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#configureCoreOptions(java.util.Hashtable)
	 */
	protected void configureCoreOptions(Hashtable options) {
		super.configureCoreOptions(options);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, JavaCore.INSERT);
	}

	public void testThisMethod() throws Exception {
		assertMethodBodyProposal("this.|", "hashCode(", "this.hashCode ( )|");
	}

	public void testMethod() throws Exception {
		assertMethodBodyProposal("h", "hashCode(", "hashCode ( )");
	}

	public void testMethodWithParam() throws Exception {
		if (BUG_DISABLED_DUE_TO_FORMATTER_CONTEXT_INFO_INTERATION) {
			// FIXME
			System.out.println("disabled due to formatter - context info interation");
			return;
		}

		assertMethodBodyProposal("e", "equals(", "equals ( |)");
	}

	/* inserting */

	public void testInsertThisMethod() throws Exception {
		assertMethodBodyProposal("this.|class", "hashCode(", "this.hashCode ( )|class");
	}

	public void testInsertMethod() throws Exception {
		assertMethodBodyProposal("h|foobar", "hashCode(", "hashCode ( )|foobar");
	}

	public void testInsertMethodWithParam() throws Exception {
		if (BUG_DISABLED_DUE_TO_FORMATTER_CONTEXT_INFO_INTERATION) {
			// FIXME
			System.out.println("disabled due to formatter - context info interation");
			return;
		}

		assertMethodBodyProposal("e|foobar", "equals(", "equals ( |)foobar");
	}

	public void testFormattedMethodWithParameterFilling1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
		assertMethodBodyProposal("fList.", "add(O", "fList.add ( |arg0| )");
	}

	public void testFormattedMethodWithParameterFilling2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
		assertMethodBodyProposal("fList.", "add(int", "fList.add ( |arg0|, arg1 );");
	}

	public void testFormattedMethodWithParameterGuessing1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		addMembers("private java.util.List fList;");
		addLocalVariables("int foo= 3; Object obj= null;\n");

		assertMethodBodyProposal("fList.", "add(O", "fList.add ( |obj| )");
	}

	public void testFormattedMethodWithParameterGuessing2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		addMembers("private java.util.List fList;");
		addLocalVariables("int foo= 3; Object obj= null;\n");

		assertMethodBodyProposal("fList.", "add(int", "fList.add ( |foo|, obj );");
	}

}
