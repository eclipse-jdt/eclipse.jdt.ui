/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.ITypeRoot;

import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * @since 3.1
 */
public class ActivateJavaEditorTest extends ActivateEditorTest {

	private static final Class<ActivateJavaEditorTest> THIS= ActivateJavaEditorTest.class;

	private static final String SHORT_NAME= "Activate " + ActivateEditorTest.getNumberOfEditors() + " Java editors";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected String getEditorId() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}

	@Override
	public void testActivateEditor() {
		setShortName(SHORT_NAME);
		super.testActivateEditor();
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.ActivateEditorTest#waitUntilReady()
	 * @since 3.4
	 */
	@Override
	protected void waitUntilReady(AbstractTextEditor editor) {
		ITypeRoot cu= EditorUtility.getEditorInputJavaElement(editor, false);
		SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_ACTIVE_ONLY, null);
	}
}
