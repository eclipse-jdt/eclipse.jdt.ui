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

import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.swt.events.PaintEvent;

import org.eclipse.jface.text.source.AnnotationPainter;

import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Measure the number of invocations of {@link org.eclipse.jface.text.source.AnnotationPainter#paintControl(PaintEvent)}
 * while scrolling with error annotations in the Java editor.
 *
 * @since 3.1
 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
 */
public class ScrollAnnotatedJavaEditorInvocationCountTest extends AbstractScrollAnnotatedJavaEditorTest {

	private static final Class THIS= ScrollAnnotatedJavaEditorInvocationCountTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test someTest) {
		return new PerformanceTestSetup(someTest);
	}

	protected void setUp(AbstractTextEditor editor) throws Exception {
		editor.showChangeInformation(false); // don't need to test quick diff...
		super.setUp(editor);
	}

	/**
	 * Measure the number of invocations of {@link org.eclipse.jface.text.source.AnnotationPainter#paintControl(PaintEvent)}
	 * while scrolling page wise with error annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testPageWise() throws Exception {
		measure(PAGE_WISE, createInvocationCountPerformanceMeter(), 0, 1);
	}

	/**
	 * Measure the number of invocations of {@link org.eclipse.jface.text.source.AnnotationPainter#paintControl(PaintEvent)}
	 * while scrolling line wise with error annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testLineWise() throws Exception {
		measure(LINE_WISE, createInvocationCountPerformanceMeter(), 0, 1);
	}

	/**
	 * Measure the number of invocations of {@link org.eclipse.jface.text.source.AnnotationPainter#paintControl(PaintEvent)}
	 * while scrolling and selecting line wise with error annotations in the
	 * Java editor.
	 *
	 * @throws Exception
	 */
	public void testLineWiseSelect() throws Exception {
		measure(LINE_WISE_SELECT, createInvocationCountPerformanceMeter(), 0, 1);
	}

	/**
	 * Measure the number of invocations of {@link org.eclipse.jface.text.source.AnnotationPainter#paintControl(PaintEvent)}
	 * while scrolling line wise without moving the caret with error
	 * annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testLineWiseNoCaretMove() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE, createInvocationCountPerformanceMeter(), 0, 1);
	}

	private InvocationCountPerformanceMeter createInvocationCountPerformanceMeter() throws NoSuchMethodException {
		return createInvocationCountPerformanceMeter(new Method[] {
			AnnotationPainter.class.getMethod("paintControl", new Class[] { PaintEvent.class }),
		});
	}
}
