/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import org.eclipse.jdt.text.tests.codemining.CodeMiningTriggerTest;
import org.eclipse.jdt.text.tests.codemining.ParameterNamesCodeMiningTest;
import org.eclipse.jdt.text.tests.contentassist.ContentAssistTestSuite;
import org.eclipse.jdt.text.tests.folding.FoldingTestSuite;
import org.eclipse.jdt.text.tests.semantictokens.SemanticTokensProviderTest;
import org.eclipse.jdt.text.tests.spelling.SpellCheckEngineTestCase;
import org.eclipse.jdt.text.tests.templates.TemplatesTestSuite;


/**
 * JDT Text Test Suite.
 *
 * @since 3.0
 */
@Suite
@SelectClasses({
	PluginsNotLoadedTest.class,
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
	BracketInserterTest.class,
	SpellCheckEngineTestCase.class,
	SemanticHighlightingTest.class,
	SemanticTokensProviderTest.class,
	AutoboxingSemanticHighlightingTest.class,
	Java23SemanticHighlightingTest.class,
	NewForLoopJavaContextTest.class,
	IteratorForLoopJavaContextTest.class,
	ArrayWithTempVarForLoopJavaContextTest.class,
	JavaDoubleClickSelectorTest.class,
	JavaStringDoubleClickStrategyTest.class,
	BreakContinueTargetFinderTest.class,
	EnumConstructorTargetFinderTest.class,
	ContentAssistTestSuite.class,
	IndentActionTest.class,
	IndentActionTest15.class,
	TemplatesTestSuite.class,
	JavaElementPrefixPatternMatcherTest.class,
	CodeMiningTriggerTest.class,
	ParameterNamesCodeMiningTest.class,
	FoldingTestSuite.class,
})
public class JdtTextTestSuite {
}