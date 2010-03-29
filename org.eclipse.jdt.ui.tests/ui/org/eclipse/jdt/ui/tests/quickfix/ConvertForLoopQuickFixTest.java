/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.fix.ConvertForLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public class ConvertForLoopQuickFixTest extends QuickFixTest {

	private static final Class THIS= ConvertForLoopQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private FixCorrectionProposal fConvertLoopProposal;

	public ConvertForLoopQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
//		return new ProjectTestSetup(new ConvertForLoopQuickFixTest("testSimplestClean"));
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fConvertLoopProposal= null;
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
		fJProject1= null;
		fSourceFolder= null;
		fConvertLoopProposal= null;
	}

	public void testSimplestSmokeCase() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testInferPrimitiveTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		char[] array = {'1','2'};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testInferTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testSimplestClean() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testLotsOfRefereces() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testInferCollectionFromInitializers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testNiceReduction() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testNiceReductionArrayIsField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testArrayIsQualifiedByThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testArrayIsAccessedByMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testArrayIsAccessedByMethodInvocation2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testMatrix() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testMatrix2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testArrayIsAssigned() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testArrayIsAssigned2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testArrayCannotBeInferred() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexBruteModified() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexBruteModified2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexReadOutsideArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexReadOutsideArrayAccess_StringConcatenation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexReadOutsideInferredArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexReadOutsideInferredArrayAccess2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testReverseTraversalIsNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testCollectionIsNotArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		java.util.List list = new ArrayList();\n");
		buf.append("		list.add(null);\n");
		buf.append("		for (int i = 0; i < list.size(); i++){\n");
		buf.append("			System.out.println(list.get(i);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testAdditionalLocalIsNotReferenced() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testTwoIndexesNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testAdditionalLocalIsNotReferenced2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testCollectionTypeBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testCollectionBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testCollectionsNotAcceptedYet() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testIndexDoesNotStartFromZero() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array= null;\n");
		buf.append("		for (int i= 1; i < array.length; i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug127346() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int[] arr= new int[7]; 1 < arr.length;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug130139_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug130139_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug130293_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug130293_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug138353_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug138353_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug148419() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug149797() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testBug163050_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug163050_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int j = 0; j < x.length; j++) {\n");
		buf.append("            System.out.println(x[0]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug163121() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i= 0; i < y.length; i++)\n");
		buf.append("            for (Object element : x)\n");
		buf.append("                System.out.println(y[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	private List fetchConvertingProposal(StringBuffer buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);

		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	public void testInitializerPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testInitializerPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 1; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	public void testInitializerPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testInitializerPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testInitializerPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; i < x.length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testInitializerPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testExpressionPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length > i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testExpressionPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length <= i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	public void testExpressionPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; x.length < j; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	public void testExpressionPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testExpressionPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testExpressionPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testExpressionPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; i < length; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testExpressionPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0, length= x.length; length > i; i++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testUpdatePrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i+= 1) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testUpdatePrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= 1 + i) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testUpdatePrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= i + 1) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	public void testUpdatePrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i= i + 2) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	public void testUpdatePrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPreconditio10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBodyPrecondition13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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
	
	public void testBug110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug175827() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug214340_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBug214340_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

	public void testBug214340_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		List proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
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

	public void testBug231575_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

	private boolean satisfiesPrecondition(ICompilationUnit cu) {
		ForStatement statement= getForStatement(cu);
		ConvertLoopOperation op= new ConvertForLoopOperation(statement);
		return op.satisfiesPreconditions().isOK();
	}

	private static ForStatement getForStatement(ICompilationUnit cu) {
		CompilationUnit ast= SharedASTProvider.getAST(cu, SharedASTProvider.WAIT_YES, new NullProgressMonitor());

		final ForStatement[] statement= new ForStatement[1];
		ast.accept(new GenericVisitor() {
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
