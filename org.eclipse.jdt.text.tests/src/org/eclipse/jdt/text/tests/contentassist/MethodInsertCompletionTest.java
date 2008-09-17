/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher <eclipse@tom.eicher.name> - [content assist] prefix complete casted method proposals - https://bugs.eclipse.org/bugs/show_bug.cgi?id=247547
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;


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

	public void testFieldThisQualification() throws Exception {
		addMembers("private String qqqString;");
		addLocalVariables("String qqqString;");
		assertMethodBodyProposal("q|", "this.qqqString", "this.qqqString|");
	}

	public void testCastMethod() throws Exception {
		// bug 208540
		addLocalVariables("Object o;");
		assertMethodBodyProposal("if (o instanceof Integer) o.get|", "getInteger", "if (o instanceof Integer) ((Integer) o).getInteger(|)");
	}

	public void testCastMethodIncremental() throws Exception {
		// bug 208540
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		addLocalVariables("Object o;");
		assertMethodBodyIncrementalCompletion("if (o instanceof Integer) o.g|", "if (o instanceof Integer) o.get|");
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
