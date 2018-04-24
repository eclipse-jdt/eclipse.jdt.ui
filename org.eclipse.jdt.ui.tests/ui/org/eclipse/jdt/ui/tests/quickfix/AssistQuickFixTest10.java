/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.core.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AssistQuickFixTest10 extends QuickFixTest {

	private static final Class<AssistQuickFixTest10> THIS= AssistQuickFixTest10.class;

	private static final Class<?>[] TYPE_CHANGE_PROPOSAL_TYPE= { TypeChangeCorrectionProposal.class };

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public AssistQuickFixTest10(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java10ProjectTestSetup(test) {
			@Override
			protected void setUp() throws Exception {
				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
				super.setUp();
			}

			@Override
			protected void tearDown() throws Exception {
				super.tearDown();
				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
			}
		};
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= Java10ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java10ProjectTestSetup.getDefaultClasspath());
	}

	public void testChangeVarTypeToBindingTypeProposal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    String one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeTypeToVarTypeProposal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    var one= new String();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testChangeVarTypeToBindingTypeProposalWithTypeAnnotation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
		buf= new StringBuffer();
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

	public void testChangeTypeToVarTypeProposalWithoutTypeAnnotation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
		buf= new StringBuffer();
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
	
	public void testChangeVarToTypeNoTypeChangeProposal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
	
	public void testChangeTypeToVarChangeProposalRemoveUnusedImport() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Second {\n");
		buf.append("   public static List<String> getList() { \n");
		buf.append("      return null;\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Second.java", buf.toString(), false, null);


		buf= new StringBuffer();
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
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("   var one = Second.getList(); \n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeTypeToVarChangeProposalDonotRemoveUsedImport() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("import java.util.List;\n\n");
		buf.append("public class Second {\n");
		buf.append("   public static List<String> getList() { \n");
		buf.append("      return null;\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Second.java", buf.toString(), false, null);


		buf= new StringBuffer();
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
		buf= new StringBuffer();
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

	public void testChangeParametrizedTypeToVarTypeProposalDoesNotLoseTypeArguments() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);
		
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
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
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("import java.util.ArrayList;\n\n");
		buf.append("public class Cls {\n");
		buf.append(" {\n");
		buf.append("    var one= new ArrayList<String>();\n");
		buf.append(" }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

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
		for (int k= 0; k < expectedTypes.length; k++) {
			if (expectedTypes[k].isInstance(curr)) {
				return true;
			}
		}
		return false;
	}

}
