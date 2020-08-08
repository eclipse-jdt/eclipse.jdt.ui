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

import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;

/**
 *
 * @since 3.2
 */
public class CamelCaseCompletionTest extends AbstractCompletionTest {
	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#configureCoreOptions(java.util.Hashtable)
	 */
	@Override
	protected void configureCoreOptions(Hashtable<String, String> options) {
		super.configureCoreOptions(options);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
	}

	@Test
	public void testMethod() throws Exception {
		addMembers("void methodCallWithParams(int par) {}");
		assertMethodBodyProposal("this.mCW|", "methodCallWith", "this.methodCallWithParams(|);");
	}

	@Test
	public void testMethodWithTrailing() throws Exception {
		addMembers("void methodCallWithParams(int par) {}");
		assertMethodBodyProposal("this.mCWith|", "methodCallWith", "this.methodCallWithParams(|);");
	}

	@Test
	public void testField() throws Exception {
		addMembers("int multiCamelCaseField;");
		assertMethodBodyProposal("this.mCC|", "multiCamel", "this.multiCamelCaseField|");
	}
}
