/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.swt.custom.StyledText;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;


/**
 * Tests the Java Editor's override indicator feature.
 *
 * @since 3.1
 */
public class OverrideIndicatorTest extends TestCase {

	private static final String OVERRIDE_INDICATOR_ANNOTATION= "org.eclipse.jdt.ui.overrideIndicator";

	private JavaEditor fEditor;
	private IDocument fDocument;
	private IAnnotationModel fAnnotationModel;
	private StyledText fTextWidget;
	private Annotation[] fOverrideAnnotations;


	public static Test setUpTest(Test someTest) {
		return new JUnitProjectTestSetup(someTest);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(OverrideIndicatorTest.class));
	}


	protected void setUp() throws Exception {
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/TestCase.java"));
		assertNotNull(fEditor);
		fTextWidget= fEditor.getViewer().getTextWidget();
		assertNotNull(fTextWidget);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		fAnnotationModel= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 * @since 3.1
	 */
	protected void tearDown() throws Exception {
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

	public void testCountOverrideIndicators() {
		int count= 0;
		long timeOut= System.currentTimeMillis() + 60000;
		while (true) {
			EditorTestHelper.runEventQueue(fEditor);
			computeOverrideIndicators();
			count= fOverrideAnnotations.length;
			if (count > 0)
				break;

			synchronized (this) {
				try {
					wait(200);
				} catch (InterruptedException e1) {
				}
			}
			assertTrue(System.currentTimeMillis() < timeOut);
		}
		assertEquals(3, count);
	}

	public void testOverrideIndicatorState() {
		testCountOverrideIndicators();
		int count= 0;
		for (int i= 0; i < fOverrideAnnotations.length; i++) {
			Accessor indicator= new Accessor(fOverrideAnnotations[i], "org.eclipse.jdt.internal.ui.javaeditor.OverrideIndicatorManager$OverrideIndicator", getClass().getClassLoader());
			if (indicator.getBoolean("fIsOverwriteIndicator"))
				count++;
		}
		assertEquals(2, count);
	}

	public void testOverrideIndicatorText() {
		testCountOverrideIndicators();
		for (int i= 0; i < fOverrideAnnotations.length; i++) {
			String text= fOverrideAnnotations[i].getText();
			assertTrue(text != null
					&& (text.equals("overrides java.lang.Object.toString")
							|| text.equals("implements junit.framework.Test.run")
							|| text.equals("implements junit.framework.Test.countTestCases")
					));
		}
	}

	private void computeOverrideIndicators() {
		ArrayList annotations= new ArrayList();
		Iterator iter= fAnnotationModel.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation)iter.next();
			if (OVERRIDE_INDICATOR_ANNOTATION.equals(annotation.getType()))
				annotations.add(annotation);
		}
		fOverrideAnnotations= (Annotation[])annotations.toArray(new Annotation[annotations.size()]);
	}
}
