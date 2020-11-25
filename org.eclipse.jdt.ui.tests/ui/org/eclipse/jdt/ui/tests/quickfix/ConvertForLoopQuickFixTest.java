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
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.fix.ConvertForLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public class ConvertForLoopQuickFixTest extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	IPackageFragmentRoot fSourceFolder;
	private FixCorrectionProposal fConvertLoopProposal;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fConvertLoopProposal= null;
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
		fJProject1= null;
		fSourceFolder= null;
		fConvertLoopProposal= null;
	}

	@Test
	public void testSimplestSmokeCase() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int element : array) {\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNotEqualsComparison() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class B {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0; i != array.length; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class B {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int element : array) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLengthVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class C {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i < len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class C {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int element : array) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLengthVariableNotEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class D {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i != len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("D.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class D {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int element : array) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testWrongComparison() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class E {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i > len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bld.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCaptureList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class F {\n");
		bld.append("    public void foo(List<?> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            Object element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class F {\n");
		bld.append("    public void foo(List<?> wildcardList) {\n");
		bld.append("        for (Object element : wildcardList) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testWildcardList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class G {\n");
		bld.append("    public void foo(List<? extends String> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            String element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("G.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class G {\n");
		bld.append("    public void foo(List<? extends String> wildcardList) {\n");
		bld.append("        for (String element : wildcardList) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testUpperBoundList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class H {\n");
		bld.append("    public void foo(List<? super String> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            Object element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("H.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class H {\n");
		bld.append("    public void foo(List<? super String> wildcardList) {\n");
		bld.append("        for (Object element : wildcardList) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.Collection;\n");
		bld.append("\n");
		bld.append("public class I {\n");
		bld.append("    public void foo(Collection<? super String> wildcardCollection) {\n");
		bld.append("        for (int q = 0; q < wildcardCollection.size(); q++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("I.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);

		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.Collection;\n");
		bld.append("\n");
		bld.append("public class I {\n");
		bld.append("    public void foo(Collection<? super String> wildcardCollection) {\n");
		bld.append("        for (Object element : wildcardCollection) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		String expected= bld.toString();

		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionChildren() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] children = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < children.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] children = {1,2,3,4};\n");
		buf.append("		for (int child : children) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionS() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locations = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < locations.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locations = {1,2,3,4};\n");
		buf.append("		for (int location : locations) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionlocationEntries() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locationEntries = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < locationEntries.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locationEntries = {1,2,3,4};\n");
		buf.append("		for (int locationEntry : locationEntries) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionEntries() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] Entries = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < Entries.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] Entries = {1,2,3,4};\n");
		buf.append("		for (int entry : Entries) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionProxies() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] proxies = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < proxies.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] proxies = {1,2,3,4};\n");
		buf.append("		for (int proxy : proxies) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionAllChildren() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		String[] AllChildren = {\"aaa\",\"bbb\",\"ccc\"};\n");
		buf.append("		for (int i = 0; i < AllChildren.length; i++){\n");
		buf.append("			System.out.println(AllChildren[i]);\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		String[] AllChildren = {\"aaa\",\"bbb\",\"ccc\"};\n");
		buf.append("		for (String child : AllChildren) {\n");
		buf.append("			System.out.println(child);\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferPrimitiveTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		char[] array = {'1','2'};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		char[] array = {'1','2'};\n");
		buf.append("		for (char element : array) {\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (String element : array) {\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testSimplestClean() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (String element : array) {\n");
		buf.append("			System.out.println(element);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLotsOfRefereces() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			if (array[i].equals(\"2\"))\n");
		buf.append("				System.out.println(array[i]);\n");
		buf.append("			else if ((array[i] + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(array[i]) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (String element : array) {\n");
		buf.append("			if (element.equals(\"2\"))\n");
		buf.append("				System.out.println(element);\n");
		buf.append("			else if ((element + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(element) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferCollectionFromInitializers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0, max = array.length; i < max; i++){\n");
		buf.append("			if (array[i].equals(\"2\"))\n");
		buf.append("				System.out.println(array[i]);\n");
		buf.append("			else if ((array[i] + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(array[i]) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (String element : array) {\n");
		buf.append("			if (element.equals(\"2\"))\n");
		buf.append("				System.out.println(element);\n");
		buf.append("			else if ((element + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(element) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNiceReduction() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("private Weirdy[] weirdies;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("private Weirdy[] weirdies;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (Weirdy p : weirdies) {\n");
		buf.append("			System.out.println();\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNiceReductionArrayIsField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (Weirdy p : weirdies) {\n");
		buf.append("			System.out.println();\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsQualifiedByThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = this.weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = this.weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (Weirdy p : this.weirdies) {\n");
		buf.append("			System.out.println();\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsAccessedByMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("	private Weirdy[] getArray(){\n");
		buf.append("		return weirdies;\n");
		buf.append("	}\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = this.weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = getArray()[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayIsAccessedByMethodInvocation2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("	private Weirdy[] getArray(){\n");
		buf.append("		return weirdies;\n");
		buf.append("	}\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = getArray().length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = getArray()[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMatrix() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int i = 0; i < matrix.length; i++){\n");
		buf.append("			System.out.println(matrix[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int[] element : matrix) {\n");
		buf.append("			System.out.println(element);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMatrix2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int i = 0; i < matrix.length; i++){\n");
		buf.append("			for(int j = 0; j < matrix[i].length; j++){\n");
		buf.append("				System.out.println(matrix[i][j]);\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int[] element : matrix) {\n");
		buf.append("			for(int j = 0; j < element.length; j++){\n");
		buf.append("				System.out.println(element[j]);\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsAssigned() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			array[i]=0;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayIsAssigned2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			++array[i];\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayCannotBeInferred() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < 4; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexBruteModified() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("			i++;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexBruteModified2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			i = array.lenght;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			if (i == 1){};\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideArrayAccess_StringConcatenation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(i + array[i]);");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideInferredArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		int[] array2 = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + array2[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideInferredArrayAccess2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public int get(int i) {\n");
		buf.append("        return i; \n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = null;\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[get(i)]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testReverseTraversalIsNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = array.length; i > 0; --i){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionIsNotArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		java.util.List list = new ArrayList();\n");
		buf.append("		list.add(null);\n");
		buf.append("		for (int i = 0; i < list.size(); i++) {\n");
		buf.append("			System.out.println(list.get(i));\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		java.util.List list = new ArrayList();\n");
		buf.append("		list.add(null);\n");
		buf.append("		for (Object element : list) {\n");
		buf.append("			System.out.println(element);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testAdditionalLocalIsNotReferenced() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0, j = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + j++);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testTwoIndexesNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0, j = 0; i < array.length; i++, j++){\n");
		buf.append("			System.out.println(array[i] + j);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testAdditionalLocalIsNotReferenced2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		int i,j;\n");
		buf.append("		for (i = 0, j = 1; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + j++);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionTypeBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		in[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < arra.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List strings= new ArrayList();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List strings= new ArrayList();\n");
		buf.append("		for (Object string : strings);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> strings= new ArrayList<>();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> strings= new ArrayList<>();\n");
		buf.append("		for (String string : strings);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.TreeSet;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		Set<String> strings= new TreeSet<>();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.TreeSet;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		Set<String> strings= new TreeSet<>();\n");
		buf.append("		for (String string : strings);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMapCollectionsNotAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(Map<String, String> map) {\n");
		buf.append("		for (int i= 0; i < map.size(); i++) {\n");
		buf.append("			String x= map.get(\"\" + i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMultipleCollectionsNotAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(Map<String, String> map) {\n");
        buf.append("        List<File> a = new ArrayList<>();\n");
        buf.append("        List<File> b = new ArrayList<>();\n");
        buf.append("        for (int i = 0; i < a.size(); i++) {\n");
        buf.append("            System.out.print(a.get(i) + \" \" + b.get(i));\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug550726() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        List<File> a = new ArrayList<>();\n");
		buf.append("        for (int i = 0; i < a.size(); i++) {\n");
		buf.append("            System.out.print(a);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        List<File> a = new ArrayList<>();\n");
		buf.append("        for (File element : a) {\n");
		buf.append("            System.out.print(a);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayAccessWithCollections() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("        List<String> list = ArrayList.asList(\"a\", \"b\", \"c\", \"d\");\n");
		buf.append("		for (int i = 0, i < list.size(); i++){\n");
		buf.append("			System.out.println(array[i] + list.get(i));\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexDoesNotStartFromZero() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array= null;\n");
		buf.append("		for (int i= 1; i < array.length; i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug127346() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int[] arr= new int[7]; 1 < arr.length;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130139_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String[] strings) {\n");
		buf.append("        int x= 1;\n");
		buf.append("        for (int i= x; i < strings.length; i++) {\n");
		buf.append("            System.out.println(strings[i]);\n");
		buf.append("        }  \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130139_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String[] strings) {\n");
		buf.append("        for (int i= x(); i < strings.length; i++) {\n");
		buf.append("            System.out.println(strings[i]);\n");
		buf.append("        }  \n");
		buf.append("    }\n");
		buf.append("    private int x(){\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130293_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private int[] arr;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.arr.length; i++) {\n");
		buf.append("            System.out.println(this.arr[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private int[] arr;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int element : this.arr) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug130293_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class E1Sub {\n");
		buf.append("        public int[] array;\n");
		buf.append("    }\n");
		buf.append("    private E1Sub e1sub;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.e1sub.array.length; i++) {\n");
		buf.append("            System.out.println(this.e1sub.array[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class E1Sub {\n");
		buf.append("        public int[] array;\n");
		buf.append("    }\n");
		buf.append("    private E1Sub e1sub;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int element : this.e1sub.array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug138353_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class Bar {\n");
		buf.append("        public int[] getBar() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Bar bar1= null;\n");
		buf.append("        Bar bar2= null;\n");
		buf.append("        for (int i = 0; i < bar1.getBar().length; i++) {\n");
		buf.append("            System.out.println(bar1.getBar()[i]);\n");
		buf.append("            System.out.println(bar2.getBar()[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug138353_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class Bar {\n");
		buf.append("        public int[] getBar() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Bar bar1= null;\n");
		buf.append("        for (int i = 0; i < bar1.getBar().length; i++) {\n");
		buf.append("            System.out.println(bar1.getBar()[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug148419() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] ints;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.ints.length; i++) {\n");
		buf.append("            this.ints[i]= 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug149797() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int ba r() {return 0;}\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (int i = 0, max = ints.length, b= bar(); i < max; i++) {\n");
		buf.append("            System.out.println(ints[i] + b);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug163050_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        int i = 0;\n");
		buf.append("        for (int j = 0; j < x.length; j++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        int i = 0;\n");
		buf.append("        for (Object element : x) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug163050_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int j = 0; j < x.length; j++) {\n");
		buf.append("            System.out.println(x[0]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (Object element : x) {\n");
		buf.append("            System.out.println(x[0]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug163121() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i= 0; i < y.length; i++)\n");
		buf.append("            for (Object element : x)\n");
		buf.append("                System.out.println(y[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (Object element2 : y)\n");
		buf.append("            for (Object element : x)\n");
		buf.append("                System.out.println(element2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal(StringBuilder buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal2(StringBuilder buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		offset= buf.toString().indexOf("for", offset+3);
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	@Test
	public void testInitializerPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 1; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        int i;");
		buf.append("        for (i = 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        int i, f;\n");
		buf.append("        for (i = 0, f= 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int j = 0, a = init(); j < x.length; j++) {\n");
		buf.append("            System.out.println(x[j]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int init() {return 0;}\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length > i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length <= i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length < j; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private static class MyClass {\n");
		buf.append("        public int length;\n");
		buf.append("    }\n");
		buf.append("    public void foo(MyClass x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; i < length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; length > i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i+= 1) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= 1 + i) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= i + 1) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= i + 2) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("		int j= 0");
		buf.append("        for (int i = 0; i < x.length; i= j + 1) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition07() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349782
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; ++i) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            System.out.println(this.x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("			i++;");
		buf.append("            System.out.println(this.x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("			System.out.println(i);");
		buf.append("            System.out.println(this.x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            this.x= null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            x= null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            x= null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            x[i]= null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPreconditio10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            --x[i];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            x[i]++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; length > i; i++) {\n");
		buf.append("            System.out.println(length);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(List<String> data) {\n");
		buf.append("        for (Iterator<String> iterator = data.iterator(); iterator.hasNext();) {\n");
		buf.append("            String row = iterator.next();\n");
		buf.append("            row.equals(iterator.hasNext());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.x.length; i++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.x.length; i++) {\n");
		buf.append("            System.out.println(this.x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Object[] x;\n");
		buf.append("    public void foo(Object obj) {\n");
		buf.append("        for (int i = 0; i < ((E) obj).x.length; i++) {\n");
		buf.append("            System.out.println(((E) obj).x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E implements Comparable<Object> {\n");
		buf.append("    private int[] tokens;\n");
		buf.append("    public int compareTo(Object obj) {\n");
		buf.append("        for (int i = 0; i < tokens.length; i++) {\n");
		buf.append("            int v = compare(tokens[i], ((E) obj).tokens[i]);\n");
		buf.append("            if (v != 0)\n");
		buf.append("                return v;\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    private int compare(int i, int j) {\n");
		buf.append("        return i < j ? -1 : i == j ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E implements Comparable<Object> {\n");
		buf.append("    private int[] tokens;\n");
		buf.append("    public int compareTo(Object obj) {\n");
		buf.append("        for (int i = 0; i < this.tokens.length; i++) {\n");
		buf.append("            int v = compare(tokens[i], ((E) obj).tokens[i]);\n");
		buf.append("            if (v != 0)\n");
		buf.append("                return v;\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    private int compare(int i, int j) {\n");
		buf.append("        return i < j ? -1 : i == j ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E implements Comparable<Object> {\n");
		buf.append("    private int[] tokens;\n");
		buf.append("    public int compareTo(Object obj) {\n");
		buf.append("        for (int i = 0; i < ((E) obj).tokens.length; i++) {\n");
		buf.append("            int v = compare(((E) obj).tokens[i], tokens[i]);\n");
		buf.append("            if (v != 0)\n");
		buf.append("                return v;\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    private int compare(int i, int j) {\n");
		buf.append("        return i < j ? -1 : i == j ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_7() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E implements Comparable<Object> {\n");
		buf.append("    private int[] tokens;\n");
		buf.append("    public int compareTo(Object obj) {\n");
		buf.append("        for (int i = 0; i < ((E) obj).tokens.length; i++) {\n");
		buf.append("            int v = compare(((E) obj).tokens[i], this.tokens[i]);\n");
		buf.append("            if (v != 0)\n");
		buf.append("                return v;\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    private int compare(int i, int j) {\n");
		buf.append("        return i < j ? -1 : i == j ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_8() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E implements Comparable<E> {\n");
		buf.append("    private int[] tokens;\n");
		buf.append("    public int compareTo(E obj) {\n");
		buf.append("        for (int i = 0; i < obj.tokens.length; i++) {\n");
		buf.append("            int v = compare(obj.tokens[i], this.tokens[i]);\n");
		buf.append("            if (v != 0)\n");
		buf.append("                return v;\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    private int compare(int i, int j) {\n");
		buf.append("        return i < j ? -1 : i == j ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_9() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    String[] tokens;\n");
		buf.append("    E other;\n");
		buf.append("    private E get(E a) {\n");
		buf.append("        return a;\n");
		buf.append("    }\n");
		buf.append("    public void foo(E arg) {\n");
		buf.append("        for (int i = 0; i < get(other).tokens.length; i++) {\n");
		buf.append("            E other = this; // local var shadows field\n");
		buf.append("            System.out.println(get(other).tokens[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int i = 0; i < ints.length; i++) {\n");
		buf.append("            System.out.println(ints[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int j : ints) {\n");
		buf.append("            System.out.println(j);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug175827() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int i = 0; i < ints.length; i++) {\n");
		buf.append("            System.out.println(ints[i]);\n");
		buf.append("        }\n");
		buf.append("        //Comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int j : ints) {\n");
		buf.append("            System.out.println(j);\n");
		buf.append("        }\n");
		buf.append("        //Comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug214340_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int[] array = new int[3];\n");
		buf.append("\n");
		buf.append("    boolean same(E1 that) {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (this.array[i] != that.array[i])\n");
		buf.append("                return false;\n");
		buf.append("        }\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug214340_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int[] array = new int[3];\n");
		buf.append("    static boolean same(E1 one, E1 two) {\n");
		buf.append("        for (int i = 0; i < one.array.length; i++) {\n");
		buf.append("            if (one.array[i] != two.array[i])\n");
		buf.append("                return false;\n");
		buf.append("        }\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug214340_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int[] array = new int[3];\n");
		buf.append("    static boolean same(E1 one, E1 two) {\n");
		buf.append("        for (int i = 0; i < one.array.length; i++) {\n");
		buf.append("            System.out.println(one.array[i]);\n");
		buf.append("        }\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int[] array = new int[3];\n");
		buf.append("    static boolean same(E1 one, E1 two) {\n");
		buf.append("        for (int element : one.array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug231575_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public void method(E1 copy) {\n");
		buf.append("        for (int i = 0; i < copy.array.length; i++) {\n");
		buf.append("            array[i].equals(null);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug510758_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (!isNull(array[i])) {\n");
		buf.append("                System.out.println(array[i].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (Object element : array) {\n");
		buf.append("            if (!isNull(element)) {\n");
		buf.append("                System.out.println(element.toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug510758_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (!isNull(array[i+1])) {\n");
		buf.append("                System.out.println(array[i+1].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug510758_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (~isNull(array[i])) {\n");
		buf.append("                System.out.println(array[i].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug542936() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder orig= new StringBuilder();

		orig.append("package test1;\n");
		orig.append("public class E1 {\n");
		orig.append("    private Object[] array1;\n");
		orig.append("    private Object[] array2;\n");
		orig.append("    public void method() {\n");
		orig.append("        outer:\n");
		orig.append("        for (int i = 0; i < array1.length; i++) {\n");
		orig.append("            Object o1= array1[i];\n");
		orig.append("            for (int j = 0; j < array2.length; j++) {\n");
		orig.append("                Object o2= array2[j];\n");
		orig.append("                if (o2.equals(o1)) {\n");
		orig.append("                    continue outer;\n");
		orig.append("                }\n");
		orig.append("            }\n");
		orig.append("        }\n");
		orig.append("    }\n");
		orig.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", orig.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(orig, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array1;\n");
		buf.append("    private Object[] array2;\n");
		buf.append("    public void method() {\n");
		buf.append("        outer:\n");
		buf.append("        for (Object o1 : array1) {\n");
		buf.append("            for (int j = 0; j < array2.length; j++) {\n");
		buf.append("                Object o2= array2[j];\n");
		buf.append("                if (o2.equals(o1)) {\n");
		buf.append("                    continue outer;\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);

		proposals= fetchConvertingProposal2(orig, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview2= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array1;\n");
		buf.append("    private Object[] array2;\n");
		buf.append("    public void method() {\n");
		buf.append("        outer:\n");
		buf.append("        for (int i = 0; i < array1.length; i++) {\n");
		buf.append("            Object o1= array1[i];\n");
		buf.append("            for (Object o2 : array2) {\n");
		buf.append("                if (o2.equals(o1)) {\n");
		buf.append("                    continue outer;\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();
		assertEqualString(preview2, expected);
	}

	@Test
	public void testBug562291_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append(
				"package test1;\n" +
				"import java.io.Serializable;\n" +
				"public class A {\n" +
				"	public void test() {\n" +
				"		Object[] foo= new Object[5];\n" +
				"		for (int i= 0; i < foo.length; i++) {\n" +
				"			if (!(foo[i] instanceof Serializable)) {\n" +
				"			}\n" +
				"		}\n" +
				"	}\n" +
				"}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append(
				"package test1;\n" +
				"import java.io.Serializable;\n" +
				"public class A {\n" +
				"	public void test() {\n" +
				"		Object[] foo= new Object[5];\n" +
				"		for (Object element : foo) {\n" +
				"			if (!(element instanceof Serializable)) {\n" +
				"			}\n" +
				"		}\n" +
				"	}\n" +
				"}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug562291_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append(
				"package test1;\n" +
				"import java.io.Serializable;\n" +
				"public class A {\n" +
				"	public void test() {\n" +
				"		boolean[] foo= new boolean[5];\n" +
				"		for (int i= 0; i < foo.length; i++) {\n" +
				"			if (!foo[i]) {\n" +
				"			}\n" +
				"		}\n" +
				"	}\n" +
				"}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuilder();
		buf.append(
				"package test1;\n" +
				"import java.io.Serializable;\n" +
				"public class A {\n" +
				"	public void test() {\n" +
				"		boolean[] foo= new boolean[5];\n" +
				"		for (boolean element : foo) {\n" +
				"			if (!element) {\n" +
				"			}\n" +
				"		}\n" +
				"	}\n" +
				"}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	private boolean satisfiesPrecondition(ICompilationUnit cu) {
		ForStatement statement= getForStatement(cu);
		ConvertLoopOperation op= new ConvertForLoopOperation(statement);
		return op.satisfiesPreconditions().isOK();
	}

	private static ForStatement getForStatement(ICompilationUnit cu) {
		CompilationUnit ast= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, new NullProgressMonitor());

		final ForStatement[] statement= new ForStatement[1];
		ast.accept(new GenericVisitor() {
			@Override
			protected boolean visitNode(ASTNode node) {
				if (node instanceof ForStatement) {
					statement[0]= (ForStatement)node;
					return false;
				} else {
					return super.visitNode(node);
				}
			}
		});

		return statement[0];
	}
}
