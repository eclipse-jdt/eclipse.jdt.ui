/*******************************************************************************
 * Copyright (c) 2018, 2020 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.codemining;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

@SuppressWarnings("unchecked")
public class CodeMiningTriggerTest {
	private IPreferenceStore fPreferenceStore;
	private boolean wasCodeMiningEnabled;

	private IJavaProject fJavaProject;
	private IPackageFragment pack;

	public static class TestCodeMiningProvider implements ICodeMiningProvider {
		static boolean isOn = false;

		@Override
		public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
			try {
				TestCodeMining cm = new TestCodeMining(0, viewer.getDocument(), this);
				return CompletableFuture.completedFuture(Collections.singletonList(cm));
			} catch (BadLocationException e) {
				return CompletableFuture.completedFuture(Collections.emptyList());
			}
		}

		@Override
		public void dispose() {
			// nothing
		}
	}

	static class TestCodeMining extends LineHeaderCodeMining {
		static String codeMiningText = "Default Code Mining";

		public TestCodeMining(int beforeLineNumber, IDocument document, ICodeMiningProvider provider) throws BadLocationException {
			super(beforeLineNumber, document, provider);
			setLabel(codeMiningText);
		}

	}

	public static class TestCodeMiningProviderPropertyTester extends PropertyTester {
		@Override
		public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
			if ("isOn".equals(property)) {
				return TestCodeMiningProvider.isOn;
			}
			return false;
		}
	}

	@Before
	public void setUp() throws CoreException {
		fPreferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		this.wasCodeMiningEnabled = fPreferenceStore.getBoolean(PreferenceConstants.EDITOR_CODEMINING_ENABLED);
		fPreferenceStore.setValue(PreferenceConstants.EDITOR_CODEMINING_ENABLED, true);

		fJavaProject= JavaProjectHelper.createJavaProject(getClass().getName(), "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		pack= root.createPackageFragment("testA.testB", true, null);

		TestCodeMiningProvider.isOn = true;
	}

	@After
	public void tearDown() {
		TestCodeMiningProvider.isOn = false;
		this.fPreferenceStore.setValue(PreferenceConstants.EDITOR_CODEMINING_ENABLED, wasCodeMiningEnabled);
	}

	@Test
	public void testPullCodeMining() throws Exception {
		String contents= """
			public class Foo {
				int ab, ba;
				void m() {
				}
			}
			""";
		ICompilationUnit compilationUnit= pack.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		disableCodeMiningReconciler(editor);
		assertCodeMiningAnnotation(editor.getViewer(), TestCodeMining.codeMiningText, 1000);

		TestCodeMining.codeMiningText = "Some other code mining";
		((ISourceViewerExtension5)editor.getViewer()).updateCodeMinings();
		assertCodeMiningAnnotation(editor.getViewer(), TestCodeMining.codeMiningText, 1000);
	}

	/**
	 * Disables Java reconciler (after AST is parsed) but keeps the default code mining
	 * mechanics working.
	 */
	private void disableCodeMiningReconciler(JavaEditor editor) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = JavaEditor.class.getDeclaredMethod("uninstallJavaCodeMining");
		method.setAccessible(true);
		method.invoke(editor);
	}

	private void assertCodeMiningAnnotation(ISourceViewer viewer, String message, int timeout) throws Exception {
		assertTrue("Cannot find CodeMining header line annotation with text `" + message + "`",
			new DisplayHelper() {
				@SuppressWarnings("restriction")
				@Override
				protected boolean condition() {
					for (Iterator<Annotation> itr = viewer.getAnnotationModel().getAnnotationIterator(); itr.hasNext();) {
						Annotation a = itr.next();
						if (a instanceof org.eclipse.jface.internal.text.codemining.CodeMiningLineHeaderAnnotation) {
							Field f;
							try {
								f= org.eclipse.jface.internal.text.codemining.CodeMiningLineHeaderAnnotation.class.getDeclaredField("fMinings");
								f.setAccessible(true);
								List<ICodeMining> minings = (List<ICodeMining>)f.get(a);
								for (ICodeMining m : minings) {
									if (m instanceof TestCodeMining) {
										if (message.equals(m.getLabel())) {
											return true;
										}
									}
								}
							} catch (Exception e) {
								ILog.of(Platform.getBundle("org.eclipse.jdt.text.tests")).log(
									new Status(IStatus.ERROR, "org.eclipse.jdt.text.tests", e.getMessage(), e)
								);
								return false;
							}
						}
					}
					return false;
				}
		}.waitForCondition(Display.getDefault(), timeout));
	}
}
