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
 *     Tom Eicher <eclipse@tom.eicher.name> - [content assist] prefix complete casted method proposals - https://bugs.eclipse.org/bugs/show_bug.cgi?id=247547
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodInsertCompletionTest extends AbstractCompletionTest {

	@Test
	public void testThisMethod() throws Exception {
		assertMethodBodyProposal("this.|", "hashCode(", "this.hashCode()|");
	}

	@Test
	public void testMethod() throws Exception {
		assertMethodBodyProposal("h", "hashCode(", "hashCode()");
	}

	@Test
	public void testMethodWithParam() throws Exception {
		assertMethodBodyProposal("e", "equals(", "equals(|)");
	}

	/* inserting */

	@Test
	public void testInsertThisMethod() throws Exception {
		assertMethodBodyProposal("this.|class", "hashCode(", "this.hashCode()|class");
	}

	@Test
	public void testInsertMethod() throws Exception {
		assertMethodBodyProposal("h|foobar", "hashCode(", "hashCode()|foobar");
	}

	@Test
	public void testInsertMethodWithParam() throws Exception {
		assertMethodBodyProposal("e|foobar", "equals(", "equals(|)foobar");
	}

	@Test
	public void testFieldThisQualification() throws Exception {
		addMembers("private String qqqString;");
		addLocalVariables("String qqqString;");
		assertMethodBodyProposal("q|", "this.qqqString", "this.qqqString|");
	}

	@Test
	public void testCastMethod() throws Exception {
		// bug 208540
		addLocalVariables("Object o;");
		assertMethodBodyProposal("if (o instanceof Integer) o.get|", "getInteger", "if (o instanceof Integer) ((Integer) o).getInteger(|)");
	}

	@Test
	public void testCastMethodIncremental() throws Exception {
		// bug 208540
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("Object o;");
		assertMethodBodyIncrementalCompletion("if (o instanceof Integer) o.ge|", "if (o instanceof Integer) o.get|");
	}

	/* camel case */

	@Test
	public void testCamelCase() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		assertMethodBodyProposal("hC", "hashCode(", "hashCode()");
	}

	@Test
	public void testCamelCaseWithEmptyPrefix() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		assertMethodBodyProposal("", "hashCode(", "hashCode()");
	}

	/* Insert common prefixes automatically */

	@Test
	public void test1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.ind|", "s.indexOf|");
	}

	@Test
	public void test2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.su|", "s.sub|");
	}

	@Test
	public void test3() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.tar|", "s.tartsWith|");
	}

	@Test
	public void test4() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.Po|", "s.Point|");
	}

	@Test
	public void test5() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.ubs|", "s.ubs|");
	}

	@Test
	public void test6() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("String s;");
		assertMethodBodyIncrementalCompletion("s.Su|", "s.sub|");
	}

	@Test
	public void testBug567743_1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		addLocalVariables("String unique_abc;");
		assertMethodBodyIncrementalCompletion("unique_|", "unique_abc|");
	}

	@Test
	public void testBug567743_2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		addMembers("static String unique_abc;");
		assertMethodBodyIncrementalCompletion("unique_|", "unique_abc|");
	}
}
