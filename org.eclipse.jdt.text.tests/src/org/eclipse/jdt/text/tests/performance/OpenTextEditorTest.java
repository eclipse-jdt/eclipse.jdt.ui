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

import org.eclipse.ui.PartInitException;

public class OpenTextEditorTest extends OpenEditorTest {

	public static final int N_OF_COPIES= 20;
	
	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";
	
	public static final String FILE_SUFFIX= ".txt";
	
	public static final String ORIG_FILE= PATH + FILE_PREFIX + ".java";
	
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
