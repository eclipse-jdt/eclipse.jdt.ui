/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import org.eclipse.jdt.text.tests.comments.CommentsTestSuite;
import org.eclipse.jdt.text.tests.spelling.SpellingTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * JDT Text Test Suite.
 * 
 * @since 3.0
 */
public class JdtTextTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("JDT Text Test Suite"); //$NON-NLS-1$
		//$JUnit-BEGIN$
		suite.addTestSuite(CompilationUnitDocumentProviderTest.class);
		suite.addTest(JavaHeuristicScannerTest.suite());
		suite.addTest(JavaParameterListValidatorTest.suite());
		suite.addTest(JavaDoc2HTMLTextReaderTester.suite());
		suite.addTest(PairMatcherTest.suite());
		suite.addTest(HTML2TextReaderTester.suite());
		suite.addTest(JavaPartitionerExtensionTest.suite());
		suite.addTest(JavaColoringTest.suite());
		suite.addTest(SmartSemicolonAutoEditStrategyTest.suite());
		suite.addTest(JavaLineSegmentationTest.suite());
		suite.addTest(JavaPartitionerTest.suite());
		suite.addTest(PartitionTokenScannerTest.suite());
		suite.addTest(StringsTest.suite());
		suite.addTest(MarkOccurrenceTest.suite());
		suite.addTest(BracketInserterTest.suite());
		//$JUnit-END$
		
		suite.addTest(SpellingTestSuite.suite());
		suite.addTest(CommentsTestSuite.suite());
		suite.addTest(SemanticHighlightingTest.suite());
		
		return suite;
	}
}
