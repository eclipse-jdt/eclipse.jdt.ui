/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * 
 * @since 3.2
 */
public class MethodInsertCompletionTest extends AbstractCompletionTest {
	private static final Class THIS= MethodInsertCompletionTest.class;

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
	
	/* inserting */
	
	public void testInsertThisMethod() throws Exception {
		assertMethodBodyProposal("this.|class", "hashCode(", "this.hashCode()|class");
	}
	
	public void testInsertMethod() throws Exception {
		assertMethodBodyProposal("h|foobar", "hashCode(", "hashCode()|foobar");
	}
	
	public void testInsertMethodWithParam() throws Exception {
		assertMethodBodyProposal("e|foobar", "equals(", "equals(|)foobar");
	}
	
	/* camel case */
	
	public void testCamelCase() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		assertMethodBodyProposal("hC", "hashCode(", "hashCode()");
    }
	
	public void testCamelCaseWithEmptyPrefix() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		assertMethodBodyProposal("", "hashCode(", "hashCode()");
	}
}
