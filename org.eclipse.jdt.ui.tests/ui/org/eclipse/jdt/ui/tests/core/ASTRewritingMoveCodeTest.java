package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class ASTRewritingMoveCodeTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMoveCodeTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private ICompilationUnit fCU_E;

	public ASTRewritingMoveCodeTest(String name) {
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
		fCU_E= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	

	
	public void testMoveDeclSameLevel() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			ASTRewriteAnalyzer.markAsReplaced(innerType, null);
			
			ASTNode movedNode= ASTRewriteAnalyzer.getInsertNodeForExisting(innerType);
			members.add(movedNode);
			
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
				
				ASTRewriteAnalyzer.markAsReplaced(toMove, null);
			}
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
				assertTrue("Cannot find gee()", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Has statements", statements.isEmpty());
				
				ASTNode insertNodeForMove= ASTRewriteAnalyzer.getInsertNodeForExisting(toMove);
				ASTNode insertNodeForCopy= ASTRewriteAnalyzer.getInsertNodeForExisting(toCopy);
				
				statements.add(insertNodeForCopy);
				statements.add(insertNodeForMove);
			}	
		}			
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
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
	}


	
	
}
