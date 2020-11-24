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

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
public class OverrideIndicatorTest {
	@Rule
	public JUnitProjectTestSetup jpts= new JUnitProjectTestSetup();

	private static final String OVERRIDE_INDICATOR_ANNOTATION= "org.eclipse.jdt.ui.overrideIndicator";

	private JavaEditor fEditor;
	private IDocument fDocument;
	private IAnnotationModel fAnnotationModel;
	private StyledText fTextWidget;
	private Annotation[] fOverrideAnnotations;

	@Before
	public void setUp() throws Exception {
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/TestCase.java"));
		assertNotNull(fEditor);
		fTextWidget= fEditor.getViewer().getTextWidget();
		assertNotNull(fTextWidget);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		fAnnotationModel= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
	}

	@After
	public void tearDown() throws Exception {
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

	@Test
	public void countOverrideIndicators() {
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

	@Test
	public void overrideIndicatorState() {
		countOverrideIndicators();
		int count= 0;
		for (Annotation overrideAnnotation : fOverrideAnnotations) {
			Accessor indicator= new Accessor(overrideAnnotation, "org.eclipse.jdt.internal.ui.javaeditor.OverrideIndicatorManager$OverrideIndicator", getClass().getClassLoader());
			if (indicator.getBoolean("fIsOverwriteIndicator"))
				count++;
		}
		assertEquals(2, count);
	}

	@Test
	public void overrideIndicatorText() {
		countOverrideIndicators();
		for (Annotation overrideAnnotation : fOverrideAnnotations) {
			String text= overrideAnnotation.getText();
			assertNotNull(text);
			assertTrue("overrides java.lang.Object.toString".equals(text)
						|| "implements junit.framework.Test.run".equals(text)
						|| "implements junit.framework.Test.countTestCases".equals(text));
		}
	}

	private void computeOverrideIndicators() {
		ArrayList<Annotation> annotations= new ArrayList<>();
		Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= iter.next();
			if (OVERRIDE_INDICATOR_ANNOTATION.equals(annotation.getType()))
				annotations.add(annotation);
		}
		fOverrideAnnotations= annotations.toArray(new Annotation[annotations.size()]);
	}
}
