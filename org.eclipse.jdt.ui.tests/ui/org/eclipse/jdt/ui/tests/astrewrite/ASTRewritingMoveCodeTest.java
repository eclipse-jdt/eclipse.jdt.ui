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

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.SourceModifier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;

import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class ASTRewritingMoveCodeTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMoveCodeTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingMoveCodeTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingMoveCodeTest("testNestedCopies"));
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
	
	public void testMove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int x;\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List bodyDecls= type.bodyDeclarations();
		
		ASTNode first= (ASTNode) bodyDecls.get(0);
		ASTNode placeholder= rewrite.createMove(first);
		rewrite.markAsInserted(placeholder);
		
		bodyDecls.add(placeholder);
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    int x;\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}		
	
	public void testMoveDeclSameLevelCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			rewrite.markAsRemoved(innerType);
			ASTNode movedNode= rewrite.createCopy(innerType);
			members.add(movedNode);
			rewrite.markAsInserted(movedNode);
			
			Statement toMove;
			Statement toCopy;
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Not expected number of statements", statements.size() == 4);
				
				toMove= (Statement) statements.get(1);
				toCopy= (Statement) statements.get(3);
				
				rewrite.markAsRemoved(toMove);
			}
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
				assertTrue("Cannot find gee()", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Has statements", statements.isEmpty());
				
				ASTNode insertNodeForMove= rewrite.createCopy(toMove);
				ASTNode insertNodeForCopy= rewrite.createCopy(toCopy);
				
				statements.add(insertNodeForCopy);
				statements.add(insertNodeForMove);
				
				rewrite.markAsInserted(insertNodeForMove);
				rewrite.markAsInserted(insertNodeForCopy);
			}	
		}			
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");		
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("        i= 0;\n");
		buf.append("    }\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");			
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMoveDeclSameLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			ASTNode movedNode= rewrite.createMove(innerType);
			members.add(movedNode);
			rewrite.markAsInserted(movedNode);
			
			Statement toMove;
			Statement toCopy;
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Not expected number of statements", statements.size() == 4);
				
				toMove= (Statement) statements.get(1);
				toCopy= (Statement) statements.get(3);
			}
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
				assertTrue("Cannot find gee()", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Has statements", statements.isEmpty());
				
				ASTNode insertNodeForMove= rewrite.createMove(toMove);
				ASTNode insertNodeForCopy= rewrite.createCopy(toCopy);
				
				statements.add(insertNodeForCopy);
				statements.add(insertNodeForMove);
				
				rewrite.markAsInserted(insertNodeForMove);
				rewrite.markAsInserted(insertNodeForCopy);
			}	
		}			
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");		
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("        i= 0;\n");
		buf.append("    }\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");			
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	

	public void testMoveDeclDifferentLevelCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			List innerMembers= innerType.bodyDeclarations();
			assertTrue("Not expected number of inner members", innerMembers.size() == 1);
			
			{ // move outer as inner of inner.
				TypeDeclaration outerType= findTypeDeclaration(astRoot, "G");
				assertTrue("G not found", outerType != null);
				
				rewrite.markAsRemoved(outerType);
				
				ASTNode insertNodeForCopy= rewrite.createCopy(outerType);
				innerMembers.add(insertNodeForCopy);
				rewrite.markAsInserted(insertNodeForCopy);
				
			}
			{ // copy method of inner to main type
				MethodDeclaration methodDecl= (MethodDeclaration) innerMembers.get(0);
				ASTNode insertNodeForMove= rewrite.createCopy(methodDecl);
				members.add(insertNodeForMove);
				rewrite.markAsInserted(insertNodeForMove);
			}
			{ // nest body of constructor in a while statement
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);

				Block body= methodDecl.getBody();

				WhileStatement whileStatement= ast.newWhileStatement();
				whileStatement.setExpression(ast.newBooleanLiteral(true));
				
				Statement insertNodeForCopy= (Statement) rewrite.createCopy(body);
				
				whileStatement.setBody(insertNodeForCopy); // set existing body

				Block newBody= ast.newBlock();
				List newStatements= newBody.statements();				
				newStatements.add(whileStatement);
				
				rewrite.markAsReplaced(body, newBody);
			}			
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("\n");			
		buf.append("        interface G {\n");
		buf.append("        }\n");				
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        while (true) {\n");		
		buf.append("            super();\n");
		buf.append("            i= 0;\n");
		buf.append("            k= 9;\n");
		buf.append("            if (System.out == null) {\n");
		buf.append("                gee(); // cool\n");
		buf.append("            }\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void xee() {\n");
		buf.append("        /* does nothing */\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMoveDeclDifferentLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			List innerMembers= innerType.bodyDeclarations();
			assertTrue("Not expected number of inner members", innerMembers.size() == 1);
			
			{ // move outer as inner of inner.
				TypeDeclaration outerType= findTypeDeclaration(astRoot, "G");
				assertTrue("G not found", outerType != null);
				
				ASTNode insertNodeForCopy= rewrite.createMove(outerType);
				innerMembers.add(insertNodeForCopy);
				rewrite.markAsInserted(insertNodeForCopy);
				
			}
			{ // copy method of inner to main type
				MethodDeclaration methodDecl= (MethodDeclaration) innerMembers.get(0);
				ASTNode insertNodeForMove= rewrite.createCopy(methodDecl);
				members.add(insertNodeForMove);
				rewrite.markAsInserted(insertNodeForMove);
			}
			{ // nest body of constructor in a while statement
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);

				Block body= methodDecl.getBody();

				WhileStatement whileStatement= ast.newWhileStatement();
				whileStatement.setExpression(ast.newBooleanLiteral(true));
				
				Statement insertNodeForCopy= (Statement) rewrite.createCopy(body);
				
				whileStatement.setBody(insertNodeForCopy); // set existing body

				Block newBody= ast.newBlock();
				List newStatements= newBody.statements();				
				newStatements.add(whileStatement);
				
				rewrite.markAsReplaced(body, newBody);
			}			
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("\n");			
		buf.append("        interface G {\n");
		buf.append("        }\n");				
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        while (true) {\n");		
		buf.append("            super();\n");
		buf.append("            i= 0;\n");
		buf.append("            k= 9;\n");
		buf.append("            if (System.out == null) {\n");
		buf.append("                gee(); // cool\n");
		buf.append("            }\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void xee() {\n");
		buf.append("        /* does nothing */\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}

	public void testMoveStatementsCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move first statments inside an ifstatement, move second statment inside a new while statement
		   // that is in the ifstatement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			assertTrue("Cannot find Constructor E", methodDecl != null);

			Block body= methodDecl.getBody();
			List statements= body.statements();
			
			assertTrue("Cannot find if statement", statements.get(3) instanceof IfStatement);
			
			IfStatement ifStatement= (IfStatement) statements.get(3);
			
			Statement insertNodeForCopy1= (Statement) rewrite.createCopy((ASTNode) statements.get(1));
			Statement insertNodeForCopy2= (Statement) rewrite.createCopy((ASTNode) statements.get(2));
			
			Block whileBody= ast.newBlock();
			
			WhileStatement whileStatement= ast.newWhileStatement();
			whileStatement.setExpression(ast.newBooleanLiteral(true));
			whileStatement.setBody(whileBody);
			
			List whileBodyStatements= whileBody.statements();
			whileBodyStatements.add(insertNodeForCopy2);
			
			
			assertTrue("if statement body not a block", ifStatement.getThenStatement() instanceof Block);
			
			List ifBodyStatements= ((Block)ifStatement.getThenStatement()).statements();
			
			ifBodyStatements.add(0, whileStatement);
			ifBodyStatements.add(1, insertNodeForCopy1);
			
			rewrite.markAsInserted(whileStatement);
			rewrite.markAsInserted(insertNodeForCopy1);
			
			rewrite.markAsRemoved((ASTNode) statements.get(1));
			rewrite.markAsRemoved((ASTNode) statements.get(2));
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            while (true) {\n");
		buf.append("                k= 9;\n");
		buf.append("            }\n");		
		buf.append("            i= 0;\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	public void testMoveStatements() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move first statments inside an ifstatement, move second statment inside a new while statement
		   // that is in the ifstatement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			assertTrue("Cannot find Constructor E", methodDecl != null);

			Block body= methodDecl.getBody();
			List statements= body.statements();
			
			assertTrue("Cannot find if statement", statements.get(3) instanceof IfStatement);
			
			IfStatement ifStatement= (IfStatement) statements.get(3);
			
			Statement insertNodeForCopy1= (Statement) rewrite.createMove((ASTNode) statements.get(1));
			Statement insertNodeForCopy2= (Statement) rewrite.createMove((ASTNode) statements.get(2));
			
			Block whileBody= ast.newBlock();
			
			WhileStatement whileStatement= ast.newWhileStatement();
			whileStatement.setExpression(ast.newBooleanLiteral(true));
			whileStatement.setBody(whileBody);
			
			List whileBodyStatements= whileBody.statements();
			whileBodyStatements.add(insertNodeForCopy2);
			
			
			assertTrue("if statement body not a block", ifStatement.getThenStatement() instanceof Block);
			
			List ifBodyStatements= ((Block)ifStatement.getThenStatement()).statements();
			
			ifBodyStatements.add(0, whileStatement);
			ifBodyStatements.add(1, insertNodeForCopy1);
			
			rewrite.markAsInserted(whileStatement);
			rewrite.markAsInserted(insertNodeForCopy1);
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            while (true) {\n");
		buf.append("                k= 9;\n");
		buf.append("            }\n");		
		buf.append("            i= 0;\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void tesCopyFromDeleted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete method foo, but copy if statement to goo
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			rewrite.markAsRemoved(methodDecl);
			
			Block body= methodDecl.getBody();
			List statements= body.statements();
			assertTrue("Cannot find if statement", statements.size() == 1);
			
			ASTNode placeHolder= rewrite.createMove((ASTNode) statements.get(0));
			
			
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			rewrite.markAsInserted(placeHolder);
			
			methodGoo.getBody().statements().add(placeHolder);
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}

	public void testChangesInMoveCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace statement in goo with moved ifStatement from foo. add a node to if statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("Cannot find if statement", fooStatements.size() == 1);
			
			// prepare ifStatement to move
			IfStatement ifStatement= (IfStatement) fooStatements.get(0);
			rewrite.markAsRemoved(ifStatement);
			
			ASTNode placeHolder= rewrite.createCopy(ifStatement);
			
			// add return statement to ifStatement block
			ReturnStatement returnStatement= ast.newReturnStatement();
			rewrite.markAsInserted(returnStatement);

			Block then= (Block) ifStatement.getThenStatement();
			then.statements().add(returnStatement);
	
			// replace statement in goo with moved ifStatement
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			List gooStatements= methodGoo.getBody().statements();
			assertTrue("Cannot find statement in goo", gooStatements.size() == 1);
			
			rewrite.markAsReplaced((ASTNode) gooStatements.get(0), placeHolder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testChangesInMove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace statement in goo with moved ifStatement from foo. add a node to if statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("Cannot find if statement", fooStatements.size() == 1);
			
			// prepare ifStatement to move
			IfStatement ifStatement= (IfStatement) fooStatements.get(0);
			
			ASTNode placeHolder= rewrite.createMove(ifStatement);
			
			// add return statement to ifStatement block
			ReturnStatement returnStatement= ast.newReturnStatement();
			rewrite.markAsInserted(returnStatement);

			Block then= (Block) ifStatement.getThenStatement();
			then.statements().add(returnStatement);
	
			// replace statement in goo with moved ifStatement
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			List gooStatements= methodGoo.getBody().statements();
			assertTrue("Cannot find statement in goo", gooStatements.size() == 1);
			
			rewrite.markAsReplaced((ASTNode) gooStatements.get(0), placeHolder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}

	public void testSwapCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(xoo(/*hello*/), k * 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // swap the two arguments
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("More statements than expected", fooStatements.size() == 1);
			
			ExpressionStatement statement= (ExpressionStatement) fooStatements.get(0);
			MethodInvocation invocation= (MethodInvocation) statement.getExpression();
			
			List arguments= invocation.arguments();
			assertTrue("More arguments than expected", arguments.size() == 2);
			
			ASTNode arg0= (ASTNode) arguments.get(0);
			ASTNode arg1= (ASTNode) arguments.get(1);
			
			ASTNode placeHolder0= rewrite.createCopy(arg0);
			ASTNode placeHolder1= rewrite.createCopy(arg1);
			
			rewrite.markAsReplaced(arg0, placeHolder1);
			rewrite.markAsReplaced(arg1, placeHolder0);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k * 2, xoo(/*hello*/));\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testSwap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(xoo(/*hello*/), k * 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // swap the two arguments
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("More statements than expected", fooStatements.size() == 1);
			
			ExpressionStatement statement= (ExpressionStatement) fooStatements.get(0);
			MethodInvocation invocation= (MethodInvocation) statement.getExpression();
			
			List arguments= invocation.arguments();
			assertTrue("More arguments than expected", arguments.size() == 2);
			
			ASTNode arg0= (ASTNode) arguments.get(0);
			ASTNode arg1= (ASTNode) arguments.get(1);
			
			ASTNode placeHolder0= rewrite.createMove(arg0);
			ASTNode placeHolder1= rewrite.createMove(arg1);
			
			rewrite.markAsReplaced(arg0, placeHolder1); // replace instead of remove
			rewrite.markAsReplaced(arg1, placeHolder0); // replace instead of remove
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k * 2, xoo(/*hello*/));\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	public void testCopyRangeAndInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());

			ASTNode placeholder= rewrite.createCopy(collapsed);
			
			rewrite.markAsInserted(placeholder);
			
			statements.add(placeholder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        i++;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	public void testCopyRangeAndReplace() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			
			ASTNode last= (ASTNode) ifStatementBody.get(ifStatementBody.size() - 1);
			
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());

			ASTNode placeholder= rewrite.createCopy(collapsed);			
			
			ReturnStatement returnStatement= ast.newReturnStatement();
			rewrite.markAsReplaced(last, returnStatement);
			
			rewrite.markAsReplaced(ifStatement, placeholder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        return;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMultipleCopiesOfSameNode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			
			ASTNode node= (ASTNode) ifStatementBody.get(1);
			ASTNode placeholder1= rewrite.createCopy(node);
			rewrite.markAsInserted(placeholder1);
			statements.add(placeholder1);

			ASTNode placeholder2= rewrite.createCopy(node);
			rewrite.markAsReplaced((ASTNode) ifStatementBody.get(0), placeholder2);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        i++; // comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMultipleCopiesOfSameNodeAndMove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			
			ASTNode node= (ASTNode) ifStatementBody.get(1);
			ASTNode placeholder1= rewrite.createCopy(node);
			rewrite.markAsInserted(placeholder1);
			statements.add(placeholder1);

			ASTNode placeholder2= rewrite.createCopy(node);
			rewrite.markAsReplaced((ASTNode) ifStatementBody.get(0), placeholder2);
			
			ASTNode placeholder3= rewrite.createMove(node);
			rewrite.markAsInserted(placeholder3);
			statements.add(0, placeholder3);			
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i++; // comment\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        i++; // comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	
	
	public void testMoveCollapsedCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode placeholder= rewrite.createCopy(collapsed);
			rewrite.markAsRemoved(collapsed);
			
			statements.add(placeholder);
			rewrite.markAsInserted(placeholder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("        }\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        i++;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMoveCollapsed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode placeholder= rewrite.createMove(collapsed);
			
			statements.add(placeholder);
			rewrite.markAsInserted(placeholder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("        }\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        i++;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}		
	
	public void testMoveForStatementToForBlockCD() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++)\n");
		buf.append("            foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			ForStatement forStatement= (ForStatement) statements.get(0);
			Statement body= forStatement.getBody();
			
			ASTNode placeholder= rewrite.createCopy(body);
			
			Block newBody= ast.newBlock();
			newBody.statements().add(placeholder);
			
			rewrite.markAsReplaced(body, newBody);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++) {\n");
		buf.append("            foo();\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMoveForStatementToForBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++)\n");
		buf.append("            foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			ForStatement forStatement= (ForStatement) statements.get(0);
			Statement body= forStatement.getBody();
			
			ASTNode placeholder= rewrite.createMove(body);
			
			Block newBody= ast.newBlock();
			newBody.statements().add(placeholder);
			
			rewrite.markAsReplaced(body, newBody);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++) {\n");
		buf.append("            foo();\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	public void testReplaceCollapsed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode newStatement= ast.newReturnStatement();
			rewrite.markAsReplaced(collapsed, newStatement);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            return;\n");		
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	

	public void testRemoveIndents() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");

		TextBuffer buffer= TextBuffer.create(buf.toString());
		
		int start= buf.toString().indexOf("while");
		int end= buf.toString().indexOf("return;") + "return;".length();
		
		IRegion range= new Region(start, end - start);
		String content= buffer.getContent(range.getOffset(), range.getLength());
		SourceModifier modifier= new SourceModifier(2, "    ", 4);
		MultiTextEdit edit= new MultiTextEdit(0, content.length());
		ReplaceEdit[] replaces= modifier.getModifications(content);
		for (int i= 0; i < replaces.length; i++) {
			edit.addChild(replaces[i]);
		}
		
		TextBuffer innerBuffer= TextBuffer.create(content);
		TextBufferEditor innerEditor= new TextBufferEditor(innerBuffer);
		innerEditor.add(edit);
		innerEditor.performEdits(null);
		assertTrue("Can perform edits", innerEditor.canPerformEdits());
		
		buffer.replace(range, innerBuffer.getContent());
				
		String preview= buffer.getContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        i++;\n");
		buf.append("    }\n");
		buf.append("    return;\n");
		buf.append("    }\n");
		buf.append("}\n");		
		String expected= buf.toString();		

		assertEqualString(preview, expected);
	}
	
	public void testAddIndents() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");

		TextBuffer buffer= TextBuffer.create(buf.toString());
		
		int start= buf.toString().indexOf("while");
		int end= buf.toString().indexOf("return;") + "return;".length();
		
		IRegion range= new Region(start, end - start);
		String content= buffer.getContent(range.getOffset(), range.getLength());
		SourceModifier modifier= new SourceModifier(2, "            ", 4);
		MultiTextEdit edit= new MultiTextEdit(0, content.length());
		ReplaceEdit[] replaces= modifier.getModifications(content);
		for (int i= 0; i < replaces.length; i++) {
			edit.addChild(replaces[i]);
		}
		
		TextBuffer innerBuffer= TextBuffer.create(content);
		TextBufferEditor innerEditor= new TextBufferEditor(innerBuffer);
		innerEditor.add(edit);
		innerEditor.performEdits(null);
		assertTrue("Can perform edits", innerEditor.canPerformEdits());
		
		buffer.replace(range, innerBuffer.getContent());
		
		String preview= buffer.getContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("                foo();\n");
		buf.append("                i++; // comment\n");
		buf.append("                i++;\n");
		buf.append("            }\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("}\n");		
		String expected= buf.toString();		

		assertEqualString(preview, expected);
	}
	
	public void testNestedCopies() throws Exception {
		// Disabled. Is a text edit failure.
		/*
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        int i= (String) o.indexOf('1');\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		String str= "(String) o.indexOf('1')";
		CastExpression expression= (CastExpression) NodeFinder.perform(astRoot, buf.indexOf(str), str.length());
		MethodInvocation invocation= (MethodInvocation) expression.getExpression();
		
		CastExpression newCast= ast.newCastExpression();
		newCast.setType((Type) rewrite.createCopy(expression.getType()));
		newCast.setExpression((Expression) rewrite.createCopy(invocation.getExpression()));
		ParenthesizedExpression parents= ast.newParenthesizedExpression();
		parents.setExpression(newCast);
		
		ASTNode node= rewrite.createCopy(invocation);
		rewrite.markAsReplaced(expression, node);
		rewrite.markAsReplaced(invocation.getExpression(), parents);
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        int i= ((String) o).indexOf('1');\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();		

		assertEqualString(preview, expected);
		*/
	}
	
}
