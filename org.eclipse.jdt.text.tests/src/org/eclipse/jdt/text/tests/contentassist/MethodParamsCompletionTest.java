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

import org.junit.Test;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodParamsCompletionTest extends AbstractCompletionTest {

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
	}

	@Test
	public void testMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.", "add(O", "fList.add(|arg0|)");
	}

	@Test
	public void testMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.", "add(int", "fList.add(|arg0|, arg1);");
	}

	@Test
	public void testInsertMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|arg0|)bar");
	}

	@Test
	public void testInsertMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|arg0|, arg1);bar");
	}

	@Test
	public void testOverwriteMethodWithParam1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|arg0|)");
	}

	@Test
	public void testOverwriteMethodWithParam2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|arg0|, arg1);");
	}

	@Test
	public void testParenthesisExits() throws Exception {
		assertMethodBodyProposal("fList.|", "add(O", "fList.add(|arg0|)");
		typeAndVerify(")", "fList.add(arg0)|");
	}

	@Test
	public void testParenthesisExits_voidReturn() throws Exception {
		assertMethodBodyProposal("fList.|", "add(int", "fList.add(|arg0|, arg1);");
		typeAndVerify(",)", "fList.add(arg0, arg1);|");
	}

	@Test
	public void testDontExitFromStringLiteral() throws Exception {
		assertMethodBodyProposal("fList.|", "add(O", "fList.add(|arg0|)");
		typeAndVerify("\")\"", "fList.add(\")\"|)");
	}

	@Test
	public void testDontExitFromStringLiteral_voidReturn() throws Exception {
		assertMethodBodyProposal("fList.|", "add(int", "fList.add(|arg0|, arg1);");
		typeAndVerify(",\")\"", "fList.add(arg0, \")\"|);");
	}

	@Test
	public void testDontExitFromStringLiteral_semicolon() throws Exception {
		assertMethodBodyProposal("String.|", "format(String", "String.format(|arg0|, arg1)");
		typeAndVerify("\";\",", "String.format(\";\", |arg1|)");
	}

	@Test
	public void testCommaSkipsToNextArg() throws Exception {
		assertMethodBodyProposal("fList.|", "add(int", "fList.add(|arg0|, arg1);");
		typeAndVerify(",", "fList.add(arg0, |arg1|);");
	}

	@Test
	public void testDontExitWithUnblancedParenthesis() throws Exception {
		assertMethodBodyProposal("fList.|", "add(O", "fList.add(|arg0|)");
		typeAndVerify("(1 + \")\" + 2)", "fList.add((1 + \")\" + 2)|)");
	}

	@Test
	public void testDontExitWithUnblancedParenthesis_voidReturn() throws Exception {
		assertMethodBodyProposal("fList.|", "add(int", "fList.add(|arg0|, arg1);");
		typeAndVerify(",(1 + 2)", "fList.add(arg0, (1 + 2)|);");
	}
}
