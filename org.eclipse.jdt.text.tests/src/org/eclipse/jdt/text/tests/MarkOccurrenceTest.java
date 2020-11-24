/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.text.tests.performance.DisplayHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;

import org.eclipse.ui.texteditor.AnnotationPreference;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.dom.CompilationUnit;

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
public class MarkOccurrenceTest {

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

	@Rule
	public JUnitProjectTestSetup jpts=new JUnitProjectTestSetup();

	@Before
	public void setUp() throws Exception {
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

		fOccurrences= -1; // initialize

		fMatch= null;
		fSelWASTListener= new ISelectionListenerWithAST() {

			/*
			 * @see org.eclipse.jdt.internal.ui.viewsupport.ISelectionListenerWithAST#selectionChanged(org.eclipse.ui.IEditorPart, org.eclipse.jface.text.ITextSelection, org.eclipse.jdt.core.dom.CompilationUnit)
			 * @since 3.1
			 */
			@Override
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				if (fMatch != null && selection != null && selection.getOffset() == fMatch.getOffset() && selection.getLength() == fMatch.getLength()) {
					countOccurrences();
				}
			}

			private void countOccurrences() {
				synchronized (MarkOccurrenceTest.this) {
					int occurrences= 0;
					Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
					while (iter.hasNext()) {
						Annotation annotation= iter.next();
						if (OCCURRENCE_ANNOTATION.equals(annotation.getType()))
							occurrences++;
						if (OCCURRENCE_WRITE_ANNOTATION.equals(annotation.getType()))
							occurrences++;

					}
					fOccurrences= occurrences;
				}
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(fEditor, fSelWASTListener);
	}

	@After
	public void tearDown() throws Exception {
		SelectionListenerWithASTManager.getDefault().removeListener(fEditor, fSelWASTListener);
		EditorTestHelper.closeAllEditors();
		fEditor= null;
		fTextWidget= null;
		fAnnotationModel= null;
		fDocument= null;
		fFindReplaceDocumentAdapter= null;
		fSelWASTListener= null;
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

	@Test
	public void markTypeOccurrences() {
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

	@Test
	public void markOccurrencesAfterEditorReuse() {
		IPreferenceStore store= getPlatformUIStore();
		store.setValue("REUSE_OPEN_EDITORS_BOOLEAN", true);

		int reuseOpenEditors= store.getInt("REUSE_OPEN_EDITORS");
		store.setValue("REUSE_OPEN_EDITORS", 1);

		try {
			SelectionListenerWithASTManager.getDefault().removeListener(fEditor, fSelWASTListener);

			JavaEditor newEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/Test.java"));
			assertEquals(fEditor, newEditor);
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
		} finally {
			store.setValue("REUSE_OPEN_EDITORS_BOOLEAN", false);
			store.setValue("REUSE_OPEN_EDITORS", reuseOpenEditors);
		}
	}

	/**
	 * Returns the preference store from Platform UI.
	 *
	 * @return the preference store
	 * @since 3.4
	 * @deprecated to get rid of deprecation warning in this file
	 */
	@Deprecated
	private IPreferenceStore getPlatformUIStore() {
		IPreferenceStore store= PlatformUI.getWorkbench().getPreferenceStore();
		return store;
	}

	@Test
	public void markMethodOccurrences() {
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
	@Test
	public void markFieldOccurrences() {
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

	@Test
	public void markLocalOccurrences() {
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

	@Test
	public void markMethodExitOccurrences() {
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

	@Test
	public void markMethodExceptionOccurrences() {
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

	@Test
	public void markImplementOccurrences1() {
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

	@Test
	public void markImplementOccurrences2() {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, false);

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

	@Test
	public void markImplementOccurrences3() {
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

	@Test
	public void noOccurrencesIfDisabled() {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, false);
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

		Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= iter.next();
			if (OCCURRENCE_ANNOTATION.equals(annotation.getType()))
				assertOccurrenceInWidget(fAnnotationModel.getPosition(annotation));
		}
	}

	private void assertOccurrenceInWidget(Position position) {
		for (StyleRange styleRange : fTextWidget.getStyleRanges(position.offset, position.length)) {
			if (styleRange.background != null) {
				RGB rgb= styleRange.background.getRGB();
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
			@Override
			protected boolean condition() {
				synchronized (MarkOccurrenceTest.this) {
					if (fOccurrences != -1) {
						assertEquals(expected, fOccurrences);
						return true;
					}
					return false;
				}
			}
		};
		assertTrue(helper.waitForCondition(EditorTestHelper.getActiveDisplay(), 80000));
	}
}
