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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class ASTRewritingTypeDeclTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingTypeDeclTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private ICompilationUnit fCU_E;

	public ASTRewritingTypeDeclTest(String name) {
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
		buf.append("        }\n");		
		buf.append("    }\n");		
		buf.append("    private int i;\n");	
		buf.append("    private int k;\n");	
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void hee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F implements Runnable {\n");
		buf.append("}\n");
		fCU_E= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	

	
	public void testTypeDeclChanges() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		{ // rename type, rename supertype, rename first interface, replace inner class with field
			SimpleName name= type.getName();
			SimpleName newName= ast.newSimpleName("X");
			
			ASTRewriteAnalyzer.markAsReplaced(name, newName);
			
			Name superClass= type.getSuperclass();
			assertTrue("Has super type", superClass != null);
			
			SimpleName newSuperclass= ast.newSimpleName("Object");
			ASTRewriteAnalyzer.markAsReplaced(superClass, newSuperclass);

			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			
			SimpleName newSuperinterface= ast.newSimpleName("Cloneable");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) superInterfaces.get(0), newSuperinterface);
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("fCount"));
			FieldDeclaration newFieldDecl= ast.newFieldDeclaration(frag);
			newFieldDecl.setModifiers(Modifier.PRIVATE);
			newFieldDecl.setType(ast.newPrimitiveType(PrimitiveType.DOUBLE));
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) members.get(0), newFieldDecl);		
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X extends Object implements Cloneable, Serializable {\n");
		buf.append("    private double fCount;\n");
		buf.append("    private int i;\n");	
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void hee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F implements Runnable {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testTypeDeclRemoves() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		{ // remove supertype, remove first interface, remove field
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
			Name superClass= type.getSuperclass();
			assertTrue("Has super type", superClass != null);
			
			ASTRewriteAnalyzer.markAsReplaced(superClass, null);

			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) superInterfaces.get(0), null);
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
					
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) members.get(1), null);
			
			MethodDeclaration meth= findMethodDeclaration(type, "hee");
			ASTRewriteAnalyzer.markAsReplaced(meth, null);
		}
		{ // remove interface
			TypeDeclaration type= findTypeDeclaration(astRoot, "F");
			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) superInterfaces.get(0), null);
		}			
		

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E implements Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("        }\n");		
		buf.append("    }\n");		
		buf.append("    private int k;\n");	
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeDeclInserts() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		{ // add interface
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			
			SimpleName newSuperinterface= ast.newSimpleName("Cloneable");
			superInterfaces.add(0, newSuperinterface);
			ASTRewriteAnalyzer.markAsInserted(newSuperinterface);
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);

			SimpleName newSuperclass= ast.newSimpleName("Exception");
			innerType.setSuperclass(newSuperclass);
			ASTRewriteAnalyzer.markAsInserted(newSuperclass);					
					
//			ASTRewriteAnalyzer.markAsReplaced((ASTNode) members.get(1), null);
//			
//			MethodDeclaration meth= findMethodDeclaration(type, "hee");
//			ASTRewriteAnalyzer.markAsReplaced(meth, null);
		}
		{ // remove interface
			TypeDeclaration type= findTypeDeclaration(astRoot, "F");
			
			SimpleName newSuperclass= ast.newSimpleName("Exception");
			type.setSuperclass(newSuperclass);
			
			ASTRewriteAnalyzer.markAsInserted(newSuperclass);
		}			
		

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Cloneable, Runnable, Serializable {\n");
		buf.append("    public static class EInner extends Exception {\n");
		buf.append("        public void xee() {\n");
		buf.append("        }\n");		
		buf.append("    }\n");		
		buf.append("    private int i;\n");	
		buf.append("    private int k;\n");	
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void hee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F extends Exception implements Runnable {\n");
		buf.append("}\n");
		
		assertEqualString(cu.getSource(), buf.toString());
	}	
	
	
	
}
