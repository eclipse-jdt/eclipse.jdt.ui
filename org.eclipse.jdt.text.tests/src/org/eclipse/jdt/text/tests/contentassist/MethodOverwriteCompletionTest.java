/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 *
 * @since 3.2
 */
public class MethodOverwriteCompletionTest extends AbstractCompletionTest {
	private static final Class<MethodOverwriteCompletionTest> THIS= MethodOverwriteCompletionTest.class;

	public static Test setUpTest(Test test) {
		return new CompletionTestSetup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS, suiteName(THIS)));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
	}

	public void testThisMethod() throws Exception {
		assertMethodBodyProposal("this.|", "hashCode(", "this.hashCode()|");
	}

	public void testMethod() throws Exception {
		assertMethodBodyProposal("h", "hashCode(", "hashCode()");
	}

	public void testMethodWithParam() throws Exception {
		assertMethodBodyProposal("e", "equals(", "equals(|)");
	}

	/* overwriting */

	public void testOverwriteThisMethod() throws Exception {
		assertMethodBodyProposal("this.|class", "hashCode(", "this.hashCode()|");
	}

	public void testOverwriteMethod() throws Exception {
		assertMethodBodyProposal("h|foobar", "hashCode(", "hashCode()|");
	}

	public void testOverwriteMethodWithParam() throws Exception {
		assertMethodBodyProposal("e|foobar", "equals(", "equals(|)");
	}

}
