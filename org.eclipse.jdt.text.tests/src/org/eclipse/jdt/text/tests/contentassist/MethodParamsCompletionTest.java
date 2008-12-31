/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodParamsCompletionTest extends AbstractCompletionTest {
	private static final Class THIS= MethodParamsCompletionTest.class;

	public static Test allTests() {
		return new TestSuite(THIS, suiteName(THIS));
	}

	public static Test setUpTest(Test test) {
		return new CompletionTestSetup(test);
	}

	public static Test suite() {
		return new CompletionTestSetup(allTests());
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		addMembers("private java.util.List fList;");
	}

	public void testMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.", "add(O", "fList.add(|arg0|)");
	}

	public void testMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.", "add(int", "fList.add(|arg0|, arg1)");
	}

	public void testInsertMethodWithParam1() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|arg0|)bar");
	}

	public void testInsertMethodWithParam2() throws Exception {
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|arg0|, arg1)bar");
	}

	public void testOverwriteMethodWithParam1() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(O", "fList.add(|arg0|)");
	}

	public void testOverwriteMethodWithParam2() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertMethodBodyProposal("fList.|bar", "add(int", "fList.add(|arg0|, arg1)");
	}

}
