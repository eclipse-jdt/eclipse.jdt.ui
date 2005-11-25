/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.NewImportRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.osgi.service.prefs.BackingStoreException;

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
			return setUpTest(new AddImportTest("testRemoveImports1"));
		}	
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}


	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}


	protected void tearDown() throws Exception {
		setOrganizeImportSettings(null, 99, fJProject1);
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
		
		NewImportRewrite imports= newImportsRewrite(cu, order, 2, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 2, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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
		
		NewImportRewrite imports= newImportsRewrite(cu, order, 2, true);
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
		
		NewImportRewrite imports= newImportsRewrite(cu, order, 2, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 2, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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
		NewImportRewrite importsRewrite= newImportsRewrite(cu2, order, threshold, true);
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("IOException"), "IOException".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).internalToSignature();
			
			importsRewrite.addImportFromSignature(signature, ast);
		}
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("URL"), "URL".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).internalToSignature();
			
			importsRewrite.addImportFromSignature(signature, ast);
		}
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("List"), "List".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).internalToSignature();
			
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

	private static final boolean BUG_87929= false;
	
	public void testImportStructureWithSignatures2() throws Exception {
		if (BUG_87929) {
			return;
		}
		
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
		NewImportRewrite importsRewrite= newImportsRewrite(cu2, order, threshold, true);
		{
			IJavaElement[] elements= cu1.codeSelect(content.indexOf("Map"), "Map".length());
			assertEquals(1, elements.length);
			String key= ((IType) elements[0]).getKey();
			String signature= new BindingKey(key).internalToSignature();
			
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

		NewImportRewrite imports= newImportsRewrite(cu, order, 99, true);
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
	
	private void assertAddedAndRemoved(NewImportRewrite imports, String[] expectedAdded, String[] expectedRemoved, String[] expectedAddedStatic, String[] expectedRemovedStatic) {
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("Vector");
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
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
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("Vector");
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
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
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("Vector");
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
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
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("Vector");
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
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
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("Vector.class") + "Vector.class".length();
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
			op.run(null);

			buf= new StringBuffer();
			buf.append("package pack1;\n");
			buf.append("\n");
			buf.append("import java.lang.System;\n");
			buf.append("\n");		
			buf.append("public class C {\n");
			buf.append("    java.util.Vector.class x;\n"); // no change
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
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
		
		IPath path= cu.getPath();
		
		FileBuffers.getTextFileBufferManager().connect(path, null);
		try {
			IDocument doc= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path).getDocument();
			int selOffset= buf.indexOf("separator");
		
			AddImportsOperation op= new AddImportsOperation(cu, doc, selOffset, 0, null);
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
			assertEqualString(doc.get(), buf.toString());

		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(path, null);
		}
	}	

	private NewImportRewrite newImportsRewrite(ICompilationUnit cu, String[] order, int threshold, boolean restoreExistingImports) throws CoreException, BackingStoreException {
		setOrganizeImportSettings(order, threshold, cu.getJavaProject());
		return NewImportRewrite.create(cu, restoreExistingImports);
	}
		
	private void apply(NewImportRewrite rewrite) throws CoreException {
		TextEdit edit= rewrite.rewriteImports(null);
		JavaModelUtil.applyEdit(rewrite.getCompilationUnit(), edit, true, null);
	}
	
}
