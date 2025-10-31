/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;


import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.presentation.PresentationReconciler;


/**
 * @since 3.1
 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
 */
@Deprecated
public class OpenJavaEditorInvocationCountTest extends OpenEditorTest {

	private static final Class<OpenJavaEditorInvocationCountTest> THIS= OpenJavaEditorInvocationCountTest.class;

	private static final String FILE= PerformanceTestSetup.TEXT_LAYOUT;

	@Deprecated
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Deprecated
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	@Deprecated
	public void test() throws Exception {
		InvocationCountPerformanceMeter performanceMeter= createInvocationCountPerformanceMeter(new Method[] {
			PresentationReconciler.class.getDeclaredMethod("createPresentation", IRegion.class, IDocument.class),
//			AnnotationRulerColumn.class.getDeclaredMethod("doPaint1", new Class[] { GC.class }),
//			AbstractDocument.class.getDeclaredMethod("get", new Class[] { }),
		});
		measureOpenInEditor(new IFile[] { ResourceTestHelper.findFile(FILE) }, performanceMeter, false);
	}
}
