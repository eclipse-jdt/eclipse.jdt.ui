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

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.swt.events.PaintEvent;

import org.eclipse.jface.text.source.AnnotationPainter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Counts number of repaints ({@link AnnotationPainter#paintControl(PaintEvent)})
 * when typing on a line with annotations shown as squiggles in the Java editor.
 * 
 * @since 3.1
 */
public class JavaTypingInvocationCountTest extends TypingInvocationCountTest {

	private static class Setup extends TypingInvocationCountTest.Setup {
		
		public Setup(Test test) {
			super(test);
		}
		
		protected String getEditorId() {
			return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
		}

		protected String getPerspectiveId() {
			return EditorTestHelper.JAVA_PERSPECTIVE;
		}
	}

	private static final Class THIS= TypingInvocationCountTest.class;
	
	public JavaTypingInvocationCountTest() {
		super();
	}
	public JavaTypingInvocationCountTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(new JavaTypingInvocationCountTest("test00"));
		suite.addTest(new JavaTypingInvocationCountTest("test01"));
		suite.addTest(new JavaTypingInvocationCountTest("test02"));
		suite.addTest(new JavaTypingInvocationCountTest("test03"));
		suite.addTest(new JavaTypingInvocationCountTest("test10"));
		suite.addTest(new JavaTypingInvocationCountTest("test11"));
		suite.addTest(new JavaTypingInvocationCountTest("test12"));
		suite.addTest(new JavaTypingInvocationCountTest("test13"));
		return new Setup(suite);
	}
}
