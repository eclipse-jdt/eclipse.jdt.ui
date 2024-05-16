/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

import org.eclipse.jdt.internal.ui.util.ASTHelper;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class JDTFlagsTest18 {
	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Rule
	public Java1d8ProjectTestSetup j18p= new Java1d8ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		fJProject1= j18p.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, j18p.getDefaultClasspath());
	}

	protected CompilationUnit getCompilationUnitNode(String source) {
		ASTParser parser = ASTParser.newParser(ASTHelper.JLS8);
		parser.setSource(source.toCharArray());
		Hashtable<String, String> options= JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		CompilationUnit cuNode = (CompilationUnit) parser.createAST(null);
		return cuNode;
	}

	@Test
	public void testIsStaticInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public interface Snippet {
			    public static int staticMethod(Object[] o) throws IOException{return 10;}
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("public static");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		assertTrue(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isAbstract(method));
		assertFalse(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(fJProject1);
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertTrue(JdtFlags.isStatic(binding));
			assertFalse(JdtFlags.isAbstract(binding));
			assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}

		MethodDeclaration methodNode= ASTNodeSearchUtil.getMethodDeclarationNode(method, getCompilationUnitNode(str));
		assertTrue(JdtFlags.isStatic(methodNode));
	}

	@Test
	public void testNestedEnumInEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			enum Snippet {
			    A;
			    enum E {
			    }
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("enum E");
		IJavaElement elem= cUnit.getElementAt(offset);
		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)elem, getCompilationUnitNode(str));
		assertTrue(JdtFlags.isStatic(enumNode));
		assertTrue(JdtFlags.isStatic((IType)elem));
	}

	@Test
	public void testNestedEnumInInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Snippet {
			    enum CoffeeSize {
			        BIG, HUGE{
			            public String getLidCode() {
			                return "B";
			            }
			        }, OVERWHELMING {
			
			            public String getLidCode() {
			                return "A";
			            }
			        };
			        public String getLidCode() {
			            return "B";
			        }
			    }
			    enum Colors{
			        RED, BLUE, GREEN;
			    }
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("enum CoffeeSize");
		IJavaElement elem= cUnit.getElementAt(offset);
		IMember type= (IMember)elem;
		assertTrue(JdtFlags.isStatic(type));
		assertFalse(JdtFlags.isAbstract(type));

		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)elem, getCompilationUnitNode(str));
		assertTrue(JdtFlags.isStatic(enumNode));

		// testcase for isF an enum
		assertFalse(JdtFlags.isFinal(type));
		offset= cUnit.getSource().indexOf("enum Colors");
		type= (IMember)cUnit.getElementAt(offset);
		assertTrue(JdtFlags.isFinal(type));
	}

	@Test
	public void testNestedEnumInClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Snippet {   \s
			    enum Color {
			        RED,
			        BLUE;
			    Runnable r = new Runnable() {
			       \s
			        @Override
			        public void run() {
			            // TODO Auto-generated method stub
			           \s
			        }
			    };
			    }
			   \s
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		// testing nested enum
		int offset= cUnit.getSource().indexOf("enum");
		CompilationUnit cuNode= getCompilationUnitNode(str);
		IJavaElement javaElem= cUnit.getElementAt(offset);
		IMember element= (IMember)javaElem;
		assertTrue(JdtFlags.isStatic(element));
		assertFalse(JdtFlags.isAbstract(element));

		EnumDeclaration enumNode= ASTNodeSearchUtil.getEnumDeclarationNode((IType)javaElem, cuNode);
		assertTrue(JdtFlags.isStatic(enumNode));

		// testing enum constant
		offset= cUnit.getSource().indexOf("RED");
		javaElem= cUnit.getElementAt(offset);
		element= (IMember)javaElem;
		assertTrue(JdtFlags.isStatic(element));
		assertFalse(JdtFlags.isAbstract(element));

		EnumConstantDeclaration enumConst= ASTNodeSearchUtil.getEnumConstantDeclaration((IField)javaElem, cuNode);
		assertTrue(JdtFlags.isStatic(enumConst));

		// testing enum constant
		offset= cUnit.getSource().indexOf("Runnable r");
		element= (IMember)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isFinal(element));

	}

	@Test
	public void testNestedEnumIsFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Snippet {   \s
			    enum Color {
			        BLUE{};
			    }
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("enum Color");
		IMember element= (IMember)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isFinal(element));
	}

	@Test
	public void testIsStaticInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertNotNull("lib not found", clsJarPath);//$NON-NLS-1$
		assertTrue("lib not found", clsJarPath.exists());
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(jarRoot.exists());
		assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		assertTrue(pf.exists());
		IOrdinaryClassFile classFile2= (IOrdinaryClassFile) pf.getClassFile("Snippet.class");
		IMethod[] clsFile= classFile2.getType().getMethods();
		IMethod method= clsFile[0];
		assertTrue(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isAbstract(method));
		assertFalse(JdtFlags.isDefaultMethod(method));
	}

	@Test
	public void testIsDefaultInInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Snippet {
			     public default String defaultMethod(){
			         return "default";
			     }
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("public default");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isAbstract(method));
		assertTrue(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertFalse(JdtFlags.isStatic(binding));
			assertFalse(JdtFlags.isAbstract(binding));
			assertTrue(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	@Test
	public void testIsDefaultInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertNotNull("lib not found", clsJarPath);//$NON-NLS-1$
		assertTrue("lib not found", clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(jarRoot.exists());
		assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		assertTrue(pf.exists());
		IOrdinaryClassFile classFile2= (IOrdinaryClassFile) pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("defaultMethod", null);
		assertTrue(JdtFlags.isDefaultMethod(method));
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isAbstract(method));
	}

	@Test
	public void testIsDefaultInClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class Snippet {
			     public String notDefaultMethod(){
			         return "not default";
			     }
			    public @interface A_test109 {
			        public String notDefaultIntMet() default "not default";
			    }
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("public String notDefaultMethod");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isAbstract(method));
		assertFalse(JdtFlags.isDefaultMethod(method));

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertFalse(JdtFlags.isStatic(binding));
			assertFalse(JdtFlags.isAbstract(binding));
			assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}

		offset= cUnit.getSource().indexOf("public String notDefaultIntMet");
		method= (IMethod)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isStatic(method));
		assertTrue(JdtFlags.isAbstract(method));
		assertFalse(JdtFlags.isDefaultMethod(method));

		p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertFalse(JdtFlags.isStatic(binding));
			assertTrue(JdtFlags.isAbstract(binding));
			assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	@Test
	public void testImplicitAbstractInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public interface Snippet {
			    float abstractMethod();
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("float");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isDefaultMethod(method));
		assertTrue(JdtFlags.isAbstract(method));

		MethodDeclaration methodNode= ASTNodeSearchUtil.getMethodDeclarationNode(method, getCompilationUnitNode(str));
		assertFalse(JdtFlags.isStatic(methodNode));

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertFalse(JdtFlags.isStatic(binding));
			assertTrue(JdtFlags.isAbstract(binding));
			assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	@Test
	public void testExplicitAbstractInSrcFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public interface Snippet {
			    public abstract float abstractMethod();
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		int offset= cUnit.getSource().indexOf("public abstract");
		IMethod method= (IMethod)cUnit.getElementAt(offset);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isDefaultMethod(method));
		assertTrue(JdtFlags.isAbstract(method));

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(pack1.getJavaProject());
		p.setBindingsRecovery(true);
		try {
			IMethodBinding binding= (IMethodBinding)p.createBindings(new IJavaElement[] { method }, null)[0];
			assertFalse(JdtFlags.isStatic(binding));
			assertTrue(JdtFlags.isAbstract(binding));
			assertFalse(JdtFlags.isDefaultMethod(binding));
		} catch (OperationCanceledException e) {
		}
	}

	@Test
	public void testExplicitAbstractInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertNotNull("lib not found", clsJarPath);//$NON-NLS-1$
		assertTrue("lib not found", clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(jarRoot.exists());
		assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		assertTrue(pf.exists());
		IOrdinaryClassFile classFile2= (IOrdinaryClassFile) pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("explicitAbstractMethod", null);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isDefaultMethod(method));
		assertTrue(JdtFlags.isAbstract(method));
	}

	@Test
	public void testImplicitAbstractInBinaryFile() throws Exception {
		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/JDTFlagsTest18.zip"));
		assertNotNull("lib not found", clsJarPath);//$NON-NLS-1$
		assertTrue("lib not found", clsJarPath.exists());//$NON-NLS-1$
		IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), null, null);
		fJProject1.open(null);
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(jarRoot.exists());
		assertTrue(jarRoot.isArchive());
		IPackageFragment pf= jarRoot.getPackageFragment("pack1");//$NON-NLS-1$
		assertTrue(pf.exists());
		IOrdinaryClassFile classFile2= (IOrdinaryClassFile) pf.getClassFile("Snippet.class");
		IMethod method= classFile2.getType().getMethod("implicitAbstractMethod", null);
		assertFalse(JdtFlags.isStatic(method));
		assertFalse(JdtFlags.isDefaultMethod(method));
		assertTrue(JdtFlags.isAbstract(method));
	}

	@Test
	public void testIsStaticAnnotationType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public @interface Snippet {
			    int i= 0;
			    public String name();
			}
			""";
		ICompilationUnit cUnit= pack1.createCompilationUnit("Snippet.java", str, false, null);
		CompilationUnit cuNode= getCompilationUnitNode(str);

		int offset= cUnit.getSource().indexOf("i=");
		IJavaElement elem= cUnit.getElementAt(offset);
		FieldDeclaration field= ASTNodeSearchUtil.getFieldDeclarationNode((IField)elem, cuNode);
		assertTrue(JdtFlags.isStatic(field));
		assertTrue(JdtFlags.isStatic((IField)elem));

		offset= cUnit.getSource().indexOf("name");
		elem= cUnit.getElementAt(offset);
		AnnotationTypeMemberDeclaration annotationMember= ASTNodeSearchUtil.getAnnotationTypeMemberDeclarationNode((IMethod)elem, cuNode);
		assertFalse(JdtFlags.isStatic(annotationMember));
		assertFalse(JdtFlags.isStatic((IMethod)elem));
	}
}
