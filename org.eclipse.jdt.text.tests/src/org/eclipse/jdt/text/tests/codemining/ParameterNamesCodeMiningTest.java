/*******************************************************************************
 * Copyright (c) 2019, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.codemining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.custom.StyledText;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.text.tests.util.DisplayHelper;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaCodeMiningReconciler;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.javaeditor.codemining.JavaMethodParameterCodeMiningProvider;

public class ParameterNamesCodeMiningTest {
	private IJavaProject fProject;
	private IPackageFragment fPackage;
	private JavaMethodParameterCodeMiningProvider fParameterNameCodeMiningProvider;
	private boolean wasCodeMiningEnabled;

	@Before
	public void setUp() throws Exception {
		if(!welcomeClosed) {
			closeIntro(PlatformUI.getWorkbench());
		}
		fProject= JavaProjectHelper.createJavaProject(getClass().getName(), "bin");
		JavaProjectHelper.addRTJar_17(fProject, true);

		Map<String, String> options= fProject.getOptions(false);
		JavaProjectHelper.set17_CompilerOptions(options);
		fProject.setOptions(options);

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		fPackage= root.getPackageFragment("");
		fParameterNameCodeMiningProvider= new JavaMethodParameterCodeMiningProvider();
		wasCodeMiningEnabled= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CODEMINING_ENABLED);
		PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.EDITOR_CODEMINING_ENABLED, true);
	}

	@After
	public void tearDown() throws Exception {
		PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.EDITOR_CODEMINING_ENABLED, wasCodeMiningEnabled);
		IWorkbenchPage workbenchPage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		for (IEditorReference ref : workbenchPage.getEditorReferences()) {
			workbenchPage.closeEditor(ref.getEditor(false), false);
		}
	}

	private void waitReconciled(JavaSourceViewer viewer) {
		assertTrue("Editor not reconciled", new DisplayHelper() {
			@Override
			protected boolean condition() {
				return JavaCodeMiningReconciler.isReconciled(viewer);
			}
		}.waitForCondition(viewer.getTextWidget().getDisplay(), 2000));
	}

	@Test
	public void testParameterNamesOK() throws Exception {
		String contents= """
			public class Foo {
				int n= System.out.append("abc", 0, 3);
			}
			""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);

		assertEquals(3, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testVarargs() throws Exception {
		String contents= """
			public class Foo {
				String s= String.format("%d %d", 1, 2);
			}
			""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);
		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testMultiLines() throws Exception {
		String contents =
				"""
			class Foo {
			   public int max(long a, long b) {
			      return a - b;
			   }
			   public void foo() {
				  int n= max(System.currentTimeMillis(
								), 0);
			   }
			}""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		CompilationUnitEditor editor= (CompilationUnitEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		viewer.setCodeMiningProviders(new ICodeMiningProvider[] {
				fParameterNameCodeMiningProvider
		});
		waitReconciled(viewer);
		StyledText widget= viewer.getTextWidget();
		//
		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
		//
		int charWidth= widget.getTextBounds(0, 1).width;
		LongSupplier drawnCodeMiningsCount= () -> Arrays.stream(widget.getStyleRanges()).filter(style ->
			style.metrics != null && style.metrics.width > charWidth).count();
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return drawnCodeMiningsCount.getAsLong() == 2;
			}
		}.waitForCondition(widget.getDisplay(), 500);
		assertEquals(2, drawnCodeMiningsCount.getAsLong());
	}

	@Test
	public void testUnresolvedMethodBinding() throws Exception {
		String contents= """
			public class Foo {
				public void mehod() {
					List<String> list= Arrays.asList("foo", "bar");
					System.out.printf("%s %s", list.get(0), list.get(1));
				}
			}""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);
		// Only code mining on "printf" parameters
		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testCollapsedFolding() throws Exception {
		String contents= """
			/**
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 * aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
			 */\
			public class Foo {
				int n= Math.max(1, 2);
			}""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		viewer.doOperation(ProjectionViewer.COLLAPSE_ALL);
		viewer.setCodeMiningProviders(new ICodeMiningProvider[] {
			fParameterNameCodeMiningProvider
		});
		waitReconciled(viewer);

		AtomicReference<IStatus> errorInLog= new AtomicReference<>();
		ILogListener logListener= (status, plugin) -> {
			if (status.getSeverity() == IStatus.ERROR) {
				errorInLog.set(status);
			}
		};
		try {
			Platform.addLogListener(logListener);
			DisplayHelper.sleep(editor.getViewer().getTextWidget().getDisplay(), 1000);
			assertNull(errorInLog.get());
		} finally {
			Platform.removeLogListener(logListener);
		}
	}

	@Test
	public void testCollapsedFoldingAndToggleHighlight() throws Exception {
		String contents= """
			/**
			 *
			 */\
			public class Foo {
				int n= Integer.divideUnsigned(1, 2);
			}""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		viewer.setCodeMiningProviders(new ICodeMiningProvider[] {
			fParameterNameCodeMiningProvider
		});
		waitReconciled(viewer);
		//
		StyledText widget= viewer.getTextWidget();
		int charWidth= widget.getTextBounds(0, 1).width;
		assertTrue("Code mining not available on expected chars", new DisplayHelper() {
			@Override
			protected boolean condition() {
				return Arrays.stream(widget.getStyleRanges(widget.getText().indexOf(", 2"), 3)).anyMatch(style ->
						style.metrics != null && style.metrics.width > charWidth);
			}
		}.waitForCondition(widget.getDisplay(), 2000));
		//
		viewer.doOperation(ProjectionViewer.COLLAPSE_ALL);
		assertTrue("Code mining not available on expected chars after collapsing", new DisplayHelper() {
			@Override
			protected boolean condition() {
				return Arrays.stream(widget.getStyleRanges(widget.getText().indexOf(", 2"), 3)).anyMatch(style ->
						style.metrics != null && style.metrics.width > charWidth);
			}
		}.waitForCondition(widget.getDisplay(), 2000));
		//
		viewer.setSelectedRange(viewer.getDocument().get().indexOf("divideUnsigned") + 1, 0);
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		boolean initial= preferenceStore.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
		try {
			preferenceStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
			assertTrue("Occurence annotation not added", new org.eclipse.jdt.text.tests.performance.DisplayHelper() {
				@Override
				protected boolean condition() {
					AtomicInteger annotationCount= new AtomicInteger();
					viewer.getAnnotationModel().getAnnotationIterator().forEachRemaining(annotation -> {
						if (annotation.getType().contains("occurrence")) {
							annotationCount.incrementAndGet();
						}
					});
					return annotationCount.get() != 0;
				}
			}.waitForCondition(widget.getDisplay(), 2000));
			assertTrue("Code mining space at undesired offset after collapsing", new DisplayHelper() {
				@Override
				protected boolean condition() {
					return Arrays.stream(widget.getStyleRanges())
							.filter(range -> range.metrics != null && range.metrics.width > charWidth)
							.allMatch(style -> {
								char c= widget.getText().charAt(style.start + 1);
								return c == '1' || c == '2';
							});
				}
			}.waitForCondition(widget.getDisplay(), 2000));
		} finally {
			preferenceStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, initial);
		}
	}

	@Test
	public void testBug547232() throws Exception {
		String contents= "public class Test {\n" +
				"    public final Object object;\n" +
				"    public final String string;\n" +
				"\n" +
				"    Test(Object object, String string) {\n" +
				"        this.object = object;\n" +
				"        this.string = string;\n" +
				"    }\n" +
				"\n" +
				"    void f() {\n" +
				"        new Test(null, \"test\");\n" +
				"    }\n" +
				"}\n" +
				"";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Test.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);

		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testBug549023() throws Exception {
		String contents= "class Base {\n" +
				"    public final Object object;\n" +
				"    public final String string;\n" +
				"\n" +
				"    Base(Object object, String string) {\n" +
				"        this.object = object;\n" +
				"        this.string = string;\n" +
				"    }\n" +
				"}\n" +
				"\n" +
				"public class Test extends Base {\n" +
				"    Test() {\n" +
				"        super(null, \"\");\n" +
				"    }\n" +
				"}\n" +
				"";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Test.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);

		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testBug549126() throws Exception {
		String contents= "public enum TestEnum {\n" +
				"    A(\"bla\", null);\n" +
				"\n" +
				"    public final String string;\n" +
				"    public final Object object;\n" +
				"\n" +
				"    TestEnum(String string, Object object) {\n" +
				"        this.string = string;\n" +
				"        this.object = object;\n" +
				"    }\n" +
				"}\n" +
				"";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("TestEnum.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);

		assertEquals(2, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testRecordConstructorOK() throws Exception {
		String contents= "import java.util.Map;\n"
				+ "public record Edge(int fromNodeId,\n"
				+ "        int toNodeId,\n"
				+ "        Map.Entry<Integer, Integer> fromPoint,\n"
				+ "        Map.Entry<Integer, Integer> toPoint,\n"
				+ "        double length,\n"
				+ "        String profile) {\n"
				+ "}\n"
				+ "";
		fPackage.createCompilationUnit("Edge.java", contents, true, new NullProgressMonitor());

		contents = """
			import java.util.Map;
			public class Test {
			    public void test () {
			        Edge e = new Edge(
			        0,
			        1,
			        Map.entry(0, 0),
			        Map.entry(1, 1),
			        1,
			        "dev");
			    }
			}""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Test.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);

		// 6 parameter names from Edge
		// 4 parameter names from the 2 Map.entry(int, int) calls
		assertEquals(10, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	@Test
	public void testIssue457() throws Exception {
		String contents=
				"""
			import java.util.List;
			public class Foo {
			  public void messageError(String message, int error) {}
			  public void beginEnd(String beginObject, String endObject) {}
			  public void startEnd(String startObject, String endObject) {}
			  public void firstSecond(String firstMsg, String secondMsg) {}
			  public void firstLast(String firstMsg, String lastMsg) {}
			  public void keyValue(int key, int value) {}
			  public void minMax(int minValue, int maxValue) {}
			  public void setProperty(int key, int value) {}
			  public void fromTo(int fromValue, int toValue) {}
			  public void printf(String format, Object arguments) {}
			  public void foo() {
			    int x = Math.max(1, 2);
			    int y = Double.compare(1.8, 2.5);
			    List<String> list= List.of("abc", "def", "ghi");
			    messageError("errormsg", 3);
				 beginEnd("abc", "def");
				 startEnd("abc", "def");
			    firstSecond("abc", "def");
			    firstLast("abc", "def");
			    keyValue(2, 4);
			    minMax(5, 6);
			    setProperty(1, 3);
			    fromTo(1, 2);
			    printf("%d", 5);
			  }
			}
			""";
		ICompilationUnit compilationUnit= fPackage.createCompilationUnit("Foo.java", contents, true, new NullProgressMonitor());
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		fParameterNameCodeMiningProvider.setContext(editor);
		JavaSourceViewer viewer= (JavaSourceViewer)editor.getViewer();
		waitReconciled(viewer);
		assertEquals(0, fParameterNameCodeMiningProvider.provideCodeMinings(viewer, new NullProgressMonitor()).get().size());
	}

	private static boolean welcomeClosed;
	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window= wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im= wb.getIntroManager();
			IIntroPart intro= im.getIntro();
			if (intro != null) {
				welcomeClosed= im.closeIntro(intro);
			}
		}
	}
}
