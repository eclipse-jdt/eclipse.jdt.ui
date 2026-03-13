/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to test Java 16 quick assists
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AssistQuickFixTest16 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java16ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}

	}

	@Test
	public void testExtractPatternInstanceof() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
				public int foo(Object o, int p) {
					if (o instanceof Integer oint &&
							oint < 3) {
						return p;
					}
					return 3;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("&&");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
				public int foo(Object o, int p) {
					boolean b = o instanceof Integer oint &&
							oint < 3;
					if (b) {
						return p;
					}
					return 3;
				}
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_extract_to_local_description);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToRecord1() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("a;");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public record Cls(int a, String b) {
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		String str2= """
				package test;

				public class Cls2 {
					public void foo() {
						Cls cls= new Cls(3, "abc");
						System.out.println(cls.getA());
						System.out.println(cls.getB());
					}
				}
				""";
		ICompilationUnit cu2= pack.createCompilationUnit("Cls2.java", str2, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public record Cls(int a, String b) {
				}
				""";

		assertEqualString(expected, preview);

		String preview2= cu2.getBuffer().getContents();

		String expected2= """
				package test;

				public class Cls2 {
					public void foo() {
						Cls cls= new Cls(3, "abc");
						System.out.println(cls.a());
						System.out.println(cls.b());
					}
				}
				""";

		assertEqualString(expected2, preview2);
	}

	@Test
	public void testConvertToRecord3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					// Inner class
					public static class Inner {
						private final int a;
						private final String b;

						public Inner(int a, String b) {
							this.a= a;
							this.b= b;
						}

						public int getA() {
							return a;
						}

						public String getB() {
							return b;
						}
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		String str2= """
				package test;

				public class Cls2 {
					public void foo() {
						Cls.Inner cls= new Cls.Inner(3, "abc");
						System.out.println(cls.getA());
						System.out.println(cls.getB());
					}
				}
				""";
		ICompilationUnit cu2= pack.createCompilationUnit("Cls2.java", str2, false, null);

		int index= str1.indexOf("Inner {");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public class Cls {
					// Inner class
					public static record Inner(int a, String b) {
					}
				}
				""";

		assertEqualString(expected, preview);

		String preview2= cu2.getBuffer().getContents();

		String expected2= """
				package test;

				public class Cls2 {
					public void foo() {
						Cls.Inner cls= new Cls.Inner(3, "abc");
						System.out.println(cls.a());
						System.out.println(cls.b());
					}
				}
				""";

		assertEqualString(expected2, preview2);
	}

	@Test
	public void testConvertToRecord4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					/**
					 * Class Inner
					 */
					private final class Inner {
						private final int a;
						private final String b;
						private double c;

						public Inner(int a, String b, double c) {
							this.a= a;
							this.b= b;
							this.c= c;
						}

						public int getA() {
							return a;
						}

						public String getB() {
							return b;
						}

						public double getC() {
							return c;
						}
					}
					public void foo() {
						Inner inner= new Inner(1, "comment", 4.3);
						System.out.println(inner.getA());
						System.out.println(inner.getB());
						System.out.println(inner.getC());
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Inner(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public class Cls {
					/**
					 * Class Inner
					 */
					private record Inner(int a, String b, double c) {
					}
					public void foo() {
						Inner inner= new Inner(1, "comment", 4.3);
						System.out.println(inner.a());
						System.out.println(inner.b());
						System.out.println(inner.c());
					}
				}
				""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testConvertToRecord5() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				/* Class Cls */
				public class Cls extends Object {
					private final int a;
					private final String b;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				/* Class Cls */
				public record Cls(int a, String b) {
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord6() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				import java.util.Objects;

				/* Class Cls */
				public class Cls extends Object {
					private final int a;
					private final String b;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}

					@Override
					public String toString() {
						return "A [a=" + a + ", b=" + b + "]";
					}

					@Override
					public int hashCode() {
						return Objects.hash(a, b);
					}

					@Override
					public boolean equals(Object obj) {
						if (this == obj)
							return true;
						if (obj == null)
							return false;
						if (getClass() != obj.getClass())
							return false;
						Cls other = (Cls) obj;
						return a == other.a && Objects.equals(b, other.b);
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				import java.util.Objects;

				/* Class Cls */
				public record Cls(int a, String b) {
					@Override
					public String toString() {
						return "A [a=" + a + ", b=" + b + "]";
					}
					@Override
					public int hashCode() {
						return Objects.hash(a, b);
					}
					@Override
					public boolean equals(Object obj) {
						if (this == obj)
							return true;
						if (obj == null)
							return false;
						if (getClass() != obj.getClass())
							return false;
						Cls other = (Cls) obj;
						return a == other.a && Objects.equals(b, other.b);
					}
				}
				""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testConvertToRecord7() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public Cls(int a) {
						this(a, "abc");
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("a;");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public record Cls(int a, String b) {
					public Cls(int a) {
						this(a, "abc");
					}
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord8() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;
					public static int c;

					static {
						c = 3;
					}

					public static int getC() {
						return c;
					}

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("a;");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				public record Cls(int a, String b) {
					static {
						c = 3;
					}
					public static int c;
					public static int getC() {
						return c;
					}
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord9() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				interface Blah {
					void printSomething();
				}

				public class Cls implements Blah {
					private final int a;
					private final String b;
					public static int c;

					static {
						c = 3;
					}

					public static int getC() {
						return c;
					}

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}

					@Override
					public void printSomething() {
						System.out.println("here");
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("a;");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected= """
				package test;

				interface Blah {
					void printSomething();
				}

				public record Cls(int a, String b) implements Blah {
					static {
						c = 3;
					}
					public static int c;
					public static int getC() {
						return c;
					}
					@Override
					public void printSomething() {
						System.out.println("here");
					}
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord10() throws Exception { // Class with Generics
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class Pair<T, U> {
					private final T first;
					private final U second;

					public Pair(T first, U second) {
						this.first = first;
						this.second = second;
					}

					public T getFirst() {
						return first;
					}

					public U getSecond() {
						return second;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("Pair.java", str1, false, null);

		int index= str1.indexOf("getFirst");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected = """
				package test;

				public record Pair<T, U>(T first, U second) {
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord11() throws Exception { // Class with Annotations
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;

				@Deprecated
				public class User {
					@NotNull
					private final String name;

					@Range(min = 0, max = 150)
					private final int age;

					public User(@NotNull String name, int age) {
						this.name = name;
						this.age = age;
					}

					@NotNull
					public String getName() {
						return name;
					}

					public int getAge() {
						return age;
					}
				}

				@Retention(RetentionPolicy.RUNTIME)
				@interface NotNull {}

				@Retention(RetentionPolicy.RUNTIME)
				@interface Range {
					int min();
					int max();
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("User.java", str1, false, null);

		int index= str1.indexOf("getAge");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected = """
				package test;

				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;

				@Deprecated
				public record User(@NotNull String name, @Range(min = 0, max = 150) int age) {
				}

				@Retention(RetentionPolicy.RUNTIME)
				@interface NotNull {}

				@Retention(RetentionPolicy.RUNTIME)
				@interface Range {
					int min();
					int max();
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord12() throws Exception { // Single Field Class
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class Identifier {
					private final String id;

					public Identifier(String id) {
						this.id = id;
					}

					public String getId() {
						return id;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("Identifier.java", str1, false, null);

		int index= str1.indexOf("getId");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected = """
				package test;

				public record Identifier(String id) {
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testConvertToRecord13() throws Exception { // Package-Private Class
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				class PackagePrivateCls {
					private final int value;

					PackagePrivateCls(int value) {
						this.value = value;
					}

					int getValue() {
						return value;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("PackagePrivateCls.java", str1, false, null);

		int index= str1.indexOf("getValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		ChangeCorrectionProposal proposal= (ChangeCorrectionProposal) proposals.get(0);
		proposal.apply();
		String preview= cu.getBuffer().getContents();

		String expected = """
				package test;

				record PackagePrivateCls(int value) {
				}
				""";

		assertEqualString(expected, preview);
	}

	@Test
	public void testNoConvertToRecord1() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;
					private double c;

					public Cls(int a, String b, double c) {
						this.a= a;
						this.b= b;
						this.c= c;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord2() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;
					private double c = 2.4;;

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord3() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;

					{
						System.out.println("abc");
					}

					public Cls(int a, String b) {
						this.a= a;
						this.b= b;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord4() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;
					private double c;;

					public Cls(int a, String b, double c) {
						this.a= a;
						this.b= b;
						this.c= c;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}

					public double getC() {
						return c;
					}

					private int getSum() {
						return a + b.length();
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord5() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					private final int a;
					private final String b;
					public double c;;

					public Cls(int a, String b, double c) {
						this.a= a;
						this.b= b;
						this.c= c;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}

					public double getC() {
						return c;
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord6() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2681
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				class K {
				}
				public class Cls extends K {
					private final int a;
					private final String b;
					private double c;;

					public Cls(int a, String b, double c) {
						this.a= a;
						this.b= b;
						this.c= c;
					}

					public int getA() {
						return a;
					}

					public String getB() {
						return b;
					}

					public double getC() {
						return c;
					}
				}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Cls(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 3);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord7() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					/**
					 * Class Inner
					 */
					private class Inner {
						private final int a;
						private final String b;
						private double c;

						public Inner(int a, String b, double c) {
							this.a= a;
							this.b= b;
							this.c= c;
						}

						public int getA() {
							return a;
						}

						public String getB() {
							return b;
						}

						public double getC() {
							return c;
						}
					}
					private class Inner2 extends Inner {
						public Inner2() {
							super(2, "blah", 5.2);
						}
					}
					public void foo() {
						Inner inner= new Inner(1, "comment", 4.3);
						System.out.println(inner.getA());
						System.out.println(inner.getB());
						System.out.println(inner.getC());
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Inner(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord8() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					/**
					 * Class Inner
					 */
					private class Inner {
						private final int a;
						private final String b;
						private double c;

						public Inner(int a, String b, double c) {
							this.a= a;
							this.b= b;
						}

						public int getA() {
							return a;
						}

						public String getB() {
							return b;
						}

						public double getC() {
							return c;
						}
					}
					public void foo() {
						Inner inner= new Inner(1, "comment", 4.3);
						System.out.println(inner.getA());
						System.out.println(inner.getB());
						System.out.println(inner.getC());
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Inner(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord9() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
				package test;

				public class Cls {
					/**
					 * Class Inner
					 */
					private class Inner {
						private final int a;
						private final String b;
						private double c;

						public Inner(int a, String b, double c) {
							this.a= a;
							this.b= b;
							this.c= c;
						}

						public Inner(int a, String b) {
							this.a= a;
							this.b= b;
							this.c= 2.0;
						}

						public int getA() {
							return a;
						}

						public String getB() {
							return b;
						}

						public double getC() {
							return c;
						}
					}
					public void foo() {
						Inner inner= new Inner(1, "comment", 4.3);
						System.out.println(inner.getA());
						System.out.println(inner.getB());
						System.out.println(inner.getC());
					}
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("Inner(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}
	@Test
	public void testNoConvertToRecord10() throws Exception { // Getter with Wrong Return Type
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
				module test {
		}
		""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class WrongTypeCls {
					private final int a;

					public WrongTypeCls(int a) {
						this.a = a;
					}

					public long getA() {
						return (long) a;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("WrongTypeCls.java", str1, false, null);

		int index= str1.indexOf("Inner(");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord11() throws Exception { // Class with Instance Initializer Blocks
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class InitializerCls {
					private final int value;
					private final String name;

					{
						System.out.println("Instance initializer");
					}

					public InitializerCls(int value, String name) {
						this.value = value;
						this.name = name;
					}

					public int getValue() {
						return value;
					}

					public String getName() {
						return name;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("InitializerCls.java", str1, false, null);

		int index= str1.indexOf("getValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord12() throws Exception { // Constructor with Additional Logic
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class CalculatedCls {
					private final int value;
					private final int doubled;

					public CalculatedCls(int value) {
						this.value = value;
						this.doubled = value * 2;
					}

					public int getValue() {
						return value;
					}

					public int getDoubled() {
						return doubled;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("CalculatedCls.java", str1, false, null);

		int index= str1.indexOf("getValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}


	@Test
	public void testNoConvertToRecord13() throws Exception { // Class with Native Methods
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class NativeCls {
					private final int value;

					public NativeCls(int value) {
						this.value = value;
					}

					public int getValue() {
						return value;
					}

					public native void nativeMethod();
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("NativeCls.java", str1, false, null);

		int index= str1.indexOf("getValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	@Test
	public void testNoConvertToRecord14() throws Exception { // Class with Finalize Method
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class FinalizeCls {
					private final int value;

					public FinalizeCls(int value) {
						this.value = value;
					}

					public int getValue() {
						return value;
					}

					@Override
					protected void finalize() throws Throwable {
						super.finalize();
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("FinalizeCls.java", str1, false, null);

		int index= str1.indexOf("getValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 8);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

	public void testNoConvertToRecord15() throws Exception { // Constructor Not Initializing All Final Fields
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

		String str1 = """
				package test;

				public class PartialInitCls {
					private final int a;
					private final int b = 10;

					public PartialInitCls(int a) {
						this.a = a;
					}

					public int getA() {
						return a;
					}

					public int getB() {
						return b;
					}
				}
				""";
		ICompilationUnit cu = pack.createCompilationUnit("PartialInitCls.java", str1, false, null);

		int index= str1.indexOf("getA");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, RefactoringCoreMessages.ConvertToRecordRefactoring_name);
	}

}
