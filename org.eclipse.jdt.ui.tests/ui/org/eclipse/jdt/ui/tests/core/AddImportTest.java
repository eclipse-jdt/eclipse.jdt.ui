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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

public class AddImportTest extends CoreTests {

	private static final Class THIS= AddImportTest.class;

	private IJavaProject fJProject1;

	public AddImportTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			return setUpTest(new AddImportTest("testAddImportContextSensitive06"));
		}
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}


	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		JavaProjectHelper.set15CompilerOptions(fJProject1);

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testAddImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import pack.List;\n");
		buf.append("import pack.List2;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("java.net.Socket");
		imports.addImport("p.A");
		imports.addImport("com.something.Foo");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.net.Socket;\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import com.something.Foo;\n");
		buf.append("\n");
		buf.append("import p.A;\n");
		buf.append("\n");
		buf.append("import pack.List;\n");
		buf.append("import pack.List2;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java", "java.util", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("java.x.Socket");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.x.Socket;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}


	public void testAddImports3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set; // comment\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java", "java.util", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.util.Vector");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set; // comment\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testRemoveImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import pack.List;\n");
		buf.append("import pack.List2;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.removeImport("java.util.Set");
		imports.removeImport("pack.List");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import pack.List2;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testRemoveImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.Vector; // comment\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.removeImport("java.util.Vector");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Set;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}


	public void testAddImports_bug23078() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import p.A.*;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("p.Inner");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import p.Inner;\n");
		buf.append("import p.A.*;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImports_bug25113() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.awt.Panel;\n");
		buf.append("\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java.awt", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.applet.Applet");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.awt.Panel;\n");
		buf.append("\n");
		buf.append("import java.applet.Applet;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImports_bug42637() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.io.Exception");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.io.Exception;\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddStaticImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "#", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addStaticImport("java.lang.Math", "min", true);
		imports.addImport("java.lang.Math");
		imports.addStaticImport("java.lang.Math", "max", true);

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import static java.lang.Math.max;\n");
		buf.append("import static java.lang.Math.min;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddStaticImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "#", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addStaticImport("xx.MyConstants", "SIZE", true);
		imports.addStaticImport("xy.MyConstants", "*", true);
		imports.addImport("xy.MyConstants");

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import static xx.MyConstants.SIZE;\n");
		buf.append("import static xy.MyConstants.*;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("import xy.MyConstants;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testImportStructureWithSignatures() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.*;\n");
		buf.append("import java.net.*;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        IOException s;\n");
		buf.append("        URL[][] t;\n");
		buf.append("        List<SocketAddress> x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", content, false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		String content2= buf.toString();
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", content2, false, null);



		String[] order= new String[] { "java.util", "java.io", "java.net" };
		int threshold= 99;
		AST ast= AST.newAST(AST.JLS3);
		ImportRewrite importsRewrite= newImportsRewrite(cu2, order, threshold, true);
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("IOException"), "IOException".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).toSignature();

			importsRewrite.addImportFromSignature(signature, ast);
		}
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("URL"), "URL".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).toSignature();

			importsRewrite.addImportFromSignature(signature, ast);
		}
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("List"), "List".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).toSignature();

			importsRewrite.addImportFromSignature(signature, ast);
		}
		apply(importsRewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("import java.net.SocketAddress;\n");
		buf.append("import java.net.URL;\n");
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("}\n");

		assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());

	}

	public void testAddTypesWithCaptures1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		String str= "getClass()";
		MethodInvocation inv= (MethodInvocation) NodeFinder.perform(astRoot, buf.indexOf(str), str.length());
		ITypeBinding binding= inv.resolveTypeBinding();

		ImportRewrite rewrite= newImportsRewrite(astRoot, new String[0], 99, 99, true);

		String string= rewrite.addImport(binding);
		assertEquals("Class<? extends E>", string);

		Type resNode= rewrite.addImport(binding, astRoot.getAST());
		assertEquals("Class<? extends E>", ASTNodes.asString(resNode));

		String signature= new BindingKey(binding.getKey()).toSignature();

		Type resNode2= rewrite.addImportFromSignature(signature, astRoot.getAST());
		assertEquals("Class<? extends E>", ASTNodes.asString(resNode2));
	}

	private static final boolean BUG_120082= true;

	public void testAddTypesWithCaptures2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E<X> {\n");
		buf.append("    public static <T> E<T> bar(T t) { return null; }\n");
		buf.append("    public void foo(E<?> e) {\n");
		buf.append("        bar(e);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		String str= "bar(e)";
		MethodInvocation inv= (MethodInvocation) NodeFinder.perform(astRoot, buf.indexOf(str), str.length());
		ITypeBinding binding= inv.resolveTypeBinding();

		ImportRewrite rewrite= newImportsRewrite(astRoot, new String[0], 99, 99, true);

		String string= rewrite.addImport(binding);
		assertEquals("E<?>", string);

		Type resNode= rewrite.addImport(binding, astRoot.getAST());
		assertEquals("E<?>", ASTNodes.asString(resNode));

		if (!BUG_120082) {
			String signature= new BindingKey(binding.getKey()).toSignature();

			Type resNode2= rewrite.addImportFromSignature(signature, astRoot.getAST());
			assertEquals("E<?>", ASTNodes.asString(resNode2));
		}
	}

	public void testAddTypesWithCaptures3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E<X> {\n");
		buf.append("    public static <T> E<? extends T> bar(T t) { return null; }\n");
		buf.append("    public void foo(E<?> e) {\n");
		buf.append("        bar(e);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		String str= "bar(e)";
		MethodInvocation inv= (MethodInvocation) NodeFinder.perform(astRoot, buf.indexOf(str), str.length());
		ITypeBinding binding= inv.resolveTypeBinding();

		ImportRewrite rewrite= newImportsRewrite(astRoot, new String[0], 99, 99, true);

		String string= rewrite.addImport(binding);
		assertEquals("E<?>", string);

		Type resNode= rewrite.addImport(binding, astRoot.getAST());
		assertEquals("E<?>", ASTNodes.asString(resNode));

		if (!BUG_120082) {
			String signature= new BindingKey(binding.getKey()).toSignature();

			Type resNode2= rewrite.addImportFromSignature(signature, astRoot.getAST());
			assertEquals("E<?>", ASTNodes.asString(resNode2));
		}
	}


	public void testImportStructureWithSignatures2() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.*;\n");
		buf.append("import java.net.*;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Map<?, ? extends Set<? super ServerSocket>> z;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", content, false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		String content2= buf.toString();
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", content2, false, null);

		String[] order= new String[] { "java.util", "java.io", "java.net" };
		int threshold= 99;
		AST ast= AST.newAST(AST.JLS3);
		ImportRewrite importsRewrite= newImportsRewrite(cu2, order, threshold, true);
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("Map"), "Map".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).toSignature();

			importsRewrite.addImportFromSignature(signature, ast);
		}

		apply(importsRewrite);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Map;\n");
		buf.append("import java.util.Set;\n");
		buf.append("\n");
		buf.append("import java.net.ServerSocket;\n");
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("}\n");

		assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());

	}


	public void testAddedRemovedImportsAPI() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    public final static int CONST= 9;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		String[] order= new String[] { "#", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addStaticImport("java.lang.Math", "min", true);
		imports.addImport("java.lang.Math");

		assertAddedAndRemoved(imports,
				new String[] { "java.lang.Math" }, new String[] {},
				new String[] { "java.lang.Math.min" }, new String[] {}
		);

		imports.addImport("java.lang.Math");
		imports.addStaticImport("java.lang.Math", "max", true);

		assertAddedAndRemoved(imports,
				new String[] { "java.lang.Math" }, new String[] {},
				new String[] { "java.lang.Math.min", "java.lang.Math.max" }, new String[] {}
		);

		imports.removeImport("java.lang.Math");
		imports.removeImport("java.util.Vector");
		imports.removeStaticImport("java.lang.Math.dup");

		assertAddedAndRemoved(imports,
				new String[] { }, new String[] { "java.util.Vector"},
				new String[] { "java.lang.Math.min", "java.lang.Math.max" }, new String[] {}
		);

		imports.addImport("java.util.Vector");
		imports.addStaticImport("pack1.C", "CONST", true);

		assertAddedAndRemoved(imports,
				new String[] { }, new String[] { },
				new String[] { "java.lang.Math.min", "java.lang.Math.max", "pack1.C.CONST" }, new String[] {}
		);

		apply(imports);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import static java.lang.Math.max;\n");
		buf.append("import static java.lang.Math.min;\n");
		buf.append("import static pack1.C.CONST;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    public final static int CONST= 9;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	private void assertAddedAndRemoved(ImportRewrite imports, String[] expectedAdded, String[] expectedRemoved, String[] expectedAddedStatic, String[] expectedRemovedStatic) {
		assertEqualStringsIgnoreOrder(imports.getAddedImports(), expectedAdded);
		assertEqualStringsIgnoreOrder(imports.getAddedStaticImports(), expectedAddedStatic);
		assertEqualStringsIgnoreOrder(imports.getRemovedImports(), expectedRemoved);
		assertEqualStringsIgnoreOrder(imports.getRemovedStaticImports(), expectedRemovedStatic);
	}

	public void testAddImportAction1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Vector c= null;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Vector");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportAction2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Vector");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportAction3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null\n"); // missing semicolon
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Vector");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportAction4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Vector c= null\n"); // missing semicolon
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Vector");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    Vector c= null\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImports_bug107206() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Vector.class x;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Vector.class") + "Vector.class".length();

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Vector.class x;\n"); // no change
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportActionStatic1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    String str= java.io.File.separator;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("separator");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import static java.io.File.separator;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    String str= separator;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportActionStaticWith14() throws Exception {
		JavaProjectHelper.set14CompilerOptions(fJProject1);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    String str= java.io.File.separator;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("separator");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    String str= java.io.File.separator;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testAddImportContextSensitive01() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo() {//<-insert\n}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "java.lang.Math.PI", "PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"java.lang.Math.PI"});
	}

	public void testAddImportContextSensitive02() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static final double PI= 3.0;\n");
		buf.append("    void foo() {//<-insert\n}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "java.lang.Math.PI", "java.lang.Math.PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	public void testAddImportContextSensitive03() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo() {//<-insert\n}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertStaticImportAsExpected(cu, "java.lang.Math", "PI", "PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"java.lang.Math.PI"});
	}

	public void testAddImportContextSensitive04() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static final double PI= 3.0;\n");
		buf.append("    void foo() {//<-insert\n}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertStaticImportAsExpected(cu, "java.lang.Math", "PI", "java.lang.Math.PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	public void testAddImportContextSensitive05() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E11 {\n");
		buf.append("        class E12 {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("//<-insert\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack2.E1.E11.E12", "E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack2.E1.E11.E12"});
	}

	public void testAddImportContextSensitive06() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E11 {\n");
		buf.append("        class E12 {        \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("//<-insert\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E11.E12", "pack1.E1.E11.E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	public void testAddImportContextSensitive07() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E11 {\n");
		buf.append("        class E12 {        \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private class E21 {\n");
		buf.append("        //<-insert\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E11.E12", "pack1.E1.E11.E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	public void testAddImportContextSensitive08() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static class E2 {}\n");
		buf.append("    interface I1 {void bar();}\n");
		buf.append("    public void foo() {\n");
		buf.append("        I1 i1= new I1() {\n");
		buf.append("            public void bar() {\n");
		buf.append("                //<-insert\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2", "E2", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	public void testAddImportContextSensitive09() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static class E2 {\n");
		buf.append("        public static class E22 {}\n");
		buf.append("    }\n");
		buf.append("    interface I1 {void bar();}\n");
		buf.append("    public void foo() {\n");
		buf.append("        I1 i1= new I1() {\n");
		buf.append("            public void bar() {\n");
		buf.append("                //<-insert\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2.E22", "E22", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack1.E1.E2.E22"});
	}

	public void testAddImportContextSensitive_Bug139000() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo() {\n");
		buf.append("        //<-insert\n");
		buf.append("    }\n");
		buf.append("    class E2<T> {\n");
		buf.append("        public class E3 {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2.E3", "E3", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack1.E1.E2.E3"});
	}

	public void testAddImportContextSensitive10() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public abstract class E1 {\n");
		buf.append("    protected class C {}\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    //<-insert\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.C", "C", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	private String[] assertImportQualifierAsExpected(ICompilationUnit cu, String toImport, String expectedQualifier, String[] importOrder, int threshold, int staticThreshold) throws JavaModelException {
		String buf= cu.getSource();
		String selection= "//<-insert";
		int offset= buf.indexOf(selection);
		int length= 0;
		AssistContext context= new AssistContext(cu, offset, length);

		ImportRewrite importRewrite= newImportsRewrite(context.getASTRoot(), importOrder, threshold, staticThreshold, true);

		String qualifier= importRewrite.addImport(toImport, new ContextSensitiveImportRewriteContext(context.getASTRoot(), offset, importRewrite));
		assertEquals("Type conflict not detected", expectedQualifier, qualifier);
		return importRewrite.getAddedImports();
	}

	private String[] assertStaticImportAsExpected(ICompilationUnit cu, String declaringClassName, String fieldName, String expectedQualifier, String[] importOrder, int threshold, int staticThreshold) throws JavaModelException {
		String code= cu.getSource();
		String selection= "//<-insert";
		int offset= code.indexOf(selection);
		int length= 0;
		AssistContext context= new AssistContext(cu, offset, length);

		ImportRewrite importRewrite= newImportsRewrite(context.getASTRoot(), importOrder, threshold, staticThreshold, true);

		String qualifier= importRewrite.addStaticImport(declaringClassName, fieldName, true, new ContextSensitiveImportRewriteContext(context.getASTRoot(), offset, importRewrite));
		assertEquals("Type conflict not detected", expectedQualifier, qualifier);
		return importRewrite.getAddedStaticImports();
	}

	private ImportRewrite newImportsRewrite(ICompilationUnit cu, String[] order, int threshold, boolean restoreExistingImports) throws CoreException {
		return newImportsRewrite(cu, order, threshold, threshold, restoreExistingImports);
	}

	private void apply(ImportRewrite rewrite) throws CoreException {
		TextEdit edit= rewrite.rewriteImports(null);
		JavaModelUtil.applyEdit(rewrite.getCompilationUnit(), edit, true, null);
	}

}
