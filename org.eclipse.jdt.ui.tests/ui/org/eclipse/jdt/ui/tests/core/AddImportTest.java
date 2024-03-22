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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - Import static (Ctrl+Shift+M) creates imports for private methods - https://bugs.eclipse.org/409594
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

public class AddImportTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		JavaProjectHelper.set15CompilerOptions(fJProject1);

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testAddImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Map;
			import java.util.Set;
			import java.util.Vector;
			
			import pack.List;
			import pack.List2;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("java.net.Socket");
		imports.addImport("p.A");
		imports.addImport("com.something.Foo");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.net.Socket;
			import java.util.Map;
			import java.util.Set;
			import java.util.Vector;
			
			import com.something.Foo;
			
			import pack.List;
			import pack.List2;
			
			import p.A;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "java.util", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("java.x.Socket");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.x.Socket;
			
			import java.util.Set;
			import java.util.Vector;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImports3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Set; // comment
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "java.util", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.util.Vector");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.util.Set; // comment
			import java.util.Vector;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testRemoveImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			import pack.List2;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.removeImport("java.util.Set");
		imports.removeImport("pack.List");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.util.*;
			
			import pack.List2;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testRemoveImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector; // comment
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "com", "pack" };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.removeImport("java.util.Vector");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.util.Set;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testRemoveImports3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public class Inner {
			    }
			}
			""";
		pack.createCompilationUnit("A.java", str, false, null);

		IPackageFragment test1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			import pack.A;
			import pack.A.Inner;
			import pack.A.NotThere;
			import pack.B;
			import pack.B.Inner;
			import pack.B.NotThere;
			
			public class T {
			}
			""";
		ICompilationUnit cuT= test1.createCompilationUnit("T.java", str1, false, null);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(cuT);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		ImportRewrite imports= newImportsRewrite(astRoot, new String[0], 99, 99, true);
		imports.setUseContextToFilterImplicitImports(true);

		imports.removeImport("pack.A.Inner");
		imports.removeImport("pack.A.NotThere");
		imports.removeImport("pack.B.Inner");
		imports.removeImport("pack.B.NotThere");

		apply(imports);

		String str2= """
			package test1;
			
			import pack.A;
			import pack.B;
			
			public class T {
			}
			""";
		assertEqualString(cuT.getSource(), str2);
	}

	@Test
	public void testAddImports_bug23078() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import p.A.*;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { };

		ImportRewrite imports= newImportsRewrite(cu, order, 2, true);
		imports.addImport("p.Inner");

		apply(imports);

		String str1= """
			package pack1;
			
			import p.Inner;
			import p.A.*;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImports_bug25113() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.awt.Panel;
			
			import java.math.BigInteger;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java.awt", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.applet.Applet");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.awt.Panel;
			
			import java.applet.Applet;
			import java.math.BigInteger;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImports_bug42637() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.lang.System;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addImport("java.io.Exception");

		apply(imports);

		String str1= """
			package pack1;
			
			import java.io.Exception;
			import java.lang.System;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddStaticImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.lang.System;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "#", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addStaticImport("java.lang.Math", "min", true);
		imports.addImport("java.lang.Math");
		imports.addStaticImport("java.lang.Math", "max", true);

		apply(imports);

		String str1= """
			package pack1;
			
			import static java.lang.Math.max;
			import static java.lang.Math.min;
			
			import java.lang.System;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddStaticImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.lang.System;
			
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "#", "java" };

		ImportRewrite imports= newImportsRewrite(cu, order, 99, true);
		imports.addStaticImport("xx.MyConstants", "SIZE", true);
		imports.addStaticImport("xy.MyConstants", "*", true);
		imports.addImport("xy.MyConstants");

		apply(imports);

		String str1= """
			package pack1;
			
			import static xx.MyConstants.SIZE;
			import static xy.MyConstants.*;
			
			import java.lang.System;
			
			import xy.MyConstants;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImportStaticForSubclassReference() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			public class TSub extends T {
			}
			""";
		pack1.createCompilationUnit("TSub.java", str1, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("test2", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("import test1.TSub;\n");
		buf.append("public class S {\n");
		buf.append("    public S() {\n");
		buf.append("        TSub.foo();\n");
		buf.append("        TSub.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("foo");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str2= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.TSub;
			public class S {
			    public S() {
			        foo();
			        TSub.foo();
			    }
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testImportStructureWithSignatures() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String content= """
			package test1;
			import java.util.*;
			import java.net.*;
			import java.io.*;
			public class A {
			    public void foo() {
			        IOException s;
			        URL[][] t;
			        List<SocketAddress> x;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", content, false, null);

		String content2= """
			package test1;
			public class B {
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", content2, false, null);



		String[] order= new String[] { "java.util", "java.io", "java.net" };
		int threshold= 99;
		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
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

		String str= """
			package test1;
			
			import java.util.List;
			
			import java.io.IOException;
			
			import java.net.SocketAddress;
			import java.net.URL;
			
			public class B {
			}
			""";
		assertEqualStringIgnoreDelim(cu2.getSource(), str);

	}

	@Test
	public void testAddTypesWithCaptures1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
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

	@Test
	public void testAddTypesWithCaptures2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E<X> {\n");
		buf.append("    public static <T> E<T> bar(T t) { return null; }\n");
		buf.append("    public void foo(E<?> e) {\n");
		buf.append("        bar(e);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
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

		String signature= new BindingKey(binding.getKey()).toSignature();

		Type resNode2= rewrite.addImportFromSignature(signature, astRoot.getAST());
		assertEquals("E<?>", ASTNodes.asString(resNode2));
	}

	@Test
	public void testAddTypesWithCaptures3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class E<X> {\n");
		buf.append("    public static <T> E<? extends T> bar(T t) { return null; }\n");
		buf.append("    public void foo(E<?> e) {\n");
		buf.append("        bar(e);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
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

		String signature= new BindingKey(binding.getKey()).toSignature();

		Type resNode2= rewrite.addImportFromSignature(signature, astRoot.getAST());
		assertEquals("E<?>", ASTNodes.asString(resNode2));
	}


	@Test
	public void testImportStructureWithSignatures2() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String content= """
			package test1;
			import java.util.*;
			import java.net.*;
			import java.io.*;
			public class A {
			    public void foo() {
			        Map<?, ? extends Set<? super ServerSocket>> z;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", content, false, null);

		String content2= """
			package test1;
			public class B {
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", content2, false, null);

		String[] order= new String[] { "java.util", "java.io", "java.net" };
		int threshold= 99;
		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		ImportRewrite importsRewrite= newImportsRewrite(cu2, order, threshold, true);
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("Map"), "Map".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).toSignature();

			importsRewrite.addImportFromSignature(signature, ast);
		}

		apply(importsRewrite);

		String str= """
			package test1;
			
			import java.util.Map;
			import java.util.Set;
			
			import java.net.ServerSocket;
			
			public class B {
			}
			""";
		assertEqualStringIgnoreDelim(cu2.getSource(), str);

	}


	@Test
	public void testAddedRemovedImportsAPI() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.Vector;
			
			public class C {
			    public final static int CONST= 9;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

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

		String str1= """
			package pack1;
			
			import static java.lang.Math.max;
			import static java.lang.Math.min;
			import static pack1.C.CONST;
			
			import java.util.Vector;
			
			public class C {
			    public final static int CONST= 9;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	private void assertAddedAndRemoved(ImportRewrite imports, String[] expectedAdded, String[] expectedRemoved, String[] expectedAddedStatic, String[] expectedRemovedStatic) {
		assertEqualStringsIgnoreOrder(imports.getAddedImports(), expectedAdded);
		assertEqualStringsIgnoreOrder(imports.getAddedStaticImports(), expectedAddedStatic);
		assertEqualStringsIgnoreOrder(imports.getRemovedImports(), expectedRemoved);
		assertEqualStringsIgnoreOrder(imports.getRemovedStaticImports(), expectedRemovedStatic);
	}

	@Test
	public void testAddImportAction1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import java.lang.System;
			import java.util.Vector;
			
			public class C {
			    Vector c= null;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportAction2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import java.lang.System;
			import java.util.Vector;
			
			public class C {
			    Vector c= null;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportAction3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import java.lang.System;
			import java.util.Vector;
			
			public class C {
			    Vector c= null
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportAction4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import java.lang.System;
			import java.util.Vector;
			
			public class C {
			    Vector c= null
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportAction5() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    private static class Serializable { }\n");
		buf.append("    public void bar() {\n");
		buf.append("        java.io.Serializable ser= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Secondary {\n");
		buf.append("    Serializable s;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("ser=") - 2;

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    private static class Serializable { }\n");
		buf.append("    public void bar() {\n");
		buf.append("        java.io.Serializable ser= null;\n"); // no change
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Secondary {\n");
		buf.append("    Serializable s;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	@Test
	public void testAddImportActionBug_409594_test1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		String input =
				"""
			package p;
			
			class A {
				static void foo() {
					A.bar();
				}
			
				private static void bar() {
				}
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", input, false, null);

		int selOffset= input.indexOf("bar");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 3, null, true);
		op.run(null);

		String expected =
				"""
			package p;
			
			class A {
				static void foo() {
					bar();
				}
			
				private static void bar() {
				}
			}""";
		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testAddImportActionBug_409594_test2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		String input =
				"""
			package p;
			
			class A {
				static void foo() {
					A.bar();
				}
			
				public static void bar() {
				}
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", input, false, null);

		int selOffset= input.indexOf("bar");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 3, null, true);
		op.run(null);

		String expected =
				"""
			package p;
			
			class A {
				static void foo() {
					bar();
				}
			
				public static void bar() {
				}
			}""";
		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testAddImportActionBug_409594_test3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		String input =
				"""
			package p;
			class SnippetX {
			    private static class Test {
			        class X {
			            void foo() {
			                Test.bar();
			            }
			        }
			        public static void bar() {}
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", input, false, null);

		int selOffset= input.indexOf("bar");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 3, null, true);
		op.run(null);

		String expected =
				"""
			package p;
			class SnippetX {
			    private static class Test {
			        class X {
			            void foo() {
			                bar();
			            }
			        }
			        public static void bar() {}
			    }
			}""";
		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testAddImportActionBug_409594_test4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("class SnippetY {\n");
		buf.append("    static class Test {\n");
		buf.append("        static void bar() {}\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("        Test.bar();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("bar();");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 3, null, true);
		op.run(null);

		String str= """
			package p;
			
			import static p.SnippetY.Test.bar;
			
			class SnippetY {
			    static class Test {
			        static void bar() {}
			    }
			
			    void foo() {
			        bar();
			    }
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionBug_409594_test5() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package organize.imports.pvtStaticMembers.bug409594;\n");
		buf.append("\n");
		buf.append("class SnippetY {    \n");
		buf.append("    private static class Test {\n");
		buf.append("        static void bar() {}        \n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    void foo() {\n");
		buf.append("         Test.bar();\n");
		buf.append("     }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("bar();");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 3, null, true);
		op.run(null);

		assertEqualString(cu.getSource(), cu.getSource());
	}

	@Test
	public void testAddImportActionBug_409594_test6() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		String inputA=
				"""
			package q;
			
			public class A {
				protected static void bar() {
				}
			}""";
		String inputB=
				"package p;\n" +
						"\n" +
						"import q.A;\n" +
						"\n" +
						"class B extends A {\n" +
						"	void foo() {\n" +
						"		A.bar();\n" +
						"	}\n" +
						"}\n" +
						"";
		pack1.createCompilationUnit("A.java", inputA, false, null);
		ICompilationUnit cuB= pack1.createCompilationUnit("B.java", inputB, false, null);

		int selOffset= inputB.indexOf("bar");

		AddImportsOperation op= new AddImportsOperation(cuB, selOffset, 3, null, true);
		op.run(null);

		String expected=
				"package p;\n" +
						"\n" +
						"import q.A;\n" +
						"\n" +
						"class B extends A {\n" +
						"	void foo() {\n" +
						"		bar();\n" +
						"	}\n" +
						"}\n" +
						"";
		assertEqualString(cuB.getSource(), expected);
	}

	@Test
	public void testAddImportAction_bug107206() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.System;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Vector.class x;\n"); // no change
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	@Test
	public void testAddImportActionStatic1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import static java.io.File.separator;
			
			import java.lang.System;
			
			public class C {
			    String str= separator;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionStaticWith14() throws Exception {
		JavaProjectHelper.set14CompilerOptions(fJProject1);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
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

		String str= """
			package pack1;
			
			import java.lang.System;
			
			public class C {
			    String str= java.io.File.separator;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionNestedType1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.lang.Thread.State s;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Thread");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			public class C {
			    Thread.State s;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionNestedType2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.lang.Thread.State s;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("State");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.lang.Thread.State;
			
			public class C {
			    State s;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionParameterizedType1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Map<String, Integer> m;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Map");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.util.Map;
			
			public class C {
			    Map<String, Integer> m;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionParameterizedType2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Map.Entry e;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Entry");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.util.Map.Entry;
			
			public class C {
			    Entry e;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionParameterizedType3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Map.Entry<String, Object> e;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Map");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.util.Map;
			
			public class C {
			    Map.Entry<String, Object> e;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionParameterizedType4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.util.Map.Entry<String, Object> e;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Entry");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.util.Map.Entry;
			
			public class C {
			    Entry<String, Object> e;
			}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportActionParameterizedType5() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str= """
			package pack2;
			
			public class Outer {
			    public class Middle<M> {
			        public class Inner<I> {
			        }
			    }
			}
			""";
		pack2.createCompilationUnit("Outer.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    pack2.Outer.Middle<String>.Inner<Integer> i;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Middle");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str1= """
			package pack1;
			
			import pack2.Outer.Middle;
			
			public class C {
			    Middle<String>.Inner<Integer> i;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	// Don't touch nested type if outer is parameterized.
	@Test
	public void testAddImportActionParameterizedType6() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str= """
			package pack2;
			
			public class Outer {
			    public class Middle<M> {
			        public class Inner<I> {
			        }
			    }
			}
			""";
		pack2.createCompilationUnit("Outer.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    pack2.Outer.Middle<String>.Inner<Integer> i;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Inner");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str1= """
			package pack1;
			
			public class C {
			    pack2.Outer.Middle<String>.Inner<Integer> i;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAddImportActionAnnotatedType1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.text.@A Format.@A Field f;\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface A {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Format");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.text.Format;
			
			public class C {
			    @A Format.@A Field f;
			}
			@Target(ElementType.TYPE_USE) @interface A {}
			""";
		assertEqualString(cu.getSource(), str);
	}

	// Don't touch nested type if outer is annotated.
	@Test
	public void testAddImportActionAnnotatedType2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    java.text.@A Format.@A Field f;\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface A {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int selOffset= buf.indexOf("Field");

		AddImportsOperation op= new AddImportsOperation(cu, selOffset, 0, null, true);
		op.run(null);

		String str= """
			package pack1;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class C {
			    java.text.@A Format.@A Field f;
			}
			@Target(ElementType.TYPE_USE) @interface A {}
			""";
		assertEqualString(cu.getSource(), str);
	}

	@Test
	public void testAddImportContextSensitive01() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    void foo() {//<-insert
			}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "java.lang.Math.PI", "PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"java.lang.Math.PI"});
	}

	@Test
	public void testAddImportContextSensitive02() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    public static final double PI= 3.0;
			    void foo() {//<-insert
			}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "java.lang.Math.PI", "java.lang.Math.PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	@Test
	public void testAddImportContextSensitive03() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    void foo() {//<-insert
			}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertStaticImportAsExpected(cu, "java.lang.Math", "PI", "PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"java.lang.Math.PI"});
	}

	@Test
	public void testAddImportContextSensitive04() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    public static final double PI= 3.0;
			    void foo() {//<-insert
			}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertStaticImportAsExpected(cu, "java.lang.Math", "PI", "java.lang.Math.PI", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	@Test
	public void testAddImportContextSensitive05() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    private class E11 {
			        class E12 {
			        }
			    }
			//<-insert
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack2.E1.E11.E12", "E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack2.E1.E11.E12"});
	}

	@Test
	public void testAddImportContextSensitive06() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    private class E11 {
			        class E12 {       \s
			        }
			    }
			//<-insert
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E11.E12", "pack1.E1.E11.E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	@Test
	public void testAddImportContextSensitive07() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    private class E11 {
			        class E12 {       \s
			        }
			    }
			    private class E21 {
			        //<-insert
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E11.E12", "pack1.E1.E11.E12", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	@Test
	public void testAddImportContextSensitive08() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    public static class E2 {}
			    interface I1 {void bar();}
			    public void foo() {
			        I1 i1= new I1() {
			            public void bar() {
			                //<-insert
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2", "E2", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[0]);
	}

	@Test
	public void testAddImportContextSensitive09() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    public static class E2 {
			        public static class E22 {}
			    }
			    interface I1 {void bar();}
			    public void foo() {
			        I1 i1= new I1() {
			            public void bar() {
			                //<-insert
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2.E22", "E22", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack1.E1.E2.E22"});
	}

	@Test
	public void testAddImportContextSensitive_Bug139000() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class E1 {
			    void foo() {
			        //<-insert
			    }
			    class E2<T> {
			        public class E3 {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String[] addedImports= assertImportQualifierAsExpected(cu, "pack1.E1.E2.E3", "E3", new String[0], 99, 99);
		assertEqualStringsIgnoreOrder(addedImports, new String[] {"pack1.E1.E2.E3"});
	}

	@Test
	public void testAddImportContextSensitive10() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public abstract class E1 {
			    protected class C {}
			}
			""";
		pack1.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package pack1;
			public class E2 extends E1 {
			    //<-insert
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

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
