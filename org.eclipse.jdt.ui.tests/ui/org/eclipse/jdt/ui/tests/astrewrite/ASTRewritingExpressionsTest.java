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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;

public class ASTRewritingExpressionsTest extends ASTRewritingTest {
	private static final Class THIS= ASTRewritingExpressionsTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingExpressionsTest(String name) {
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
			suite.addTest(new ASTRewritingExpressionsTest("testThisExpression"));
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

	
	
	public void testArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int[] o= new int[] { 1, 2, 3 };\n");		
		buf.append("    public void foo() {\n");
		buf.append("        o[3 /* comment*/ - 1]= this.o[3 - 1];\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{	// replace left hand side index, replace right hand side index by left side index
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			ArrayAccess left= (ArrayAccess) assignment.getLeftHandSide();
			ArrayAccess right= (ArrayAccess) assignment.getRightHandSide();
			
			NumberLiteral name= ast.newNumberLiteral("1");
			rewrite.markAsReplaced(left.getIndex(), name);
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder(left.getIndex());
			rewrite.markAsReplaced(right.getIndex(), placeHolder);
			
			SimpleName newName= ast.newSimpleName("o");
			rewrite.markAsReplaced(right.getArray(), newName);
		}

				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int[] o= new int[] { 1, 2, 3 };\n");		
		buf.append("    public void foo() {\n");
		buf.append("        o[1]= o[3 /* comment*/ - 1];\n");
		buf.append("    }\n");
		buf.append("}\n");	
			
		assertEqualString(preview, buf.toString());
	}


