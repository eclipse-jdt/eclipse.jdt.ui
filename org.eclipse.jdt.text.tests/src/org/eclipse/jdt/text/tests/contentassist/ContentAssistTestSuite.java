/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - ContinuousTypingCompletionTest
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Content assist test suite.
 *
 * @since 3.2
 */
public class ContentAssistTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(ContentAssistTestSuite.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(CamelCaseCompletionTest.suite());
		suite.addTest(JavadocCompletionTest.suite());
		suite.addTest(ContentAssistHistoryTest.suite());
		suite.addTest(MethodInsertCompletionTest.suite());
		suite.addTest(MethodInsertionFormattedCompletionTest.suite());
		suite.addTest(MethodOverwriteCompletionTest.suite());
		suite.addTest(MethodParamsCompletionTest.suite());
		suite.addTest(MethodParameterGuessingCompletionTest.suite());
		suite.addTest(TypeCompletionTest.suite());
		suite.addTest(TypeCompletionTest17.suite());
		suite.addTest(SpecialMethodsCompletionTest.suite());
		suite.addTest(CodeCompletionTest.suite());
		suite.addTest(CodeCompletionTest18.suite());
		suite.addTest(ContinuousTypingCompletionTest.suite());
		//$JUnit-END$

		return suite;
	}
}
