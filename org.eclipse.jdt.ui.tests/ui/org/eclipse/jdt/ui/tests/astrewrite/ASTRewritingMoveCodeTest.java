/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;

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
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingMoveCodeTest("testCollapsedTargetNodes2"));
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List bodyDecls= type.bodyDeclarations();
		
		ASTNode first= (ASTNode) bodyDecls.get(0);
		ASTNode placeholder= rewrite.createMovePlaceholder(first);
		
		rewrite.getListRewrite(type, ASTNodeConstants.BODY_DECLARATIONS).insertLast(placeholder, null);
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    int x;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			rewrite.markAsRemoved(innerType);
			ASTNode movedNode= rewrite.createCopyPlaceholder(innerType);

			rewrite.getListRewrite(type, ASTNodeConstants.BODY_DECLARATIONS).insertLast(movedNode, null);
			
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
				
				ASTNode insertNodeForMove= rewrite.createCopyPlaceholder(toMove);
				ASTNode insertNodeForCopy= rewrite.createCopyPlaceholder(toCopy);
				
				rewrite.getListRewrite(body, ASTNodeConstants.STATEMENTS).insertLast(insertNodeForCopy, null);
				rewrite.getListRewrite(body, ASTNodeConstants.STATEMENTS).insertLast(insertNodeForMove, null);
				
			}	
		}			
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			ASTNode movedNode= rewrite.createMovePlaceholder(innerType);
			rewrite.getListRewrite(type, ASTNodeConstants.BODY_DECLARATIONS).insertLast(movedNode, null);

			
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
				
				ASTNode insertNodeForMove= rewrite.createMovePlaceholder(toMove);
				ASTNode insertNodeForCopy= rewrite.createCopyPlaceholder(toCopy);
				
				rewrite.getListRewrite(body, ASTNodeConstants.STATEMENTS).insertLast(insertNodeForCopy, null);
				rewrite.getListRewrite(body, ASTNodeConstants.STATEMENTS).insertLast(insertNodeForMove, null);
			}	
		}			
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
				
				ASTNode insertNodeForCopy= rewrite.createCopyPlaceholder(outerType);
				
				rewrite.getListRewrite(innerType, ASTNodeConstants.BODY_DECLARATIONS).insertLast(insertNodeForCopy, null);

				
			}
			{ // copy method of inner to main type
				MethodDeclaration methodDecl= (MethodDeclaration) innerMembers.get(0);
				ASTNode insertNodeForMove= rewrite.createCopyPlaceholder(methodDecl);
				
				rewrite.getListRewrite(type, ASTNodeConstants.BODY_DECLARATIONS).insertLast(insertNodeForMove, null);

			}
			{ // nest body of constructor in a while statement
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);

				Block body= methodDecl.getBody();

				WhileStatement whileStatement= ast.newWhileStatement();
				whileStatement.setExpression(ast.newBooleanLiteral(true));
				
				Statement insertNodeForCopy= (Statement) rewrite.createCopyPlaceholder(body);
				
				whileStatement.setBody(insertNodeForCopy); // set existing body

				Block newBody= ast.newBlock();
				List newStatements= newBody.statements();				
				newStatements.add(whileStatement);
				
				rewrite.markAsReplaced(body, newBody);
			}			
		}	
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
				
				ASTNode insertNodeForCopy= rewrite.createMovePlaceholder(outerType);
				
				rewrite.getListRewrite(innerType, ASTNodeConstants.BODY_DECLARATIONS).insertLast(insertNodeForCopy, null);
			}
			{ // copy method of inner to main type
				MethodDeclaration methodDecl= (MethodDeclaration) innerMembers.get(0);
				ASTNode insertNodeForMove= rewrite.createCopyPlaceholder(methodDecl);
				
				rewrite.getListRewrite(type, ASTNodeConstants.BODY_DECLARATIONS).insertLast(insertNodeForMove, null);
			}
			{ // nest body of constructor in a while statement
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);

				Block body= methodDecl.getBody();

				WhileStatement whileStatement= ast.newWhileStatement();
				whileStatement.setExpression(ast.newBooleanLiteral(true));
				
				Statement insertNodeForCopy= (Statement) rewrite.createCopyPlaceholder(body);
				
				whileStatement.setBody(insertNodeForCopy); // set existing body

				Block newBody= ast.newBlock();
				List newStatements= newBody.statements();				
				newStatements.add(whileStatement);
				
				rewrite.markAsReplaced(body, newBody);
			}			
		}	
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			Statement insertNodeForCopy1= (Statement) rewrite.createCopyPlaceholder((ASTNode) statements.get(1));
			Statement insertNodeForCopy2= (Statement) rewrite.createCopyPlaceholder((ASTNode) statements.get(2));
			
			Block whileBody= ast.newBlock();
			
			WhileStatement whileStatement= ast.newWhileStatement();
			whileStatement.setExpression(ast.newBooleanLiteral(true));
			whileStatement.setBody(whileBody);
			
			List whileBodyStatements= whileBody.statements();
			whileBodyStatements.add(insertNodeForCopy2);
			
			
			assertTrue("if statement body not a block", ifStatement.getThenStatement() instanceof Block);
			
			
			Block block= (Block)ifStatement.getThenStatement();
			
			rewrite.getListRewrite(block, ASTNodeConstants.STATEMENTS).insertFirst(whileStatement, null);
			rewrite.getListRewrite(block, ASTNodeConstants.STATEMENTS).insertAfter(insertNodeForCopy1, whileStatement, null);
			
			rewrite.markAsRemoved((ASTNode) statements.get(1));
			rewrite.markAsRemoved((ASTNode) statements.get(2));
		}	
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			Statement insertNodeForCopy1= (Statement) rewrite.createMovePlaceholder((ASTNode) statements.get(1));
			Statement insertNodeForCopy2= (Statement) rewrite.createMovePlaceholder((ASTNode) statements.get(2));
			
			Block whileBody= ast.newBlock();
			
			WhileStatement whileStatement= ast.newWhileStatement();
			whileStatement.setExpression(ast.newBooleanLiteral(true));
			whileStatement.setBody(whileBody);
			
			List whileBodyStatements= whileBody.statements();
			whileBodyStatements.add(insertNodeForCopy2);
			
			
			assertTrue("if statement body not a block", ifStatement.getThenStatement() instanceof Block);
						
			Block block= (Block) ifStatement.getThenStatement();
			rewrite.getListRewrite(block, ASTNodeConstants.STATEMENTS).insertFirst(whileStatement, null);
			rewrite.getListRewrite(block, ASTNodeConstants.STATEMENTS).insertAfter(insertNodeForCopy1, whileStatement, null);
		}	
					

		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete method foo, but copy if statement to goo
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			rewrite.markAsRemoved(methodDecl);
			
			Block body= methodDecl.getBody();
			List statements= body.statements();
			assertTrue("Cannot find if statement", statements.size() == 1);
			
			ASTNode placeHolder= rewrite.createMovePlaceholder((ASTNode) statements.get(0));
			
			
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			rewrite.getListRewrite(methodGoo.getBody(), ASTNodeConstants.STATEMENTS).insertLast(placeHolder, null);
		}	
					

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder(ifStatement);
			
			// add return statement to ifStatement block
			ReturnStatement returnStatement= ast.newReturnStatement();
			Block then= (Block) ifStatement.getThenStatement();
			rewrite.getListRewrite(then, ASTNodeConstants.STATEMENTS).insertLast(returnStatement, null);
	
			// replace statement in goo with moved ifStatement
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			List gooStatements= methodGoo.getBody().statements();
			assertTrue("Cannot find statement in goo", gooStatements.size() == 1);
			
			rewrite.markAsReplaced((ASTNode) gooStatements.get(0), placeHolder);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			ASTNode placeHolder= rewrite.createMovePlaceholder(ifStatement);
			
			// add return statement to ifStatement block
			ReturnStatement returnStatement= ast.newReturnStatement();

			Block then= (Block) ifStatement.getThenStatement();
			rewrite.getListRewrite(then, ASTNodeConstants.STATEMENTS).insertLast(returnStatement, null);

	
			// replace statement in goo with moved ifStatement
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			List gooStatements= methodGoo.getBody().statements();
			assertTrue("Cannot find statement in goo", gooStatements.size() == 1);
			
			rewrite.markAsReplaced((ASTNode) gooStatements.get(0), placeHolder);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			ASTNode placeHolder0= rewrite.createCopyPlaceholder(arg0);
			ASTNode placeHolder1= rewrite.createCopyPlaceholder(arg1);
			
			rewrite.markAsReplaced(arg0, placeHolder1);
			rewrite.markAsReplaced(arg1, placeHolder0);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k * 2, xoo(/*hello*/));\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
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
			
			ASTNode placeHolder0= rewrite.createMovePlaceholder(arg0);
			ASTNode placeHolder1= rewrite.createMovePlaceholder(arg1);
			
			rewrite.markAsReplaced(arg0, placeHolder1); // replace instead of remove
			rewrite.markAsReplaced(arg1, placeHolder0); // replace instead of remove
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k * 2, xoo(/*hello*/));\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			
			ASTNode node= (ASTNode) ifStatementBody.get(1);
			ASTNode placeholder1= rewrite.createCopyPlaceholder(node);
			rewrite.getListRewrite(methodDecl.getBody(), ASTNodeConstants.STATEMENTS).insertLast(placeholder1, null);

			ASTNode placeholder2= rewrite.createCopyPlaceholder(node);
			rewrite.markAsReplaced((ASTNode) ifStatementBody.get(0), placeholder2);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			
			ASTNode node= (ASTNode) ifStatementBody.get(1);
			ASTNode placeholder1= rewrite.createCopyPlaceholder(node);
			rewrite.getListRewrite(methodDecl.getBody(), ASTNodeConstants.STATEMENTS).insertLast(placeholder1, null);

			ASTNode placeholder2= rewrite.createCopyPlaceholder(node);
			rewrite.markAsReplaced((ASTNode) ifStatementBody.get(0), placeholder2);
			
			ASTNode placeholder3= rewrite.createMovePlaceholder(node);
			rewrite.getListRewrite(methodDecl.getBody(), ASTNodeConstants.STATEMENTS).insertFirst(placeholder3, null);

		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			ForStatement forStatement= (ForStatement) statements.get(0);
			Statement body= forStatement.getBody();
			
			ASTNode placeholder= rewrite.createCopyPlaceholder(body);
			
			Block newBody= ast.newBlock();
			newBody.statements().add(placeholder);
			
			rewrite.markAsReplaced(body, newBody);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++) {\n");
		buf.append("            foo();\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			ForStatement forStatement= (ForStatement) statements.get(0);
			Statement body= forStatement.getBody();
			
			ASTNode placeholder= rewrite.createMovePlaceholder(body);
			
			Block newBody= ast.newBlock();
			newBody.statements().add(placeholder);
			
			rewrite.markAsReplaced(body, newBody);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for(int i= 0; i < 8; i++) {\n");
		buf.append("            foo();\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}	
		
	public void testNestedCopies() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        int i= (String) o.indexOf('1');\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false, null, null);
		AST ast= astRoot.getAST();
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		TypeDeclaration typeDecl= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methDecl= findMethodDeclaration(typeDecl, "foo");
		VariableDeclarationStatement varStat= (VariableDeclarationStatement) methDecl.getBody().statements().get(0);
		
		CastExpression expression= (CastExpression) ((VariableDeclarationFragment) varStat.fragments().get(0)).getInitializer();
		MethodInvocation invocation= (MethodInvocation) expression.getExpression();
		
		CastExpression newCast= ast.newCastExpression();
		newCast.setType((Type) rewrite.createCopyPlaceholder(expression.getType()));
		newCast.setExpression((Expression) rewrite.createCopyPlaceholder(invocation.getExpression()));
		ParenthesizedExpression parents= ast.newParenthesizedExpression();
		parents.setExpression(newCast);
		
		ASTNode node= rewrite.createCopyPlaceholder(invocation);
		rewrite.markAsReplaced(expression, node);
		rewrite.markAsReplaced(invocation.getExpression(), parents);
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        int i= ((String) o).indexOf('1');\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();		

		assertEqualString(preview, expected);
	}
	

}
