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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingStatementsTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingStatementsTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private ICompilationUnit fCU_C;
	private ICompilationUnit fCU_D;

	public ASTRewritingStatementsTest(String name) {
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
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public Object foo() {\n");
		buf.append("        if (this.equals(new Object())) {\n");
		buf.append("            toString();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		fCU_C= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class D {\n");
		buf.append("    public Object goo() {\n");
		buf.append("        return new Integer(3);\n");
		buf.append("    }\n");
		buf.append("    public void hoo(int p1, Object p2) {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("}\n");	
		fCU_D= pack1.createCompilationUnit("D.java", buf.toString(), false, null);
				
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	
	
	public void testAdd() throws Exception {
		{	/* foo(): append a return statement */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "C");
			
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);	
			
			List statements= block.statements();
			ReturnStatement returnStatement= block.getAST().newReturnStatement();
			returnStatement.setExpression(ASTResolving.getNullExpression(methodDecl.getReturnType()));
			statements.add(returnStatement);
			ASTRewriteAnalyzer.markAsInserted(returnStatement);
			
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class C {\n");
			buf.append("    public Object foo() {\n");
			buf.append("        if (this.equals(new Object())) {\n");
			buf.append("            toString();\n");
			buf.append("        }\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			
			assertEqualString(cu.getSource(), buf.toString());
		}
		{	/* hoo(): return; -> return false;  */
			ICompilationUnit cu= fCU_D;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "D");
		
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hoo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);
			
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			Expression expr= block.getAST().newBooleanLiteral(false);
			
			returnStatement.setExpression(expr);
			ASTRewriteAnalyzer.markAsInserted(expr);
			
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class D {\n");
			buf.append("    public Object goo() {\n");
			buf.append("        return new Integer(3);\n");
			buf.append("    }\n");
			buf.append("    public void hoo(int p1, Object p2) {\n");
			buf.append("        return false;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());			
		}
		
	}
	
	public void testRemove() throws Exception {
		{	/* foo():  remove if... */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "C");
			
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);	
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) statements.get(0));
					
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class C {\n");
			buf.append("    public Object foo() {\n");
			buf.append("    }\n");
			buf.append("}\n");
				
			assertEqualString(cu.getSource(), buf.toString());
		}
		{	/* goo(): remove new Integer(3) */
			ICompilationUnit cu= fCU_D;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "D");
		
			MethodDeclaration methodDecl= findMethodDeclaration(type, "goo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);
			
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			Expression expr= returnStatement.getExpression();
			ASTRewriteAnalyzer.markAsRemoved(expr);
			
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class D {\n");
			buf.append("    public Object goo() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");
			buf.append("    public void hoo(int p1, Object p2) {\n");
			buf.append("        return;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());			
		}
	}
	
	public void testReplace() throws Exception {
		{	/* foo(): if.. -> return; */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "C");
			
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);	
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			
			ReturnStatement returnStatement= block.getAST().newReturnStatement();
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) statements.get(0), returnStatement);
					
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class C {\n");
			buf.append("    public Object foo() {\n");
			buf.append("        return;\n");			
			buf.append("    }\n");
			buf.append("}\n");
				
			assertEqualString(cu.getSource(), buf.toString());
		}		
		{	/* goo(): new Integer(3) -> 'null' */
			ICompilationUnit cu= fCU_D;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
			TypeDeclaration type= findTypeDeclaration(astRoot, "D");
		
			MethodDeclaration methodDecl= findMethodDeclaration(type, "goo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);
			
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			Expression expr= returnStatement.getExpression();
			Expression modified= ASTResolving.getNullExpression(methodDecl.getReturnType());
	
			ASTRewriteAnalyzer.markAsReplaced(expr, modified);
			
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class D {\n");
			buf.append("    public Object goo() {\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("    public void hoo(int p1, Object p2) {\n");
			buf.append("        return;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());
		}
	}
	
	public void testBreakStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        break;\n");
		buf.append("        break label;\n");
		buf.append("        break label;\n");
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
		{ // insert label
			BreakStatement statement= (BreakStatement) statements.get(0);
			assertTrue("Has label", statement.getLabel() == null);
			
			SimpleName newLabel= ast.newSimpleName("label2");	
			statement.setLabel(newLabel);
			
			ASTRewriteAnalyzer.markAsInserted(newLabel);
		}
		{ // replace label
			BreakStatement statement= (BreakStatement) statements.get(1);
			
			SimpleName label= statement.getLabel();
			assertTrue("Has no label", label != null);
			
			SimpleName newLabel= ast.newSimpleName("label2");	

			ASTRewriteAnalyzer.markAsReplaced(label, newLabel);
		}
		{ // remove label
			BreakStatement statement= (BreakStatement) statements.get(2);
			
			SimpleName label= statement.getLabel();
			assertTrue("Has no label", label != null);
			
			ASTRewriteAnalyzer.markAsRemoved(label);
		}	
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        break label2;\n");
		buf.append("        break label2;\n");
		buf.append("        break;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testConstructorInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(String e, String f) {\n");
		buf.append("        this();\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(\"Hello\", true);\n");
		buf.append("    }\n");		
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration[] declarations= type.getMethods();
		assertTrue("Number of statements not 2", declarations.length == 2);			

		{ // add parameters
			Block block= declarations[0].getBody();
			List statements= block.statements();
			assertTrue("Number of statements not 1", statements.size() == 1);
			
			ConstructorInvocation invocation= (ConstructorInvocation) statements.get(0);
	
			List arguments= invocation.arguments();
			
			StringLiteral stringLiteral1= ast.newStringLiteral();
			stringLiteral1.setLiteralValue("Hello");
			arguments.add(stringLiteral1);
			ASTRewriteAnalyzer.markAsInserted(stringLiteral1);
			
			StringLiteral stringLiteral2= ast.newStringLiteral();
			stringLiteral2.setLiteralValue("World");
			arguments.add(stringLiteral2);
			ASTRewriteAnalyzer.markAsInserted(stringLiteral2);
		}
		{ //remove parameters
			Block block= declarations[1].getBody();
			List statements= block.statements();
			assertTrue("Number of statements not 1", statements.size() == 1);			
			ConstructorInvocation invocation= (ConstructorInvocation) statements.get(0);
	
			List arguments= invocation.arguments();
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) arguments.get(0));
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) arguments.get(1));
		}		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(String e, String f) {\n");
		buf.append("        this(\"Hello\", \"World\");\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this();\n");
		buf.append("    }\n");		
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testContinueStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        continue;\n");
		buf.append("        continue label;\n");
		buf.append("        continue label;\n");
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
		{ // insert label
			ContinueStatement statement= (ContinueStatement) statements.get(0);
			assertTrue("Has label", statement.getLabel() == null);
			
			SimpleName newLabel= ast.newSimpleName("label2");	
			statement.setLabel(newLabel);
			
			ASTRewriteAnalyzer.markAsInserted(newLabel);
		}
		{ // replace label
			ContinueStatement statement= (ContinueStatement) statements.get(1);
			
			SimpleName label= statement.getLabel();
			assertTrue("Has no label", label != null);
			
			SimpleName newLabel= ast.newSimpleName("label2");	

			ASTRewriteAnalyzer.markAsReplaced(label, newLabel);
		}
		{ // remove label
			ContinueStatement statement= (ContinueStatement) statements.get(2);
			
			SimpleName label= statement.getLabel();
			assertTrue("Has no label", label != null);
			
			ASTRewriteAnalyzer.markAsRemoved(label);
		}	
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        continue label2;\n");
		buf.append("        continue label2;\n");
		buf.append("        continue;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testDoStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");
		buf.append("            System.beep();\n");
		buf.append("        } while (i == j);\n");		
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

		{ // replace body and expression
			DoStatement doStatement= (DoStatement) statements.get(0);
			
			Block newBody= ast.newBlock();
			
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName("hoo"));
			invocation.arguments().add(ast.newNumberLiteral("11"));
			
			newBody.statements().add(ast.newExpressionStatement(invocation));
			
			ASTRewriteAnalyzer.markAsReplaced(doStatement.getBody(), newBody);

			BooleanLiteral literal= ast.newBooleanLiteral(true);
			ASTRewriteAnalyzer.markAsReplaced(doStatement.getExpression(), literal);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");
		buf.append("            hoo(11);\n");
		buf.append("        } while (true);\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}		

	public void testExpressionStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
		AST ast= astRoot.getAST();
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		assertTrue("Parse errors", (block.getFlags() & ASTNode.MALFORMED) == 0);
		
		List statements= block.statements();
		assertTrue("Number of statements not 1", statements.size() == 1);
		{ // replace expression
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			
			Assignment assignment= (Assignment) stmt.getExpression();
			Expression placeholder= (Expression) ASTRewriteAnalyzer.createCopyTarget(assignment);
									
			Assignment newExpression= ast.newAssignment();
			newExpression.setLeftHandSide(ast.newSimpleName("x"));
			newExpression.setRightHandSide(placeholder);
			newExpression.setOperator(Assignment.Operator.ASSIGN);
	
			ASTRewriteAnalyzer.markAsReplaced(stmt.getExpression(), newExpression);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        x = i= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());

	}
	
}
