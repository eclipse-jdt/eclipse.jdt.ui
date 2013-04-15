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

/**
 *
 * @since 3.2
 */
public class CamelCaseCompletionTest extends AbstractCompletionTest {
	private static final Class THIS= CamelCaseCompletionTest.class;

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
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
	}

	public void testMethod() throws Exception {
		addMembers("void methodCallWithParams(int par) {}");
		assertMethodBodyProposal("this.mCW|", "methodCallWith", "this.methodCallWithParams(|);");
	}

	public void testMethodWithTrailing() throws Exception {
		addMembers("void methodCallWithParams(int par) {}");
		assertMethodBodyProposal("this.mCWith|", "methodCallWith", "this.methodCallWithParams(|);");
	}

	public void testField() throws Exception {
		addMembers("int multiCamelCaseField;");
		assertMethodBodyProposal("this.mCC|", "multiCamel", "this.multiCamelCaseField|");
	}

}
