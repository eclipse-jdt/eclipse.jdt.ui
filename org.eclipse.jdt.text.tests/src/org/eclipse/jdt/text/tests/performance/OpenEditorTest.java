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

import org.eclipse.core.resources.IFile;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;


public abstract class OpenEditorTest extends TextPerformanceTestCase {
	
	public OpenEditorTest() {
		super();
	}

	public OpenEditorTest(String name) {
		super(name);
	}

	protected void measureOpenInEditor(IFile[] files, PerformanceMeter performanceMeter) throws PartInitException {
		for (int i= 0, n= files.length; i < n; i++) {
			performanceMeter.start();
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(files[i], true);
			performanceMeter.stop();
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 100, 10000, 100);
		}
		performanceMeter.commit();
		assertPerformance(performanceMeter);
	}
}
