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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
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
	
	
	public void testArrayAccessDeclaration() throws Exception {
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 2", statements.size() == 1);
		{	// replace left hand side index, replace right hand side index by left side index
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			Assignment assignment= (Assignment) stmt.getExpression();
			
			ArrayAccess left= (ArrayAccess) assignment.getLeftHandSide();
			ArrayAccess right= (ArrayAccess) assignment.getRightHandSide();
			
			NumberLiteral name= ast.newNumberLiteral("1");
			ASTRewriteAnalyzer.markAsReplaced(left.getIndex(), name);
			
			ASTNode placeHolder= ASTRewriteAnalyzer.getPlaceholderForExisting(left.getIndex());
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
		
	
	
	
	
}
