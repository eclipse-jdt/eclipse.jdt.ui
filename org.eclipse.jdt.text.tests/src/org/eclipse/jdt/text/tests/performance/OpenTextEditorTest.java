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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ui.PartInitException;

/**
 * Opens 20 instances of org.eclipse.swt.graphics.TextLayout leaving
 * each of them open and then closes all together them (close all).
 * Then repeats above scenario.
 * <p>
 * This tests a mid-size file.
 * </p> 
 * 
 * @since 3.1
 */
public class OpenTextEditorTest extends OpenEditorTest {

	private static final Class THIS= OpenTextEditorTest.class;
	
	public static final int N_OF_COPIES= 20;
	
	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";
	
	public static final String FILE_SUFFIX= ".txt";
	
	public static final String ORIG_FILE= PATH + FILE_PREFIX + ".java";

	public static Test suite() {
		return new PerformanceTestSetup(new OpenTextEditorTestSetup(new TestSuite(THIS)));
	}
	
	protected void setUp() {
		EditorTestHelper.runEventQueue();
	}

	public void testOpenTextEditor1() throws PartInitException {
		// cold run
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES));
	}
	public void testOpenTextEditor2() throws PartInitException {
		// warm run
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES));
	}
}
