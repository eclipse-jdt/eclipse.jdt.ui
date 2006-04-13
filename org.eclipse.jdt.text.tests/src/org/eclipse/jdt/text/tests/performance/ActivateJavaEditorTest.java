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

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.dom.CompilationUnit;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST;

/**
 * @since 3.1
 */
public class ActivateJavaEditorTest extends ActivateEditorTest {

	private ISelectionListenerWithAST fListener;
	
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
	
	protected void setUp() throws Exception {
		super.setUp();
		
		// Adjust to 3.2 behavior to get better comparable results
		fListener= new ISelectionListenerWithAST() {
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				// do nothing
			}
		};
		NewSelectionListenerWithASTManager manager= NewSelectionListenerWithASTManager.getDefault();
		for (int i= 0; i < fEditors.length; i++)
			manager.addListener(fEditors[i], fListener);
		
		while (true)
			Display.getDefault().readAndDispatch();
		
	}
	
	protected void tearDown() throws Exception {
		NewSelectionListenerWithASTManager manager= NewSelectionListenerWithASTManager.getDefault();
		for (int i= 0; i < fEditors.length; i++)
			manager.removeListener(fEditors[i], fListener);
		
		super.tearDown();
	}
	
}
