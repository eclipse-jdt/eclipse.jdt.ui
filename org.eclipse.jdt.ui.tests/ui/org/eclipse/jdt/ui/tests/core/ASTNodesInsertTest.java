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
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

/**
  */
public class ASTNodesInsertTest extends CoreTests {

	private static final Class THIS= ASTNodesInsertTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTNodesInsertTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {

		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testInsert1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    int[] fGlobal;\n");
		buf.append("    public void goo(int param1, int param2) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the first field

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the normal method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // before the normal method

		declaration= createNewType(ast, 0);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before all

	}

	public void testInsert2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    class Inner {\n");
		buf.append("    }\n");
		buf.append("    static final int CONST= 1;\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the constructor

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the inner
	}

	public void testInsert3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    static final int CONST= 1;\n");
		buf.append("    static int fgStatic= 1;\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    class Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the method

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the static

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(4, insertionIndex); // after the method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the constructor

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the inner
	}

	public void testInsert4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    private int fInt;\n");
		buf.append("    private int fInt1;\n");
		buf.append("    private int fInt2;\n");
		buf.append("\n");
		buf.append("    private static final int THREE = 3;\n");
		buf.append("    private static final int FOUR = 4;\n");
		buf.append("    public void foo() {}\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after fInt2

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before normal field

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(6, insertionIndex); // after the method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= ASTNodes.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // after the inner
	}

	private FieldDeclaration createNewField(AST ast, int modifiers) {
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName("newField"));
		FieldDeclaration declaration= ast.newFieldDeclaration(fragment);
		declaration.setType(ast.newPrimitiveType(PrimitiveType.INT));
		declaration.modifiers().addAll(ast.newModifiers(modifiers));
		return declaration;
	}

	private MethodDeclaration createNewMethod(AST ast, int modifiers, boolean isConstructor) {
		MethodDeclaration declaration= ast.newMethodDeclaration();
		declaration.setName(ast.newSimpleName("newMethod"));
		declaration.setReturnType2(ast.newPrimitiveType(PrimitiveType.INT));
		declaration.modifiers().addAll(ast.newModifiers(modifiers));
		declaration.setConstructor(isConstructor);
		return declaration;
	}

	private TypeDeclaration createNewType(AST ast, int modifiers) {
		TypeDeclaration declaration= ast.newTypeDeclaration();
		declaration.setName(ast.newSimpleName("newType"));
		declaration.modifiers().addAll(ast.newModifiers(modifiers));
		return declaration;
	}

	private CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}


}
