/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class GetterSetterQuickFixTest extends QuickFixTest {

	private static final Class THIS= GetterSetterQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public GetterSetterQuickFixTest(String name) {
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
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testInvisibleFieldToGetterSetter() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("b112441", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package b112441;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    private byte test;\n");
		buf.append("\n");
		buf.append("    public byte getTest() {\n");
		buf.append("        return this.test;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void setTest(byte test) {\n");
		buf.append("        this.test = test;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class D {\n");
		buf.append("    public void foo(){\n");
		buf.append("        C c=new C();\n");
		buf.append("        ++c.test;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package b112441;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    private byte test;\n");
		buf.append("\n");
		buf.append("    public byte getTest() {\n");
		buf.append("        return this.test;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void setTest(byte test) {\n");
		buf.append("        this.test = test;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class D {\n");
		buf.append("    public void foo(){\n");
		buf.append("        C c=new C();\n");
		buf.append("        c.setTest((byte) (c.getTest() + 1));\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package b112441;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    byte test;\n");
		buf.append("\n");
		buf.append("    public byte getTest() {\n");
		buf.append("        return this.test;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void setTest(byte test) {\n");
		buf.append("        this.test = test;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class D {\n");
		buf.append("    public void foo(){\n");
		buf.append("        C c=new C();\n");
		buf.append("        ++c.test;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testCreateFieldUsingSef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    private int t;\n");
		buf.append("    {\n");
		buf.append("        System.out.println(t);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B {\n");
		buf.append("    {\n");
		buf.append("        new A().t=5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    int t;\n");
		buf.append("    {\n");
		buf.append("        System.out.println(t);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B {\n");
		buf.append("    {\n");
		buf.append("        new A().t=5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    private int t;\n");
		buf.append("    {\n");
		buf.append("        System.out.println(getT());\n");
		buf.append("    }\n");
		buf.append("    public void setT(int t) {\n");
		buf.append("        this.t = t;\n");
		buf.append("    }\n");
		buf.append("    public int getT() {\n");
		buf.append("        return t;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B {\n");
		buf.append("    {\n");
		buf.append("        new A().setT(5);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

}
