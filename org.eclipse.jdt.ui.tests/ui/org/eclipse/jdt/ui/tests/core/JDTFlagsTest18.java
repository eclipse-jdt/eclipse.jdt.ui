/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.File;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class JDTFlagsTest18 extends TestCase {

	private static final Class THIS= JDTFlagsTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public JDTFlagsTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fJProject1= Java18ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	protected CompilationUnit getCompilationUnitNode(String source) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(source.toCharArray());
		Hashtable options= JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		CompilationUnit cuNode = (CompilationUnit) parser.createAST(null);
		return cuNode;
	}

	public void testIsStaticInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Snippet {\n");
		buf.append("    public static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("public static");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertTrue(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isAbstract(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(fJProject1);
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertTrue(JdtFlags.isStatic(binding));
			Assert.assertFalse(JdtFlags.isAbstract(binding));
			Assert.assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}

		MethodDeclaration methodNode= ASTNodeSearchUtil.getMethodDeclarationNode(method, getCompilationUnitNode(buf.toString()));
		Assert.assertTrue(JdtFlags.isStatic(methodNode));
	}

	public void testNestedEnumInEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("enum Snippet {\n");
		buf.append("    A;\n");
		buf.append("    enum E {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("enum E");
		IJavaElement elem= cUnit.getElementAt(offset);
		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)elem, getCompilationUnitNode(buf.toString()));
		Assert.assertTrue(JdtFlags.isStatic(enumNode));
		Assert.assertTrue(JdtFlags.isStatic((IType)elem));
	}

	public void testNestedEnumInInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    enum CoffeeSize {\n");
		buf.append("        BIG, HUGE{\n");
		buf.append("            public String getLidCode() {\n");
		buf.append("                return \"B\";\n");
		buf.append("            }\n");
		buf.append("        }, OVERWHELMING {\n");
		buf.append("\n");
		buf.append("            public String getLidCode() {\n");
		buf.append("                return \"A\";\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        public String getLidCode() {\n");
		buf.append("            return \"B\";\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    enum Colors{\n");
		buf.append("        RED, BLUE, GREEN;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("enum CoffeeSize");
		IJavaElement elem= cUnit.getElementAt(offset);
		IMember type= (IMember)elem;
		Assert.assertTrue(JdtFlags.isStatic(type));
		Assert.assertFalse(JdtFlags.isAbstract(type));

		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)elem, getCompilationUnitNode(buf.toString()));
		Assert.assertTrue(JdtFlags.isStatic(enumNode));

		// testcase for isF an enum
		Assert.assertFalse(JdtFlags.isFinal(type));
		offset= cUnit.getSource().indexOf("enum Colors");
		type= (IMember)cUnit.getElementAt(offset);
		Assert.assertTrue(JdtFlags.isFinal(type));
	}

	public void testNestedEnumInClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Snippet {    \n");
		buf.append("    enum Color {\n");
		buf.append("        RED,\n");
		buf.append("        BLUE;\n");
		buf.append("    Runnable r = new Runnable() {\n");
		buf.append("        \n");
		buf.append("        @Override\n");
		buf.append("        public void run() {\n");
		buf.append("            // TODO Auto-generated method stub\n");
		buf.append("            \n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		// testing nested enum
		int offset= cUnit.getSource().indexOf("enum");
		CompilationUnit cuNode= getCompilationUnitNode(buf.toString());
		IJavaElement javaElem= cUnit.getElementAt(offset);
		IMember element= (IMember)javaElem;
		Assert.assertTrue(JdtFlags.isStatic(element));
		Assert.assertFalse(JdtFlags.isAbstract(element));

		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)javaElem, cuNode);
		Assert.assertTrue(JdtFlags.isStatic(enumNode));

		// testing enum constant
		offset= cUnit.getSource().indexOf("RED");
		javaElem= cUnit.getElementAt(offset);
		element= (IMember)javaElem;
		Assert.assertTrue(JdtFlags.isStatic(element));
		Assert.assertFalse(JdtFlags.isAbstract(element));

		EnumConstantDeclaration enumConst= ASTNodeSearchUtil.getEnumConstantDeclaration((IField)javaElem, cuNode);
		Assert.assertTrue(JdtFlags.isStatic(enumConst));

		// testing enum constant
		offset= cUnit.getSource().indexOf("Runnable r");
		element= (IMember)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isFinal(element));

	}

	public void testNestedEnumIsFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Snippet {    \n");
		buf.append("    enum Color {\n");
		buf.append("        BLUE{};\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("enum Color");
		IMember element= (IMember)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isFinal(element));
	}

	public void testIsStaticInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertTrue("lib not found", clsJarPath != null && clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		Assert.assertTrue(jarRoot.exists());
		Assert.assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		Assert.assertTrue(pf.exists());
		IClassFile classFile2= pf.getClassFile("Snippet.class");
		IMethod[] clsFile= classFile2.getType().getMethods();
		IMethod method= clsFile[0];
		Assert.assertTrue(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isAbstract(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));
	}

	public void testIsDefaultInInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("     public default String defaultMethod(){\n");
		buf.append("         return \"default\";\n");
		buf.append("     }\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("public default");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isAbstract(method));
		Assert.assertTrue(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertFalse(JdtFlags.isStatic(binding));
			Assert.assertFalse(JdtFlags.isAbstract(binding));
			Assert.assertTrue(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	public void testIsDefaultInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertTrue("lib not found", clsJarPath != null && clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		Assert.assertTrue(jarRoot.exists());
		Assert.assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		Assert.assertTrue(pf.exists());
		IClassFile classFile2= pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("defaultMethod", null);
		Assert.assertTrue(JdtFlags.isDefaultMethod(method));
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isAbstract(method));
	}

	public void testIsDefaultInClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class Snippet {\n");
		buf.append("     public String notDefaultMethod(){\n");
		buf.append("         return \"not default\";\n");
		buf.append("     }\n");
		buf.append("    public @interface A_test109 {\n");
		buf.append("        public String notDefaultIntMet() default \"not default\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("public String notDefaultMethod");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isAbstract(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertFalse(JdtFlags.isStatic(binding));
			Assert.assertFalse(JdtFlags.isAbstract(binding));
			Assert.assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}

		offset= cUnit.getSource().indexOf("public String notDefaultIntMet");
		method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertTrue(JdtFlags.isAbstract(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));

		p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertFalse(JdtFlags.isStatic(binding));
			Assert.assertTrue(JdtFlags.isAbstract(binding));
			Assert.assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	public void testImplicitAbstractInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Snippet {\n");
		buf.append("    float abstractMethod();\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("float");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));
		Assert.assertTrue(JdtFlags.isAbstract(method));

		MethodDeclaration methodNode= ASTNodeSearchUtil.getMethodDeclarationNode(method, getCompilationUnitNode(buf.toString()));
		Assert.assertFalse(JdtFlags.isStatic(methodNode));

		ASTParser p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertFalse(JdtFlags.isStatic(binding));
			Assert.assertTrue(JdtFlags.isAbstract(binding));
			Assert.assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	public void testExplicitAbstractInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Snippet {\n");
		buf.append("    public abstract float abstractMethod();\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		int offset= cUnit.getSource().indexOf("public abstract");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));
		Assert.assertTrue(JdtFlags.isAbstract(method));

		ASTParser p= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			Assert.assertFalse(JdtFlags.isStatic(binding));
			Assert.assertTrue(JdtFlags.isAbstract(binding));
			Assert.assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	public void testExplicitAbstractInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertTrue("lib not found", clsJarPath != null && clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		Assert.assertTrue(jarRoot.exists());
		Assert.assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		Assert.assertTrue(pf.exists());
		IClassFile classFile2= pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("explicitAbstractMethod", null);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));
		Assert.assertTrue(JdtFlags.isAbstract(method));
	}

	public void testImplicitAbstractInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertTrue("lib not found", clsJarPath != null && clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		Assert.assertTrue(jarRoot.exists());
		Assert.assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		Assert.assertTrue(pf.exists());
		IClassFile classFile2= pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("implicitAbstractMethod", null);
		Assert.assertFalse(JdtFlags.isStatic(method));
		Assert.assertFalse(JdtFlags.isDefaultMethod(method));
		Assert.assertTrue(JdtFlags.isAbstract(method));
	}

	public void testIsStaticAnnotationType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public @interface Snippet {\n");
		buf.append("    int i= 0;\n");
		buf.append("    public String name();\n");
		buf.append("}\n");
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);
		CompilationUnit cuNode= getCompilationUnitNode(buf.toString());

		int offset= cUnit.getSource().indexOf("i=");
		IJavaElement elem= cUnit.getElementAt(offset);
		FieldDeclaration field= ASTNodeSearchUtil.getFieldDeclarationNode((IField)elem, cuNode);
		Assert.assertTrue(JdtFlags.isStatic(field));
		Assert.assertTrue(JdtFlags.isStatic((IField)elem));

		offset= cUnit.getSource().indexOf("name");
		elem= cUnit.getElementAt(offset);
		AnnotationTypeMemberDeclaration annotationMember= ASTNodeSearchUtil.getAnnotationTypeMemberDeclarationNode((IMethod)elem, cuNode);
		Assert.assertFalse(JdtFlags.isStatic(annotationMember));
		Assert.assertFalse(JdtFlags.isStatic((IMethod)elem));
	}
}
