/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;

public class AssistQuickFixTest10 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new Java10ProjectTestSetup();

	private static final Class<?>[] TYPE_CHANGE_PROPOSAL_TYPE= { TypeChangeCorrectionProposal.class };
	private static final Class<?>[] LINKED_PROPOSAL_TYPE= { LinkedCorrectionProposal.class };

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

//	public static Test setUpTest(Test test) {
//		return new Java10ProjectTestSetup(test) {
//			@Override
//			protected void setUp() throws Exception {
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
//				super.setUp();
//			}
//
//			@Override
//			protected void tearDown() throws Exception {
//				super.tearDown();
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
//			}
//		};
//	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testChangeVarTypeToBindingTypeProposal() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    var one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    String one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testChangeTypeToVarTypeProposal() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    String one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    var one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	@Test
	public void testChangeVarTypeToBindingTypeProposalWithTypeAnnotation() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" @Target(ElementType.TYPE_USE)\n");
		buf.append(" public @interface N {\n\n");
		buf.append(" }\n\n");
		buf.append(" {\n");
		buf.append("    var one= (@N String) new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" @Target(ElementType.TYPE_USE)\n");
		buf.append(" public @interface N {\n\n");
		buf.append(" }\n\n");
		buf.append(" {\n");
		buf.append("    @N String one= (@N String) new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testChangeTypeToVarTypeProposalWithoutTypeAnnotation() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" @Target(ElementType.TYPE_USE)\n");
		buf.append(" public @interface N {\n\n");
		buf.append(" }\n\n");
		buf.append(" {\n");
		buf.append("    @N String one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" @Target(ElementType.TYPE_USE)\n");
		buf.append(" public @interface N {\n\n");
		buf.append(" }\n\n");
		buf.append(" {\n");
		buf.append("    var one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	@Test
	public void testChangeVarToTypeNoTypeChangeProposal() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   var one = new Object() {     // inferred\n");
		buf.append("       int field = 2;\n");
		buf.append("   };\n");
		buf.append("   var two= (CharSequence & Comparable<String>) \"x\";\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 0);

		offset= buf.toString().indexOf("two");

		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 0);

	}

	@Test
	public void testChangeTypeToVarChangeProposalRemoveUnusedImport() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Second {\n");
		buf.append("   public static List<String> getList() { \n");
		buf.append("      return null;\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Second.java", buf.toString(), false, null);


		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   List<String> one = Second.getList(); \n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   var one = Second.getList(); \n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testChangeTypeToVarChangeProposalDonotRemoveUsedImport() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Second {\n");
		buf.append("   public static List<String> getList() { \n");
		buf.append("      return null;\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Second.java", buf.toString(), false, null);


		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   List<String> one = Second.getList(); \n");
		buf.append("   List<String> two = Second.getList(); \n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   var one = Second.getList(); \n");
		buf.append("   List<String> two = Second.getList(); \n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testChangeParametrizedTypeToVarTypeProposalDoesNotLoseTypeArguments() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.ArrayList;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    ArrayList<String> one= new ArrayList<>();\n");
		buf.append(" }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import java.util.ArrayList;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    var one= new ArrayList<String>();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	@Test
	public void testConvertToForUsingIndexWithVar() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("public class Cls {\n");
		buf.append("  public void foo() {\n");
	    buf.append("    for (var y : new int[0]) {\n");
	    buf.append("      System.out.println(y);\n");
	    buf.append("    }\n");
	    buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("for");

		AssistContext context= getCorrectionContext(cu, offset, 3);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), LINKED_PROPOSAL_TYPE);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("public class Cls {\n");
		buf.append("  public void foo() {\n");
	    buf.append("    var is = new int[0];\n");
	    buf.append("    for (int i = 0; i < is.length; i++) {\n");
	    buf.append("        var y = is[i];\n");
	    buf.append("        System.out.println(y);\n");
	    buf.append("    }\n");
	    buf.append("  }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	private static final ArrayList<IJavaCompletionProposal> getExpectedProposals(ArrayList<IJavaCompletionProposal> proposals, Class<?>[] expectedTypes) {
		ArrayList<IJavaCompletionProposal> expected= new ArrayList<>(proposals);
		if (expectedTypes != null && expectedTypes.length > 0) {
			for (Iterator<IJavaCompletionProposal> iter= expected.iterator(); iter.hasNext();) {
				if (!isexpected(iter.next(), expectedTypes)) {
					iter.remove();
				}
			}
		}
		return expected;
	}

	private static boolean isexpected(Object curr, Class<?>[] expectedTypes) {
		for (Class<?> expectedType : expectedTypes) {
			if (expectedType.isInstance(curr)) {
				return true;
			}
		}
		return false;
	}

}
