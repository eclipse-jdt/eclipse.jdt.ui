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
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;

/**
 * @deprecated The new {@link org.eclipse.jdt.core.dom.rewrite.ASTRewrite} does not support collapsing of nodes
 */
public class ASTRewritingCollapseTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingCollapseTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingCollapseTest(String name) {
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
			suite.addTest(new ASTRewritingCollapseTest("testCollapsedTargetNodes2"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
// tests for collapse	
	
	
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
		
		CompilationUnit astRoot= createAST(cu);
		OldASTRewrite rewrite= new OldASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode placeholder= rewrite.createCopyTarget(collapsed);
			rewrite.remove(collapsed, null);
			
			rewrite.getListRewrite(methodDecl.getBody(), Block.STATEMENTS_PROPERTY).insertLast(placeholder, null);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= createAST(cu);
		OldASTRewrite rewrite= new OldASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode placeholder= rewrite.createMoveTarget(collapsed);
			rewrite.getListRewrite(methodDecl.getBody(), Block.STATEMENTS_PROPERTY).insertLast(placeholder, null);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
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
		assertEqualString(preview, buf.toString());

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
		
		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		OldASTRewrite rewrite= new OldASTRewrite(astRoot);
		assertTrue("Code has errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		{
			List statements= methodDecl.getBody().statements();
			IfStatement ifStatement= (IfStatement) statements.get(0);
			List ifStatementBody= ((Block) ifStatement.getThenStatement()).statements();
			ASTNode collapsed= rewrite.collapseNodes(ifStatementBody, 0, ifStatementBody.size());
			
			ASTNode newStatement= ast.newReturnStatement();
			rewrite.replace(collapsed, newStatement, null);
		}	
					
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            return;\n");		
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}	

	
}