	public void testArrayCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(new int[] { 1, 2, 3 },\n");
		buf.append("        new int[] { 1, 2, 3 },\n");
		buf.append("        new int[2][][],\n");
		buf.append("        new int[2][][],\n");
		buf.append("        new int[2][][],\n");
		buf.append("        new int[2][][]);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		ExpressionStatement statement= (ExpressionStatement) statements.get(0);
		MethodInvocation invocation= (MethodInvocation) statement.getExpression();
		List args= invocation.arguments();
		assertTrue("Number of arguments not 6", args.size() == 6);
		
		{	// replace the element type and increase the dimension
			ArrayCreation arrayCreation= (ArrayCreation) args.get(0);
			ArrayType arrayType= arrayCreation.getType();
			
			PrimitiveType floatType= ast.newPrimitiveType(PrimitiveType.FLOAT); 
			ArrayType newArrayType= ast.newArrayType(floatType, 2);
			
			rewrite.markAsReplaced(arrayType, newArrayType);
		}
		{	// remove the initializer, add a dimension expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(1);
			rewrite.markAsRemoved(arrayCreation.getInitializer());
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 0", dimensions.size() == 0);
			
			NumberLiteral literal= ast.newNumberLiteral("10");
			
			rewrite.getListRewrite(arrayCreation, ArrayCreation.DIMENSIONS_PROPERTY).insertLast(literal, null);
		}
		{	// remove all dimension except one, no dimension expression
			// insert the initializer: formatter problems
			ArrayCreation arrayCreation= (ArrayCreation) args.get(2);
			ArrayType arrayType= arrayCreation.getType();			
			PrimitiveType intType= ast.newPrimitiveType(PrimitiveType.INT); 
			ArrayType newArrayType= ast.newArrayType(intType, 1);
			
			rewrite.markAsReplaced(arrayType, newArrayType);
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);
			
			rewrite.markAsRemoved((ASTNode) dimensions.get(0));
			
			ArrayInitializer initializer= ast.newArrayInitializer();
			List expressions= initializer.expressions();
			expressions.add(ast.newNumberLiteral("10"));
		}
		{	// add 2 dimension expressions
			ArrayCreation arrayCreation= (ArrayCreation) args.get(3);
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);
			
			NumberLiteral literal1= ast.newNumberLiteral("10");
			rewrite.getListRewrite(arrayCreation, ArrayCreation.DIMENSIONS_PROPERTY).insertLast(literal1, null);

			NumberLiteral literal2= ast.newNumberLiteral("11");
			rewrite.getListRewrite(arrayCreation, ArrayCreation.DIMENSIONS_PROPERTY).insertLast(literal2, null);

		}
		{	// add 2 empty dimensions
			ArrayCreation arrayCreation= (ArrayCreation) args.get(4);
			ArrayType arrayType= arrayCreation.getType();
			assertTrue("Number of dimension not 3", arrayType.getDimensions() == 3);
			
			PrimitiveType intType= ast.newPrimitiveType(PrimitiveType.INT); 
			ArrayType newArrayType= ast.newArrayType(intType, 5);
			
			rewrite.markAsReplaced(arrayType, newArrayType);
		}
		{	// replace dimension expression, add a dimension expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(5);

			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			rewrite.markAsReplaced((ASTNode) dimensions.get(0), literal1);
			
			NumberLiteral literal2= ast.newNumberLiteral("11");
			rewrite.getListRewrite(arrayCreation, ArrayCreation.DIMENSIONS_PROPERTY).insertLast(literal2, null);

		}			
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(new float[][] { 1, 2, 3 },\n");
		buf.append("        new int[10],\n");
		buf.append("        new int[],\n");
		buf.append("        new int[2][10][11],\n");
		buf.append("        new int[2][][][][],\n");
		buf.append("        new int[10][11][]);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}

	public void testArrayInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(new int[] { 1, 2, 3 },\n");
		buf.append("        new int[] { 1, 2, 3 },\n");
		buf.append("        new int[] { 1, 2, 3 });\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		ExpressionStatement statement= (ExpressionStatement) statements.get(0);
		MethodInvocation invocation= (MethodInvocation) statement.getExpression();
		List args= invocation.arguments();
	
		{	// remove first and last initializer expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(0);
			ArrayInitializer initializer= arrayCreation.getInitializer();
			
			List expressions= initializer.expressions();
			assertTrue("Number of initializer expressions not 3", expressions.size() == 3);
			
			rewrite.markAsRemoved((ASTNode) expressions.get(0));
			rewrite.markAsRemoved((ASTNode) expressions.get(2));
		}
		{	// insert at second and last position
			ArrayCreation arrayCreation= (ArrayCreation) args.get(1);
			ArrayInitializer initializer= arrayCreation.getInitializer();
			
			List expressions= initializer.expressions();
			assertTrue("Number of initializer expressions not 3", expressions.size() == 3);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			rewrite.getListRewrite(initializer, ArrayInitializer.EXPRESSIONS_PROPERTY).insertAfter(literal1, (ASTNode) expressions.get(0), null);
			
			NumberLiteral literal2= ast.newNumberLiteral("11");
			rewrite.getListRewrite(initializer, ArrayInitializer.EXPRESSIONS_PROPERTY).insertLast(literal2, null);

		}		
		{	// replace first and last initializer expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(2);
			ArrayInitializer initializer= arrayCreation.getInitializer();
			
			List expressions= initializer.expressions();
			assertTrue("Number of initializer expressions not 3", expressions.size() == 3);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			NumberLiteral literal2= ast.newNumberLiteral("11");
			
			rewrite.markAsReplaced((ASTNode) expressions.get(0), literal1);
			rewrite.markAsReplaced((ASTNode) expressions.get(2), literal2);
		}		
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(new int[] { 2 },\n");
		buf.append("        new int[] { 1, 10, 2, 3, 11 },\n");
		buf.append("        new int[] { 10, 2, 11 });\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	
	public void testAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i, j;\n");
		buf.append("        i= 0;\n");
		buf.append("        i-= j= 3;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 3);
		{ // change left side & right side
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			SimpleName name= ast.newSimpleName("j");
			rewrite.markAsReplaced(assignment.getLeftHandSide(), name);
			
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName("goo"));
			invocation.setExpression(ast.newSimpleName("other"));
			
			rewrite.markAsReplaced(assignment.getRightHandSide(), invocation);
		}
		{ // change operator and operator of inner
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			rewrite.markAsReplaced(assignment, Assignment.OPERATOR_PROPERTY, Assignment.Operator.DIVIDE_ASSIGN, null);
			
			Assignment inner= (Assignment) assignment.getRightHandSide();
						
			rewrite.markAsReplaced(inner, Assignment.OPERATOR_PROPERTY, Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, null);
		}
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i, j;\n");
		buf.append("        j= other.goo();\n");
		buf.append("        i/= j>>>= 3;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}

	public void testCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= (E) clone();\n");
		buf.append("        z= y.toList();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 2", statements.size() == 2);
		{ // change cast type and cast expression
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			CastExpression expression= (CastExpression) assignment.getRightHandSide();
			SimpleType newType= ast.newSimpleType(ast.newSimpleName("SuperE"));
			rewrite.markAsReplaced(expression.getType(), newType);
			
			SimpleName newExpression= ast.newSimpleName("a");
			rewrite.markAsReplaced(expression.getExpression(), newExpression);
		}
		{ // create cast
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			Expression rightHand= assignment.getRightHandSide();
			
			Expression placeholder= (Expression) rewrite.createCopyPlaceholder(rightHand);
			
			CastExpression newCastExpression= ast.newCastExpression();
			newCastExpression.setType(ast.newSimpleType(ast.newSimpleName("List")));
			newCastExpression.setExpression(placeholder);
			
			rewrite.markAsReplaced(rightHand, newCastExpression);
		}	
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= (SuperE) a;\n");
		buf.append("        z= (List) y.toList();\n");	
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	
	public void testCastExpression_bug28824() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        z= foo().y.toList();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // create cast
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			Expression rightHand= assignment.getRightHandSide();
			
			String rightHandString= cu.getBuffer().getText(rightHand.getStartPosition(), rightHand.getLength());
			assertEqualString(rightHandString, "foo().y.toList()");
			
			Expression placeholder= (Expression) rewrite.createCopyPlaceholder(rightHand);
			
			CastExpression newCastExpression= ast.newCastExpression();
			newCastExpression.setType(ast.newSimpleType(ast.newSimpleName("List")));
			newCastExpression.setExpression(placeholder);
			
			rewrite.markAsReplaced(rightHand, newCastExpression);
		}		
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        z= (List) foo().y.toList();\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	
	public void testCatchClause() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } catch (CoreException e) {\n");
		buf.append("        }\n");			
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 1);
		List catchClauses= ((TryStatement) statements.get(0)).catchClauses();
		assertTrue("Number of catchClauses not 2", catchClauses.size() == 2);
		{ // change exception type
			CatchClause clause= (CatchClause) catchClauses.get(0);
			
			SingleVariableDeclaration exception= clause.getException();
			
			SingleVariableDeclaration newException= ast.newSingleVariableDeclaration();
						
			newException.setType(ast.newSimpleType(ast.newSimpleName("NullPointerException")));
			newException.setName(ast.newSimpleName("ex"));
			
			rewrite.markAsReplaced(exception, newException);
		}
		{ // change body
			CatchClause clause= (CatchClause) catchClauses.get(1);
			Block body= clause.getBody();
			
			Block newBody= ast.newBlock();
			ReturnStatement returnStatement= ast.newReturnStatement();
			newBody.statements().add(returnStatement);
			
			rewrite.markAsReplaced(body, newBody);
		}
				
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("        } catch (NullPointerException ex) {\n");
		buf.append("        } catch (CoreException e) {\n");
		buf.append("            return;\n");	
		buf.append("        }\n");			
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}

	public void testClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo().new Inner();\n");
		buf.append("        new Runnable(\"Hello\") {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 2", statements.size() == 2);
		{ // remove expression, change type name, add argument, add anonym decl
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();

			rewrite.markAsRemoved(creation.getExpression());
			
			SimpleName newName= ast.newSimpleName("NewInner");
			rewrite.markAsReplaced(creation.getName(), newName);
			
			StringLiteral stringLiteral1= ast.newStringLiteral();
			stringLiteral1.setLiteralValue("Hello");
			rewrite.getListRewrite(creation, ClassInstanceCreation.ARGUMENTS_PROPERTY).insertLast(stringLiteral1, null);

			
			StringLiteral stringLiteral2= ast.newStringLiteral();
			stringLiteral2.setLiteralValue("World");
			rewrite.getListRewrite(creation, ClassInstanceCreation.ARGUMENTS_PROPERTY).insertLast(stringLiteral2, null);
			
			assertTrue("Has anonym class decl", creation.getAnonymousClassDeclaration() == null);
			
			AnonymousClassDeclaration anonymDecl= ast.newAnonymousClassDeclaration();
			MethodDeclaration anonymMethDecl= createNewMethod(ast, "newMethod", false);
			anonymDecl.bodyDeclarations().add(anonymMethDecl);
			
			rewrite.markAsInsert(creation, ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY, anonymDecl, null);			

		}
		{ // add expression, remove argument, remove anonym decl 
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();

			assertTrue("Has expression", creation.getExpression() == null);
			
			SimpleName newExpression= ast.newSimpleName("x");
			rewrite.markAsInsert(creation, ClassInstanceCreation.EXPRESSION_PROPERTY, newExpression, null);			

			
			List arguments= creation.arguments();
			assertTrue("Must have 1 argument", arguments.size() == 1);
			
			rewrite.markAsRemoved((ASTNode) arguments.get(0));
			
			rewrite.markAsRemoved(creation.getAnonymousClassDeclaration());
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new NewInner(\"Hello\", \"World\") {\n");
		buf.append("            private void newMethod(String str) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        x.new Runnable();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testConditionalExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= (k == 0) ? 1 : 2;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // change compare expression, then expression & else expression
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			ConditionalExpression condExpression= (ConditionalExpression) assignment.getRightHandSide();
			
			BooleanLiteral literal= ast.newBooleanLiteral(true);
			rewrite.markAsReplaced(condExpression.getExpression(), literal);
			
			SimpleName newThenExpre= ast.newSimpleName("x");
			rewrite.markAsReplaced(condExpression.getThenExpression(), newThenExpre);
			
			InfixExpression infixExpression= ast.newInfixExpression();
			infixExpression.setLeftOperand(ast.newNumberLiteral("1"));
			infixExpression.setRightOperand(ast.newNumberLiteral("2"));
			infixExpression.setOperator(InfixExpression.Operator.PLUS);
			
			rewrite.markAsReplaced(condExpression.getElseExpression(), infixExpression);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= true ? x : 1 + 2;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testFieldAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo().i= goo().i;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // replace field expression, replace field name
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			FieldAccess leftFieldAccess= (FieldAccess) assignment.getLeftHandSide();
			FieldAccess rightFieldAccess= (FieldAccess) assignment.getRightHandSide();
			
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName("xoo"));
			rewrite.markAsReplaced(leftFieldAccess.getExpression(), invocation);
			
			SimpleName newName= ast.newSimpleName("x");
			rewrite.markAsReplaced(leftFieldAccess.getName(), newName);

			SimpleName rightHand= ast.newSimpleName("b");
			rewrite.markAsReplaced(rightFieldAccess.getExpression(), rightHand);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        xoo().x= b.i;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testInfixExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= 1 + 2;\n");
		buf.append("        j= 1 + 2 + 3 + 4 + 5;\n");
		buf.append("        k= 1 + 2 + 3 + 4 + 5;\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 3);
		{ // change left side & right side & operand
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			InfixExpression expr= (InfixExpression) assignment.getRightHandSide();
			
			SimpleName leftOp= ast.newSimpleName("k");
			rewrite.markAsReplaced(expr.getLeftOperand(), leftOp);	

			SimpleName rightOp= ast.newSimpleName("j");
			rewrite.markAsReplaced(expr.getRightOperand(), rightOp);	
			
			// change operand
			rewrite.markAsReplaced(expr, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.MINUS, null);
		}
		
		{ // remove an ext. operand, add one and replace one
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			Assignment assignment= (Assignment) stmt.getExpression();
			InfixExpression expr= (InfixExpression) assignment.getRightHandSide();
			
			List extendedOperands= expr.extendedOperands();
			assertTrue("Number of extendedOperands not 3", extendedOperands.size() == 3);
			
			rewrite.markAsRemoved((ASTNode) extendedOperands.get(0));
			
			SimpleName newOp1= ast.newSimpleName("k");
			rewrite.markAsReplaced((ASTNode) extendedOperands.get(1), newOp1);
			
			SimpleName newOp2= ast.newSimpleName("n");
			rewrite.getListRewrite(expr, InfixExpression.EXTENDED_OPERANDS_PROPERTY).insertLast(newOp2, null);

		}
		
		{ // change operand
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			Assignment assignment= (Assignment) stmt.getExpression();
			InfixExpression expr= (InfixExpression) assignment.getRightHandSide();			
			
			rewrite.markAsReplaced(expr, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.TIMES, null);
		}			
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= k - j;\n");
		buf.append("        j= 1 + 2 + k + 5 + n;\n");
		buf.append("        k= 1 * 2 * 3 * 4 * 5;\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}

	public void testInstanceofExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k instanceof Vector);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // change left side & right side
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			
			List arguments= invocation.arguments();
			InstanceofExpression expr= (InstanceofExpression) arguments.get(0);
			
			SimpleName name= ast.newSimpleName("x");
			rewrite.markAsReplaced(expr.getLeftOperand(), name);
			
			Type newCastType= ast.newSimpleType(ast.newSimpleName("List"));

			rewrite.markAsReplaced(expr.getRightOperand(), newCastType);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(x instanceof List);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo(1, 2).goo();\n");
		buf.append("        foo(1, 2).goo();\n");
		buf.append("        foo(1, 2).goo();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 3);
		{ // remove expression, add param, change name
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			
			rewrite.markAsRemoved(invocation.getExpression());
			
			SimpleName name= ast.newSimpleName("x");
			rewrite.markAsReplaced(invocation.getName(), name);
			
			ASTNode arg= ast.newNumberLiteral("1");
			rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY).insertLast(arg, null);

		}
		{ // insert expression, delete params
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			
			MethodInvocation leftInvocation= (MethodInvocation) invocation.getExpression();
			
			SimpleName newExpression= ast.newSimpleName("x");
			rewrite.markAsInsert(leftInvocation, MethodInvocation.EXPRESSION_PROPERTY, newExpression, null);
			
			List args= leftInvocation.arguments();
			rewrite.markAsRemoved((ASTNode) args.get(0));
			rewrite.markAsRemoved((ASTNode) args.get(1));
		}
		{ // remove expression, add it as parameter
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder(invocation.getExpression());
			
			rewrite.markAsRemoved(invocation, MethodInvocation.EXPRESSION_PROPERTY, null);

			rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY).insertLast(placeHolder, null);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        x(1);\n");
		buf.append("        x.foo().goo();\n");		
		buf.append("        goo(foo(1, 2));\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}

	public void testMethodParamsRenameReorder() throws Exception {
		if (true)
			return;
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void m(boolean y, int a) {\n");
		buf.append("        m(y, a);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "m");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ 
			//params 
			List params= methodDecl.parameters();
			SingleVariableDeclaration firstParam= (SingleVariableDeclaration) params.get(0);
			SingleVariableDeclaration secondParam= (SingleVariableDeclaration) params.get(1);

			//args
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			List arguments= invocation.arguments();
			SimpleName first= (SimpleName) arguments.get(0);
			SimpleName second= (SimpleName) arguments.get(1);
			

			//rename args
			SimpleName newFirstArg= methodDecl.getAST().newSimpleName("yyy");
			SimpleName newSecondArg= methodDecl.getAST().newSimpleName("bb");
			rewrite.markAsReplaced(first, newFirstArg);
			rewrite.markAsReplaced(second, newSecondArg);
			

			//rename params
			SimpleName newFirstName= methodDecl.getAST().newSimpleName("yyy");
			SimpleName newSecondName= methodDecl.getAST().newSimpleName("bb");
			rewrite.markAsReplaced(firstParam.getName(), newFirstName);
			rewrite.markAsReplaced(secondParam.getName(), newSecondName);
			
			//reoder params
			ASTNode paramplaceholder1= rewrite.createCopyPlaceholder(firstParam);
			ASTNode paramplaceholder2= rewrite.createCopyPlaceholder(secondParam);
			
			rewrite.markAsReplaced(firstParam, paramplaceholder2);
			rewrite.markAsReplaced(secondParam, paramplaceholder1);
			
			//reorder args
			ASTNode placeholder1= rewrite.createCopyPlaceholder(first);
			ASTNode placeholder2= rewrite.createCopyPlaceholder(second);
			
			rewrite.markAsReplaced(first, placeholder2);
			rewrite.markAsReplaced(second, placeholder1);

			
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void m(int bb, boolean yyy) {\n");
		buf.append("        m(bb, yyy);\n");
		buf.append("    }\n");
		buf.append("}\n");		
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testMethodInvocation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo(foo(1, 2), 3);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // remove expression, add param, change name
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			MethodInvocation invocation= (MethodInvocation) stmt.getExpression();
			
			List arguments= invocation.arguments();
			MethodInvocation first= (MethodInvocation) arguments.get(0);
			ASTNode second= (ASTNode) arguments.get(1);
			
			ASTNode placeholder1= rewrite.createCopyPlaceholder(first);
			ASTNode placeholder2= rewrite.createCopyPlaceholder(second);
			
			rewrite.markAsReplaced(first, placeholder2);
			rewrite.markAsReplaced(second, placeholder1);
			
			List innerArguments= first.arguments();
			ASTNode innerFirst= (ASTNode) innerArguments.get(0);
			ASTNode innerSecond= (ASTNode) innerArguments.get(1);
			
			ASTNode innerPlaceholder1= rewrite.createCopyPlaceholder(innerFirst);
			ASTNode innerPlaceholder2= rewrite.createCopyPlaceholder(innerSecond);
			
			rewrite.markAsReplaced(innerFirst, innerPlaceholder2);
			rewrite.markAsReplaced(innerSecond, innerPlaceholder1);			
			
			
			
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo(3, foo(2, 1));\n");
		buf.append("    }\n");
		buf.append("}\n");		
		assertEqualString(preview, buf.toString());
		
	}	
	
	public void testParenthesizedExpression() throws Exception {
		//System.out.println(getClass().getName()+"::" + getName() +" disabled (bug 23362)");
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= (1 + 2) * 3;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // replace expression
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			InfixExpression multiplication= (InfixExpression) assignment.getRightHandSide();
			
			ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression) multiplication.getLeftOperand();
						
			SimpleName name= ast.newSimpleName("x");
			rewrite.markAsReplaced(parenthesizedExpression.getExpression(), name);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= (x) * 3;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
		
	}
	
	public void testPrefixExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= --x;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // modify operand and operation
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			PrefixExpression preExpression= (PrefixExpression) assignment.getRightHandSide();
					
			NumberLiteral newOperation= ast.newNumberLiteral("10");
			rewrite.markAsReplaced(preExpression.getOperand(), newOperation);
			
			rewrite.markAsReplaced(preExpression, PrefixExpression.OPERATOR_PROPERTY, PrefixExpression.Operator.COMPLEMENT, null);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= ~10;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}	
	
	public void testPostfixExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= x--;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // modify operand and operation
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			PostfixExpression postExpression= (PostfixExpression) assignment.getRightHandSide();
					
			NumberLiteral newOperation= ast.newNumberLiteral("10");
			rewrite.markAsReplaced(postExpression.getOperand(), newOperation);
			
			rewrite.markAsReplaced(postExpression, PostfixExpression.OPERATOR_PROPERTY, PostfixExpression.Operator.INCREMENT, null);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= 10++;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}		

	public void testSuperConstructorInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("    public E(int i) {\n");
		buf.append("        foo(i + i).super(i);\n");
		buf.append("    }\n");
		buf.append("    public E(int i, int k) {\n");
		buf.append("        Outer.super(foo(goo(x)), 1);\n");
		buf.append("    }\n");	
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		List bodyDeclarations= type.bodyDeclarations();
		assertTrue("Number of bodyDeclarations not 3", bodyDeclarations.size() == 3);
		{ // add expresssion & parameter
			MethodDeclaration methodDecl= (MethodDeclaration) bodyDeclarations.get(0);
			SuperConstructorInvocation invocation= (SuperConstructorInvocation) methodDecl.getBody().statements().get(0);

			SimpleName newExpression= ast.newSimpleName("x");
			rewrite.markAsInsert(invocation, SuperConstructorInvocation.EXPRESSION_PROPERTY, newExpression, null);

			ASTNode arg= ast.newNumberLiteral("1");
			rewrite.getListRewrite(invocation, SuperConstructorInvocation.ARGUMENTS_PROPERTY).insertLast(arg, null);

		}
		{ // remove expression, replace argument with argument of expression
			MethodDeclaration methodDecl= (MethodDeclaration) bodyDeclarations.get(1);
			SuperConstructorInvocation invocation= (SuperConstructorInvocation) methodDecl.getBody().statements().get(0);

			MethodInvocation expression= (MethodInvocation) invocation.getExpression();
			rewrite.markAsRemoved(expression);
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder((ASTNode) expression.arguments().get(0));
			
			ASTNode arg1= (ASTNode) invocation.arguments().get(0);
			
			rewrite.markAsReplaced(arg1, placeHolder);
		}
		{ // remove argument, replace expression with part of argument
			MethodDeclaration methodDecl= (MethodDeclaration) bodyDeclarations.get(2);
			SuperConstructorInvocation invocation= (SuperConstructorInvocation) methodDecl.getBody().statements().get(0);
			
			MethodInvocation arg1= (MethodInvocation) invocation.arguments().get(0);
			rewrite.markAsRemoved(arg1);
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder((ASTNode) arg1.arguments().get(0));
			
			rewrite.markAsReplaced(invocation.getExpression(), placeHolder);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        x.super(1);\n");
		buf.append("    }\n");
		buf.append("    public E(int i) {\n");
		buf.append("        super(i + i);\n");
		buf.append("    }\n");
		buf.append("    public E(int i, int k) {\n");
		buf.append("        goo(x).super(1);\n");
		buf.append("    }\n");	
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testSuperFieldInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        super.x= Outer.super.y;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // insert qualifier, replace field name, delete qualifier
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			SuperFieldAccess leftFieldAccess= (SuperFieldAccess) assignment.getLeftHandSide();
			SuperFieldAccess rightFieldAccess= (SuperFieldAccess) assignment.getRightHandSide();
			
			SimpleName newQualifier= ast.newSimpleName("X");
			rewrite.markAsInsert(leftFieldAccess, SuperFieldAccess.QUALIFIER_PROPERTY, newQualifier, null);

			SimpleName newName= ast.newSimpleName("y");
			rewrite.markAsReplaced(leftFieldAccess.getName(), newName);

			rewrite.markAsRemoved(rightFieldAccess.getQualifier());
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        X.super.y= super.y;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}	
	public void testSuperMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        super.foo();\n");
		buf.append("        Outer.super.foo(i);\n");		
		buf.append("        Outer.super.foo(foo(X.goo()), 1);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 3);
		{ // add qualifier & parameter
			ExpressionStatement statement= (ExpressionStatement) statements.get(0);
			SuperMethodInvocation invocation= (SuperMethodInvocation) statement.getExpression();

			SimpleName newExpression= ast.newSimpleName("X");
			rewrite.markAsInsert(invocation, SuperMethodInvocation.QUALIFIER_PROPERTY, newExpression, null);
			
			ASTNode arg= ast.newNumberLiteral("1");
			rewrite.getListRewrite(invocation, SuperMethodInvocation.ARGUMENTS_PROPERTY).insertLast(arg, null);
		}
		{ // remove qualifier, replace argument with argument of expression
			ExpressionStatement statement= (ExpressionStatement) statements.get(1);
			SuperMethodInvocation invocation= (SuperMethodInvocation) statement.getExpression();

			Name qualifier= invocation.getQualifier();
			rewrite.markAsRemoved(qualifier);
			
			Name placeHolder= (Name) rewrite.createCopyPlaceholder(qualifier);
			
			FieldAccess newFieldAccess= ast.newFieldAccess();
			newFieldAccess.setExpression(placeHolder);
			newFieldAccess.setName(ast.newSimpleName("count"));
			
			ASTNode arg1= (ASTNode) invocation.arguments().get(0);
			rewrite.markAsReplaced(arg1, newFieldAccess);
		}
		{ // remove argument, replace qualifier with part argument qualifier
			ExpressionStatement statement= (ExpressionStatement) statements.get(2);
			SuperMethodInvocation invocation= (SuperMethodInvocation) statement.getExpression();
			
			MethodInvocation arg1= (MethodInvocation) invocation.arguments().get(0);
			rewrite.markAsRemoved(arg1);
			
			MethodInvocation innerArg= (MethodInvocation) arg1.arguments().get(0);
			
			ASTNode placeHolder= rewrite.createCopyPlaceholder(innerArg.getExpression());
			
			rewrite.markAsReplaced(invocation.getQualifier(), placeHolder);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        X.super.foo(1);\n");
		buf.append("        super.foo(Outer.count);\n");		
		buf.append("        X.super.foo(1);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testThisExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        return this;\n");		
		buf.append("        return Outer.this;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");

		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 2", statements.size() == 2);
		{ // add qualifier
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			
			ThisExpression thisExpression= (ThisExpression) returnStatement.getExpression();

			SimpleName newExpression= ast.newSimpleName("X");
			rewrite.markAsInsert(thisExpression, ThisExpression.QUALIFIER_PROPERTY, newExpression, null);
		}
		{ // remove qualifier
			ReturnStatement returnStatement= (ReturnStatement) statements.get(1);
			
			ThisExpression thisExpression= (ThisExpression) returnStatement.getExpression();

			rewrite.markAsRemoved(thisExpression.getQualifier());
		}

			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        return X.this;\n");		
		buf.append("        return this;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testTypeLiteral() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        return E.class;\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");

		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // replace type
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			
			TypeLiteral typeLiteral= (TypeLiteral) returnStatement.getExpression();

			Type newType= ast.newPrimitiveType(PrimitiveType.VOID);
			
			rewrite.markAsReplaced(typeLiteral.getType(), newType);
		}
			
		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        return void.class;\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(preview, buf.toString());
		
	}
			
}
