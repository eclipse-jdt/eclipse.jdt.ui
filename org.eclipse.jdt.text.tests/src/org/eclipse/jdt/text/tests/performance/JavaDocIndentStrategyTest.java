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

package org.eclipse.jdt.text.tests.performance;

import java.util.Enumeration;

import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.filebuffers.tests.ResourceHelper;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaDocIndentStrategyTest extends TextPerformanceTestCase {
	
	private static class Setup extends TestSetup {

		private static final String PROJECT= "JavaDocIndentStrategyTest";
		
		private static final String LINKED_FOLDER= "/testResources/javaDocIndentStrategyTest1";
		
		private static final String FILE= "Test.java";
		
		private IJavaProject fJavaProject;
		
		public Setup(Test test) {
			super(test);
		}
		
		protected void setUp() throws Exception {
			super.setUp();
			fJavaProject= JavaProjectHelper.createJavaProject(PROJECT, "bin");
			assertNotNull("JRE is null", JavaProjectHelper.addRTJar(fJavaProject));
			
			IProject project= (IProject) fJavaProject.getUnderlyingResource();
			IFolder folder= ResourceHelper.createLinkedFolder(project, new Path("src"), JdtTextTestPlugin.getDefault(), new Path(LINKED_FOLDER));
			assertNotNull(folder);
			assertTrue(folder.exists());
			JavaProjectHelper.addSourceContainer(fJavaProject, "src");
			
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile("/" + PROJECT + "/src/" + FILE), true);
			SourceViewer sourceViewer= EditorTestHelper.getSourceViewer(editor);
			assertTrue(EditorTestHelper.joinReconciler(sourceViewer, 0, 10000, 100));
			Test test= getTest();
			setEditor(test, editor);
			
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setValue(PreferenceConstants.EDITOR_CLOSE_JAVADOCS, true);
			store.setValue(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, true);
		}

		private void setEditor(Test test, AbstractTextEditor editor) {
			if (test instanceof JavaDocIndentStrategyTest)
				((JavaDocIndentStrategyTest) test).setEditor(editor);
			else if (test instanceof TestDecorator)
				setEditor(((TestDecorator) test).getTest(), editor);
			else if (test instanceof TestSuite)
				for (Enumeration iter= ((TestSuite) test).tests(); iter.hasMoreElements();)
					setEditor((Test) iter.nextElement(), editor);
		}

		protected void tearDown () throws Exception {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setToDefault(PreferenceConstants.EDITOR_CLOSE_JAVADOCS);
			store.setToDefault(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS);

			EditorTestHelper.closeAllEditors();
			
			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);
			
			super.tearDown();
		}
	}
	
	private static final Class THIS= JavaDocIndentStrategyTest.class;

	private static final String SHORT_NAME= "JavaDoc auto completion";

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final int LINE= 1;

	private static final int COLUMN= 4;

	private AbstractTextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new Setup(new TestSuite(THIS)));
	}
	
	public void setEditor(AbstractTextEditor editor) {
		fEditor= editor;
	}

	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinBackgroundActivities();
		
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	public void testJavaDocIndentStrategy() throws Exception {
		int destOffset= EditorTestHelper.getDocument(fEditor).getLineOffset(LINE) + COLUMN;
		measureJavaDocIndentStrategy(destOffset, getNullPerformanceMeter(), getWarmUpRuns());
		PerformanceMeter performanceMeter= createPerformanceMeterForSummary(SHORT_NAME, Dimension.ELAPSED_PROCESS);
		measureJavaDocIndentStrategy(destOffset, performanceMeter, getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureJavaDocIndentStrategy(int destOffset, PerformanceMeter performanceMeter, int runs) throws Exception {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		Display display= EditorTestHelper.getActiveDisplay();
		for (int i= 0; i < runs; i++) {
			dirty(document);
			fEditor.selectAndReveal(destOffset, 0);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(fEditor), 0, 10000, 100);
			performanceMeter.start();
			SWTEventHelper.pressKeyCode(display, SWT.CR);
			performanceMeter.stop();
			EditorTestHelper.revertEditor(fEditor, true);
		}
	}

	private void dirty(IDocument document) throws BadLocationException {
		document.replace(0, 0, " ");
		document.replace(0, 1, "");
	}
}
