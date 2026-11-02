/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jdt.text.tests.performance.DisplayHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
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

	@RegisterExtension
	public JUnitProjectTestSetup jpts=new JUnitProjectTestSetup();

	@BeforeEach
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

			@Override
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				if (fMatch != null && selection != null && selection.getOffset() == fMatch.getOffset() && selection.getLength() == fMatch.getLength()) {
					countOccurrences();
				}
			}

			private void countOccurrences() {
				synchronized (MarkOccurrenceTest.this) {
					fOccurrences= countOccurrenceAnnotations();
				}
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(fEditor, fSelWASTListener);
	}

	@AfterEach
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
			fail(e.getMessage());
			return null;
		}
	}

	@Test
	public void markTypeOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(8);
		assertOccurrencesInWidget();
	}

	@Test
	public void markOccurrencesAfterEditorReuse() throws BadLocationException {
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

			fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);

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

	@Test
	public void markOccurrencesAfterCanceledUpdate() throws Exception {
		assertOccurrencesAfterCanceledUpdate(false);
	}

	@Test
	public void markOccurrencesAfterCanceledUpdateWithExistingAnnotations() throws Exception {
		assertOccurrencesAfterCanceledUpdate(true);
	}

	private void assertOccurrencesAfterCanceledUpdate(boolean withExistingAnnotations) throws Exception {
		Accessor editorAccessor= new Accessor(fEditor, JavaEditor.class);
		ISelectionListenerWithAST occurrencesListener= (ISelectionListenerWithAST) editorAccessor.get("fPostSelectionListenerWithAST");
		SelectionListenerWithASTManager manager= SelectionListenerWithASTManager.getDefault();
		manager.removeListener(fEditor, occurrencesListener);
		try {
			// Let any earlier callbacks finish, then drive the update and selection
			// validation explicitly so no background update can mask the regression.
			EditorTestHelper.joinBackgroundActivities(fEditor);
			ICompilationUnit unit= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
			assertNotNull(unit);
			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(unit);
			parser.setResolveBindings(true);
			CompilationUnit ast= (CompilationUnit) parser.createAST(null);
			editorAccessor.invoke("removeOccurrenceAnnotations", new Object[0]);

			ISelectionChangedListener selectionListener= (ISelectionChangedListener) new Accessor(fEditor, AbstractTextEditor.class).get("fSelectionListener");
			ISelectionValidator validator= (ISelectionValidator) fEditor.getSelectionProvider();
			Class<?>[] parameterTypes= { ITextSelection.class, CompilationUnit.class };

			if (withExistingAnnotations) {
				IRegion match= fFindReplaceDocumentAdapter.find(0, "fName", true, true, true, false);
				assertNotNull(match);
				ITextSelection selection= new TextSelection(fDocument, match.getOffset(), match.getLength());
				selectionListener.selectionChanged(new SelectionChangedEvent(fEditor.getSelectionProvider(), selection));
				assertTrue(validator.isValid(selection));
				editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
				assertEquals(9, countOccurrenceAnnotations());
			}

			IRegion match= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
			assertNotNull(match);
			ITextSelection selection= new TextSelection(fDocument, match.getOffset(), match.getLength());
			Object previousAnnotations= editorAccessor.get("fOccurrenceAnnotations");
			Object previousTargetRegion= editorAccessor.get("fMarkOccurrenceTargetRegion");
			Object previousModificationStamp= editorAccessor.get("fMarkOccurrenceModificationStamp");
			// The AST callback arrives before the selection validator has seen this
			// selection object. The finder must discard this update without caching it.
			assertFalse(validator.isValid(selection));
			editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
			assertEquals(withExistingAnnotations ? 9 : 0, countOccurrenceAnnotations());
			assertSame(previousAnnotations, editorAccessor.get("fOccurrenceAnnotations"), "Cancellation must preserve the installed annotations");
			assertSame(previousTargetRegion, editorAccessor.get("fMarkOccurrenceTargetRegion"), "Cancellation must preserve the last successful target region");
			assertEquals(previousModificationStamp, editorAccessor.get("fMarkOccurrenceModificationStamp"), "Cancellation must preserve the last successful modification stamp");

			selectionListener.selectionChanged(new SelectionChangedEvent(fEditor.getSelectionProvider(), selection));
			assertTrue(validator.isValid(selection));
			editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
			assertEquals(8, countOccurrenceAnnotations(), "A canceled update must allow a valid retry in the same word");

			Object annotations= editorAccessor.get("fOccurrenceAnnotations");
			editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
			assertSame(annotations, editorAccessor.get("fOccurrenceAnnotations"), "A successful update should still be cached");
		} finally {
			manager.addListener(fEditor, occurrencesListener);
		}
	}

	@Test
	public void markOccurrencesAfterConcurrentRemoval() throws Exception {
		assertOccurrencesAfterConcurrentRemoval(false);
	}

	@Test
	public void markOccurrencesAfterConcurrentRemovalWithExistingAnnotations() throws Exception {
		assertOccurrencesAfterConcurrentRemoval(true);
	}

	private void assertOccurrencesAfterConcurrentRemoval(boolean withExistingAnnotations) throws Exception {
		Accessor editorAccessor= new Accessor(fEditor, JavaEditor.class);
		ISelectionListenerWithAST occurrencesListener= (ISelectionListenerWithAST) editorAccessor.get("fPostSelectionListenerWithAST");
		SelectionListenerWithASTManager manager= SelectionListenerWithASTManager.getDefault();
		manager.removeListener(fEditor, occurrencesListener);
		try {
			// Drive updates explicitly so background callbacks cannot restore the
			// annotations and hide a stale cache left behind by removal.
			EditorTestHelper.joinBackgroundActivities(fEditor);
			ICompilationUnit unit= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
			assertNotNull(unit);
			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(unit);
			parser.setResolveBindings(true);
			CompilationUnit ast= (CompilationUnit) parser.createAST(null);
			editorAccessor.invoke("removeOccurrenceAnnotations", new Object[0]);

			ISelectionChangedListener selectionListener= (ISelectionChangedListener) new Accessor(fEditor, AbstractTextEditor.class).get("fSelectionListener");
			ISelectionValidator validator= (ISelectionValidator) fEditor.getSelectionProvider();
			Class<?>[] parameterTypes= { ITextSelection.class, CompilationUnit.class };

			if (withExistingAnnotations) {
				IRegion match= fFindReplaceDocumentAdapter.find(0, "fName", true, true, true, false);
				assertNotNull(match);
				ITextSelection selection= new TextSelection(fDocument, match.getOffset(), match.getLength());
				selectionListener.selectionChanged(new SelectionChangedEvent(fEditor.getSelectionProvider(), selection));
				assertTrue(validator.isValid(selection));
				editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
				assertEquals(9, countOccurrenceAnnotations());
			}

			IRegion match= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
			assertNotNull(match);
			ITextSelection selection= new TextSelection(fDocument, match.getOffset(), match.getLength());
			selectionListener.selectionChanged(new SelectionChangedEvent(fEditor.getSelectionProvider(), selection));
			assertTrue(validator.isValid(selection));

			Object lock= editorAccessor.invoke("getLockObject", new Class<?>[] { IAnnotationModel.class }, new Object[] { fAnnotationModel });
			FutureTask<Void> removal= new FutureTask<>(() -> {
				editorAccessor.invoke("removeOccurrenceAnnotations", new Object[0]);
				return null;
			});
			Thread removalThread= new Thread(removal, "Remove occurrence annotations");
			removalThread.setDaemon(true);
			try {
				synchronized (lock) {
					removalThread.start();
					// Wait for actual lock contention, not an assumed scheduling delay.
					// Do not dispatch UI events while holding the annotation-model lock.
					long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
					while (removalThread.isAlive() && removalThread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline)
						Thread.sleep(1);
					assertEquals(Thread.State.BLOCKED, removalThread.getState(), "Removal must wait for an in-flight annotation update");

					editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
					assertEquals(8, countOccurrenceAnnotations());
				}
				removal.get(10, TimeUnit.SECONDS);
			} finally {
				removalThread.join(10000);
				assertFalse(removalThread.isAlive(), "The annotation removal thread must finish before editor teardown");
			}

			assertEquals(0, countOccurrenceAnnotations());
			assertNull(editorAccessor.get("fOccurrenceAnnotations"));
			assertNull(editorAccessor.get("fMarkOccurrenceTargetRegion"), "Removal must invalidate the cache published by the in-flight update");
			assertEquals(Long.valueOf(IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP), editorAccessor.get("fMarkOccurrenceModificationStamp"));

			editorAccessor.invoke("updateOccurrenceAnnotations", parameterTypes, new Object[] { selection, ast });
			assertEquals(8, countOccurrenceAnnotations(), "A selection in the same word must restore annotations after removal");
		} finally {
			manager.addListener(fEditor, occurrencesListener);
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
	public void markMethodOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "getClass", true, true, true, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(2);
		assertOccurrencesInWidget();
	}
	@Test
	public void markFieldOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "fName", true, true, true, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(9);
		assertOccurrencesInWidget();
	}

	@Test
	public void markLocalOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "runMethod", true, true, true, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(4);
		assertOccurrencesInWidget();
	}

	@Test
	public void markMethodExitOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "void runTest() throws", true, true, false, false);
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(6);
		assertOccurrencesInWidget();
	}

	@Test
	public void markMethodExceptionOccurrences() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "NoSuchMethodException", true, true, true, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(2);
		assertOccurrencesInWidget();
	}

	@Test
	public void markImplementOccurrences1() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(3);
		assertOccurrencesInWidget();
	}

	@Test
	public void markImplementOccurrences2() throws BadLocationException {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, false);

		fMatch= fFindReplaceDocumentAdapter.find(0, "Test {", true, true, false, false);
		assertNotNull(fMatch);
		fMatch= new Region(fMatch.getOffset(), 4);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(1); // 1 type occurrence
		assertOccurrencesInWidget();
	}

	@Test
	public void markImplementOccurrences3() throws BadLocationException {
		fMatch= fFindReplaceDocumentAdapter.find(0, "Assert", true, true, false, false);
		assertNotNull(fMatch);

		fEditor.selectAndReveal(fMatch.getOffset(), fMatch.getLength());

		assertOccurrences(2);
		assertOccurrencesInWidget();
	}

	@Test
	public void noOccurrencesIfDisabled() throws BadLocationException {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, false);
		fMatch= fFindReplaceDocumentAdapter.find(0, "TestResult", true, true, true, false);
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
		fail("No StyleRange with expected highlight RGB for given position(" + position.offset + "," + position.length + ")");
	}

	/**
	 * Returns the occurrence annotation color.
	 *
	 * @return the occurrence annotation color
	 */
	@SuppressWarnings("restriction")
	private static RGB getHighlightRGB() {
		AnnotationPreference annotationPref= org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(OCCURRENCE_ANNOTATION);
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		if (store != null)
			return PreferenceConverter.getColor(store, annotationPref.getColorPreferenceKey());

		return null;
	}

	private int countOccurrenceAnnotations() {
		int occurrences= 0;
		Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= iter.next();
			if (OCCURRENCE_ANNOTATION.equals(annotation.getType()) || OCCURRENCE_WRITE_ANNOTATION.equals(annotation.getType()))
				occurrences++;
		}
		return occurrences;
	}

	private void assertOccurrences(final int expected) {
		DisplayHelper helper= new DisplayHelper() {
			@Override
			protected boolean condition() {
				synchronized (MarkOccurrenceTest.this) {
					// Even when expecting no annotations, first wait for a matching AST callback.
					if (fOccurrences == -1)
						return false;
					fOccurrences= countOccurrenceAnnotations();
					return fOccurrences == expected;
				}
			}
		};
		assertTrue(helper.waitForCondition(EditorTestHelper.getActiveDisplay(), 80000),
				() -> "Expected " + expected + " occurrence annotations, last count: " + fOccurrences);
	}
}
