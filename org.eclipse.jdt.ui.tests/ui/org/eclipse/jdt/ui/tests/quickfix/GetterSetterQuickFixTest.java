/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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

import java.util.ArrayList;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class GetterSetterQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testInvisibleFieldToGetterSetter() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("b112441", false, null);
		String str= """
			package b112441;
			
			public class C {
			    private byte test;
			
			    public byte getTest() {
			        return this.test;
			    }
			
			    public void setTest(byte test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        ++c.test;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package b112441;
			
			public class C {
			    private byte test;
			
			    public byte getTest() {
			        return this.test;
			    }
			
			    public void setTest(byte test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.setTest((byte) (c.getTest() + 1));
			    }
			}
			""";

		expected[1]= """
			package b112441;
			
			public class C {
			    byte test;
			
			    public byte getTest() {
			        return this.test;
			    }
			
			    public void setTest(byte test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        ++c.test;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvisibleFieldToGetterSetterBug335173_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(int x){
			        C c=new C();
			        c.test+= x;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(int x){
			        C c=new C();
			        c.setTest(c.getTest() + x);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvisibleFieldToGetterSetterBug335173_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.test+= 1 + 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.setTest(c.getTest() + 1 + 2);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvisibleFieldToGetterSetterBug335173_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.test-= 1 + 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.setTest(c.getTest() - (1 + 2));
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvisibleFieldToGetterSetterBug335173_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.test*= 1 + 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class C {
			    private int test;
			
			    public int getTest() {
			        return this.test;
			    }
			
			    public void setTest(int test) {
			        this.test = test;
			    }
			}
			
			class D {
			    public void foo(){
			        C c=new C();
			        c.setTest(c.getTest() * (1 + 2));
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateFieldUsingSef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str= """
			
			public class A {
			    private int t;
			    {
			        System.out.println(t);
			    }
			}
			
			class B {
			    {
			        new A().t=5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			
			public class A {
			    int t;
			    {
			        System.out.println(t);
			    }
			}
			
			class B {
			    {
			        new A().t=5;
			    }
			}
			""";

		expected[1]= """
			
			public class A {
			    private int t;
			    {
			        System.out.println(getT());
			    }
			    public int getT() {
			        return t;
			    }
			    public void setT(int t) {
			        this.t = t;
			    }
			}
			
			class B {
			    {
			        new A().setT(5);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}
