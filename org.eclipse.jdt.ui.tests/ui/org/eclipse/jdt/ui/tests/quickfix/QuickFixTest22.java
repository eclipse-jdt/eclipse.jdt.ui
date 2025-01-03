/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - based on QuickFixTest17
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.tests.core.rules.Java22ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class QuickFixTest22 extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectsetup= new Java22ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		JavaRuntime.getDefaultVMInstall();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();

		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
	}

	@Test
	public void testRenameToUnnamedProposal1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class Unused {

				private interface J {
					public void run(String a, String b);
				}
			    public static void main(String[] args) {
			    	J j = (a, b) -> System.out.println(a);
					j.run("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Unused.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;

			public class Unused {

				private interface J {
					public void run(String a, String b);
				}
			    public static void main(String[] args) {
			    	J j = (a, _) -> System.out.println(a);
					j.run("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testRenameToUnnamedProposal2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class Unused {

			    public static void main(String[] args) {
					record R(int i, long l) {
					}

					R r = new R(1, 1);
					switch (r) {
					case R(_, long l) -> {}
					case R r2 -> {}
					}
				}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Unused.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal1);

		String expected1= """
			package test;

			public class Unused {

			    public static void main(String[] args) {
					record R(int i, long l) {
					}

					R r = new R(1, 1);
					switch (r) {
					case R(_, long _) -> {}
					case R r2 -> {}
					}
				}
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testRenameToUnnamedProposal3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class Unused {

			    public static void main(String[] args) {
					for (String arg : args) {
						System.out.println("abc");
					}
				}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Unused.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal1);

		String expected1= """
			package test;

			public class Unused {

			    public static void main(String[] args) {
					for (String _ : args) {
						System.out.println("abc");
					}
				}
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testRenameToUnnamedProposal4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;
			import java.io.FileInputStream;

			public class Unused {

			    public static void main(String[] args) {
					try (FileInputStream x = new FileInputStream("a.b")) {
						System.out.println("abc");
					} catch (Exception e) {
					}
				}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Unused.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal1);

		String expected1= """
			package test;
			import java.io.FileInputStream;

			public class Unused {

			    public static void main(String[] args) {
					try (FileInputStream _ = new FileInputStream("a.b")) {
						System.out.println("abc");
					} catch (Exception e) {
					}
				}
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testRenameToUnnamedProposal5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class Unused {

				private static int x = 1;

				private static int sideEffect() {
					return ++x;
				}

			    public static void main(String[] args) {
					for (int i = 0, se = sideEffect(); i < 9; ++i) {
						System.out.println("abc");
					}
				}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Unused.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal1);

		String expected1= """
			package test;

			public class Unused {

				private static int x = 1;

				private static int sideEffect() {
					return ++x;
				}

			    public static void main(String[] args) {
					for (int i = 0, _ = sideEffect(); i < 9; ++i) {
						System.out.println("abc");
					}
				}
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testAddPermittedTypesToSwitchStatement() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class TestSwitch {

				sealed interface Foo<T> {
					record FooImpl_a(String x) implements Foo<String> {
					}

					record FooImpl_b(String y, String z) implements Foo<String> {
					}

					final class FooImpl_c implements Foo {}
				}

				public static void main(String[] args) {
					Foo<String> foo = getFoo();
					switch (foo) {
					}
				}

				private static Foo<String> getFoo() {
					return new Foo.FooImpl_b("a", "b");
				}
			}
			""";

			ICompilationUnit cu= pack1.createCompilationUnit("TestSwitch.java", test, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal1);

			String expected1= """
			package test;

			public class TestSwitch {

				sealed interface Foo<T> {
					record FooImpl_a(String x) implements Foo<String> {
					}

					record FooImpl_b(String y, String z) implements Foo<String> {
					}

					final class FooImpl_c implements Foo {}
				}

				public static void main(String[] args) {
					Foo<String> foo = getFoo();
					switch (foo) {
						case Foo.FooImpl_a(String x) -> {}
						case Foo.FooImpl_b(String y, String z) -> {}
						case Foo.FooImpl_c f -> {}
						case null -> {}
						default -> {}
					}
				}

				private static Foo<String> getFoo() {
					return new Foo.FooImpl_b("a", "b");
				}
			}
			""";

			assertEqualString(preview1, expected1);
	}

	@Test
	public void testAddPermittedTypesToSwitchStatement2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String shape= """
			package test;
			public sealed class Shape permits Circle, Square {
			}
			""";
		pack1.createCompilationUnit("Shape.java", shape, false, null);

		String circle= """
				package test;
				public final class Circle extends Shape {
				}
				""";
		pack1.createCompilationUnit("Circle.java", circle, false, null);

		String square= """
				package test;
				public final class Square extends Shape {
				}
				""";
		pack1.createCompilationUnit("Square.java", square, false, null);

		String test= """
			package test;

			public class TestSwitch {

				public static void main(String[] args) {
					Shape shape = getShape();
					switch (shape) {
					}
				}

				private static Shape getShape() {
					return new Circle();
				}
			}
			""";

			ICompilationUnit cu= pack1.createCompilationUnit("TestSwitch.java", test, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal1);

			String expected1= """
			package test;

			public class TestSwitch {

				public static void main(String[] args) {
					Shape shape = getShape();
					switch (shape) {
						case Circle c -> {}
						case Square s -> {}
						case null -> {}
						default -> {}
					}
				}

				private static Shape getShape() {
					return new Circle();
				}
			}
			""";

			assertEqualString(preview1, expected1);
	}

	@Test
	public void testAddPermittedTypesToSwitchExpression() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= """
			package test;

			public class TestSwitch {

				sealed interface Foo<T> {
					record FooImpl_a(String x) implements Foo<String> {
					}

					record FooImpl_b(String y, String z) implements Foo<String> {
					}

					final class FooImpl_c implements Foo {}
				}

				public static void main(String[] args) {
					Foo<String> foo = getFoo();
					int i = switch (foo) {
					};
				}

				private static Foo<String> getFoo() {
					return new Foo.FooImpl_b("a", "b");
				}
			}
			""";

			ICompilationUnit cu= pack1.createCompilationUnit("TestSwitch.java", test, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 1);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal1);

			String expected1= """
			package test;

			public class TestSwitch {

				sealed interface Foo<T> {
					record FooImpl_a(String x) implements Foo<String> {
					}

					record FooImpl_b(String y, String z) implements Foo<String> {
					}

					final class FooImpl_c implements Foo {}
				}

				public static void main(String[] args) {
					Foo<String> foo = getFoo();
					int i = switch (foo) {
						case Foo.FooImpl_a(String x) -> 0;
						case Foo.FooImpl_b(String y, String z) -> 0;
						case Foo.FooImpl_c f -> 0;
						case null -> 0;
						default -> 0;
					};
				}

				private static Foo<String> getFoo() {
					return new Foo.FooImpl_b("a", "b");
				}
			}
			""";

			assertEqualString(preview1, expected1);
	}

}
