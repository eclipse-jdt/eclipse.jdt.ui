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

package org.eclipse.jdt.text.tests;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionListenerWithASTManager;


/**
 * Tests the Java Editor's occurrence marking feature.
 * 
 * @since 3.1
 */
public class MarkOccurrenceTest extends TestCase {
	
	private static final String OCCURRENCE_ANNOTATION= "org.eclipse.jdt.ui.occurrences";
	
	private JavaEditor fEditor;
	private IDocument fDocument;
	private FindReplaceDocumentAdapter fFindReplaceDocumentAdapter;
	private int fOccurrences;
	private IAnnotationModel fAnnotationModel;
	private ISelectionListenerWithAST fSelWASTListener;
	private IRegion fMatch;
	

	public static Test setUpTest(Test someTest) {
		return new JUnitProjectTestSetup(someTest);
	}
	
	public static Test suite() {
		return setUpTest(new TestSuite(MarkOccurrenceTest.class));
	}

	
	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.1
	 */
	protected void setUp() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/TestCase.java"));
		assertNotNull(fEditor);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		fFindReplaceDocumentAdapter= new FindReplaceDocumentAdapter(fDocument);
		fAnnotationModel= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());

		fMatch= null;
		fSelWASTListener= new ISelectionListenerWithAST() {
			
			/*
			 * @see org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST#selectionChanged(org.eclipse.ui.IEditorPart, org.eclipse.jface.text.ITextSelection, org.eclipse.jdt.core.dom.CompilationUnit)
			 * @since 3.1
			 */
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				if (selection != null && selection.getOffset() == fMatch.getOffset() && selection.getLength() == fMatch.getLength()) {
					countOccurrences();
				}
			}

			private synchronized void countOccurrences() {
				fOccurrences= 0;
				Iterator iter= fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext()) {
					Annotation annotation= (Annotation)iter.next();
					if (OCCURRENCE_ANNOTATION.equals(annotation.getType()))
						fOccurrences++;
				}
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(fEditor, fSelWASTListener);
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 * @since 3.1
	 */
	protected void tearDown() throws Exception {
		SelectionListenerWithASTManager.getDefault().removeListener(fEditor, fSelWASTListener);
		EditorTestHelper.closeAllEditors();
	}
	
	private JavaEditor openJavaEditor(IPath path) {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		assertTrue(file != null && file.exists());
		try {
			return (JavaEditor)EditorTestHelper.openInEditor(file, true);
		} catch (PartInitException e) {
			fail();
			return null;
		}
	}
	
	public void testMarkTypeOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(8, fOccurrences);
	}
	public void testMarkMethodOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "getClass", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(2, fOccurrences);
	}
	public void testMarkFieldOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "fName", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(9, fOccurrences);
	}
	
	public void testMarkLocalOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "runMethod", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(4, fOccurrences);
	}
	
	public void testMarkMethodExitOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "void runTest() throws", true, true, false, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(6, fOccurrences);
	}
	
	public void testMarkMethodExceptionOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "NoSuchMethodException", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences == 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(2, fOccurrences);
	}
	
	public void testNoOccurrencesIfDisabled() {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, false);
		fOccurrences= Integer.MAX_VALUE;
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		long timeOut= System.currentTimeMillis() + 60000;
		while (fOccurrences > 0) {
			EditorTestHelper.runEventQueue(fEditor);
			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(0, fOccurrences);
	}
}
