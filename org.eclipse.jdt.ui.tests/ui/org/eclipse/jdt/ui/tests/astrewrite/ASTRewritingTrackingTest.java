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
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

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
			suite.addTest(new ASTRewritingTrackingTest("testNamesWithMove2"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), "C");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), "foo");
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		rewrite.markAsRemoved(field);
						
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), "C");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), "foo");
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(), "x1");
		
		VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
		newFrag.setName(ast.newSimpleName("newVariable"));
		newFrag.setExtraDimensions(2);
		rewrite.markAsInserted(newFrag);
		fragments.add(0, newFrag);
						
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		SimpleName newName= ast.newSimpleName("XX");
		rewrite.markAsReplaced(typeC.getName(), newName);
		rewrite.markAsTracked(newName, "XX");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), "foo");
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(), "i");
				
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(), "x1");
		
		// change modifier
		FieldDeclaration modifiedField= ast.newFieldDeclaration(ast.newVariableDeclarationFragment());
		modifiedField.setModifiers(Modifier.STATIC | Modifier.TRANSIENT | Modifier.PRIVATE);
		rewrite.markAsModified(field, modifiedField);
		
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), "C");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), "foo");
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(), "i");
					
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(), "x1");

		// move method before field
		ASTNode placeHolder= rewrite.createMove(method);
		rewrite.markAsInserted(placeHolder);
		decls.add(0, placeHolder);				
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			assertTrue("undefined range for " + name, range.getOffset() > -1);
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), "C");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(0);
		rewrite.markAsTracked(method.getName(), "foo");
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(), "i");
					

		// move method before field
		ASTNode placeHolder= rewrite.createMove(whileStatement);
		
		TryStatement tryStatement= ast.newTryStatement();
		tryStatement.getBody().statements().add(placeHolder);
		tryStatement.setFinally(ast.newBlock());
		rewrite.markAsReplaced(whileStatement, tryStatement);
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			assertTrue("undefined range for " + name, range.getOffset() > -1);
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
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
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), "C");
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), "foo");
		
		// move method before field
		ASTNode placeHolder= rewrite.createMove(method);
		rewrite.markAsInserted(placeHolder);
		decls.add(0, placeHolder);				
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= change.getGroupDescriptions();
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			TextRange range= change.getNewTextRange(descriptions[i].getTextEdits());
			assertTrue("undefined range for " + name, range.getOffset() > -1);
			String string= buf.substring(range.getOffset(), range.getExclusiveEnd());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	
	
}



