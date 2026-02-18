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
					private final static int a;
					private final String b;
					private double c;;

					public Cls(int a, String b, double c) {
						this.a= a;
						this.b= b;
						this.c= c;
					}

					public static int getAValue() {
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

					public int getSum() {
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

}
