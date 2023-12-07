/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.ui.PreferenceConstants;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @since 3.4
 */
public abstract class ScrollVerticalRulerTest extends ScrollEditorTest {

	public static Test suite() {
		TestSuite result= new TestSuite();

		result.addTest(ScrollVerticalRuler100Test.suite());
		result.addTest(ScrollVerticalRuler1000Test.suite());
		result.addTest(ScrollVerticalRuler5000Test.suite());

		return result;
	}

	@Override
	protected String getEditor() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		PreferenceConstants.getPreferenceStore().putValue(PreferenceConstants.SPELLING_PROBLEMS_THRESHOLD, Integer.toString(100000));

		boolean isInstalled= EditorsUI.getSpellingService().getSpellingEngineDescriptors().length > 0;
		assertTrue("No spelling engine installed", isInstalled);

		IPreferenceStore store= EditorsUI.getPreferenceStore();
		store.putValue(SpellingService.PREFERENCE_SPELLING_ENABLED, IPreferenceStore.TRUE);

		IEclipsePreferences editorsNode= InstanceScope.INSTANCE.getNode(EditorsUI.PLUGIN_ID);

		@SuppressWarnings("restriction")
		MarkerAnnotationPreferences markerAnnotationPreferences= org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
		Iterator<AnnotationPreference> iterator= markerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (iterator.hasNext()) {
			AnnotationPreference pref= iterator.next();
			String preferenceKey= pref.getVerticalRulerPreferenceKey();
			if ("spellingIndicationInVerticalRuler".equals(preferenceKey)) {
				editorsNode.putBoolean(preferenceKey, true);
				String textPreferenceKey= pref.getTextPreferenceKey();
				editorsNode.putBoolean(textPreferenceKey, false);
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {
		PreferenceConstants.getPreferenceStore().setToDefault(PreferenceConstants.SPELLING_PROBLEMS_THRESHOLD);

		IPreferenceStore store= EditorsUI.getPreferenceStore();
		store.setToDefault(SpellingService.PREFERENCE_SPELLING_ENABLED);

		IEclipsePreferences editorsNode= InstanceScope.INSTANCE.getNode(EditorsUI.PLUGIN_ID);

		@SuppressWarnings("restriction")
		MarkerAnnotationPreferences markerAnnotationPreferences= org.eclipse.ui.internal.editors.text.EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
		Iterator<AnnotationPreference> iterator= markerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (iterator.hasNext()) {
			AnnotationPreference pref= iterator.next();
			String preferenceKey= pref.getVerticalRulerPreferenceKey();
			if ("spellingIndicationInVerticalRuler".equals(preferenceKey)) {
				editorsNode.putBoolean(preferenceKey, false);
				String textPreferenceKey= pref.getTextPreferenceKey();
				editorsNode.putBoolean(textPreferenceKey, true);
			}
		}


		super.tearDown();
	}

	@Override
	protected AbstractTextEditor openEditor(ScrollingMode mode) throws Exception {
		IFile file= ResourceTestHelper.getProject(PerformanceTestSetup.PROJECT).getFile("faust.txt");
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(AbstractDocumentLineDifferTest.getFaust().getBytes(StandardCharsets.UTF_8)), true, null);
		}

		AbstractTextEditor result= (AbstractTextEditor) EditorTestHelper.openInEditor(file, true);

		IDocument document= EditorTestHelper.getDocument(result);
		document.replace(0, document.getLength(), getFaust(getNumberOfAnnotations()));

		result.doSave(null);

		return result;
	}

	private String getFaust(int numberOfLines) throws Exception {
		String faust= AbstractDocumentLineDifferTest.getFaust();

		IDocument document= new Document(faust);
		int lineOffset= document.getLineOffset(numberOfLines);
		return document.get(0, lineOffset);
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.ScrollEditorTest#assertEditor(org.eclipse.ui.texteditor.AbstractTextEditor)
	 */
	@Override
	protected void assertEditor(AbstractTextEditor editor) throws Exception {
		super.assertEditor(editor);

		IAnnotationModel model= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		int annotationCount= getAnnotationCount(model);
		assertTrue("Expected annotation count is: " + getNumberOfAnnotations() + " but was: " + annotationCount, annotationCount >= getNumberOfAnnotations());
	}

	private int getAnnotationCount(IAnnotationModel model) {
		int result= 0;

		Iterator<Annotation> iterator= model.getAnnotationIterator();
		while (iterator.hasNext()) {
			iterator.next();
			result++;
		}

		return result;
	}

	/**
	 * @return the number of annotations to show in the vertical ruler
	 */
	protected abstract int getNumberOfAnnotations();

}
