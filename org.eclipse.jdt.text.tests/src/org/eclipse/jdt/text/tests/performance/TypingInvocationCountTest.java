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


import java.lang.reflect.Method;

import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

import org.eclipse.jface.text.source.AnnotationPainter;

import org.eclipse.ui.internal.editors.text.EditorsPlugin;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.actions.FindAction;
import org.eclipse.jdt.ui.actions.FindReferencesAction;

/**
 * Counts number of repaints ({@link AnnotationPainter#paintControl(PaintEvent)})
 * when typing on a line with annotations shown as squiggles.
 *
 * @since 3.1
 */
public abstract class TypingInvocationCountTest extends TextPerformanceTestCase implements ITextEditorTestCase {

	protected abstract static class Setup extends TextEditorTestSetup {

		private static final String FILE= "Test.java";

		private static final String SEARCH_ANNOTATION_TYPE= "org.eclipse.search.results";

		private static final String SEARCH_VIEW= "org.eclipse.search.ui.views.SearchView";

		private IJavaProject fJavaProject;

		private boolean fWasSearchViewShown;

		private String fShownPerspective;

		public Setup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
			AnnotationPreference preference= EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(SEARCH_ANNOTATION_TYPE);
			IPreferenceStore store= EditorsUI.getPreferenceStore();
			store.setValue(preference.getHighlightPreferenceKey(), false);
			store.setValue(preference.getOverviewRulerPreferenceKey(), true);
			store.setValue(preference.getTextPreferenceKey(), true);
			if (preference.getTextStylePreferenceKey() != null)
				store.setValue(preference.getTextStylePreferenceKey(), AnnotationPreference.STYLE_SQUIGGLES);
			store.setValue(preference.getVerticalRulerPreferenceKey(), true);
			PreferenceConverter.setValue(store, preference.getColorPreferenceKey(), new RGB(255, 0, 0));

			fShownPerspective= EditorTestHelper.showPerspective(getPerspectiveId());

			if (!ResourceTestHelper.projectExists(PROJECT))
				fJavaProject= EditorTestHelper.createJavaProject(PROJECT, LINKED_FOLDER);
			else
				fJavaProject= JavaCore.create(ResourceTestHelper.getProject(PROJECT));

			fWasSearchViewShown= EditorTestHelper.isViewShown(SEARCH_VIEW);
			ITextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(getFile()), true);
			FindAction action= new FindReferencesAction(editor.getSite());
			ICompilationUnit unit= JavaCore.createCompilationUnitFrom(ResourceTestHelper.findFile(getFile()));
			IMethod method= unit.getType("Test").getMethod("test", new String[] { });
			action.run(method);
			EditorTestHelper.closeAllEditors();

			super.setUp();
			EditorTestHelper.runEventQueue(1000);
		}

		protected abstract String getPerspectiveId();

		protected String getFile() {
			return "/" + PROJECT + "/src/" + FILE;
		}

		protected void tearDown () throws Exception {
			super.tearDown();
			if (!fWasSearchViewShown)
				EditorTestHelper.showView(SEARCH_VIEW, false);
			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);
			if (fShownPerspective != null)
				EditorTestHelper.showPerspective(fShownPerspective);

			AnnotationPreference preference= EditorsPlugin.getDefault().getAnnotationPreferenceLookup().getAnnotationPreference(SEARCH_ANNOTATION_TYPE);
			IPreferenceStore store= EditorsUI.getPreferenceStore();
			store.setToDefault(preference.getHighlightPreferenceKey());
			store.setToDefault(preference.getOverviewRulerPreferenceKey());
			store.setToDefault(preference.getTextPreferenceKey());
			if (preference.getTextStylePreferenceKey() != null)
				store.setToDefault(preference.getTextStylePreferenceKey());
			store.setToDefault(preference.getVerticalRulerPreferenceKey());
			store.setToDefault(preference.getColorPreferenceKey());
		}
	}

	public static final String PROJECT= "TypingInvocationCountTest";

	public static final String LINKED_FOLDER= "/testResources/typingInvocationCountTest1";

	private AbstractTextEditor fEditor;

	public TypingInvocationCountTest() {
		super();
	}

	public TypingInvocationCountTest(String name) {
		super(name);
	}

	public void setEditor(AbstractTextEditor editor) {
		fEditor= editor;
	}

	/**
	 * Counts number of repaints when typing before one annotation on the
	 * same line.
	 *
	 * @throws Exception
	 */
	public void test00() throws Exception {
		measure(4, 22, ' ');
	}

	/**
	 * Counts number of repaints when typing after one annotation on the
	 * same line while a matching bracket is shown.
	 *
	 * @throws Exception
	 */
	public void test01() throws Exception {
		measure(4, 23, SWT.BS, true);
	}

	/**
	 * Counts number of repaints when typing after one annotation on the
	 * same line.
	 *
	 * @throws Exception
	 */
	public void test02() throws Exception {
		measure(4, 32, ' ');
	}

	/**
	 * Counts number of repaints when typing after one annotation on the
	 * same line while a matching bracket is shown.
	 *
	 * @throws Exception
	 */
	public void test03() throws Exception {
		measure(4, 33, SWT.BS, true);
	}

	/**
	 * Counts number of repaints when typing before two annotations on the
	 * same line.
	 *
	 * @throws Exception
	 */
	public void test10() throws Exception {
		measure(6, 22, ' ');
	}

	/**
	 * Counts number of repaints when typing after two annotations on the
	 * same line while a matching bracket is shown.
	 *
	 * @throws Exception
	 */
	public void test11() throws Exception {
		measure(6, 23, SWT.BS, true);
	}

	/**
	 * Counts number of repaints when typing after two annotations on the
	 * same line.
	 *
	 * @throws Exception
	 */
	public void test12() throws Exception {
		measure(6, 40, ' ');
	}

	/**
	 * Counts number of repaints when typing after two annotations on the
	 * same line while a matching bracket is shown.
	 *
	 * @throws Exception
	 */
	public void test13() throws Exception {
		measure(6, 41, SWT.BS, true);
	}

	private void measure(int line, int column, char ch) throws Exception {
		measure(line, column, ch, false);
	}

	private void measure(int line, int column, char ch,  boolean sendKeyCode) throws Exception {
		InvocationCountPerformanceMeter performanceMeter= createInvocationCountPerformanceMeter(new Method[] {
				AnnotationPainter.class.getMethod("paintControl", new Class[] { PaintEvent.class }),
		});
		performanceMeter.setTimeout(30000);
		int offset= EditorTestHelper.getDocument(fEditor).getLineOffset(line) + column;
		fEditor.selectAndReveal(offset, 0);
		Display display= EditorTestHelper.getActiveDisplay();
		EditorTestHelper.runEventQueue(200);
		performanceMeter.start();
		if (sendKeyCode)
			SWTEventHelper.pressKeyCode(display, ch);
		else
			SWTEventHelper.pressKeyChar(display, ch);
		EditorTestHelper.joinBackgroundActivities(fEditor);
		EditorTestHelper.runEventQueue(200);
		performanceMeter.stop();
		performanceMeter.commit();
	}
}
