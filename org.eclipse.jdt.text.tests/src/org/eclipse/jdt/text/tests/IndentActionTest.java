/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.util.ListResourceBundle;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.text.tests.performance.ResourceTestHelper;

/**
 * 
 * @since 3.2
 */
public class IndentActionTest extends TestCase {
	private static final String PROJECT= "IndentTests";

	private static final class EmptyBundle extends ListResourceBundle {
		protected Object[][] getContents() {
			return new Object[0][];
		}
	}

	protected static class IndentTestSetup extends TestSetup {

		private IJavaProject fJavaProject;
		
		public IndentTestSetup(Test test) {
			super(test);
		}
		
		protected void setUp() throws Exception {
			super.setUp();
			
			fJavaProject= EditorTestHelper.createJavaProject(PROJECT, "testResources/indentation");
		}

		protected void tearDown () throws Exception {
			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);
			
			super.tearDown();
		}
	}
	
	private static final Class THIS= IndentActionTest.class;
	public static Test suite() {
		return new IndentTestSetup(new TestSuite(THIS));
	}

	private JavaEditor fEditor;
	private SourceViewer fSourceViewer;
	private IDocument fDocument;

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		String filename= createFileName("Before");
		fEditor= (JavaEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(filename), true);
		fSourceViewer= EditorTestHelper.getSourceViewer(fEditor);
		fDocument= fSourceViewer.getDocument();
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		EditorTestHelper.closeEditor(fEditor);
	}
	
	private void assertIndentResult() throws Exception {
		String afterFile= createFileName("Modified");
		String expected= ResourceTestHelper.read(afterFile).toString();
		
		new IndentAction(new EmptyBundle(), "prefix", fEditor, false).run();
		
		assertEquals(expected, fDocument.get());
	}

	private String createFileName(String qualifier) {
		String name= getName();
		name= name.substring(4, 5).toLowerCase() + name.substring(5);
		return "/" + PROJECT + "/src/" + name + "/" + qualifier + ".java";
	}
	
	private void selectAll() {
		fSourceViewer.setSelectedRange(0, fDocument.getLength());
	}
	
	public void testUnchanged() throws Exception {
		selectAll();
		assertIndentResult();
	}
	
	public void testBug122261() throws Exception {
		selectAll();
		assertIndentResult();
	}

}
