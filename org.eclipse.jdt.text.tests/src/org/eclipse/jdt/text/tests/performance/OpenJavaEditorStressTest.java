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

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class OpenJavaEditorStressTest extends OpenEditorTest {

	private static final String FILE= PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";
	
	private static final int NUMBER_OF_RUNS= 100;

	public void testOpenJavaEditor1() throws PartInitException {
		IFile file= ResourceTestHelper.findFile(FILE);
		
		// make sure everything has been activated and loaded at least once
		IDE.openEditor(EditorTestHelper.getActivePage(), file);
		EditorTestHelper.closeAllEditors();
		
		Performance performance= Performance.getDefault();
		PerformanceMeter performanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		performanceMeter.start();
		try {
			for (int i= 0; i < NUMBER_OF_RUNS; i++) {
				IEditorPart part= IDE.openEditor(EditorTestHelper.getActivePage(), file);
				EditorTestHelper.runEventQueue(part);
				EditorTestHelper.closeAllEditors();
				EditorTestHelper.runEventQueue(part);
			}
			performanceMeter.stop();
			performanceMeter.commit();
			performance.assertPerformanceInRelativeBand(performanceMeter, Dimension.USED_JAVA_HEAP, -100, +100); // TODO: use absolute band
		} finally {
			performanceMeter.dispose();
			EditorTestHelper.closeAllEditors();
		}
	}
}