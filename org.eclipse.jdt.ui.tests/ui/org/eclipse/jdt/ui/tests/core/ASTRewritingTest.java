package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingTest extends TestCase {
	
	private static final Class THIS= ASTRewritingTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private ICompilationUnit fCU_C;
	private ICompilationUnit fCU_D;

	public ASTRewritingTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), THIS, args);
	}


	public static Test suite() {
		return new TestSuite(THIS);
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
		buf.append("    public void hoo() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("}\n");	
		fCU_D= pack1.createCompilationUnit("D.java", buf.toString(), false, null);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	private void assertEqualString(String str1, String str2) {	
		int len1= Math.min(str1.length(), str2.length());
		
		int diffPos= -1;
		for (int i= 0; i < len1; i++) {
			if (str1.charAt(i) != str2.charAt(i)) {
				diffPos= i;
				break;
			}
		}
		assertTrue("Content not as expected: is \n" + str1 + "\nDiffers at pos " + diffPos,  diffPos == -1);
	}
	
	private MethodDeclaration findMethodDeclaration(ICompilationUnit cu, CompilationUnit astRoot, String methodName) throws Exception {
		String source= cu.getSource();
		
		int start= source.indexOf(methodName);
		assertTrue(methodName + " not found", start != -1);
		
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, start, methodName.length());
		
		assertTrue("Node not of type SimpleName" , selectedNode instanceof SimpleName);
		assertTrue("Parent node not of type MethodDeclaration" , selectedNode.getParent() instanceof MethodDeclaration);
		
		return (MethodDeclaration) selectedNode.getParent();	
	}
	
	
	public void testAdd() throws Exception {
		{	/* foo(): append a return statement */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
			
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "foo");
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
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "hoo");
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
			buf.append("    public void hoo() {\n");
			buf.append("        return false;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());			
		}
		
	}
	
	public void testRemove() throws Exception {
		{	/* foo():  remove if... */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
			
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "foo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);	
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) statements.get(0), null);
					
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
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "goo");
			Block block= methodDecl.getBody();
			assertTrue("No block" , block != null);
			
			List statements= block.statements();
			assertTrue("No statements in block", !statements.isEmpty());
			assertTrue("No ReturnStatement", statements.get(0) instanceof ReturnStatement);
			
			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			Expression expr= returnStatement.getExpression();
			ASTRewriteAnalyzer.markAsReplaced(expr, null);
			
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
			proposal.getCompilationUnitChange().setSave(true);
			
			proposal.apply(null);
			
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class D {\n");
			buf.append("    public Object goo() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");
			buf.append("    public void hoo() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());			
		}
	}
	
	public void testReplace() throws Exception {
		{	/* foo(): if.. -> return; */
			ICompilationUnit cu= fCU_C;
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
			
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "foo");
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
			CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		
			MethodDeclaration methodDecl= findMethodDeclaration(cu, astRoot, "goo");
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
			buf.append("    public void hoo() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");		
			buf.append("}\n");	
			
			assertEqualString(cu.getSource(), buf.toString());
		}
	}	
	
}
