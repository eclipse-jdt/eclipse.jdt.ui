/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;


public class ImportOrganizeTest18 extends ImportOrganizeTest {

	private static final Class THIS= ImportOrganizeTest18.class;

	private IJavaProject fJProject1;

	public ImportOrganizeTest18(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(THIS));
	}


	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}


	protected void tearDown() throws Exception {
		setOrganizeImportSettings(null, 99, 99, fJProject1);
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testTypeUseAnnotationImport1() throws Exception { // PrimitiveType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    @TypeUse int i;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    @TypeUse int i;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeUseAnnotationImport2() throws Exception { // SimpleType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse String> list1;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse String> list1;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeUseAnnotationImport3() throws Exception { // QualifiedType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse A.B> list2;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    public class B {\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse A.B> list2;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    public class B {\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeUseAnnotationImport4() throws Exception { // PackageQualifiedType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<java.io.@TypeUse FileNotFoundException> list;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<java.io.@TypeUse FileNotFoundException> list;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeUseAnnotationImport5() throws Exception { // WildcardType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse ?> list3;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    ArrayList<@TypeUse ?> list3;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeUseAnnotationImport6() throws Exception { // ArrayType
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface TypeUse {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("TypeUse.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class C{\n");
		buf.append("    int[] arr = new int @TypeUse [5];\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import pack0.TypeUse;\n");
		buf.append("\n");
		buf.append("public class C{\n");
		buf.append("    int[] arr = new int @TypeUse [5];\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testStaticMethodReferenceImports_bug424172() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.util.function.IntPredicate;\n");
		buf.append("\n");
		buf.append("class UnusedStaticImport {\n");
		buf.append("    boolean value = match(Character::isUpperCase, 'A');\n");
		buf.append("\n");
		buf.append("    public static boolean match(IntPredicate matcher, int codePoint) {\n");
		buf.append("        return matcher.test(codePoint);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack0.createCompilationUnit("UnusedStaticImport.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("StaticMethodReferenceImports_bug424172", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.util.function.IntPredicate;\n");
		buf.append("\n");
		buf.append("class UnusedStaticImport {\n");
		buf.append("    boolean value = match(Character::isUpperCase, 'A');\n");
		buf.append("\n");
		buf.append("    public static boolean match(IntPredicate matcher, int codePoint) {\n");
		buf.append("        return matcher.test(codePoint);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testMethodReferenceImports_bug424227() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("p0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p0;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("public interface FI {\n");
		buf.append("    FI foo();\n");
		buf.append("}\n");
		pack0.createCompilationUnit("FI.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package p0;\n");
		buf.append("public class X {\n");
		buf.append("    public static FI staticMethodX() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack0.createCompilationUnit("X.java", buf.toString(), false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("p1", false, null);
		buf= new StringBuffer();
		buf.append("package p1;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi = X::staticMethodX;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MethodReferenceImports_bug424227", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package p1;\n");
		buf.append("\n");
		buf.append("import p0.FI;\n");
		buf.append("import p0.X;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi = X::staticMethodX;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

}
