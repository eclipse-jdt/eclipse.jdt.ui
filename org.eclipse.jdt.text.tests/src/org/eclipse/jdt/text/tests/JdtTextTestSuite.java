/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.eclipse.jdt.text.tests.codemining.CodeMiningTriggerTest;
import org.eclipse.jdt.text.tests.codemining.ParameterNamesCodeMiningTest;
import org.eclipse.jdt.text.tests.contentassist.ContentAssistTestSuite;
import org.eclipse.jdt.text.tests.spelling.SpellCheckEngineTestCase;
import org.eclipse.jdt.text.tests.templates.TemplatesTestSuite;


/**
 * JDT Text Test Suite.
 *
 * @since 3.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CompilationUnitDocumentProviderTest.class,
	JavaHeuristicScannerTest.class,
	JavaAutoIndentStrategyTest.class,
	JavaBreakIteratorTest.class,
	JavaParameterListValidatorTest.class,
	JavaDoc2HTMLTextReaderTester.class,
	JavaPairMatcherTest.class,
	JavaPartitionerExtensionTest.class,
	JavaColoringTest.class,
	SmartSemicolonAutoEditStrategyTest.class,
	JavaPartitionerTest.class,
	PropertiesFilePartitionerTest.class,
	PropertiesFileAutoEditStrategyTest.class,
//	PartitionTokenScannerTest.class,
	MarkOccurrenceTest.class,
	MarkOccurrenceTest1d7.class,
	MarkOccurrenceTest1d8.class,
	PluginsNotLoadedTest.class,
//	PluginsNotLoadedTest.addLoadedPlugIns(
//			new String[] {
//					"org.eclipse.core.filebuffers.tests",
//					"org.eclipse.core.variables",
//					"org.eclipse.team.cvs.core",
//					"org.eclipse.test.performance"
//			});
	BracketInserterTest.class,
	SpellCheckEngineTestCase.class,
	SemanticHighlightingTest.class,
	AutoboxingSemanticHighlightingTest.class,
	NewForLoopJavaContextTest.class,
	IteratorForLoopJavaContextTest.class,
	ArrayWithTempVarForLoopJavaContextTest.class,
	JavaDoubleClickSelectorTest.class,
	JavaStringDoubleClickStrategyTest.class,
	BreakContinueTargetFinderTest.class,
	EnumConstructorTargetFinderTest.class,
	ContentAssistTestSuite.class,
	IndentActionTest.class,
	TemplatesTestSuite.class,
	JavaElementPrefixPatternMatcherTest.class,
	CodeMiningTriggerTest.class,
	ParameterNamesCodeMiningTest.class,
})
public class JdtTextTestSuite {
}