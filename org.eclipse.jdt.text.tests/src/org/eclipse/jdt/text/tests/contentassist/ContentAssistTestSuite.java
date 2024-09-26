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
 *     Mickael Istria (Red Hat Inc.) - ContinuousTypingCompletionTest
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Content assist test suite.
 *
 * @since 3.2
 */
@Suite
@SelectClasses({
	//$JUnit-BEGIN$
			CamelCaseCompletionTest.class,
			JavadocCompletionTest.class,
			ContentAssistHistoryTest.class,
			MethodInsertCompletionTest.class,
			MethodInsertionFormattedCompletionTest.class,
			MethodOverwriteCompletionTest.class,
			MethodParamsCompletionTest.class,
			MethodParameterGuessingCompletionTest.class,
			MissingTypeCompletionTest.class,
			TypeCompletionTest.class,
			TypeCompletionTest1d7.class,
			SpecialMethodsCompletionTest.class,
			CodeCompletionTest.class,
			CodeCompletionTest1d8.class,
			CodeCompletionTest16.class,
			CodeCompletionTest23.class,
			ContinuousTypingCompletionTest.class,
			ChainCompletionTest.class,
			PostFixCompletionTest.class
			//$JUnit-END$
})
public class ContentAssistTestSuite {
}
