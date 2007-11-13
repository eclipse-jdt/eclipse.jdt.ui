/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionListenerWithASTManager;
import org.eclipse.jdt.text.tests.performance.DisplayHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AnnotationPreference;


/**
 * Tests the Java Editor's occurrence marking feature.
 * 
 * @since 3.1
 */
public class MarkOccurrenceTest extends TestCase {
	
	private static final String OCCURRENCE_ANNOTATION= "org.eclipse.jdt.ui.occurrences";
	private static final String OCCURRENCE_WRITE_ANNOTATION= "org.eclipse.jdt.ui.occurrences.write";
	private static final RGB fgHighlightRGB= getHighlightRGB();
	
	private JavaEditor fEditor;
	private IDocument fDocument;
	private FindReplaceDocumentAdapter fFindReplaceDocumentAdapter;
	private int fOccurrences;
	private IAnnotationModel fAnnotationModel;
	private ISelectionListenerWithAST fSelWASTListener;
	private IRegion fMatch;
	private StyledText fTextWidget;

	

	public static Test setUpTest(Test someTest) {
		return new JUnitProjectTestSetup(someTest);
	}
	
	public static Test suite() {
		return setUpTest(new TestSuite(MarkOccurrenceTest.class));
	}
	
	
	protected void setUp() throws Exception {
		assertNotNull(fgHighlightRGB);
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, true);
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/TestCase.java"));
		assertNotNull(fEditor);
		fTextWidget= fEditor.getViewer().getTextWidget();
		assertNotNull(fTextWidget);
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
				if (fMatch != null && selection != null && selection.getOffset() == fMatch.getOffset() && selection.getLength() == fMatch.getLength()) {
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
					if (OCCURRENCE_WRITE_ANNOTATION.equals(annotation.getType()))
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
		fEditor= null;
		fTextWidget= null;
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
		
		assertOccurrences(8);
		assertOccurrencesInWidget();
	}
	
	public void testMarkOccurrencesAfterEditorReuse() {
		IPreferenceStore store= PlatformUI.getWorkbench().getPreferenceStore();
		store.setValue("REUSE_OPEN_EDITORS_BOOLEAN", true);
		
		int reuseOpenEditors= store.getInt("REUSE_OPEN_EDITORS");
		store.setValue("REUSE_OPEN_EDITORS", 1);
		
		SelectionListenerWithASTManager.getDefault().removeListener(fEditor, fSelWASTListener);
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/Test.java"));
		SelectionListenerWithASTManager.getDefault().addListener(fEditor, fSelWASTListener);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		fFindReplaceDocumentAdapter= new FindReplaceDocumentAdapter(fDocument);
		fAnnotationModel= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
		
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);
		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(1);
		assertOccurrencesInWidget();
		
		store.setValue("REUSE_OPEN_EDITORS_BOOLEAN", false);
		store.setValue("REUSE_OPEN_EDITORS", reuseOpenEditors);
	}
	
	public void testMarkMethodOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "getClass", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(2);
		assertOccurrencesInWidget();
	}
	public void testMarkFieldOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "fName", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(9);
		assertOccurrencesInWidget();
	}
	
	public void testMarkLocalOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "runMethod", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(4);
		assertOccurrencesInWidget();
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
		
		assertOccurrences(6);
		assertOccurrencesInWidget();
	}
	
	public void testMarkMethodExceptionOccurrences() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "NoSuchMethodException", true, true, true, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(2);
		assertOccurrencesInWidget();
	}
	
	public void testMarkImplementOccurrences1() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(3);
		assertOccurrencesInWidget();
	}
	public void testMarkImplementOccurrences2() {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, false);
		fOccurrences= Integer.MAX_VALUE;
		
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(1); // 1 type occurrence
		assertOccurrencesInWidget();
	}
	
	public void testMarkImplementOccurrences3() {
		try {
			fMatch= fFindReplaceDocumentAdapter.find(0, "Assert", true, true, false, false);
		} catch (BadLocationException e) {
			fail();
		}
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());
		
		assertOccurrences(2);
		assertOccurrencesInWidget();
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
		
		assertOccurrences(0);
		assertOccurrencesInWidget();
	}
	
	private void assertOccurrencesInWidget() {
		EditorTestHelper.runEventQueue(500);

		Iterator iter= fAnnotationModel.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation)iter.next();
			if (OCCURRENCE_ANNOTATION.equals(annotation.getType()))
				assertOccurrenceInWidget(fAnnotationModel.getPosition(annotation));
		}
	}

	private void assertOccurrenceInWidget(Position position) {
		StyleRange[] styleRanges= fTextWidget.getStyleRanges(position.offset, position.length);
		for (int i= 0; i < styleRanges.length; i++) {
			if (styleRanges[i].background != null) {
				RGB rgb= styleRanges[i].background.getRGB();
				if (fgHighlightRGB.equals(rgb))
					return;
			}
		}
		fail();
		
	}
	/**
	 * Returns the occurrence annotation color.
	 * 
	 * @return the occurrence annotation color
	 */
	private static RGB getHighlightRGB() {
		AnnotationPreference annotationPref= EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(OCCURRENCE_ANNOTATION);
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		if (store != null)
			return PreferenceConverter.getColor(store, annotationPref.getColorPreferenceKey());
		
		return null;
	}
	

	private void assertOccurrences(final int expected) {
		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				if (fOccurrences > 0 && fOccurrences != Integer.MAX_VALUE) {
					assertEquals(expected, fOccurrences);
				}
				return fOccurrences == expected;
			}
		};
		assertTrue(helper.waitForCondition(EditorTestHelper.getActiveDisplay(), 80000));
	}
	
}
