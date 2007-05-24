/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 * Counts number of repaints ({@link AnnotationPainter#paintControl(PaintEvent)})
 * when typing on a line with annotations shown as squiggles in the text editor.
 * 
 * @since 3.1
 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
 */
public class TextTypingInvocationCountTest extends TypingInvocationCountTest {

	private static class Setup extends TypingInvocationCountTest.Setup {
		
		public Setup(Test test) {
			super(test);
		}
		
		protected String getEditorId() {
			return EditorTestHelper.TEXT_EDITOR_ID;
		}

		protected String getPerspectiveId() {
			return EditorTestHelper.RESOURCE_PERSPECTIVE_ID;
		}
	}

	private static final Class THIS= TypingInvocationCountTest.class;
	
	public TextTypingInvocationCountTest() {
		super();
	}
	public TextTypingInvocationCountTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(new TextTypingInvocationCountTest("test00"));
		suite.addTest(new TextTypingInvocationCountTest("test01"));
		suite.addTest(new TextTypingInvocationCountTest("test02"));
		suite.addTest(new TextTypingInvocationCountTest("test03"));
		suite.addTest(new TextTypingInvocationCountTest("test10"));
		suite.addTest(new TextTypingInvocationCountTest("test11"));
		suite.addTest(new TextTypingInvocationCountTest("test12"));
		suite.addTest(new TextTypingInvocationCountTest("test13"));
		return new Setup(suite);
	}
}
