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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

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
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingTypeDeclTest("testSingleVariableDeclaration"));
			return suite;
		}
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
		
	public void testTypeDeclChanges() throws Exception {
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
		buf.append("    public void foo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		{  // rename type, rename supertype, rename first interface, replace inner class with field
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
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
			
			FieldDeclaration newFieldDecl= createNewField(ast, "fCount");
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) members.get(0), newFieldDecl);
		}
		{ // replace method in F, change to interface
			TypeDeclaration type= findTypeDeclaration(astRoot, "F");
			
			// change flags
			TypeDeclaration modifiedNode= ast.newTypeDeclaration();
			modifiedNode.setInterface(true); 
			modifiedNode.setModifiers(0);
			ASTRewriteAnalyzer.markAsModified(type, modifiedNode);				
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", members.size() == 1);

			MethodDeclaration methodDecl= createNewMethod(ast, "newFoo", true);

			ASTRewriteAnalyzer.markAsReplaced((ASTNode) members.get(0), methodDecl);
		}
		
		{ // change to class, add supertype
			TypeDeclaration type= findTypeDeclaration(astRoot, "G");

			// change flags
			TypeDeclaration modifiedNode= ast.newTypeDeclaration();
			modifiedNode.setInterface(false); 
			modifiedNode.setModifiers(0);
			ASTRewriteAnalyzer.markAsModified(type, modifiedNode);				
			
			SimpleName newSuperclass= ast.newSimpleName("Object");
			type.setSuperclass(newSuperclass);
			ASTRewriteAnalyzer.markAsInserted(newSuperclass);
		}			
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
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
		buf.append("interface F extends Runnable {\n");
		buf.append("    private abstract void newFoo(String str);\n");
		buf.append("}\n");				
		buf.append("class G extends Object {\n");
		buf.append("}\n");			
		assertEqualString(cu.getSource(), buf.toString());
	}


	
	public void testTypeDeclRemoves() throws Exception {
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
		buf.append("    public void foo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		{ // change to interface, remove supertype, remove first interface, remove field
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			
			// change flags
			TypeDeclaration modifiedNode= ast.newTypeDeclaration();
			modifiedNode.setInterface(true); 
			modifiedNode.setModifiers(0);
			ASTRewriteAnalyzer.markAsModified(type, modifiedNode);				
		
			Name superClass= type.getSuperclass();
			assertTrue("Has super type", superClass != null);
			
			ASTRewriteAnalyzer.markAsRemoved(superClass);

			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) superInterfaces.get(0));
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
					
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) members.get(1));
			
			MethodDeclaration meth= findMethodDeclaration(type, "hee");
			ASTRewriteAnalyzer.markAsRemoved(meth);
		}
		{ // remove superinterface & method, change to interface & final
			TypeDeclaration type= findTypeDeclaration(astRoot, "F");
			
			// change flags
			TypeDeclaration modifiedNode= ast.newTypeDeclaration();
			modifiedNode.setInterface(true); 
			modifiedNode.setModifiers(Modifier.FINAL);
			ASTRewriteAnalyzer.markAsModified(type, modifiedNode);					
			
			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) superInterfaces.get(0));
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", members.size() == 1);

			ASTRewriteAnalyzer.markAsRemoved((ASTNode) members.get(0));			
		}			
		{ // remove class G
			TypeDeclaration type= findTypeDeclaration(astRoot, "G");
			ASTRewriteAnalyzer.markAsRemoved(type);		
		}				

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface E extends Serializable {\n");
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
		buf.append("final interface F {\n");
		buf.append("}\n");				
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testTypeDeclInserts() throws Exception {
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
		buf.append("    public void foo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		assertTrue("Errors in AST", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		AST ast= astRoot.getAST();
		{ // add interface & set to final
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			
			// change flags
			TypeDeclaration modifiedNode= ast.newTypeDeclaration();
			modifiedNode.setInterface(type.isInterface()); // no change 
			modifiedNode.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
			ASTRewriteAnalyzer.markAsModified(type, modifiedNode);				
		
			List superInterfaces= type.superInterfaces();
			assertTrue("Has super interfaces", !superInterfaces.isEmpty());
			
			SimpleName newSuperinterface= ast.newSimpleName("Cloneable");
			superInterfaces.add(0, newSuperinterface);
			ASTRewriteAnalyzer.markAsInserted(newSuperinterface);
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);

/*		bug 22161
			SimpleName newSuperclass= ast.newSimpleName("Exception");
			innerType.setSuperclass(newSuperclass);
			ASTRewriteAnalyzer.markAsInserted(newSuperclass);
*/

			FieldDeclaration newField= createNewField(ast, "fCount");
			
			List innerMembers= innerType.bodyDeclarations();
			innerMembers.add(0, newField);
			
			ASTRewriteAnalyzer.markAsInserted(newField);
			
			MethodDeclaration newMethodDecl= createNewMethod(ast, "newMethod", false);
			members.add(4, newMethodDecl);
			
			ASTRewriteAnalyzer.markAsInserted(newMethodDecl);
		}
		{ // add exception, add method
			TypeDeclaration type= findTypeDeclaration(astRoot, "F");
			
			SimpleName newSuperclass= ast.newSimpleName("Exception");
			type.setSuperclass(newSuperclass);
			
			ASTRewriteAnalyzer.markAsInserted(newSuperclass);
			
			List members= type.bodyDeclarations();
			
			MethodDeclaration newMethodDecl= createNewMethod(ast, "newMethod", false);
			members.add(newMethodDecl);
			
			ASTRewriteAnalyzer.markAsInserted(newMethodDecl);	
		}			
		{ // insert interface
			TypeDeclaration type= findTypeDeclaration(astRoot, "G");
						
			SimpleName newInterface= ast.newSimpleName("Runnable");
			type.superInterfaces().add(newInterface);
			
			ASTRewriteAnalyzer.markAsInserted(newInterface);
			
			List members= type.bodyDeclarations();
			
			MethodDeclaration newMethodDecl= createNewMethod(ast, "newMethod", true);
			members.add(newMethodDecl);
			
			ASTRewriteAnalyzer.markAsInserted(newMethodDecl);
		}			

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class E extends Exception implements Cloneable, Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        private double fCount;\n");	
		buf.append("        public void xee() {\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private int i;\n");	
		buf.append("    private int k;\n");	
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    private void newMethod(String str) {\n");
		buf.append("    }\n");		
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void hee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F extends Exception implements Runnable {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    private void newMethod(String str) {\n");
		buf.append("    }\n");		
		buf.append("}\n");				
		buf.append("interface G extends Runnable {\n");
		buf.append("    private abstract void newMethod(String str);\n");		
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testBug22161() throws Exception {
		System.out.println(getClass().getName()+"::" + getName() +" disabled (bug 22161)");
	/*
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class T extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("        }\n");		
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("T.java", buf.toString(), false, null);				

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		assertTrue("Errors in AST", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		AST ast= astRoot.getAST();
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "T");
		assertTrue("Outer type not found", type != null);
		
		List members= type.bodyDeclarations();
		assertTrue("Cannot find inner class", members.size() == 1 &&  members.get(0) instanceof TypeDeclaration);

		TypeDeclaration innerType= (TypeDeclaration) members.get(0);
		
		SimpleName name= innerType.getName();
		assertTrue("Name positions not correct", name.getStartPosition() != -1 && name.getLength() > 0);
		*/
	}
	
	public void testAnonymousClassDeclaration() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("        };\n");
		buf.append("        new Runnable() {\n");
		buf.append("            int i= 8;\n");
		buf.append("        };\n");
		buf.append("        new Runnable() {\n");
		buf.append("            int i= 8;\n");
		buf.append("        };\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E2");
		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		Block block= methodDecl.getBody();
		List statements= block.statements();
		assertTrue("Number of statements not 3", statements.size() == 3);
		{	// insert body decl in AnonymousClassDeclaration
			ExpressionStatement stmt= (ExpressionStatement) statements.get(0);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();
			AnonymousClassDeclaration anonym= creation.getAnonymousClassDeclaration();
			assertTrue("no anonym class decl", anonym != null);
			
			List decls= anonym.bodyDeclarations();
			assertTrue("Number of bodyDeclarations not 0", decls.size() == 0);
			
			MethodDeclaration newMethod= createNewMethod(ast, "newMethod", false);
			decls.add(newMethod);
			
			ASTRewriteAnalyzer.markAsInserted(newMethod);
		}
		{	// remove body decl in AnonymousClassDeclaration
			ExpressionStatement stmt= (ExpressionStatement) statements.get(1);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();
			AnonymousClassDeclaration anonym= creation.getAnonymousClassDeclaration();
			assertTrue("no anonym class decl", anonym != null);
			
			List decls= anonym.bodyDeclarations();
			assertTrue("Number of bodyDeclarations not 1", decls.size() == 1);

			ASTRewriteAnalyzer.markAsRemoved((ASTNode) decls.get(0));
		}		
		{	// replace body decl in AnonymousClassDeclaration
			ExpressionStatement stmt= (ExpressionStatement) statements.get(2);
			ClassInstanceCreation creation= (ClassInstanceCreation) stmt.getExpression();
			AnonymousClassDeclaration anonym= creation.getAnonymousClassDeclaration();
			assertTrue("no anonym class decl", anonym != null);
			
			List decls= anonym.bodyDeclarations();
			assertTrue("Number of bodyDeclarations not 1", decls.size() == 1);
			
			MethodDeclaration newMethod= createNewMethod(ast, "newMethod", false);

			ASTRewriteAnalyzer.markAsReplaced((ASTNode) decls.get(0), newMethod);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            private void newMethod(String str) {\n");
		buf.append("            }\n");	
		buf.append("        };\n");
		buf.append("        new Runnable() {\n");
		buf.append("        };\n");
		buf.append("        new Runnable() {\n");
		buf.append("            private void newMethod(String str) {\n");
		buf.append("            }\n");	
		buf.append("        };\n");		
		buf.append("    }\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
	}
			
	public void testImportDeclaration() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.net.*;\n");
		buf.append("import java.text.*;\n");					
		buf.append("public class Z {\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("Z.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		List imports= astRoot.imports();
		assertTrue("Number of imports not 4", imports.size() == 4);
		
		{ // rename import
			ImportDeclaration imp= (ImportDeclaration) imports.get(0);
			
			Name name= ast.newName(new String[] { "org", "eclipse", "X" });
			ASTRewriteAnalyzer.markAsReplaced(imp.getName(), name);
		}
		{ // change to import on demand
			ImportDeclaration imp= (ImportDeclaration) imports.get(1);
			
			Name name= ast.newName(new String[] { "java", "util" });
			ASTRewriteAnalyzer.markAsReplaced(imp.getName(), name);
			
			ImportDeclaration modifedNode= (ImportDeclaration) ast.newImportDeclaration();
			modifedNode.setOnDemand(true);
			
			ASTRewriteAnalyzer.markAsModified(imp, modifedNode);
		}
		{ // change to single import
			ImportDeclaration imp= (ImportDeclaration) imports.get(2);
			
			ImportDeclaration modifedNode= (ImportDeclaration) ast.newImportDeclaration();
			modifedNode.setOnDemand(false);
			
			ASTRewriteAnalyzer.markAsModified(imp, modifedNode);
		}
		{ // rename import
			ImportDeclaration imp= (ImportDeclaration) imports.get(3);
			
			Name name= ast.newName(new String[] { "org", "eclipse" });
			ASTRewriteAnalyzer.markAsReplaced(imp.getName(), name);
		}		
		
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.X;\n");
		buf.append("import java.util.*;\n");
		buf.append("import java.net;\n");
		buf.append("import org.eclipse.*;\n");			
		buf.append("public class Z {\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testPackageDeclaration() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Z {\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("Z.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		
		{ // rename package
			PackageDeclaration packageDeclaration= astRoot.getPackage();
			
			Name name= ast.newName(new String[] { "org", "eclipse" });
			
			ASTRewriteAnalyzer.markAsReplaced(packageDeclaration.getName(), name);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package org.eclipse;\n");
		buf.append("public class Z {\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testSingleVariableDeclaration() throws Exception {
/*		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, int[] k, int[] x[]) {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");

		MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
		List arguments= methodDecl.parameters();
		assertTrue("Number of arguments not 3", arguments.size() == 3);
		{
			SingleVariableDeclaration decl= (SingleVariableDeclaration) arguments.get(0);
		}
		{
			SingleVariableDeclaration decl= (SingleVariableDeclaration) arguments.get(1);
		}
		{
			SingleVariableDeclaration decl= (SingleVariableDeclaration) arguments.get(2);
		}						
			
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, int[] k, int[] x[]) {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());*/
	}	
	
	public void testVariableDeclarationFragment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i, j, k= 0, x[][], y[]= {0, 1};\n");		
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
		
		VariableDeclarationStatement variableDeclStatement= (VariableDeclarationStatement) statements.get(0);
		List fragments= variableDeclStatement.fragments();
		assertTrue("Number of fragments not 5", fragments.size() == 5);
		
		{ // rename var, add dimension
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(0);
			
			ASTNode name= ast.newSimpleName("a");
			ASTRewriteAnalyzer.markAsReplaced(fragment.getName(), name);
			
			VariableDeclarationFragment modifierNode= ast.newVariableDeclarationFragment();
			modifierNode.setExtraDimensions(2);
			
			ASTRewriteAnalyzer.markAsModified(fragment, modifierNode);
		}
		
		{ // add initializer
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(1);
			
			Expression initializer= ast.newNumberLiteral("1");
			ASTRewriteAnalyzer.markAsInserted(initializer);
			
			assertTrue("Has initializer", fragment.getInitializer() == null);
			fragment.setInitializer(initializer);
		}
		
		{ // remove initializer
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(2);
			
			assertTrue("Has no initializer", fragment.getInitializer() != null);
			ASTRewriteAnalyzer.markAsRemoved(fragment.getInitializer());
		}
		{ // add dimension, add initializer
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(3);			
			
			VariableDeclarationFragment modifierNode= ast.newVariableDeclarationFragment();
			modifierNode.setExtraDimensions(4);
			
			ASTRewriteAnalyzer.markAsModified(fragment, modifierNode);

			Expression initializer= ast.newNullLiteral();
			ASTRewriteAnalyzer.markAsInserted(initializer);
			
			assertTrue("Has initializer", fragment.getInitializer() == null);
			fragment.setInitializer(initializer);			
			
		}
		{ // remove dimension
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragments.get(4);			
			
			VariableDeclarationFragment modifierNode= ast.newVariableDeclarationFragment();
			modifierNode.setExtraDimensions(0);
			
			ASTRewriteAnalyzer.markAsModified(fragment, modifierNode);
		}					
			
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int a[][], j = 1, k, x[][][][] = null, y= {0, 1};\n");		
		buf.append("    }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
	}
	
}
