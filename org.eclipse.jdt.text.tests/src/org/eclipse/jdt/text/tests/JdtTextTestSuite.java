/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.text.tests.contentassist.ContentAssistTestSuite;
import org.eclipse.jdt.text.tests.spelling.SpellingTestSuite;
import org.eclipse.jdt.text.tests.templates.TemplatesTestSuite;


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
		suite.addTest(JavaPairMatcherTest.suite());
		suite.addTest(JavaPartitionerExtensionTest.suite());
		suite.addTest(JavaColoringTest.suite());
		suite.addTest(SmartSemicolonAutoEditStrategyTest.suite());
		suite.addTest(JavaPartitionerTest.suite());
		suite.addTest(PropertiesFilePartitionerTest.suite());
//		suite.addTest(PartitionTokenScannerTest.suite());
		suite.addTest(MarkOccurrenceTest.suite());
		suite.addTest(PluginsNotLoadedTest.suite());
		PluginsNotLoadedTest.addLoadedPlugIns(
				new String[] {
						"org.eclipse.core.filebuffers.tests",
						"org.eclipse.core.variables",
						"org.eclipse.jsch.core",
						"org.eclipse.team.cvs.core"
				});
		suite.addTest(BracketInserterTest.suite());
		suite.addTest(SpellingTestSuite.suite());
		suite.addTest(SemanticHighlightingTest.suite());
		suite.addTest(AutoboxingSemanticHighlightingTest.suite());
		suite.addTest(NewForLoopJavaContextTest.suite());
		suite.addTest(JavaDoubleClickSelectorTest.suite());
		suite.addTest(BreakContinueTargetFinderTest.suite());
		suite.addTest(ContentAssistTestSuite.suite());
		suite.addTest(IndentActionTest.suite());
		suite.addTest(TemplatesTestSuite.suite());
		//$JUnit-END$

		return suite;
	}
}
