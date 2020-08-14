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
public class MethodParameterGuessingCompletionTest extends AbstractCompletionTest {

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		addMembers("private java.util.List fList;");
		addLocalVariables("int foo= 3; Object obj= null;\n");
	}

	@Test
	public void testMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.", "add(O", "fList.add(|obj|)");
	}

	@Test
	public void testMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.", "add(int", "fList.add(|foo|, obj);");
	}

	@Test
	public void testInsertMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|obj|)bar");
	}

	@Test
	public void testInsertMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|foo|, obj);bar");
	}

	@Test
	public void testOverwriteMethodWithParam1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|obj|)");
	}

	@Test
	public void testOverwriteMethodWithParam2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|foo|, obj);");
	}
}
