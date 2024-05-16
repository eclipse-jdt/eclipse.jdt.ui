/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class UnresolvedTypesQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		String newFileTemplate= "${package_declaration}\n\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    Vector1 vec;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			import java.util.Vector;
			
			public class E {
			    Vector vec;
			}
			""";

		String expected2= """
			package test1;
			
			public class Vector1 {
			
			}
			""";

		String expected3= """
			package test1;
			
			public interface Vector1 {
			
			}
			""";

		String expected4= """
			package test1;
			
			public enum Vector1 {
			
			}
			""";

		String expected5= """
			package test1;
			public class E<Vector1> {
			    Vector1 vec;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5 });

	}

	@Test
	public void testTypeInMethodArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(Vect1or[] vec) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			import java.util.Vector;
			
			public class E {
			    void foo(Vector[] vec) {
			    }
			}
			""";

		String expected2= """
			package test1;
			
			public class Vect1or {
			
			}
			""";

		String expected3= """
			package test1;
			
			public interface Vect1or {
			
			}
			""";

		String expected4= """
			package test1;
			
			public enum Vect1or {
			
			}
			""";

		String expected5= """
			package test1;
			public class E<Vect1or> {
			    void foo(Vect1or[] vec) {
			    }
			}
			""";

		String expected6= """
			package test1;
			public class E {
			    <Vect1or> void foo(Vect1or[] vec) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInMethodReturnType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    Vect1or[] foo() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			import java.util.Vector;
			
			public class E {
			    Vector[] foo() {
			        return null;
			    }
			}
			""";

		String expected2= """
			package test1;
			
			public class Vect1or {
			
			}
			""";

		String expected3= """
			package test1;
			
			public interface Vect1or {
			
			}
			""";

		String expected4= """
			package test1;
			
			public enum Vect1or {
			
			}
			""";

		String expected5= """
			package test1;
			public class E<Vect1or> {
			    Vect1or[] foo() {
			        return null;
			    }
			}
			""";

		String expected6= """
			package test1;
			public class E {
			    <Vect1or> Vect1or[] foo() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInExceptionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() throws IOExcpetion {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			import java.io.IOException;
			
			public class E {
			    void foo() throws IOException {
			    }
			}
			""";

		String expected2= """
			package test1;
			
			public class IOExcpetion extends Exception {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}


	@Test
	public void testTypeInVarDeclWithWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    void foo(ArrayList<? extends Runnable> a) {
			        XY v= a.get(0);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);


		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    void foo(ArrayList<? extends Runnable> a) {
			        Runnable v= a.get(0);
			    }
			}
			""";

		String expected2= """
			package test1;
			
			public class XY {
			
			}
			""";

		String expected3= """
			package test1;
			
			public interface XY {
			
			}
			""";

		String expected4= """
			package test1;
			
			public enum XY {
			
			}
			""";

		String expected5= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    <XY> void foo(ArrayList<? extends Runnable> a) {
			        XY v= a.get(0);
			    }
			}
			""";

		String expected6= """
			package test1;
			import java.util.ArrayList;
			public class E<XY> {
			    void foo(ArrayList<? extends Runnable> a) {
			        XY v= a.get(0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    void foo() {
			        ArrayList v= new ArrayListist();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    void foo() {
			        ArrayList v= new ArrayList();
			    }
			}
			""";

		String expected2= """
			package test1;
			
			import java.util.ArrayList;
			
			public class ArrayListist extends ArrayList {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}


	@Test
	public void testArrayTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.*;
			public class E {
			    void foo() {
			        Serializable[] v= new ArrayListExtra[10];
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.io.*;
			public class E {
			    void foo() {
			        Serializable[] v= new Serializable[10];
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.io.*;
			import java.util.ArrayList;
			public class E {
			    void foo() {
			        Serializable[] v= new ArrayList[10];
			    }
			}
			""";

		String expected3= """
			package test1;
			
			import java.io.Serializable;
			
			public class ArrayListExtra implements Serializable {
			
			}
			""";

		String expected4= """
			package test1;
			
			import java.io.Serializable;
			
			public interface ArrayListExtra extends Serializable {
			
			}
			""";

		String expected5= """
			package test1;
			
			import java.io.Serializable;
			
			public enum ArrayListExtra implements Serializable {
			
			}
			""";

		String expected6= """
			package test1;
			import java.io.*;
			public class E<ArrayListExtra> {
			    void foo() {
			        Serializable[] v= new ArrayListExtra[10];
			    }
			}
			""";

		String expected7= """
			package test1;
			import java.io.*;
			public class E {
			    <ArrayListExtra> void foo() {
			        Serializable[] v= new ArrayListExtra[10];
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7});
	}

	@Test
	public void testQualifiedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        test2.Test t= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test2;
			
			public class Test {
			
			}
			""";

		String expected2= """
			package test2;
			
			public interface Test {
			
			}
			""";

		String expected3= """
			package test2;
			
			public enum Test {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testInnerType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        Object object= new F.Inner() {
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class F {
			}
			""";
		pack1.createCompilationUnit("F.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        Object object= new Object() {
			        };
			    }
			}
			""";

		String expected2= """
			package test1;
			public class F {
			
			    public class Inner {
			
			    }
			}
			""";

		String expected3= """
			package test1;
			public class F {
			
			    public interface Inner {
			
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testTypeInCatchBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			        } catch (XXX x) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			public class XXX extends Exception {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E extends XXX {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			public class XXX {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInSuperInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E extends XXX {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			public interface XXX {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInAnnotation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@Xyz
			public interface E {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			public @interface Xyz {
			
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInAnnotation_bug153881() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("a", false, null);
		String str= """
			package a;
			public class SomeClass {
			        @scratch.Unimportant void foo() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("SomeClass.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package scratch;
			
			public @interface Unimportant {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testPrimitiveTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    floot vec= 1.0;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    double vec= 1.0;
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    Float vec= 1.0;
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    float vec= 1.0;
			}
			""";

		String expected4= """
			package test1;
			
			public class floot {
			
			}
			""";

		String expected5= """
			package test1;
			
			public interface floot {
			
			}
			""";

		String expected6= """
			package test1;
			
			public enum floot {
			
			}
			""";

		String expected7= """
			package test1;
			public class E<floot> {
			    floot vec= 1.0;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7});
	}

	@Test
	public void testTypeInTypeArguments1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    class SomeType { }
			    void foo() {
			        E<XYX> list= new E<SomeType>();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String[] expected= new String[6];
		expected[0]= """
			package test1;
			public class E<T> {
			    class SomeType { }
			    void foo() {
			        E<SomeType> list= new E<SomeType>();
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			public class XYX {
			
			}
			""";

		expected[2]= """
			package test1;
			
			public interface XYX {
			
			}
			""";

		expected[3]= """
			package test1;
			
			public enum XYX {
			
			}
			""";

		expected[4]= """
			package test1;
			public class E<T> {
			    class SomeType { }
			    <XYX> void foo() {
			        E<XYX> list= new E<SomeType>();
			    }
			}
			""";

		expected[5]= """
			package test1;
			public class E<T, XYX> {
			    class SomeType { }
			    void foo() {
			        E<XYX> list= new E<SomeType>();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeInTypeArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			public class E<T> {
			    static class SomeType { }
			    void foo() {
			        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String[] expected= new String[6];
		expected[0]= """
			package test1;
			import java.util.Map;
			public class E<T> {
			    static class SomeType { }
			    void foo() {
			        E<Map<String, ? extends SomeType>> list= new E<Map<String, ? extends SomeType>>() {
			        };
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			import test1.E.SomeType;
			
			public class XYX extends SomeType {
			
			}
			""";

		expected[2]= """
			package test1;
			
			public interface XYX {
			
			}
			""";

		expected[3]= """
			package test1;
			
			public enum XYX {
			
			}
			""";

		expected[4]= """
			package test1;
			import java.util.Map;
			public class E<T> {
			    static class SomeType { }
			    <XYX> void foo() {
			        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {
			        };
			    }
			}
			""";

		expected[5]= """
			package test1;
			import java.util.Map;
			public class E<T, XYX> {
			    static class SomeType { }
			    void foo() {
			        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testParameterizedType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    void foo(XXY<String> b) {
			        b.foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			public class XXY<T> {
			
			}
			""";

		expected[1]= """
			package test1;
			
			public interface XXY<T> {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testParameterizedType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			public class E<T> {
			    static class SomeType<S1, S2> { }
			    void foo() {
			        SomeType<String, String> list= new XXY<String, String>() { };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		proposals.addAll(collectCorrections(cu, problems[1], null));

		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			import java.util.Map;
			public class E<T> {
			    static class SomeType<S1, S2> { }
			    void foo() {
			        SomeType<String, String> list= new SomeType<String, String>() { };
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			import test1.E.SomeType;
			
			public class XXY<T1, T2> extends SomeType<String, String> {
			
			}
			""";

		expected[2]= """
			package test1;
			
			public interface XXY<T1, T2> {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	private void createSomeAmbiguity(boolean ifc, boolean isException) throws Exception {

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack3.createCompilationUnit("A.java", buf.toString(), false, null);

		String str= """
			package test3;
			public class B {
			}
			""";
		pack3.createCompilationUnit("B.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);

		String str1= """
			package test2;
			public class C {
			}
			""";
		pack2.createCompilationUnit("C.java", str1, false, null);
	}


	@Test
	public void testAmbiguousTypeInSuperClass() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E extends A {
			    B b;
			    C c;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E extends A {
			    B b;
			    C c;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E extends A {
			    B b;
			    C c;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInInterface() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E implements A {
			    B b;
			    C c;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E implements A {
			    B b;
			    C c;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E implements A {
			    B b;
			    C c;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInField() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E {
			    A a;
			    B b;
			    C c;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E {
			    A a;
			    B b;
			    C c;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E {
			    A a;
			    B b;
			    C c;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInArgument() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo(A a) {\
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo(A a) {\
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E {
			    B b;
			    C c;
			    public void foo(A a) {\
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInReturnType() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public A foo() {\
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public A foo() {\
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E {
			    B b;
			    C c;
			    public A foo() {\
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInExceptionType() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo() throws A {\
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo() throws A {\
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E {
			    B b;
			    C c;
			    public void foo() throws A {\
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInCatchBlock() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import test2.*;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo() {\
			        try {
			        } catch (A e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.*;
			import test2.A;
			import test3.*;
			public class E {
			    B b;
			    C c;
			    public void foo() {\
			        try {
			        } catch (A e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.*;
			import test3.*;
			import test3.A;
			public class E {
			    B b;
			    C c;
			    public void foo() {\
			        try {
			        } catch (A e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	/**
	 * Offers to raise visibility of method instead of class.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=94755
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testIndirectRefDefaultClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test1;
			class B {
			    public Object get(Object c) {
			    	return null;
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			    B b = new B();
			    public B getB() {
			    	return b;
			    }
			}
			""";
		cu= pack1.createCompilationUnit("A.java", str1, false, null);

		String str2= """
			package test2;
			import test1.A;
			public class C {
			    public Object getSide(A a) {
			    	return a.getB().get(this);
			    }
			}
			""";
		cu= pack2.createCompilationUnit("C.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class B {
			    public Object get(Object c) {
			    	return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testForEachMissingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			import java.util.*;
			
			public class E {
			    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {
			        for (element: list) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.*;
			
			public class E {
			    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {
			        for (HashSet<? super Integer> element: list) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
	@Test
	public void testBug530193() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragmentRoot testSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src-tests", new Path[0], new Path[0], "bin-tests",
				new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") });

		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		String str= """
			package pp;
			public class C1 {
			    Tests at=new Tests();
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", str, false, null);

		IPackageFragment pack2= testSourceFolder.createPackageFragment("pt", false, null);
		String str1= """
			package pt;
			public class Tests {
			}
			""";
		pack2.createCompilationUnit("Tests.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot, 2, 1);
		assertFalse(proposals.stream().anyMatch(p -> "Import 'Tests' (pt)".equals(p.getDisplayString())));
	}

	@Test
	public void testBug321464() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			class E<T extends StaticInner> {
			    public static class StaticInner {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			class E<T extends pack.E.StaticInner> {
			    public static class StaticInner {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testIssue485() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    public void foo() {
			        Date d1= new Date();
			        Date d2;
			        d2=new Date();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.Date;
			
			public class E {
			    public void foo() {
			        Date d1= new Date();
			        Date d2;
			        d2=new Date();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testIssue649() throws Exception {
		IPackageFragment defaultPackage= fSourceFolder.createPackageFragment("", false, null);
		String str= """
			//Comment 1
			//Comment 2
			
			public class E {
			    public void foo() {
			        Date d1= new Date();
			        Date d2;
			        d2=new Date();
			    }
			}
			""";
		ICompilationUnit cu= defaultPackage.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			//Comment 1
			//Comment 2
			
			import java.util.Date;
			
			public class E {
			    public void foo() {
			        Date d1= new Date();
			        Date d2;
			        d2=new Date();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
}
