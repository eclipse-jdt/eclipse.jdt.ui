/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

public class TypeParameterMismatchTest extends QuickFixTest {

	private static final Class THIS= TypeParameterMismatchTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public TypeParameterMismatchTest(String name) {
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
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testRemoveTypeParameter() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("import java.util.*;\n");
			buf.append("public class A {\n");
			buf.append("}\n");
			buf.append("class B extends A<String> {\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 1);

			String[] expected= new String[1];
			buf= new StringBuffer();
			buf.append("import java.util.*;\n");
			buf.append("public class A {\n");
			buf.append("}\n");
			buf.append("class B extends A {\n");
			buf.append("}\n");
			expected[0]= buf.toString();

			assertExpectedExistInProposals(proposals, expected);
	}

	public void testRemoveTypeParameter2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("import java.util.*;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		buf.append("class C extends A<Set<String>>{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("import java.util.*;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		buf.append("class C extends A{\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
		}



}
