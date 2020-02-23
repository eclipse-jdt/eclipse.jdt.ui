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

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.PartInitException;

import org.eclipse.ui.texteditor.AbstractTextEditor;


public abstract class OpenEditorTest extends TextPerformanceTestCase {

	private static final Class<OpenEditorTest> THIS= OpenEditorTest.class;

	public OpenEditorTest() {
		super();
	}

	public OpenEditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(OpenJavaEditorTest.suite());
		suite.addTest(OpenTextEditorTest.suite());
		return suite;
	}

	protected void measureOpenInEditor(IFile[] files, PerformanceMeter performanceMeter, boolean closeEach) throws PartInitException {
		for (IFile file : files) {
			performanceMeter.start();
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(file, true);
			performanceMeter.stop();
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 100, 10000, 100);
			if (closeEach) {
				EditorTestHelper.closeEditor(editor);
				EditorTestHelper.runEventQueue(100);
			}
		}
		performanceMeter.commit();
		assertPerformance(performanceMeter);
	}

	protected void measureOpenInEditor(String file, PerformanceMeter performanceMeter) throws PartInitException {
		measureOpenInEditor(arrayOf(file, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter(), true);
		measureOpenInEditor(arrayOf(file, getMeasuredRuns()), performanceMeter, true);
	}

	private IFile[] arrayOf(String file, int n) {
		IFile[] files= new IFile[n];
		Arrays.fill(files, ResourceTestHelper.findFile(file));
		return files;
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.TextPerformanceTestCase#createPerformanceMeter()
	 * @since 3.4
	 */
	@Override
	protected PerformanceMeter createPerformanceMeter() {
		PerformanceMeter perfMeter= super.createPerformanceMeter();
		return perfMeter;
	}
}
