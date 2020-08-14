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
		assertTypeBodyProposal("toS|", "toString(", "/* (non-Javadoc)\n" +
				"     * @see java.lang.Object#toString()\n" +
				"     */\n" +
				"    @Override\n" +
				"    public String toString() {\n" +
				"        //TODO\n" +
				"        return super.toString();\n" +
				"    }|");
	}

	@Test
	public void testMethodCreation() throws Exception {
		assertTypeBodyProposal("foobar|", "foobar(", "/**\n" +
				"     * Method.\n" +
				"     */\n" +
				"    private void foobar() {\n" +
				"        //TODO\n" +
				"\n" +
				"    }|");
	}

	@Test
	public void testGetterCreation() throws Exception {
		addMembers("String test;");
		assertTypeBodyProposal("get|", "getTest(", "/**\n" +
				"     * @return the test\n" +
				"     */\n" +
				"    public String getTest() {\n" +
				"        return test;\n" +
				"    }|");
	}

	@Test
	public void testConstGetterCreation() throws Exception {
		addMembers("static final String TEST;");
		assertTypeBodyProposal("get|", "getTest(", "/**\n" +
				"     * @return the test\n" +
				"     */\n" +
				"    public static String getTest() {\n" +
				"        return TEST;\n" +
				"    }|");
	}


	@Test
	public void testDuplicateGetterCreation() throws Exception {
		addMembers("static final String TEST;");
		addMembers("String test;");
		assertTypeBodyProposal("get|", "getTest(", "/**\n" +
				"     * @return the test\n" +
				"     */\n" +
				"    public String getTest() {\n" +
				"        return test;\n" +
				"    }|");
	}

	@Test
	public void testSetterCreation() throws Exception {
		addMembers("String test;");
		assertTypeBodyProposal("set|", "setTest(", "/**\n" +
				"     * @param test the test to set\n" +
				"     */\n" +
				"    public void setTest(String test) {\n" +
				"        this.test = test;\n" +
				"    }|");
	}

	@Test
	public void testNoFinalSetterCreation() throws Exception {
		addMembers("final String test;");
		assertNoTypeBodyProposal("set|", "setTest(");
	}
}
