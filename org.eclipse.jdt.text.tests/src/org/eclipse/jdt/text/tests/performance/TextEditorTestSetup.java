/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import java.util.Enumeration;

import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * @since 3.1
 */
public abstract class TextEditorTestSetup extends TestSetup {

	protected final static String DEFAULT_EDITOR_ID= "";

	private AbstractTextEditor fEditor;

	public TextEditorTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		if (getEditorId() == DEFAULT_EDITOR_ID)
			fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), true);
		else
			fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), getEditorId(), true);
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setEditor(getTest(), fEditor);
	}

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
			for (Enumeration iter= ((TestSuite) test).tests(); iter.hasMoreElements();)
				setEditor((Test) iter.nextElement(), editor);
	}
}
