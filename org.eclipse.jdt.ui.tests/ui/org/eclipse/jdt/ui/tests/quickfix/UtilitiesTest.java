/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.SimilarElementsRequestor;

/**
 *
 */
public class UtilitiesTest extends QuickFixTest {

	private static final Class THIS= UtilitiesTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UtilitiesTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testGuessBindingForTypeReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class E<T> {\n");
		buf.append("\n"); // array types
		buf.append("    public class F<U> {}\n");
		buf.append("    X[] x1= new String[0];\n");
		buf.append("    X[][] x2= new String[][0];\n");
		buf.append("    X[][] x3= new String[][][0];\n");
		buf.append("\n"); // parameterized types
		buf.append("    ArrayList<X> x4= new ArrayList<String>();\n");
		buf.append("    X<String> x5= new ArrayList<String>();\n");
		buf.append("    HashMap<ArrayList<String>, X> x6= new HashMap<ArrayList<String>, Number>();\n");
		buf.append("    HashMap<ArrayList<X>, Number> x7= new HashMap<ArrayList<String>, Number>();\n");
		buf.append("\n"); // wildcard types
		buf.append("    HashMap<? extends X, Number> x8= new HashMap<? extends Number, Number>();\n");
		buf.append("\n"); // qualified types
		buf.append("    E<String>.F<X> x9 = new E<String>().new F<Number>();\n");
		buf.append("    E<String>.X<Number> x10 = new E<String>().new F<Number>();\n");
		buf.append("    X<String>.F<Number> x11 = new E<String>().new F<Number>();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		String[] expected= {
				"java.lang.String", "java.lang.String", "null",
				"java.lang.String", "java.util.ArrayList<java.lang.String>", "java.lang.Number", "java.lang.String",
				"java.lang.Number",
				"java.lang.Number", "test1.E<java.lang.String>.F<java.lang.Number>", "test1.E<java.lang.String>"
		};

		CompilationUnit astRoot= getASTRoot(cu);
		FieldDeclaration[] fields= ((TypeDeclaration) astRoot.types().get(0)).getFields();
		for (int i= 0; i < fields.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, buf.indexOf("X", fields[i].getStartPosition()), 1);
			ITypeBinding actualBinding= ASTResolving.guessBindingForTypeReference(node);
			String actual= actualBinding != null ? actualBinding.getQualifiedName() : "null";
			if (!actual.equals(expected[i])) {
				assertEquals("Guessing failed for " + fields[i].toString(), expected[i], actual);
			}
		}
	}

	public void testGuessBindingForTypeReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    private Object o;\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        foo((X) o);\n");
		buf.append("    }\n");
		buf.append("    public void foo2(List<Number> s) {\n");
		buf.append("        foo2((X<Number>) o);\n");
		buf.append("    }\n");
		buf.append("    public void foo3() {\n");
		buf.append("        X.foo3();\n");
		buf.append("    }\n");
		buf.append("    public Class[][] foo4() {\n");
		buf.append("        return new X[][];\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		String[] expected= { "java.lang.String", "java.util.List<java.lang.Number>", "null",  "java.lang.Class" };

		CompilationUnit astRoot= getASTRoot(cu);
		MethodDeclaration[] methods= ((TypeDeclaration) astRoot.types().get(0)).getMethods();
		for (int i= 0; i < methods.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, buf.indexOf("X", methods[i].getStartPosition()), 1);
			ITypeBinding actualBinding= ASTResolving.guessBindingForTypeReference(node);
			String actual= actualBinding != null ? actualBinding.getQualifiedName() : "null";
			if (!actual.equals(expected[i])) {
				assertEquals("Guessing failed for " + methods[i].toString(), expected[i], actual);
			}
		}

	}

	public void testGetPossibleTypeKinds() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class E<T> {\n");
		buf.append("    X x1;\n");
		buf.append("    X[] x2;\n");
		buf.append("    X<String> x3;\n");
		buf.append("    ArrayList<X> x4;\n");
		buf.append("    ArrayList<? extends X> x5;\n");
		buf.append("\n");
		buf.append("    E<String>.X x6;\n");
		buf.append("    X<String>.A x7;\n");
		buf.append("    Object x8= new X();\n");
		buf.append("    Object x9= new X() { };\n");
		buf.append("    Object x10= (X) x1;\n");
		buf.append("\n");
		buf.append("    X.A x11;\n");
		buf.append("    Object x12= X.class;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int[] expected= {
				SimilarElementsRequestor.ALL_TYPES,
				SimilarElementsRequestor.ALL_TYPES,
				SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES, // X<String>
				SimilarElementsRequestor.REF_TYPES_AND_VAR,
				SimilarElementsRequestor.REF_TYPES_AND_VAR,

				SimilarElementsRequestor.REF_TYPES, // E<String>.X
				SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES, //X<String>.A
				SimilarElementsRequestor.CLASSES, //new X();
				SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES, //new X() { };
				SimilarElementsRequestor.ALL_TYPES,

				SimilarElementsRequestor.REF_TYPES,
				SimilarElementsRequestor.REF_TYPES,
		};

		CompilationUnit astRoot= getASTRoot(cu);
		FieldDeclaration[] fields= ((TypeDeclaration) astRoot.types().get(0)).getFields();
		for (int i= 0; i < fields.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, buf.indexOf("X", fields[i].getStartPosition()), 1);
			int kinds= ASTResolving.getPossibleTypeKinds(node, true);
			if (kinds != expected[i]) {
				assertEquals("Guessing failed for " + fields[i].toString(), expected[i], kinds);
			}
		}
	}
}
