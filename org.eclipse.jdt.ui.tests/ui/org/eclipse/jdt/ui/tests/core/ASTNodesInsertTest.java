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

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class ASTNodesInsertTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {

		fJProject1= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testInsert1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str= """
			package test1.ae;
			public class E {
			    int[] fGlobal;
			    public void goo(int param1, int param2) {
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List<BodyDeclaration> bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the first field

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the normal method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // before the normal method

		declaration= createNewType(ast, 0);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before all

	}

	@Test
	public void testInsert2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str= """
			package test1.ae;
			public class E {
			    class Inner {
			    }
			    static final int CONST= 1;
			    public E() {
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List<BodyDeclaration> bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the constructor

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the inner
	}

	@Test
	public void testInsert3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str= """
			package test1.ae;
			public class E {
			    static final int CONST= 1;
			    static int fgStatic= 1;
			    public E() {
			    }
			    public void foo() {
			    }
			    class Inner {
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List<BodyDeclaration> bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the method

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // after the static

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(1, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(4, insertionIndex); // after the method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(2, insertionIndex); // before the constructor

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the inner
	}

	@Test
	public void testInsert4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str= """
			package test1.ae;
			public class E {
			    private int fInt;
			    private int fInt1;
			    private int fInt2;
			
			    private static final int THREE = 3;
			    private static final int FOUR = 4;
			    public void foo() {}
			
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);

		TypeDeclaration typeDecl= (TypeDeclaration) astRoot.types().get(0);
		List<BodyDeclaration> bodyDecls= typeDecl.bodyDeclarations();

		AST ast = astRoot.getAST();

		BodyDeclaration declaration= createNewField(ast, Modifier.PRIVATE);
		int insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(3, insertionIndex); // after fInt2

		declaration= createNewField(ast, Modifier.STATIC);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before normal field

		declaration= createNewField(ast, Modifier.STATIC | Modifier.FINAL);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the const

		declaration= createNewMethod(ast, Modifier.PRIVATE, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(6, insertionIndex); // after the method

		declaration= createNewMethod(ast, Modifier.STATIC, false);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(0, insertionIndex); // before the normal field

		declaration= createNewMethod(ast, 0, true);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
		assertEquals(5, insertionIndex); // after the constructor

		declaration= createNewType(ast, 0);
		insertionIndex= BodyDeclarationRewrite.getInsertionIndex(declaration, bodyDecls);
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
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

}
