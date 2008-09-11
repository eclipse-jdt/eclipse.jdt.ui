/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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


/**
 * Content assist test suite.
 *
 * @since 3.2
 */
public class ContentAssistTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("Content Assist Test Suite"); //$NON-NLS-1$
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
		suite.addTest(SpecialMethodsCompletionTest.suite());
		suite.addTest(CodeCompletionTest.suite());
		//$JUnit-END$

		return suite;
	}
}
