/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.astrewrite;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;

public class ASTRewritingTrackingTest extends ASTRewritingTest {

	private static final Class THIS= ASTRewritingTrackingTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	
	

	public ASTRewritingTrackingTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingTrackingTest("testNamesWithPlaceholder"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	private TextEditGroup getEditGroup(List all, String name) {
		TextEditGroup desc= new TextEditGroup(name);
		all.add(desc);
		return desc;
	}
	
	
	public void testNamesWithDelete() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");		
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		rewrite.markAsRemoved(field);
						
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}
	
	private void assertCorrectTracking(TextEditGroup[] descriptions, String expected) {
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= TextEdit.getCoverage(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
	}

	public void testNamesWithInsert() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getEditGroup(gd, "x1"));
		
		VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
		newFrag.setName(ast.newSimpleName("newVariable"));
		newFrag.setExtraDimensions(2);

		rewrite.getListRewrite(field, FieldDeclaration.FRAGMENTS_PROPERTY).insertFirst(newFrag, null);

						
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int newVariable[][], x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}
	
	public void testNamesWithReplace() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		SimpleName newName= ast.newSimpleName("XX");
		rewrite.markAsReplaced(typeC.getName(), newName);
		rewrite.markAsTracked(newName,  getEditGroup(gd, "XX"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getEditGroup(gd, "i"));
				
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getEditGroup(gd, "x1"));
		
		// change modifier
		int newModifiers= Modifier.STATIC | Modifier.TRANSIENT | Modifier.PRIVATE;
		rewrite.markAsReplaced(field, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
								
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class XX {\n");
		buf.append("\n");
		buf.append("    private static transient int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}
	
	public void testNamesWithMove1() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getEditGroup(gd, "i"));
					
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getEditGroup(gd, "x1"));

		// move method before field
		ASTNode placeHolder= rewrite.createMovePlaceholder(method);
		rewrite.getListRewrite(typeC, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(placeHolder, null);
								
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}
	
	public void testNamesWithMove2() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(0);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getEditGroup(gd, "i"));
					

		// move method before field
		ASTNode placeHolder= rewrite.createMovePlaceholder(whileStatement);
		
		TryStatement tryStatement= ast.newTryStatement();
		tryStatement.getBody().statements().add(placeHolder);
		tryStatement.setFinally(ast.newBlock());
		rewrite.markAsReplaced(whileStatement, tryStatement);
								
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        try {\n");
		buf.append("            while (i == 0) {\n");
		buf.append("                ++i;\n");
		buf.append("            }\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}	
	
	public void testNamesWithMove3() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		// move method before field
		ASTNode placeHolder= rewrite.createMovePlaceholder(method);
		
		rewrite.getListRewrite(typeC, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(placeHolder, null);
								
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}
	public void testNamesWithPlaceholder() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public String foo(Object s) {\n");
		buf.append("        return s;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(ast);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getEditGroup(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(0);
		rewrite.markAsTracked(method.getName(), getEditGroup(gd, "foo"));
		
		ReturnStatement returnStatement= (ReturnStatement) method.getBody().statements().get(0);
		
		CastExpression castExpression= ast.newCastExpression();
		Type type= (Type) rewrite.createStringPlaceholder("String", NewASTRewrite.TYPE);
		Expression expression= (Expression) rewrite.createMovePlaceholder(returnStatement.getExpression());
		castExpression.setType(type);
		castExpression.setExpression(expression);
		
		rewrite.markAsReplaced(returnStatement.getExpression(), castExpression);
		
		rewrite.markAsTracked(type, getEditGroup(gd, "String"));
		rewrite.markAsTracked(expression, getEditGroup(gd, "s"));
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public String foo(Object s) {\n");
		buf.append("        return (String) s;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(preview, expected);
		
		TextEditGroup[] descriptions= (TextEditGroup[]) gd.toArray(new TextEditGroup[gd.size()]);
		assertCorrectTracking(descriptions, expected);

	}	

	
}



