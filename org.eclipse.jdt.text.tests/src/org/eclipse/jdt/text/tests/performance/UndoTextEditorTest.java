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

public class UndoTextEditorTest extends UndoEditorTest {
	
	private static final Class THIS= UndoTextEditorTest.class;
	
	private static final String FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";
	
	private static final String ORIG_FILE= FILE_PREFIX + ".java";

	private static final String FILE= FILE_PREFIX + ".txt";

	private static final int N_OF_RUNS= 4;

	private static final int N_OF_COLD_RUNS= 2;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		ResourceTestHelper.copy(ORIG_FILE, FILE);
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(FILE);
	}
	
	public void testUndoTextEditor2() throws PartInitException {
		measureUndo(ResourceTestHelper.findFile(FILE), N_OF_RUNS, N_OF_COLD_RUNS);
	}
}
