/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.core.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ModifierCorrectionsQuickFixTest9 extends QuickFixTest {

	private static final Class<ModifierCorrectionsQuickFixTest9> THIS= ModifierCorrectionsQuickFixTest9.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public ModifierCorrectionsQuickFixTest9(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java9ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= Java9ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java9ProjectTestSetup.getDefaultClasspath());
	}

	public void testAddSafeVarargs1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    private <T> List<T> asList(T ... a) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    @SafeVarargs\n");
		buf.append("    private <T> List<T> asList(T ... a) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
	
	public void testAddSafeVarargs2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import java.util.List;\n");
		buf.append("public interface E {\n");
		buf.append("    private <T> List<T> asList(T ... a) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import java.util.List;\n");
		buf.append("public interface E {\n");
		buf.append("    @SafeVarargs\n");
		buf.append("    private <T> List<T> asList(T ... a) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

}
