/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.core.manipulation.TypeKinds;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class UtilitiesTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testGuessBindingForTypeReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.HashMap;
			public class E<T> {

			    public class F<U> {}
			    X[] x1= new String[0];
			    X[][] x2= new String[][0];
			    X[][] x3= new String[][][0];

			    ArrayList<X> x4= new ArrayList<String>();
			    X<String> x5= new ArrayList<String>();
			    HashMap<ArrayList<String>, X> x6= new HashMap<ArrayList<String>, Number>();
			    HashMap<ArrayList<X>, Number> x7= new HashMap<ArrayList<String>, Number>();

			    HashMap<? extends X, Number> x8= new HashMap<? extends Number, Number>();

			    E<String>.F<X> x9 = new E<String>().new F<Number>();
			    E<String>.X<Number> x10 = new E<String>().new F<Number>();
			    X<String>.F<Number> x11 = new E<String>().new F<Number>();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String[] expected= {
				"java.lang.String", "java.lang.String", "null",
				"java.lang.String", "java.util.ArrayList<java.lang.String>", "java.lang.Number", "java.lang.String",
				"java.lang.Number",
				"java.lang.Number", "test1.E<java.lang.String>.F<java.lang.Number>", "test1.E<java.lang.String>"
		};

		CompilationUnit astRoot= getASTRoot(cu);
		FieldDeclaration[] fields= ((TypeDeclaration) astRoot.types().get(0)).getFields();
		for (int i= 0; i < fields.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, str.indexOf("X", fields[i].getStartPosition()), 1);
			ITypeBinding actualBinding= ASTResolving.guessBindingForTypeReference(node);
			String actual= actualBinding != null ? actualBinding.getQualifiedName() : "null";
			if (!actual.equals(expected[i])) {
				assertEquals("Guessing failed for " + fields[i].toString(), expected[i], actual);
			}
		}
	}

	@Test
	public void testGuessBindingForTypeReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    private Object o;
			    public void foo(String s) {
			        foo((X) o);
			    }
			    public void foo2(List<Number> s) {
			        foo2((X<Number>) o);
			    }
			    public void foo3() {
			        X.foo3();
			    }
			    public Class[][] foo4() {
			        return new X[][];
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String[] expected= { "java.lang.String", "java.util.List<java.lang.Number>", "null",  "java.lang.Class" };

		CompilationUnit astRoot= getASTRoot(cu);
		MethodDeclaration[] methods= ((TypeDeclaration) astRoot.types().get(0)).getMethods();
		for (int i= 0; i < methods.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, str.indexOf("X", methods[i].getStartPosition()), 1);
			ITypeBinding actualBinding= ASTResolving.guessBindingForTypeReference(node);
			String actual= actualBinding != null ? actualBinding.getQualifiedName() : "null";
			if (!actual.equals(expected[i])) {
				assertEquals("Guessing failed for " + methods[i].toString(), expected[i], actual);
			}
		}

	}

	@Test
	public void testGetPossibleTypeKinds() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.HashMap;
			public class E<T> {
			    X x1;
			    X[] x2;
			    X<String> x3;
			    ArrayList<X> x4;
			    ArrayList<? extends X> x5;

			    E<String>.X x6;
			    X<String>.A x7;
			    Object x8= new X();
			    Object x9= new X() { };
			    Object x10= (X) x1;

			    X.A x11;
			    Object x12= X.class;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int[] expected= {
				TypeKinds.ALL_TYPES,
				TypeKinds.ALL_TYPES,
				TypeKinds.CLASSES | TypeKinds.INTERFACES, // X<String>
				TypeKinds.REF_TYPES_AND_VAR,
				TypeKinds.REF_TYPES_AND_VAR,

				TypeKinds.REF_TYPES, // E<String>.X
				TypeKinds.CLASSES | TypeKinds.INTERFACES, //X<String>.A
				TypeKinds.CLASSES, //new X();
				TypeKinds.CLASSES | TypeKinds.INTERFACES, //new X() { };
				TypeKinds.ALL_TYPES,

				TypeKinds.REF_TYPES,
				TypeKinds.REF_TYPES,
		};

		CompilationUnit astRoot= getASTRoot(cu);
		FieldDeclaration[] fields= ((TypeDeclaration) astRoot.types().get(0)).getFields();
		for (int i= 0; i < fields.length; i++) {
			ASTNode node= NodeFinder.perform(astRoot, str.indexOf("X",fields[i].getStartPosition()), 1);
			int kinds= ASTResolving.getPossibleTypeKinds(node);
			if (kinds != expected[i]) {
				assertEquals("Guessing failed for " + fields[i].toString(), expected[i], kinds);
			}
		}
	}

	@Test
	public void testGetPossibleTypeKindsForTypes() throws Exception {
		JavaProjectHelper.set17CompilerOptions(fJProject1, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public sealed interface E permits X {}
			public sealed class F permits X {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int[] expected= {
				TypeKinds.CLASSES | TypeKinds.INTERFACES,
				TypeKinds.CLASSES
		};

		CompilationUnit astRoot= getASTRoot(cu);
		for (int i = 0; i < astRoot.types().size(); i++) {
			TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(i);
			ASTNode node= NodeFinder.perform(astRoot, str.indexOf("X", typeDecl.getStartPosition()), 1);
			int kinds= ASTResolving.getPossibleTypeKinds(node);
			assertEquals("Guessing failed for " + node.toString(), expected[i], kinds);
		}
	}
}
