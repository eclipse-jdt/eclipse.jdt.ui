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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class ImportOrganizeTest1d8 extends ImportOrganizeTest {
	@Rule
	public Java1d8ProjectTestSetup j18p= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void before() throws Exception {
		fJProject1= j18p.getProject();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}


	@After
	public void after() throws Exception {
		setOrganizeImportSettings(null, 99, 99, fJProject1);
		JavaProjectHelper.clear(fJProject1, j18p.getDefaultClasspath());
	}

	@Test
	public void typeUseAnnotationImport1() throws Exception { // PrimitiveType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    @TypeUse int i;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import pack0.TypeUse;

			public class C{
			    @TypeUse int i;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void typeUseAnnotationImport2() throws Exception { // SimpleType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    ArrayList<@TypeUse String> list1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import java.util.ArrayList;
			import pack0.TypeUse;

			public class C{
			    ArrayList<@TypeUse String> list1;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void typeUseAnnotationImport3() throws Exception { // QualifiedType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    ArrayList<@TypeUse A.B> list2;
			}

			class A {
			    public class B {
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import java.util.ArrayList;
			import pack0.TypeUse;

			public class C{
			    ArrayList<@TypeUse A.B> list2;
			}

			class A {
			    public class B {
			       \s
			    }
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void typeUseAnnotationImport4() throws Exception { // PackageQualifiedType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    ArrayList<java.io.@TypeUse FileNotFoundException> list;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import java.util.ArrayList;
			import pack0.TypeUse;

			public class C{
			    ArrayList<java.io.@TypeUse FileNotFoundException> list;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void typeUseAnnotationImport5() throws Exception { // WildcardType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    ArrayList<@TypeUse ?> list3;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import java.util.ArrayList;
			import pack0.TypeUse;

			public class C{
			    ArrayList<@TypeUse ?> list3;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void typeUseAnnotationImport6() throws Exception { // ArrayType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			@Target(ElementType.TYPE_USE)
			public @interface TypeUse {
			}
			""";
		pack0.createCompilationUnit("TypeUse.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			public class C{
			    int[] arr = new int @TypeUse [5];
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;

			import pack0.TypeUse;

			public class C{
			    int[] arr = new int @TypeUse [5];
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void staticMethodReferenceImports_bug424172() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;

			import java.util.function.IntPredicate;

			class UnusedStaticImport {
			    boolean value = match(Character::isUpperCase, 'A');

			    public static boolean match(IntPredicate matcher, int codePoint) {
			        return matcher.test(codePoint);
			    }
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("UnusedStaticImport.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("StaticMethodReferenceImports_bug424172", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package p;

			import java.util.function.IntPredicate;

			class UnusedStaticImport {
			    boolean value = match(Character::isUpperCase, 'A');

			    public static boolean match(IntPredicate matcher, int codePoint) {
			        return matcher.test(codePoint);
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void methodReferenceImports_bug424227() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("p0", false, null);
		String str= """
			package p0;
			@FunctionalInterface
			public interface FI {
			    FI foo();
			}
			""";
		pack0.createCompilationUnit("FI.java", str, false, null);

		String str1= """
			package p0;
			public class X {
			    public static FI staticMethodX() {
			        return null;
			    }
			}
			""";
		pack0.createCompilationUnit("X.java", str1, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("p1", false, null);
		String str2= """
			package p1;

			public class C1 {
			    FI fi = X::staticMethodX;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str2, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MethodReferenceImports_bug424227", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str3= """
			package p1;

			import p0.FI;
			import p0.X;

			public class C1 {
			    FI fi = X::staticMethodX;
			}
			""";
		assertEqualString(cu.getSource(), str3);
	}

	@Test
	public void testStaticImportFavorite_Issue2893() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2893
		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "test.Assertions.*;test1.Assertions.*");
		IJavaProject p1= null;
		IJavaProject referencing1= null;
		IJavaProject referencing2= null;
		try {
			p1= JavaProjectHelper.createJavaProject("p1", "bin");
			referencing1= JavaProjectHelper.createJavaProject("p2", "bin");
			referencing2= JavaProjectHelper.createJavaProject("p3", "bin");

			JavaProjectHelper.addRTJar(p1);
			IPackageFragmentRoot p1SourceFolder= JavaProjectHelper.addSourceContainer(p1, "src");
			IPackageFragment pack= p1SourceFolder.createPackageFragment("test", false, null);

			String str= """
					package test;

					public class Assertions {
						public static boolean assertEquals(Object obj1, Object obj2) throws Exception {
							if (Object.equals(obj1, obj2)) {
								throw new Exception();
							}
						}
					}
					""";
			pack.createCompilationUnit("Assertions.java", str, false, null);

			JavaProjectHelper.addRTJar(referencing1);
			IPackageFragmentRoot ref1SourceFolder= JavaProjectHelper.addSourceContainer(referencing1, "src");
			IPackageFragment pack1= ref1SourceFolder.createPackageFragment("test1", false, null);
			String str1= """
					package test1;

					public class Assertions {
						public static boolean assertEquals(Object obj1, Object obj2) throws Exception {
							if (Object.equals(obj1, obj2)) {
								throw new Exception();
							}
						}
					}
					""";
			pack1.createCompilationUnit("Assertions.java", str1, false, null);

			JavaProjectHelper.addRTJar(referencing2);
			JavaProjectHelper.addRequiredProject(referencing2, referencing1);

			IPackageFragmentRoot ref2SourceFolder= JavaProjectHelper.addSourceContainer(referencing2, "src");
			IPackageFragment pack2= ref2SourceFolder.createPackageFragment("test2", false, null);
			String str2= """
					package test2;

					public class E2 {
						public boolean foo2(Object obj1, Object obj2) throws Exception {
							assertEquals(obj1, obj2);
						}
					}
					""";
			ICompilationUnit cu2= pack2.createCompilationUnit("E2.java", str2, false, null);


			IAccessRule[] accessRules2= new IAccessRule[] {
					JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE)
			};
			IClasspathAttribute[] extraAttributes2= new IClasspathAttribute[] {
					JavaCore.newClasspathAttribute("myTestAttribute", "val")
			};
			IClasspathEntry cpe2= JavaCore.newProjectEntry(p1.getProject().getFullPath(), accessRules2, true, extraAttributes2, false);
			JavaProjectHelper.addToClasspath(referencing2, cpe2);

			String[] order= new String[] {};
			IChooseImportQuery query= createQuery("StaticMethodReferenceImports_bug424172", new String[] {}, new int[] {});

			OrganizeImportsOperation op= createOperation(cu2, order, 99, false, true, true, query);
			op.run(null);

			String expected= """
					package test2;

					import static test1.Assertions.assertEquals;

					public class E2 {
						public boolean foo2(Object obj1, Object obj2) throws Exception {
							assertEquals(obj1, obj2);
						}
					}
					""";
			assertEqualString(cu2.getSource(), expected);
		} finally {
			preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "");
			if (referencing1 != null && referencing1.exists())
				JavaProjectHelper.removeSourceContainer(referencing1, "src");
			if (referencing2 != null && referencing2.exists())
				JavaProjectHelper.removeSourceContainer(referencing2, "src");

			if (p1 != null && p1.exists())
				JavaProjectHelper.delete(p1);
			if (referencing1 != null && referencing1.exists())
				JavaProjectHelper.delete(referencing1);
			if (referencing2 != null && referencing2.exists())
				JavaProjectHelper.delete(referencing2);
		}
	}
}
