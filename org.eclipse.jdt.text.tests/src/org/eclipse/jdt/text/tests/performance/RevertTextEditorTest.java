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

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.ui.PartInitException;


/**
 * @since 3.1
 */
public class RevertTextEditorTest extends RevertEditorTest {

	private static final int N_OF_COPIES= 20;
	private static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	private static final String FILE_PREFIX= "TextLayout";
	private static final String FILE_SUFFIX= ".txt";
	
	protected void setUp() {
		EditorTestHelper.runEventQueue();
	}

	public void testRevertTextEditor() throws PartInitException, BadLocationException {
		// cold run
		measureRevert(EditorTestHelper.findFiles(OpenEditorTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES));
	}

}
