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
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingMethodDeclTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMethodDeclTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingMethodDeclTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingMethodDeclTest("testMethodDeclChanges"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	public void testMethodDeclChanges() throws Exception {
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // convert constructor to method: insert return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			
			// from constructor to method
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.RETURN_TYPE, newReturnType, null);
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.IS_CONSTRUCTOR, Boolean.FALSE, null);
		}
		{ // change return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			assertTrue("Has no return type: gee", methodDecl.getReturnType() != null);
			
			Type returnType= methodDecl.getReturnType();
			Type newReturnType= astRoot.getAST().newPrimitiveType(PrimitiveType.FLOAT);
			rewrite.markAsReplaced(returnType, newReturnType);
		}
		{ // remove return type
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			assertTrue("Has no return type: hee", methodDecl.getReturnType() != null);
						
			// from method to constructor
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.IS_CONSTRUCTOR, Boolean.TRUE, null);
		}
		{ // rename method name
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			
			SimpleName name= methodDecl.getName();
			SimpleName newName= ast.newSimpleName("xii");
			
			rewrite.markAsReplaced(name, newName);
		}				
		{ // rename first param & last throw statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			rewrite.markAsReplaced((ASTNode) parameters.get(0), newParam);
						
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException);			
		}
		{ // rename first and second param & rename first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			rewrite.markAsReplaced((ASTNode) parameters.get(0), newParam1);
			rewrite.markAsReplaced((ASTNode) parameters.get(1), newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException1= ast.newSimpleName("ArrayStoreException");
			Name newThrownException2= ast.newSimpleName("InterruptedException");
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(0), newThrownException1);
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(2), newThrownException2);
		}		
		{ // rename all params & rename second exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");			
			SingleVariableDeclaration newParam3= createNewParam(ast, "m3");	
			rewrite.markAsReplaced((ASTNode) parameters.get(0), newParam1);
			rewrite.markAsReplaced((ASTNode) parameters.get(1), newParam2);
			rewrite.markAsReplaced((ASTNode) parameters.get(2), newParam3);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			Name newThrownException= ast.newSimpleName("ArrayStoreException");
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException);
		}				
		
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
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
		clearRewrite(rewrite);
	}
	
	public void testListRemoves() throws Exception {
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete first param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
		}
		{ // delete second param & remove exception & remove public
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.MODIFIERS, new Integer(0), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(0));
		}		
		{ // delete last param
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(2));	
		}				
		{ // delete first and second param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(0));	
		}				
		{ // delete first and last param & remove second
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(2));
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(1));			
		}
		{ // delete second and last param & remove first exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			rewrite.markAsRemoved((ASTNode) parameters.get(2));
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(1));
		}		
		{ // delete all params & remove first and last exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			rewrite.markAsRemoved((ASTNode) parameters.get(2));
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(0));
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(2));				
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p2, int p3) {}\n");
		buf.append("    void gee(int p1, int p3) {}\n");
		buf.append("    public void hee(int p1, int p2) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p3) throws IllegalAccessException {}\n");
		buf.append("    public void jee(int p2) throws IllegalArgumentException {}\n");
		buf.append("    public abstract void kee(int p1) throws IllegalArgumentException, SecurityException;\n");
		buf.append("    public abstract void lee() throws IllegalAccessException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testListInserts() throws Exception {
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert before first param & insert an exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(0, newParam);
			rewrite.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			rewrite.markAsInserted(newThrownException);
		}
		{ // insert before second param & insert before first exception & add synchronized
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.SYNCHRONIZED;
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(1, newParam);
			rewrite.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(0, newThrownException);
			rewrite.markAsInserted(newThrownException);
		}		
		{ // insert after last param & insert after first exception & add synchronized, static
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.SYNCHRONIZED | Modifier.STATIC;
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(newParam);
			rewrite.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			rewrite.markAsInserted(newThrownException);
		}				
		{ // insert 2 params before first & insert between two exception
			MethodDeclaration methodDecl= findMethodDeclaration(type, "iee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam1);
			parameters.add(1, newParam2);
			rewrite.markAsInserted(newParam1);
			rewrite.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(1, newThrownException);
			rewrite.markAsInserted(newThrownException);
		}			
		{ // insert 2 params after first & replace the second exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "jee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(1, newParam1);
			parameters.add(2, newParam2);
			rewrite.markAsInserted(newParam1);
			rewrite.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 2 thrown exceptions", thrownExceptions.size() == 2);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException1);
			rewrite.markAsInserted(newThrownException1);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(1), newThrownException2);
		}
		{ // insert 2 params after last & remove the last exception and insert new after
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(newParam1);
			parameters.add(newParam2);
			rewrite.markAsInserted(newParam1);
			rewrite.markAsInserted(newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException);
			rewrite.markAsInserted(newThrownException);
			
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(2));			
		}	
		{ // insert at first and last position & remove 2nd, add after 2nd, remove 3rd
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam1);
			parameters.add(newParam2);
			rewrite.markAsInserted(newParam1);
			rewrite.markAsInserted(newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(1));
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(2));
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(2, newThrownException);
			rewrite.markAsInserted(newThrownException);			
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m, int p1, int p2, int p3) throws InterruptedException {}\n");
		buf.append("    public synchronized void gee(int p1, float m, int p2, int p3) throws InterruptedException, IllegalArgumentException {}\n");
		buf.append("    public static synchronized void hee(int p1, int p2, int p3, float m) throws IllegalArgumentException, InterruptedException {}\n");
		buf.append("    public void iee(float m1, float m2, int p1, int p2, int p3) throws IllegalArgumentException, InterruptedException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, float m1, float m2, int p2, int p3) throws IllegalArgumentException, ArrayStoreException, InterruptedException {}\n");
		buf.append("    public abstract void kee(int p1, int p2, int p3, float m1, float m2) throws IllegalArgumentException, IllegalAccessException, InterruptedException;\n");
		buf.append("    public abstract void lee(float m1, int p1, int p2, int p3, float m2) throws IllegalArgumentException, InterruptedException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}

	public void testListInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // insert at first and last position & remove 2nd, add after 2nd, remove 3rd
			MethodDeclaration methodDecl= findMethodDeclaration(type, "lee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam1);
			parameters.add(newParam2);
			rewrite.markAsInserted(newParam1);
			rewrite.markAsInserted(newParam2);
			
			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 3 thrown exceptions", thrownExceptions.size() == 3);
			
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(1));
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(2));
			
			Name newThrownException= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(2, newThrownException);
			rewrite.markAsInserted(newThrownException);			
		}				


	
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void lee(float m1, int p1, int p2, int p3, float m2) throws IllegalArgumentException, InterruptedException;\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testListCombinations() throws Exception {
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete all and insert after & insert 2 exceptions
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
		
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			rewrite.markAsRemoved((ASTNode) parameters.get(2));

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(newParam);
			rewrite.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException1);
			rewrite.markAsInserted(newThrownException1);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);
			
		}
		{ // delete first 2, replace last and insert after & replace first exception and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			rewrite.markAsReplaced((ASTNode) parameters.get(2), newParam1);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(newParam2);
			rewrite.markAsInserted(newParam2);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			Name modifiedThrownException= ast.newSimpleName("InterruptedException");
			rewrite.markAsReplaced((ASTNode) thrownExceptions.get(0), modifiedThrownException);
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);
		}		
		{ // delete first 2, replace last and insert at first & remove first and insert before
			MethodDeclaration methodDecl= findMethodDeclaration(type, "hee");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);

			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			rewrite.markAsReplaced((ASTNode) parameters.get(2), newParam1);
						
			SingleVariableDeclaration newParam2= createNewParam(ast, "m2");
			parameters.add(0, newParam2);
			rewrite.markAsInserted(newParam2);


			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 1 thrown exceptions", thrownExceptions.size() == 1);
			
			rewrite.markAsRemoved((ASTNode) thrownExceptions.get(0));
						
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);
		}				


		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
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
		clearRewrite(rewrite);
	}
	
	public void testListCombination() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {}\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete all and insert after & insert 2 exceptions
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			List parameters= methodDecl.parameters();
			assertTrue("must be 3 parameters", parameters.size() == 3);
		
			rewrite.markAsRemoved((ASTNode) parameters.get(0));
			rewrite.markAsRemoved((ASTNode) parameters.get(1));
			rewrite.markAsRemoved((ASTNode) parameters.get(2));

			SingleVariableDeclaration newParam= createNewParam(ast, "m");
			parameters.add(newParam);
			rewrite.markAsInserted(newParam);

			List thrownExceptions= methodDecl.thrownExceptions();
			assertTrue("must be 0 thrown exceptions", thrownExceptions.size() == 0);
			
			Name newThrownException1= ast.newSimpleName("InterruptedException");
			thrownExceptions.add(newThrownException1);
			rewrite.markAsInserted(newThrownException1);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			thrownExceptions.add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);
			
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(float m) throws InterruptedException, ArrayStoreException {}\n");
		buf.append("}\n");	
			
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	
	public void testMethodBody() throws Exception {
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			
			Block body= methodDecl.getBody();
			assertTrue("No body: E", body != null);
			
			Block newBlock= ast.newBlock();

			rewrite.markAsReplaced(body, newBlock);
		}
		{ // delete block & set abstract
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			// change flags
			int newModifiers= Modifier.PUBLIC | Modifier.ABSTRACT;
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			Block body= methodDecl.getBody();
			assertTrue("No body: gee", body != null);

			rewrite.markAsRemoved(body);
		}
		{ // insert block & set to private
			MethodDeclaration methodDecl= findMethodDeclaration(type, "kee");
			
			// change flags
			int newModifiers= Modifier.PRIVATE;
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			
			Block body= methodDecl.getBody();
			assertTrue("Has body", body == null);
			
			Block newBlock= ast.newBlock();
			methodDecl.setBody(newBlock);

			rewrite.markAsInserted(newBlock);
		}		

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public E(int p1, int p2, int p3) {\n");
		buf.append("    }\n");
		buf.append("    public abstract void gee(int p1, int p2, int p3) throws IllegalArgumentException;\n");
		buf.append("    public void hee(int p1, int p2, int p3) throws IllegalArgumentException {}\n");
		buf.append("    public void iee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    public void jee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException {}\n");
		buf.append("    private void kee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException {\n");
		buf.append("    }\n");
		buf.append("    public abstract void lee(int p1, int p2, int p3) throws IllegalArgumentException, IllegalAccessException, SecurityException;\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMethodDeclarationExtraDimensions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1() { return null; }\n");
		buf.append("    public Object foo2() throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo3()[][] { return null; }\n");
		buf.append("    public Object foo4()[][] throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo5()[][] { return null; }\n");
		buf.append("    public Object foo6(int i)[][] throws IllegalArgumentException { return null; }\n");
		buf.append("    public Object foo7(int i)[][] { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(1), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			methodDecl.thrownExceptions().add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);			
		}
		{ // add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo2");
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(1), null);
			
			rewrite.markAsRemoved((ASTNode) methodDecl.thrownExceptions().get(0));			
		}		
		{ // remove extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo3");

			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(1), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			methodDecl.thrownExceptions().add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);			
		}
		{ // add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo4");
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(1), null);
			
			rewrite.markAsRemoved((ASTNode) methodDecl.thrownExceptions().get(0));			
		}
		{ // add params, add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo5");
			
			SingleVariableDeclaration newParam1= createNewParam(ast, "m1");
			methodDecl.parameters().add(newParam1);
			rewrite.markAsInserted(newParam1);						
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(4), null);
			
			Name newThrownException2= ast.newSimpleName("ArrayStoreException");
			methodDecl.thrownExceptions().add(newThrownException2);
			rewrite.markAsInserted(newThrownException2);			
		}
		{ // remove params, add extra dim, remove throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo6");
			
			rewrite.markAsRemoved((ASTNode) methodDecl.parameters().get(0));		
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(4), null);
			
			rewrite.markAsRemoved((ASTNode) methodDecl.thrownExceptions().get(0));			
		}
		{ // remove block
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo7");
			rewrite.markAsRemoved(methodDecl.getBody());			
		}					
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1()[] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo2()[] { return null; }\n");
		buf.append("    public Object foo3()[] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo4()[] { return null; }\n");
		buf.append("    public Object foo5(float m1)[][][][] throws ArrayStoreException { return null; }\n");
		buf.append("    public Object foo6()[][][][] { return null; }\n");
		buf.append("    public Object foo7(int i)[][];\n");		
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	
	
	public void testFieldDeclaration() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    int i1= 1;\n");
		buf.append("    int i2= 1, k2= 2, n2= 3;\n");
		buf.append("    static final int i3= 1, k3= 2, n3= 3;\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "A");
		
		FieldDeclaration[] fieldDeclarations= type.getFields();
		assertTrue("Number of fieldDeclarations not 3", fieldDeclarations.length == 3);
		{	// add modifier, change type, add fragment
			FieldDeclaration decl= fieldDeclarations[0];
			
			// add modifier
			int newModifiers= Modifier.FINAL;
			rewrite.markAsReplaced(decl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			PrimitiveType newType= ast.newPrimitiveType(PrimitiveType.BOOLEAN);
			rewrite.markAsReplaced(decl.getType(), newType);
			
			List fragments= decl.fragments();
			
			VariableDeclarationFragment frag=	ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("k1"));
			frag.setInitializer(null);
			
			rewrite.markAsInserted(frag);
			
			fragments.add(frag);
		}
		{	// add modifiers, remove first two fragments, replace last
			FieldDeclaration decl= fieldDeclarations[1];
			
			// add modifier
			int newModifiers= Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT;
			rewrite.markAsReplaced(decl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			List fragments= decl.fragments();
			assertTrue("Number of fragments not 3", fragments.size() == 3);
			
			rewrite.markAsRemoved((ASTNode) fragments.get(0));
			rewrite.markAsRemoved((ASTNode) fragments.get(1));
			
			VariableDeclarationFragment frag=	ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("k2"));
			frag.setInitializer(null);
			
			rewrite.markAsReplaced((ASTNode) fragments.get(2), frag);
		}
		{	// remove modifiers
			FieldDeclaration decl= fieldDeclarations[2];
			
			// change modifier
			int newModifiers= 0;
			rewrite.markAsReplaced(decl, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    final boolean i1= 1, k1;\n");
		buf.append("    static final transient int k2;\n");
		buf.append("    int i3= 1, k3= 2, n3= 3;\n");
		buf.append("}\n");	
		
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    {\n");
		buf.append("        foo();\n");
		buf.append("    }\n");
		buf.append("    static {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		AST ast= astRoot.getAST();
		
		assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
		TypeDeclaration type= findTypeDeclaration(astRoot, "A");
		
		List declarations= type.bodyDeclarations();
		assertTrue("Number of fieldDeclarations not 2", declarations.size() == 2);
		{	// change modifier, replace body
			Initializer initializer= (Initializer) declarations.get(0);
			
			// add modifier
			int newModifiers= Modifier.STATIC;
			rewrite.markAsReplaced(initializer, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
			
			Block block= ast.newBlock();
			block.statements().add(ast.newReturnStatement());
			
			rewrite.markAsReplaced(initializer.getBody(), block);
		}
		{	// change modifier
			Initializer initializer= (Initializer) declarations.get(1);
			
			int newModifiers= 0;
			rewrite.markAsReplaced(initializer, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
			
		}
				
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    static {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	
	public void testMethodDeclarationParamShuffel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(int i, boolean b) { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		AST ast= astRoot.getAST();
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // add extra dim, add throws
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			List params= methodDecl.parameters();
			
			SingleVariableDeclaration first= (SingleVariableDeclaration) params.get(0);
			SingleVariableDeclaration second= (SingleVariableDeclaration) params.get(1);
			rewrite.markAsReplaced(first.getName(), ast.newSimpleName("x"));
			rewrite.markAsReplaced(second.getName(), ast.newSimpleName("y"));
				
			ASTNode copy1= rewrite.createCopy(first);
			ASTNode copy2= rewrite.createCopy(second);
			
			rewrite.markAsReplaced(first, copy2);
			rewrite.markAsReplaced(second, copy1);
			
		}
	
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(boolean y, int x) { return null; }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	

	public void testMethodDeclarationParamShuffel1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(int i, boolean b) { return null; }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ 
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo1");
			
			List params= methodDecl.parameters();
			
			SingleVariableDeclaration first= (SingleVariableDeclaration) params.get(0);
			SingleVariableDeclaration second= (SingleVariableDeclaration) params.get(1);
				
			ASTNode copy2= rewrite.createCopy(second);

			rewrite.markAsReplaced(first, copy2);
			rewrite.markAsRemoved(second);
		}
	
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		CompilationUnitChange compilationUnitChange= proposal.getCompilationUnitChange();
		compilationUnitChange.setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public Object foo1(boolean b) { return null; }\n");
		buf.append("}\n");	
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
		
	public void testMethodDeclaration_bug24916() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    private int DD()[]{\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "DD");
			
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.IS_CONSTRUCTOR, Boolean.TRUE, null);
			rewrite.markAsReplaced(methodDecl, ASTNodeConstants.EXTRA_DIMENSIONS, new Integer(0), null);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    private DD(){\n");
		buf.append("    };\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMethodComments1() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.markAsRemoved(methodDecl);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    /**\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMethodComments2() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo2");
			ASTNode node= rewrite.createCopy(methodDecl);
			
			type.bodyDeclarations().add(1, node);
			
			rewrite.markAsInserted(node);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("    private void foo(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");
		buf.append("\n");				
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testMethodComments3() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.markAsRemoved(methodDecl);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");			
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	private static final boolean BUG_38447= true;
	
	
	public void testBUG_38447() throws Exception {
		
		if (BUG_38447) {
			return;
		}
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("\n"); // missing closing bracket
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			rewrite.markAsRemoved(methodDecl);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class DD {\n");
		buf.append("\n");			
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}

	public void testMethodComments4() throws Exception {
	
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			ASTNode copy= rewrite.createCopy(methodDecl);
			
			type.bodyDeclarations().add(copy);
			rewrite.markAsInserted(copy);
			
			MethodDeclaration newMethodDecl= createNewMethod(astRoot.getAST(), "xoo", false);
			rewrite.markAsReplaced(methodDecl, newMethodDecl);
			
			//MethodDeclaration methodDecl2= findMethodDeclaration(type, "foo1");
			//rewrite.markAsReplaced(methodDecl2, copy);
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    // one line comment\n");
		buf.append("\n");		
		buf.append("    private void xoo(String str) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo2(){\n");
		buf.append("    }\n");
		buf.append("\n");	
		buf.append("    private void foo(){\n");
		buf.append("    } // another\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}
	
	public void testInsertFieldAfter() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    private int fCount1;\n");
		buf.append("\n");	
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("DD.java", buf.toString(), false, null);

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		TypeDeclaration type= findTypeDeclaration(astRoot, "DD");
		{
			List decls= type.bodyDeclarations();
			//FieldDeclaration field= (FieldDeclaration) decls.get(0);

			VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("fColor"));
			FieldDeclaration newField= ast.newFieldDeclaration(frag);
			newField.setType(ast.newPrimitiveType(PrimitiveType.CHAR));
			newField.setModifiers(Modifier.PRIVATE);
			
			rewrite.markAsInserted(newField);
			decls.add(1, newField);
			
		}

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);

		proposal.apply(null);

		buf= new StringBuffer();
		buf.append("package test1;\n");

		buf.append("public class DD {\n");
		buf.append("    private int fCount1;\n");
		buf.append("    private char fColor;\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     */\n");
		buf.append("    private void foo1(){\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
		clearRewrite(rewrite);
	}	
	
	
	
	
}
