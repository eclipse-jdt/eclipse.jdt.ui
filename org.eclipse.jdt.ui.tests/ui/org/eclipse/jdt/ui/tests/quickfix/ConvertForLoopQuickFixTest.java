/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.ConvertForLoopProposal;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class ConvertForLoopQuickFixTest extends QuickFixTest {

	private static final Class THIS= ConvertForLoopQuickFixTest.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private ConvertForLoopProposal fConvertLoopProposal;

	public ConvertForLoopQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ConvertForLoopQuickFixTest("testSimplestClean"));
			return new ProjectTestSetup(suite);
		}
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		        String preview1 = getPreviewContent(fConvertLoopProposal);
		        
		
		        buf = new StringBuffer();
		        buf.append("package test1;\n");
		        buf.append("class Weirdy{}\n");
		        buf.append("public class A {\n");
		        buf.append("	private Weirdy[] weirdies;\n");
		        buf.append("	private Weirdy[] getArray(){\n");
		        buf.append("		return weirdies;\n");
		        buf.append("	}\n");
		        buf.append("    public void foo(){\n");
		        buf.append("		for (Weirdy p : getArray()) {\n");
		        buf.append("			System.out.println();\n");
			    buf.append("		    if (p != null){\n");
			    buf.append("				System.out.println(p);\n");
			    buf.append("	    	}\n");
			    buf.append("	    }\n");
		        buf.append("    }\n");
		        buf.append("}\n");
		        String expected = buf.toString();
		        assertEqualString(preview1, expected);
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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

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

		List proposals= fecthConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}
	
	public void testIndexDoesNotStartFromZero() throws Exception{
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

		List proposals= fecthConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}
	private List fecthConvertingProposal(StringBuffer buf, ICompilationUnit cu) {
		int offset= buf.toString().indexOf("for");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);

		for (Iterator it= proposals.iterator(); it.hasNext();) {
			CUCorrectionProposal proposal= (CUCorrectionProposal)it.next();
			if (proposal instanceof ConvertForLoopProposal) {
				fConvertLoopProposal= (ConvertForLoopProposal)proposal;
			}
		}
		return proposals;
	}
}
