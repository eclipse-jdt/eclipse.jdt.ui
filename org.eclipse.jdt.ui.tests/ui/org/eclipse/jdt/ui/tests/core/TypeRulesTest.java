/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.TypeRules;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class TypeRulesTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		fJProject1= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	private VariableDeclarationFragment[] createVariables() throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			import java.util.Collection;
			import java.io.Serializable;
			import java.net.Socket;
			public class E<T, U extends Number> {
			    boolean bool= false;
			    char c= 0;
			    byte b= 0;
			    short s= 0;
			    int i= 0;
			    long l= 0;
			    float f= 0;
			    double d= 0;
			    Boolean bool_class= null;
			    Character c_class= null;
			    Byte b_class= null;
			    Short s_class= null;
			    Integer i_class= null;
			    Long l_class= null;
			    Float f_class= null;
			    Double d_class= null;
			    Object object= null;
			    Vector vector= null;
			    Socket socket= null;
			    Cloneable cloneable= null;
			    Collection collection= null;
			    Serializable serializable= null;
			    Object[] objectArr= null;
			    int[] int_arr= null;
			    long[] long_arr= null;
			    Vector[] vector_arr= null;
			    Socket[] socket_arr= null;
			    Collection[] collection_arr= null;
			    Object[][] objectArrArr= null;
			    Collection[][] collection_arrarr= null;
			    Vector[][] vector_arrarr= null;
			    Socket[][] socket_arrarr= null;
			    Collection<String> collection_string= null;
			    Collection<Object> collection_object= null;
			    Collection<Number> collection_number= null;
			    Collection<Integer> collection_integer= null;
			    Collection<? extends Number> collection_upper_number= null;
			    Collection<? super Number> collection_lower_number= null;
			    Vector<Object> vector_object= null;
			    Vector<Number> vector_number= null;
			    Vector<Integer> vector_integer= null;
			    Vector<? extends Number> vector_upper_number= null;
			    Vector<? super Number> vector_lower_number= null;
			    Vector<? extends Exception> vector_upper_exception= null;
			    Vector<? super Exception> vector_lower_exception= null;
			    T t= null;
			    U u= null;
			    Vector<T> vector_t= null;
			    Vector<U> vector_u= null;
			    Vector<? extends T> vector_upper_t= null;
			    Vector<? extends U> vector_upper_u= null;
			    Vector<? super T> vector_lower_t= null;
			    Vector<? super U> vector_lower_u= null;
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(cu1);
		parser.setResolveBindings(true);

		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 0);

		TypeDeclaration type= (TypeDeclaration) astRoot.types().get(0);
		FieldDeclaration[] fields= type.getFields();

		VariableDeclarationFragment[] targets= new VariableDeclarationFragment[fields.length];
		for (int i= 0; i < fields.length; i++) {
			List<VariableDeclarationFragment> fragments= fields[i].fragments();
			targets[i]= fragments.get(0);
		}
		return targets;
	}

	//TODO: only tests behavior for ITypeBindings from the same AST. See bug 80715.
	@Test
	public void testIsAssignmentCompatible() throws Exception {
		VariableDeclarationFragment[] targets= createVariables();

		StringBuilder errors= new StringBuilder();
		for (VariableDeclarationFragment f1 : targets) {
			for (VariableDeclarationFragment f2 : targets) {
				String line= f2.getName().getIdentifier() + "= " + f1.getName().getIdentifier();
				StringBuilder buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class F<T, U extends Number> extends E<T, U> {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();

				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(content);
				parser.setResolveBindings(true);
				parser.setProject(fJProject1);
				parser.setUnitName("F.java");

				CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
				IProblem[] problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);
				boolean res2= b1.isAssignmentCompatible(b2);
				if (res2 != (problems.length == 0)) {
					errors.append(line).append('\n');
				}
			}
		}
		assertEquals(errors.toString(), 0, errors.length());
	}

	@Test
	public void testCanAssign() throws Exception {
		VariableDeclarationFragment[] targets= createVariables();

		StringBuilder errors= new StringBuilder();
		for (VariableDeclarationFragment f1 : targets) {
			for (VariableDeclarationFragment f2 : targets) {
				String line= f2.getName().getIdentifier() + "= " + f1.getName().getIdentifier();

				StringBuilder buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class F<T, U extends Number> extends E<T, U> {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();

				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(content);
				parser.setResolveBindings(true);
				parser.setProject(fJProject1);
				parser.setUnitName("F.java");

				CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
				IProblem[] problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);

				boolean res2= TypeRules.canAssign(b1, b2);
				if (res2 != (problems.length == 0)) {
					errors.append(line).append('\n');
				}
			}
		}
		assertEquals(errors.toString(), 0, errors.length());
	}

	@Test
	public void testIsCastCompatible() throws Exception {
		StringBuilder errors= new StringBuilder();
		VariableDeclarationFragment[] targets= createVariables();
		for (VariableDeclarationFragment f1 : targets) {
			for (VariableDeclarationFragment f2 : targets) {
				String castType= f2.resolveBinding().getType().getQualifiedName();
				String line= castType + " x= (" + castType + ") " + f1.getName().getIdentifier();

				StringBuilder buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class F<T, U extends Number> extends E<T, U> {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();

				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(content);
				parser.setResolveBindings(true);
				parser.setProject(fJProject1);
				parser.setUnitName("F.java");

				CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
				IProblem[] problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);
				boolean res= b2.isCastCompatible(b1);
				if (res != (problems.length == 0)) {
					errors.append(line).append('\n');
				}
			}
		}
		assertEquals(errors.toString(), 0, errors.length());
	}

	@Test
	public void testCanCast() throws Exception {
		StringBuilder errors= new StringBuilder();
		VariableDeclarationFragment[] targets= createVariables();
		for (VariableDeclarationFragment f1 : targets) {
			for (VariableDeclarationFragment f2 : targets) {
				String castType= f2.resolveBinding().getType().getQualifiedName();
				String line= castType + " x= (" + castType + ") " + f1.getName().getIdentifier();

				StringBuilder buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class F<T, U extends Number> extends E<T, U> {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();

				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(content);
				parser.setResolveBindings(true);
				parser.setProject(fJProject1);
				parser.setUnitName("F.java");

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);

				//old implementation does not support generics:
				if (b1.isParameterizedType() || b1.isWildcardType() || b1.isTypeVariable())
					continue;
				if (b2.isParameterizedType() || b2.isWildcardType() || b2.isTypeVariable())
					continue;
				if (b1.isRawType() != b2.isRawType())
					continue;
				//old implementation does not support autoboxing:
				if (b1.isPrimitive() != b2.isPrimitive())
					continue;

				CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
				IProblem[] problems= astRoot.getProblems();

				boolean res= TypeRules.canCast(b2, b1);
				if (res != (problems.length == 0)) {
					errors.append(line).append('\n');
				}
			}
		}
		assertEquals(errors.toString(), 0, errors.length());
	}

}
