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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.TypeRules;


public class TypeRulesTest extends CoreTests {
	
	private static final Class THIS= TypeRulesTest.class;
	
	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public TypeRulesTest(String name) {
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
			suite.addTest(new TypeRulesTest("test1"));
			return new ProjectTestSetup(suite);
		}	
	}

	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
		JavaCore.setOptions(options);
		
		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	
	private VariableDeclarationFragment[] createVariables() throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Collection;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("public class E {\n");
		buf.append("    boolean bool= false;\n");
		buf.append("    char c= 0;\n");
		buf.append("    byte b= 0;\n");
		buf.append("    short s= 0;\n");
		buf.append("    int i= 0;\n");
		buf.append("    long l= 0;\n");
		buf.append("    float f= 0;\n");
		buf.append("    double d= 0;\n");
		buf.append("    Object object= null;\n");
		buf.append("    Vector vector= null;\n");
		buf.append("    Socket socket= null;\n");
		buf.append("    Cloneable cloneable= null;\n");
		buf.append("    Collection collection= null;\n");
		buf.append("    Serializable serializable= null;\n");
		buf.append("    Object[] objectArr= null;\n");
		buf.append("    int[] int_arr= null;\n");
		buf.append("    long[] long_arr= null;\n");
		buf.append("    Vector[] vector_arr= null;\n");
		buf.append("    Socket[] socket_arr= null;\n");
		buf.append("    Collection[] collection_arr= null;\n");
		buf.append("    Object[][] objectArrArr= null;\n");
		buf.append("    Collection[][] collection_arrarr= null;\n");
		buf.append("    Vector[][] vector_arrarr= null;\n");
		buf.append("    Socket[][] socket_arrarr= null;\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 0);
		
		TypeDeclaration type= (TypeDeclaration) astRoot.types().get(0);
		FieldDeclaration[] fields= type.getFields();
		
		VariableDeclarationFragment[] targets= new VariableDeclarationFragment[fields.length];
		for (int i= 0; i < fields.length; i++) {
			targets[i]= (VariableDeclarationFragment) fields[i].fragments().get(0);
		}
		return targets;
	}
	
	public void testCanAssign() throws Exception {
		VariableDeclarationFragment[] targets= createVariables();
		
		for (int k= 0; k < targets.length; k++) {
			for (int n= 0; n < targets.length; n++) {
				VariableDeclarationFragment f1= targets[k];
				VariableDeclarationFragment f2= targets[n];
				String line= f2.getName().getIdentifier() + "= " + f1.getName().getIdentifier();
				
				StringBuffer buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class F extends E {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();
				
				CompilationUnit astRoot= AST.parseCompilationUnit(content, "F.java", fJProject1);
				IProblem[] problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);
				boolean res= TypeRules.canAssign(b1, b2.getQualifiedName());
				assertEquals(line, problems.length == 0, res);
				boolean res2= TypeRules.canAssign(b1, b2);
				assertEquals(line, problems.length == 0, res2);			
			}	
		}
	}
	
	public void testCanCast() throws Exception {
		VariableDeclarationFragment[] targets= createVariables();
		for (int k= 0; k < targets.length; k++) {
			for (int n= 0; n < targets.length; n++) {
				VariableDeclarationFragment f1= targets[k];
				VariableDeclarationFragment f2= targets[n];
				
				String castType= f2.resolveBinding().getType().getQualifiedName();
				String line= castType + " x= (" + castType + ") " + f1.getName().getIdentifier();
				
				StringBuffer buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class F extends E {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();
				
				CompilationUnit astRoot= AST.parseCompilationUnit(content, "F.java", fJProject1);
				IProblem[] problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);
				boolean res= TypeRules.canCast(b2, b1);
				if (res != (problems.length == 0)) {
					res= TypeRules.canCast(b2, b1);
					assertTrue(line, false);
				}
			}	
		}
	}
		
	
}
