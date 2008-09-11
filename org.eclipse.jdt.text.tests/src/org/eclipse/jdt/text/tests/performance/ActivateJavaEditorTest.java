/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.ITypeRoot;

import org.eclipse.jdt.ui.SharedASTProvider;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * @since 3.1
 */
public class ActivateJavaEditorTest extends ActivateEditorTest {

	private static final Class THIS= ActivateJavaEditorTest.class;

	private static final String SHORT_NAME= "Activate " + ActivateEditorTest.getNumberOfEditors() + " Java editors";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditorId() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}

	public void testActivateEditor() {
		setShortName(SHORT_NAME);
		super.testActivateEditor();
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.ActivateEditorTest#waitUntilReady()
	 * @since 3.4
	 */
	protected void waitUntilReady(AbstractTextEditor editor) {
		ITypeRoot cu= EditorUtility.getEditorInputJavaElement(editor, false);
		SharedASTProvider.getAST(cu, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
	}
}
