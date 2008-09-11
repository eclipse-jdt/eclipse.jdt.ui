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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.text.tests.performance.ResourceTestHelper;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenter;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;


public class AbstractSemanticHighlightingTest extends TestCase {

	protected static class SemanticHighlightingTestSetup extends TestSetup {

		private IJavaProject fJavaProject;
		private final String fTestFilename;

		public SemanticHighlightingTestSetup(Test test, String testFilename) {
			super(test);
			fTestFilename= testFilename;
		}

		protected void setUp() throws Exception {
			super.setUp();
			fJavaProject= EditorTestHelper.createJavaProject(PROJECT, LINKED_FOLDER);

			disableAllSemanticHighlightings();

			fEditor= (JavaEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(fTestFilename), true);
			fSourceViewer= EditorTestHelper.getSourceViewer(fEditor);
			assertTrue(EditorTestHelper.joinReconciler(fSourceViewer, 0, 10000, 100));
		}

		protected String getTestFilename() {
			return "/SHTest/src/SHTest.java";
		}

		protected void tearDown () throws Exception {
			EditorTestHelper.closeEditor(fEditor);
			fEditor= null;
			fSourceViewer= null;

			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();

			SemanticHighlighting[] semanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
			for (int i= 0, n= semanticHighlightings.length; i < n; i++) {
				String enabledPreferenceKey= SemanticHighlightings.getEnabledPreferenceKey(semanticHighlightings[i]);
				if (!store.isDefault(enabledPreferenceKey))
					store.setToDefault(enabledPreferenceKey);
			}

			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);

			super.tearDown();
		}
	}

	public static final String LINKED_FOLDER= "testResources/semanticHighlightingTest1";

	public static final String PROJECT= "SHTest";

	private static JavaEditor fEditor;

	private static SourceViewer fSourceViewer;

	protected void setUp() throws Exception {
		super.setUp();
		disableAllSemanticHighlightings();
	}

	protected void assertEqualPositions(Position[] expected, Position[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i= 0, n= expected.length; i < n; i++) {
			assertEquals(expected[i].isDeleted(), actual[i].isDeleted());
			assertEquals(expected[i].getOffset(), actual[i].getOffset());
			assertEquals(expected[i].getLength(), actual[i].getLength());
		}
	}

	protected Position createPosition(int line, int column, int length) throws BadLocationException {
		IDocument document= fSourceViewer.getDocument();
		return new Position(document.getLineOffset(line) + column, length);
	}

	String toString(Position[] positions) throws BadLocationException {
		StringBuffer buf= new StringBuffer();
		IDocument document= fSourceViewer.getDocument();
		buf.append("Position[] expected= new Position[] {\n");
		for (int i= 0, n= positions.length; i < n; i++) {
			Position position= positions[i];
			int line= document.getLineOfOffset(position.getOffset());
			int column= position.getOffset() - document.getLineOffset(line);
			buf.append("\tcreatePosition(" + line + ", " + column + ", " + position.getLength() + "),\n");
		}
		buf.append("};\n");
		return buf.toString();
	}

	protected Position[] getSemanticHighlightingPositions() throws BadPositionCategoryException {
		SemanticHighlightingManager manager= (SemanticHighlightingManager) new Accessor(fEditor, JavaEditor.class).get("fSemanticManager");
		SemanticHighlightingPresenter presenter= (SemanticHighlightingPresenter) new Accessor(manager, manager.getClass()).get("fPresenter");
		String positionCategory= (String) new Accessor(presenter, presenter.getClass()).invoke("getPositionCategory", new Object[0]);
		IDocument document= fSourceViewer.getDocument();
		return document.getPositions(positionCategory);
	}

	protected void setUpSemanticHighlighting(String semanticHighlighting) {
		enableSemanticHighlighting(semanticHighlighting);
		EditorTestHelper.forceReconcile(fSourceViewer);
		assertTrue(EditorTestHelper.joinReconciler(fSourceViewer, 0, 10000, 100));
		EditorTestHelper.runEventQueue(100);
	}

	private void enableSemanticHighlighting(String preferenceKey) {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(getEnabledPreferenceKey(preferenceKey), true);
	}

	private String getEnabledPreferenceKey(String preferenceKey) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + preferenceKey + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
	}

	private static void disableAllSemanticHighlightings() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		SemanticHighlighting[] semanticHilightings= SemanticHighlightings.getSemanticHighlightings();
		for (int i= 0, n= semanticHilightings.length; i < n; i++) {
			SemanticHighlighting semanticHilighting= semanticHilightings[i];
			if (store.getBoolean(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting)))
				store.setValue(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting), false);
		}
	}
}
