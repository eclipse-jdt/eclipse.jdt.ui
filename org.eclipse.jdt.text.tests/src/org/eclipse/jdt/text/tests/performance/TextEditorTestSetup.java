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

	public TextEditorTestSetup(Test test) {
		super(test);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), true);
		EditorTestHelper.joinBackgroundActivities(editor);
		setEditor(getTest(), editor);
	}
	
	
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}
	
	protected abstract String getFile();

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
