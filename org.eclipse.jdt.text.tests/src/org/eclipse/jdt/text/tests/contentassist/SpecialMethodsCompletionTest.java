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

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *
 * @since 3.2
 */
public class SpecialMethodsCompletionTest extends AbstractCompletionTest {
	private static final Class THIS= SpecialMethodsCompletionTest.class;

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

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#configureCoreOptions(java.util.Hashtable)
	 */
	protected void configureCoreOptions(Hashtable options) {
		super.configureCoreOptions(options);
	}

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

	public void testMethodCreation() throws Exception {
		assertTypeBodyProposal("foobar|", "foobar(", "/**\n" +
				"     * Method.\n" +
				"     */\n" +
				"    private void foobar() {\n" +
				"        //TODO\n" +
				"\n" +
				"    }|");
	}
}
