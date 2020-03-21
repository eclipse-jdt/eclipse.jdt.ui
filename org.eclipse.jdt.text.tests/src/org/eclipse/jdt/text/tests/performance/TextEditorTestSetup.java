/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.Enumeration;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.1
 */
public abstract class TextEditorTestSetup extends TestSetup {

	protected final static String DEFAULT_EDITOR_ID= "";

	private AbstractTextEditor fEditor;

	public TextEditorTestSetup(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (DEFAULT_EDITOR_ID.equals(getEditorId()))
			fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), true);
		else
			fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), getEditorId(), true);
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setEditor(getTest(), fEditor);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
		fEditor= null;
	}

	protected abstract String getFile();

	protected String getEditorId() {
		return DEFAULT_EDITOR_ID;
	}

	protected AbstractTextEditor getEditor() {
		return fEditor;
	}

	private void setEditor(Test test, AbstractTextEditor editor) {
		if (test instanceof ITextEditorTestCase)
			((ITextEditorTestCase) test).setEditor(editor);
		else if (test instanceof TestDecorator)
			setEditor(((TestDecorator) test).getTest(), editor);
		else if (test instanceof TestSuite)
			for (Enumeration<Test> iter= ((TestSuite) test).tests(); iter.hasMoreElements();)
				setEditor(iter.nextElement(), editor);
	}
}
