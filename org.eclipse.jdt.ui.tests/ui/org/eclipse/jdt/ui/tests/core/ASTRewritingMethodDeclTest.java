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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingMethodDeclTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMethodDeclTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private ICompilationUnit fCU_E;

	public ASTRewritingMethodDeclTest(String name) {
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
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		fCU_E= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	

	
	public void testMethodDeclChanges() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			methodDecl.setReturnType(newReturnType);
			
			ASTRewriteAnalyzer.markAsInserted(newReturnType);
		}
		{ // change return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			assertTrue("Has no return type: gee", methodDecl.getReturnType() != null);
			
			Type returnType= methodDecl.getReturnType();
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			ASTRewriteAnalyzer.markAsReplaced(returnType, newReturnType);
		}
		{ // remove return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			assertTrue("Has no return type: hee", methodDecl.getReturnType() != null);
			
			Type returnType= methodDecl.getReturnType();
			ASTRewriteAnalyzer.markAsReplaced(returnType, null);
		}
		{ // rename method name
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			
			SimpleName name= methodDecl.getName();
			SimpleName newName= ast.newSimpleName("xii");
			
			ASTRewriteAnalyzer.markAsReplaced(name, newName);
		}				
		{ // rename first param & last throw statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), newParam);
						
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException);			
		}
		{ // rename first and second param & rename first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), newParam1);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException1= ast.newSimpleName("ArrayStoreException");
			Name newThrownException2= ast.newSimpleName("InterruptedException");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), newThrownException1);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(2), newThrownException2);
		}		
		{ // rename all params & rename second exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");			
			SingleVariableDeclaration newParam3= createNewParam(ast, "m3");	
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), newParam1);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), newParam2);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), newParam3);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException);
		}				
		
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public float E(int p1, int p2, int p3) {}\n");
		buf.append("    public float gee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void xii(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(float m, int p2, int p3) throws IllegalArgumentException, ArrayStoreException {}\n");
		buf.append("    public abstract void kee(float m1, float m2, int p3) throws ArrayStoreException, IllegalAccessException, InterruptedException;\n");
		buf.append("    public abstract void lee(float m1, float m2, float m3) throws IllegalArgumentException, ArrayStoreException, SecurityException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testListRemoves() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete first param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
		}
		{ // delete second param & remove exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), null);
		}		
		{ // delete last param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), null);	
		}				
		{ // delete first and second param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), null);	
		}				
		{ // delete first and last param & remove second
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), null);			
		}
		{ // delete second and last param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), null);
		}		
		{ // delete all params & remove first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), null);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(2), null);				
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p2, int p3) {}\n");
		buf.append("    public void gee(int p1, int p3) {}\n");
		buf.append("    public void hee(int p1, int p2) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p3) throws IllegalAccessException {}\n");
		buf.append("    public void jee(int p2) throws IllegalArgumentException {}\n");
		buf.append("    public abstract void kee(int p1) throws IllegalArgumentException, SecurityException;\n");
		buf.append("    public abstract void lee() throws IllegalAccessException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
	}	
	
	public void testListInserts() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert before first param & insert an exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(0, newParam);
			ASTRewriteAnalyzer.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);
		}
		{ // insert before second param & insert before first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(1, newParam);
			ASTRewriteAnalyzer.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(0, newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);
		}		
		{ // insert after last param & insert after first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(newParam);
			ASTRewriteAnalyzer.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);
		}				
		{ // insert 2 params before first & insert between two exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam1);
			parameters.add(1, newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam1);
			ASTRewriteAnalyzer.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(1, newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);
		}			
		{ // insert 2 params after first & replace the second exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(1, newParam1);
			parameters.add(2, newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam1);
			ASTRewriteAnalyzer.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException1);
			ASTRewriteAnalyzer.markAsInserted(newThrownException1);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException2);
		}
		{ // insert 2 params after last & remove the last exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(newParam1);
			parameters.add(newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam1);
			ASTRewriteAnalyzer.markAsInserted(newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(2), null);			
		}	
		{ // insert at first and last position & remove 2nd, add after 2nd, remove 3rd
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam1);
			parameters.add(newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam1);
			ASTRewriteAnalyzer.markAsInserted(newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(1), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(2), null);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(2, newThrownException);
			ASTRewriteAnalyzer.markAsInserted(newThrownException);			
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m, int p1, int p2, int p3) throws InterruptedException {}\n");
		buf.append("    public void gee(int p1, float m, int p2, int p3) throws InterruptedException, IllegalArgumentException {}\n");
		buf.append("    public void hee(int p1, int p2, int p3, float m) throws IllegalArgumentException, InterruptedException {}\n");
		buf.append("    public void iee(float m1, float m2, int p1, int p2, int p3) throws IllegalArgumentException, InterruptedException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, float m1, float m2, int p2, int p3) throws IllegalArgumentException, ArrayStoreException, InterruptedException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3, float m1, float m2) throws IllegalArgumentException, IllegalAccessException, InterruptedException;\n");
		buf.append("    public abstract void lee(float m1, int p1, int p2, int p3, float m2) throws IllegalArgumentException, InterruptedException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testListCombinations() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete all and insert after & insert 2 exceptions
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
		
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), null);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(newParam);
			ASTRewriteAnalyzer.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException1);
			ASTRewriteAnalyzer.markAsInserted(newThrownException1);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			ASTRewriteAnalyzer.markAsInserted(newThrownException2);
			
		}
		{ // delete first 2, replace last and insert after & replace first exception and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), newParam1);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name modifiedThrownException= ast.newSimpleName("InterruptedException");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), modifiedThrownException);
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			ASTRewriteAnalyzer.markAsInserted(newThrownException2);
		}		
		{ // delete first 2, replace last and insert at first & remove first and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(0), null);
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(1), null);
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) parameters.get(2), newParam1);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam2);
			ASTRewriteAnalyzer.markAsInserted(newParam2);


			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) thrownExceptions.get(0), null);
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			ASTRewriteAnalyzer.markAsInserted(newThrownException2);
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("    public void gee(float m1, float m2) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("    public void hee(float m2, float m1) throws ArrayStoreException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testMethodBody() throws Exception {
		ICompilationUnit cu= fCU_E;
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Block body= methodDecl.getBody();
			assertTrue("No body: E", body != null);
			
			Block newBlock= ast.newBlock();

			ASTRewriteAnalyzer.markAsReplaced(body, newBlock);
		}
		{ // delete block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Block body= methodDecl.getBody();
			assertTrue("No body: gee", body != null);

			ASTRewriteAnalyzer.markAsReplaced(body, null);
		}
		{ // insert block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			
			Block body= methodDecl.getBody();
			assertTrue("Has body", body == null);
			
			Block newBlock= ast.newBlock();
			methodDecl.setBody(newBlock);

			ASTRewriteAnalyzer.markAsInserted(newBlock);
		}		

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {\n");
		buf.append("    }\n");
		buf.append("    public void gee(int p1, int p2, int p3) throws IllegalArgumentException ;\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException {\n");
		buf.append("    }\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		
	}
	
}
