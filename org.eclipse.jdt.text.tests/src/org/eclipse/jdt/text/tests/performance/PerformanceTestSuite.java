/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @since 3.1
 */
public class PerformanceTestSuite extends TestSuite {

	public static Test suite() {
		return new PerformanceTestSetup(new PerformanceTestSuite());
	}
	
	public PerformanceTestSuite() {
		addTest(ActivateJavaEditorTest.suite());
		addTest(RevertTextEditorTest.suite());
		addTest(RevertJavaEditorTest.suite());
		addTest(ToggleCommentTest.suite());
		addTest(UndoTextEditorTest.suite());
		addTest(UndoJavaEditorTest.suite());
		addTest(OpenQuickOutlineTest.suite());
		addTest(OpenJavaContentAssistTest.suite());
		addTest(JavaSmartPasteTest.suite());
		addTest(ActivateTextEditorTest.suite());
		addTest(SaveTextEditorTest.suite());
		addTest(SaveJavaEditorTest.suite());
		addTest(JavaFormatterTest.suite());
		addTest(JavaExpandSelectionTest.suite());
		addTest(ConvertLineDelimitersProjectTest.suite());
		addTest(JavaFormatterProjectTest.suite());
		addTest(ContentTypeTest.suite());
		addTest(CodeCompletionPerformanceTest.suite());
		addTest(DocumentLineDifferInitializationTest.suite());
		addTest(SynchronizedLineDifferInitializationTest.suite());

		// FIXME: Disabled, no longer works since at least M3, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=121710
//		addTest(DocumentLineDifferModificationTest.suite());

		addTest(LineTrackerPerformanceTest.suite());
		addTest(new OpenJavaEditorTest.Setup(EmptyTestCase.suite(), false)); // the actual test runs in its own workbench (see test.xml)
	}
}
