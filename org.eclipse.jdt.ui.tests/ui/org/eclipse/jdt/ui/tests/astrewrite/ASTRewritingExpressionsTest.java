package org.eclipse.jdt.ui.tests.astrewrite;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingExpressionsTest extends ASTRewritingTest {
	private static final Class THIS= ASTRewritingExpressionsTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingExpressionsTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), THIS, args);
	}


	public static Test suite() {
		return new TestSuite(THIS);
//		TestSuite suite= new TestSuite();
//		suite.addTest(new ASTRewritingTest("testListCombinations"));
//		return suite;
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");				
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(left.getIndex(), name);
			
			ASTNode placeHolder= ASTRewriteAnalyzer.createCopyTarget(left.getIndex());
			ASTRewriteAnalyzer.markAsReplaced(right.getIndex(), placeHolder);
			
			SimpleName newName= ast.newSimpleName("o");
			ASTRewriteAnalyzer.markAsReplaced(right.getArray(), newName);
		}

				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int[] o= new int[] { 1, 2, 3 };\n");		
		buf.append("    public void foo() {\n");
		buf.append("        o[1]= o[3 /* comment*/ - 1];\n");
		buf.append("    }\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			
			ASTRewriteAnalyzer.markAsReplaced(arrayType, newArrayType);
		}
		{	// remove the initializer, add a dimension expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(1);
			ASTRewriteAnalyzer.markAsRemoved(arrayCreation.getInitializer());
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 0", dimensions.size() == 0);
			
			NumberLiteral literal= ast.newNumberLiteral("10");
			dimensions.add(literal);
			
			ASTRewriteAnalyzer.markAsInserted(literal);
		}
		{	// remove all dimension except one, no dimension expression
			// insert the initializer: formatter problems
			ArrayCreation arrayCreation= (ArrayCreation) args.get(2);
			ArrayType arrayType= arrayCreation.getType();			
			PrimitiveType intType= ast.newPrimitiveType(PrimitiveType.INT); 
			ArrayType newArrayType= ast.newArrayType(intType, 1);
			
			ASTRewriteAnalyzer.markAsReplaced(arrayType, newArrayType);
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) dimensions.get(0));
			
			ArrayInitializer initializer= ast.newArrayInitializer();
			List expressions= initializer.expressions();
			expressions.add(ast.newNumberLiteral("10"));
		}
		{	// add 2 dimension expressions
			ArrayCreation arrayCreation= (ArrayCreation) args.get(3);
			
			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);
			
			NumberLiteral literal1= ast.newNumberLiteral("10");
			dimensions.add(literal1);
			ASTRewriteAnalyzer.markAsInserted(literal1);
			
			NumberLiteral literal2= ast.newNumberLiteral("11");
			dimensions.add(literal2);
			ASTRewriteAnalyzer.markAsInserted(literal2);
		}
		{	// add 2 empty dimensions
			ArrayCreation arrayCreation= (ArrayCreation) args.get(4);
			ArrayType arrayType= arrayCreation.getType();
			assertTrue("Number of dimension not 3", arrayType.getDimensions() == 3);
			
			PrimitiveType intType= ast.newPrimitiveType(PrimitiveType.INT); 
			ArrayType newArrayType= ast.newArrayType(intType, 5);
			
			ASTRewriteAnalyzer.markAsReplaced(arrayType, newArrayType);
		}
		{	// replace dimension expression, add a dimension expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(5);

			List dimensions= arrayCreation.dimensions();
			assertTrue("Number of dimension expressions not 1", dimensions.size() == 1);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) dimensions.get(0), literal1);
			
			NumberLiteral literal2= ast.newNumberLiteral("11");
			dimensions.add(literal2);
			ASTRewriteAnalyzer.markAsInserted(literal2);			
		}			
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) expressions.get(0));
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) expressions.get(2));
		}
		{	// insert at second and last position
			ArrayCreation arrayCreation= (ArrayCreation) args.get(1);
			ArrayInitializer initializer= arrayCreation.getInitializer();
			
			List expressions= initializer.expressions();
			assertTrue("Number of initializer expressions not 3", expressions.size() == 3);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			expressions.add(1, literal1);
			ASTRewriteAnalyzer.markAsInserted(literal1);
			
			NumberLiteral literal2= ast.newNumberLiteral("11");
			expressions.add(literal2);
			ASTRewriteAnalyzer.markAsInserted(literal2);
		}		
		{	// replace first and last initializer expression
			ArrayCreation arrayCreation= (ArrayCreation) args.get(2);
			ArrayInitializer initializer= arrayCreation.getInitializer();
			
			List expressions= initializer.expressions();
			assertTrue("Number of initializer expressions not 3", expressions.size() == 3);

			NumberLiteral literal1= ast.newNumberLiteral("10");
			NumberLiteral literal2= ast.newNumberLiteral("11");
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) expressions.get(0), literal1);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) expressions.get(2), literal2);
		}		
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(new int[] { 2 },\n");
		buf.append("        new int[] { 1, 10, 2, 3, 11 },\n");
		buf.append("        new int[] { 10, 2, 11 });\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(assignment.getLeftHandSide(), name);
			
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName("goo"));
			invocation.setExpression(ast.newSimpleName("other"));
			
			ASTRewriteAnalyzer.markAsReplaced(assignment.getRightHandSide(), invocation);
		}
		{ // change operator and operator of inner
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			Assignment modifiedNode= ast.newAssignment();
			modifiedNode.setOperator(Assignment.Operator.DIVIDE_ASSIGN);
			ASTRewriteAnalyzer.markAsModified(assignment, modifiedNode);
			
			Assignment inner= (Assignment) assignment.getRightHandSide();
			
			Assignment modifiedInner= ast.newAssignment();
			modifiedInner.setOperator(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN);
			ASTRewriteAnalyzer.markAsModified(inner, modifiedInner);			
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i, j;\n");
		buf.append("        j= other.goo();\n");
		buf.append("        i/= j>>>= 3;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(expression.getType(), newType);
			
			SimpleName newExpression= ast.newSimpleName("a");
			ASTRewriteAnalyzer.markAsReplaced(expression.getExpression(), newExpression);
		}
		{ // create cast
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			Expression rightHand= assignment.getRightHandSide();
			
			Expression placeholder= (Expression) ASTRewriteAnalyzer.createCopyTarget(rightHand);
			
			CastExpression newCastExpression= ast.newCastExpression();
			newCastExpression.setType(ast.newSimpleType(ast.newSimpleName("List")));
			newCastExpression.setExpression(placeholder);
			
			ASTRewriteAnalyzer.markAsReplaced(rightHand, newCastExpression);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= (SuperE) a;\n");
		buf.append("        z= (List) y.toList();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			
			ASTRewriteAnalyzer.markAsReplaced(exception, newException);
		}
		{ // change body
			CatchClause clause= (CatchClause) catchClauses.get(1);
			Block body= clause.getBody();
			
			Block newBody= ast.newBlock();
			ReturnStatement returnStatement= ast.newReturnStatement();
			newBody.statements().add(returnStatement);
			
			ASTRewriteAnalyzer.markAsReplaced(body, newBody);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
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
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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

			ASTRewriteAnalyzer.markAsRemoved(creation.getExpression());
			
			SimpleName newName= ast.newSimpleName("NewInner");
			ASTRewriteAnalyzer.markAsReplaced(creation.getName(), newName);
			
			List arguments= creation.arguments();
			
			StringLiteral stringLiteral1= ast.newStringLiteral();
			stringLiteral1.setLiteralValue("Hello");
			arguments.add(stringLiteral1);
			ASTRewriteAnalyzer.markAsInserted(stringLiteral1);
			
			StringLiteral stringLiteral2= ast.newStringLiteral();
			stringLiteral2.setLiteralValue("World");
			arguments.add(stringLiteral2);
			ASTRewriteAnalyzer.markAsInserted(stringLiteral2);

			
			assertTrue("Has anonym class decl", creation.getAnonymousClassDeclaration() == null);
			
			AnonymousClassDeclaration anonymDecl= ast.newAnonymousClassDeclaration();
			MethodDeclaration anonymMethDecl= createNewMethod(ast, "newMethod", false);
			anonymDecl.bodyDeclarations().add(anonymMethDecl);
			
			creation.setAnonymousClassDeclaration(anonymDecl);
			ASTRewriteAnalyzer.markAsInserted(anonymDecl);			

		}
		{ // add expression, remove argument, remove anonym decl 
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();

			assertTrue("Has expression", creation.getExpression() == null);
			
			SimpleName newExpression= ast.newSimpleName("x");
			creation.setExpression(newExpression);
			
			ASTRewriteAnalyzer.markAsInserted(newExpression);
			
			List arguments= creation.arguments();
			assertTrue("Must have 1 argument", arguments.size() == 1);
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) arguments.get(0));
			
			ASTRewriteAnalyzer.markAsRemoved(creation.getAnonymousClassDeclaration());
		}
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new NewInner(\"Hello\", \"World\"){\n");
		buf.append("            private void newMethod(String str) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        x.new Runnable();\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(condExpression.getExpression(), literal);
			
			SimpleName newThenExpre= ast.newSimpleName("x");
			ASTRewriteAnalyzer.markAsReplaced(condExpression.getThenExpression(), newThenExpre);
			
			InfixExpression infixExpression= ast.newInfixExpression();
			infixExpression.setLeftOperand(ast.newNumberLiteral("1"));
			infixExpression.setRightOperand(ast.newNumberLiteral("2"));
			infixExpression.setOperator(InfixExpression.Operator.PLUS);
			
			ASTRewriteAnalyzer.markAsReplaced(condExpression.getElseExpression(), infixExpression);
		}
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= true ? x : 1 + 2;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(leftFieldAccess.getExpression(), invocation);
			
			SimpleName newName= ast.newSimpleName("x");
			ASTRewriteAnalyzer.markAsReplaced(leftFieldAccess.getName(), newName);

			SimpleName rightHand= ast.newSimpleName("b");
			ASTRewriteAnalyzer.markAsReplaced(rightFieldAccess.getExpression(), rightHand);
		}
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        xoo().x= b.i;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(expr.getLeftOperand(), leftOp);	

			SimpleName rightOp= ast.newSimpleName("j");
			ASTRewriteAnalyzer.markAsReplaced(expr.getRightOperand(), rightOp);	
			
			// change operand
			InfixExpression modifiedNode= ast.newInfixExpression();
			modifiedNode.setOperator(InfixExpression.Operator.MINUS);
			ASTRewriteAnalyzer.markAsModified(expr, modifiedNode);
		}
		
		{ // remove an ext. operand, add one and replace one
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			Assignment assignment= (Assignment) stmt.getExpression();
			InfixExpression expr= (InfixExpression) assignment.getRightHandSide();
			
			List extendedOperands= expr.extendedOperands();
			assertTrue("Number of extendedOperands not 3", extendedOperands.size() == 3);
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) extendedOperands.get(0));
			
			SimpleName newOp1= ast.newSimpleName("k");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) extendedOperands.get(1), newOp1);
			
			SimpleName newOp2= ast.newSimpleName("n");
			ASTRewriteAnalyzer.markAsInserted(newOp2);
			
			extendedOperands.add(newOp2);
		}
		
		{ // change operand
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			Assignment assignment= (Assignment) stmt.getExpression();
			InfixExpression expr= (InfixExpression) assignment.getRightHandSide();			
			
			InfixExpression modifiedNode= ast.newInfixExpression();
			modifiedNode.setOperator(InfixExpression.Operator.TIMES);
			ASTRewriteAnalyzer.markAsModified(expr, modifiedNode);
		}			
			
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= k - j;\n");
		buf.append("        j= 1 + 2 + k + 5 + n;\n");
		buf.append("        k= 1 * 2 * 3 * 4 * 5;\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
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
			ASTRewriteAnalyzer.markAsReplaced(expr.getLeftOperand(), name);
			
			Type newCastType= ast.newSimpleType(ast.newSimpleName("List"));

			ASTRewriteAnalyzer.markAsReplaced(expr.getRightOperand(), newCastType);
		}
			
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(x instanceof List);\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}	
}
