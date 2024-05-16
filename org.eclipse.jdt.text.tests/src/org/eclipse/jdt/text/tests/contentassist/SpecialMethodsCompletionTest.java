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

/**
 *
 * @since 3.2
 */
public class SpecialMethodsCompletionTest extends AbstractCompletionTest {

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#configureCoreOptions(java.util.Hashtable)
	 */
	@Override
	protected void configureCoreOptions(Hashtable<String, String> options) {
		super.configureCoreOptions(options);
	}

	@Test
	public void testInheritedMethod() throws Exception {
		assertTypeBodyProposal("toS|", "toString(", """
			/* (non-Javadoc)
			     * @see java.lang.Object#toString()
			     */
			    @Override
			    public String toString() {
			        //TODO
			        return super.toString();
			    }|""");
	}

	@Test
	public void testMethodCreation() throws Exception {
		assertTypeBodyProposal("foobar|", "foobar(", """
			/**
			     * Method.
			     */
			    private void foobar() {
			        //TODO
			
			    }|""");
	}

	@Test
	public void testGetterCreation() throws Exception {
		addMembers("String test;");
		assertTypeBodyProposal("get|", "getTest(", """
			/**
			     * @return the test
			     */
			    public String getTest() {
			        return test;
			    }|""");
	}

	@Test
	public void testConstGetterCreation() throws Exception {
		addMembers("static final String TEST;");
		assertTypeBodyProposal("get|", "getTest(", """
			/**
			     * @return the test
			     */
			    public static String getTest() {
			        return TEST;
			    }|""");
	}


	@Test
	public void testDuplicateGetterCreation() throws Exception {
		addMembers("static final String TEST;");
		addMembers("String test;");
		assertTypeBodyProposal("get|", "getTest(", """
			/**
			     * @return the test
			     */
			    public String getTest() {
			        return test;
			    }|""");
	}

	@Test
	public void testSetterCreation() throws Exception {
		addMembers("String test;");
		assertTypeBodyProposal("set|", "setTest(", """
			/**
			     * @param test the test to set
			     */
			    public void setTest(String test) {
			        this.test = test;
			    }|""");
	}

	@Test
	public void testNoFinalSetterCreation() throws Exception {
		addMembers("final String test;");
		assertNoTypeBodyProposal("set|", "setTest(");
	}
}
