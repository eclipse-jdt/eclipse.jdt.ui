/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Suite moved from QuickFixTest.java
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	QuickFixTest9.class,
	QuickFixTest1d8.class,
	QuickFixTest14.class,
	QuickFixTest15.class,
	QuickFixTest17.class,
	SerialVersionQuickFixTest.class,
	UtilitiesTest.class,
	UnresolvedTypesQuickFixTest.class,
	UnresolvedVariablesQuickFixTest.class,
	UnresolvedMethodsQuickFixTest.class,
	UnresolvedMethodsQuickFixTest1d8.class,
	UnresolvedMethodsQuickFixTest16.class,
	ReturnTypeQuickFixTest.class,
	LocalCorrectionsQuickFixTest.class,
	LocalCorrectionsQuickFixTest1d7.class,
	LocalCorrectionsQuickFixTest1d8.class,
	TypeMismatchQuickFixTests.class,
	ReorgQuickFixTest.class,
	ModifierCorrectionsQuickFixTest.class,
	ModifierCorrectionsQuickFixTest1d7.class,
	ModifierCorrectionsQuickFixTest9.class,
	GetterSetterQuickFixTest.class,
	AssistQuickFixTest.class,
	AssistQuickFixTest1d7.class,
	AssistQuickFixTest1d8.class,
	AssistQuickFixTest10.class,
	AssistQuickFixTest12.class,
	AssistQuickFixTest14.class,
	AssistQuickFixTest15.class,
	ChangeNonStaticToStaticTest.class,
	MarkerResolutionTest.class,
	JavadocQuickFixTest.class,
	JavadocQuickFixTest9.class,
	JavadocQuickFixTest16.class,
	ConvertForLoopQuickFixTest.class,
	ConvertIterableLoopQuickFixTest.class,
	ConvertIterableLoopQuickFixTest1d7.class,
	AdvancedQuickAssistTest.class,
	AdvancedQuickAssistTest1d7.class,
	AdvancedQuickAssistTest1d8.class,
	AdvancedQuickAssistTest10.class,
	CleanUpTestCaseSuite.class,
	QuickFixEnablementTest.class,
	SurroundWithTemplateTest.class,
	TypeParameterMismatchTest.class,
	PropertiesFileQuickAssistTest.class,
	NullAnnotationsQuickFixTest.class,
	NullAnnotationsQuickFixTest1d8.class,
	NullAnnotationsQuickFixTest1d8Mix.class,
	AnnotateAssistTest1d5.class,
	AnnotateAssistTest1d8.class,
	TypeAnnotationQuickFixTest.class
})
public class QuickFixTestSuite {
}
