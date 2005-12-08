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
		//$JUnit-END$
		
		return suite;
	}
}
