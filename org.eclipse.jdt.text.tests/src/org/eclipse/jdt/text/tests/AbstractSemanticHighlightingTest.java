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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.text.tests.performance.ResourceTestHelper;

import org.eclipse.core.runtime.CoreException;

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

public class AbstractSemanticHighlightingTest {

	protected static class SemanticHighlightingTestSetup extends ExternalResource {

		private IJavaProject fJavaProject;
		private final String fTestFilename;

		public SemanticHighlightingTestSetup(String testFilename) {
			fTestFilename= testFilename;
		}

		@Override
		public void before() throws Exception {
			fJavaProject= EditorTestHelper.createJavaProject(PROJECT, LINKED_FOLDER);

			disableAllSemanticHighlightings();

			fEditor= (JavaEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(fTestFilename), true);
			fSourceViewer= EditorTestHelper.getSourceViewer(fEditor);
			assertTrue(EditorTestHelper.joinReconciler(fSourceViewer, 0, 10000, 100));
		}

		protected String getTestFilename() {
			return "/SHTest/src/SHTest.java";
		}

		@Override
		public void after() {
			EditorTestHelper.closeEditor(fEditor);
			fEditor= null;
			fSourceViewer= null;

			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();

			SemanticHighlighting[] semanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
			for (SemanticHighlighting semanticHighlighting : semanticHighlightings) {
				String enabledPreferenceKey= SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting);
				if (!store.isDefault(enabledPreferenceKey))
					store.setToDefault(enabledPreferenceKey);
			}

			if (fJavaProject != null)
				try {
					JavaProjectHelper.delete(fJavaProject);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
	}

	public static final String LINKED_FOLDER= "testResources/semanticHighlightingTest1";

	public static final String PROJECT= "SHTest";

	private static JavaEditor fEditor;

	private static SourceViewer fSourceViewer;

	@Before
	public void before() throws Exception {
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
		StringBuilder buf= new StringBuilder();
		IDocument document= fSourceViewer.getDocument();
		buf.append("Position[] expected= new Position[] {\n");
		for (Position position : positions) {
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
		for (SemanticHighlighting semanticHilighting : SemanticHighlightings.getSemanticHighlightings()) {
			if (store.getBoolean(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting)))
				store.setValue(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting), false);
		}
	}
}
