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


import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.presentation.PresentationReconciler;


/**
 * @since 3.1
 */
public class OpenJavaEditorCountTest extends OpenEditorTest {

	private static final Class THIS= OpenJavaEditorCountTest.class;

	private static final String FILE= PerformanceTestSetup.TEXT_LAYOUT;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	public void test() throws Exception {
		InvocationCountPerformanceMeter performanceMeter= new InvocationCountPerformanceMeter(getDefaultScenarioId(), new Method[] {
			PresentationReconciler.class.getDeclaredMethod("createPresentation", new Class[] { IRegion.class, IDocument.class }),
//			AnnotationRulerColumn.class.getDeclaredMethod("doPaint1", new Class[] { GC.class }),
//			AbstractDocument.class.getDeclaredMethod("get", new Class[] { }),
		});
		measureOpenInEditor(new IFile[] { ResourceTestHelper.findFile(FILE) }, performanceMeter, false);
	}
}
