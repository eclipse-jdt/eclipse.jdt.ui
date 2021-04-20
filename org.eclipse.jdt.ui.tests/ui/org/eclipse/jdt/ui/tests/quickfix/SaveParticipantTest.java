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

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class SaveParticipantTest extends CleanUpTestCase {
	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		IEclipsePreferences node= InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN);
		node.putBoolean("editor_save_participant_" + CleanUpPostSaveListener.POSTSAVELISTENER_ID, true);
		node.put(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.TRUE);
	}

	private static void editCUInEditor(ICompilationUnit cu, String newContent) throws JavaModelException, PartInitException {
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);

		cu.getBuffer().setContents(newContent);
		editor.doSave(null);
	}

	@Test
	public void testFormatAll01() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        String s= (String)o;\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        String s    = (String)o;\n" //
				+ "    }\n" //
				+ "}";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Object o) {\n" //
				+ "        String s = (String) o;\n" //
				+ "    }\n" //
				+ "}";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChanges01() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        String s= (String)o;\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        String s    = (String)o;\n" //
				+ "    }\n" //
				+ "}";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        String s = (String) o;\n" //
				+ "    }\n" //
				+ "}";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChanges02() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        Object s= (String)o;\n" //
				+ "}}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        Object s       = (String)o;\n" //
				+ "}}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo( Object o ) {\n" //
				+ "        Object s = o;\n" //
				+ "}}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug205177() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int        a= 1;\n" //
				+ "    int        b= 2;\n" //
				+ "    int        c= 3;\n" //
				+ "    int        d= 4;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int        a= 1;\n" //
				+ "    int        b= 2;//\n" //
				+ "    int        c= 3;\n" //
				+ "    int        d= 4;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int        a= 1;\n" //
				+ "    int b = 2;//\n" //
				+ "    int        c= 3;\n" //
				+ "    int        d= 4;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug205308() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int        a= 1;\n" //
				+ "    int        b= 2;\n" //
				+ "    int        c= 3;\n" //
				+ "    int        d= 4;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int         a= 1;\n" //
				+ "    int        b= 2;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    int a = 1;\n" //
				+ "    int        b= 2;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug205301() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * adsfdas\n" //
				+ "     * dafs\n" //
				+ "     */\n" //
				+ "    int a = 2;\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * adsfasd \n" //
				+ "     * asd\n" //
				+ "     */\n" //
				+ "    int b = 2;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * adsfdas\n" //
				+ "     * dafs \n" //
				+ "     */\n" //
				+ "    int a = 2;\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * adsfasd \n" //
				+ "     * asd\n" //
				+ "     */\n" //
				+ "    int b = 2;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * adsfdas dafs\n" //
				+ "     */\n" //
				+ "    int a = 2;\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * adsfasd \n" //
				+ "     * asd\n" //
				+ "     */\n" //
				+ "    int b = 2;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug207965() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "       protected String foo(String string) {  \n" //
				+ "          int i = 10;\n" //
				+ "          return (\"\" + string + \"\") + \"\";  \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "       protected String foo(String string) {  \n" //
				+ "          int i = 10;\n" //
				+ "          return  (\"\" + string + \"\") + \"\";  \n" //
				+ "    }\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    protected String foo(String string) {\n" //
				+ "        int i = 10;\n" //
				+ "        return (\"\" + string + \"\") + \"\";\n" //
				+ "    }\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug207965_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int i = 10;\n" //
				+ "    \n" //
				+ "    public int j = 10;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int i= 10;\n" //
				+ "    \n" //
				+ "    public int j= 10;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int i = 10;\n" //
				+ "\n" //
				+ "    public int j = 10;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangesBug208568() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int i = 10;    \n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public  int i= 10;    \n" //
				+ "\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int i = 10;\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug213248_1() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ " \n" //
				+ "    public int field;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug213248_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ " \n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug213248_3() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ " \n" //
				+ "    public int field;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug213248_4() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ " \n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public int field;\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug228659() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package a;\n" //
				+ "public class Test {\n" //
				+ "    /**\n" //
				+ "     */\n" //
				+ "    public void foo() {\n" //
				+ "        String s1 = \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package a;\n" //
				+ "public class Test {\n" //
				+ "    /**\n" //
				+ "     */\n" //
				+ "    public void foo() {\n" //
				+ "        String s1  = \"\";\n" //
				+ "    }\n" //
				+ "}\n";

		String expected1= "" //
				+ "package a;\n" //
				+ "public class Test {\n" //
				+ "    /**\n" //
				+ "     */\n" //
				+ "    public void foo() {\n" //
				+ "        String s1 = \"\";\n" //
				+ "    }\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug232768_1() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * A Java comment on\n" //
				+ "     * two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * A Java comment on\n" //
				+ "     *  two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /**\n" //
				+ "     * A Java comment on two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug232768_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /*\n" //
				+ "     * A block comment on\n" //
				+ "     * two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /*\n" //
				+ "     * A block comment on\n" //
				+ "     *  two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /*\n" //
				+ "     * A block comment on two lines\n" //
				+ "     */\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testBug232768_3() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    //long long long long long long long long long long long long long long long long\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    // long long long long long long long long long long long long long long long long\n" //
				+ "\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    // long long long long long long long long long long long long long long\n" //
				+ "    // long long\n" //
				+ "\n" //
				+ "}\n";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	@Test
	public void testFormatChangeBug488229_1() throws Exception {
		Hashtable<String, String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo\n" //
					+ "     * @param a - integer input\n" //
					+ "     * @return integer\n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo  \n" //
					+ "     * @param a - integer input  \n" //
					+ "     * @return integer  \n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";

			String expected1= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "	/**\n" //
					+ "	 * Method foo\n" //
					+ "	 *\n" //
					+ "	 * @param a\n" //
					+ "	 *            - integer input\n" //
					+ "	 * @return integer\n" //
					+ "	 */\n" //
					+ "	public int foo(int a) {\n" //
					+ "		return 0;\n" //
					+ "	}\n" //
					+ "}";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug488229_2() throws Exception {
		Hashtable<String,String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo\n" //
					+ "     * @param a - integer input\n" //
					+ "     * @return integer\n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo  \n" //
					+ "     * @param a - integer input  \n" //
					+ "     * @return integer  \n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";

			String expected1= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "	/**\n" //
					+ "	 * Method foo\n" //
					+ "	 *\n" //
					+ "	 * @param a\n" //
					+ "	 *            - integer input\n" //
					+ "	 *\n" //
					+ "	 * @return integer\n" //
					+ "	 */\n" //
					+ "	public int foo(int a) {\n" //
					+ "		return 0;\n" //
					+ "	}\n" //
					+ "}";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug488229_3() throws Exception {
		Hashtable<String,String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo\n" //
					+ "     *\t          @param a - integer input\n" //
					+ "     * @return integer\n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo  \n" //
					+ "     *\t          @param a - integer input  \n" //
					+ "     * @return integer  \n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";

			String expected1= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "	/**\n" //
					+ "	 * Method foo\n" //
					+ "	 *\n" //
					+ "	 * @param a\n" //
					+ "	 *            - integer input\n" //
					+ "	 *\n" //
					+ "	 * @return integer\n" //
					+ "	 */\n" //
					+ "	public int foo(int a) {\n" //
					+ "		return 0;\n" //
					+ "	}\n" //
					+ "}";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug561164() throws Exception {
		Hashtable<String, String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo with a really long description that will wrap lines on save operation\n" //
					+ "     *\t          @param a - integer input\n" //
					+ "     * @return integer\n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    /**\n" //
					+ "     * Method foo with a really long description that will wrap lines on save operation  \n" //
					+ "     *\t          @param a - integer input  \n" //
					+ "     * @return integer  \n" //
					+ "     */\n" //
					+ "    public int foo( int a ) {\n" //
					+ "        return 0;\n" //
					+ "    }\n" //
					+ "}";

			String expected1= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "	/**\n" //
					+ "	 * Method foo with a really long description that will wrap lines on save\n" //
					+ "	 * operation\n" //
					+ "	 *\n" //
					+ "	 * @param a\n" //
					+ "	 *            - integer input\n" //
					+ "	 *\n" //
					+ "	 * @return integer\n" //
					+ "	 */\n" //
					+ "	public int foo(int a) {\n" //
					+ "		return 0;\n" //
					+ "	}\n" //
					+ "}";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug560429() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= "" //
				+ "package test;\r\n" //
				+ "import java.util.ArrayList;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "import java.util.List;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		List<List<Integer>> mylistlist=new ArrayList<>();\r\n" //
				+ "		for (Iterator<List<Integer>> mylistlistiterator= mylistlist.iterator(); mylistlistiterator.hasNext(); ) {\r\n" //
				+ "			for (Iterator<Integer> mylistiterator= mylistlistiterator.next().iterator(); mylistiterator.hasNext(); ) {\r\n" //
				+ "				int foo= mylistiterator.next().intValue();\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= "" //
				+ "package test;\r\n" //
				+ "import java.util.ArrayList;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "import java.util.List;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		List<List<Integer>> mylistlist=new ArrayList<>();\r\n" //
				+ "		for (Iterator<List<Integer>> mylistlistiterator= mylistlist.iterator(); mylistlistiterator.hasNext(); ) {\r\n" //
				+ "			for (Iterator<Integer> mylistiterator= mylistlistiterator.next().iterator(); mylistiterator.hasNext(); ) {\r\n" //
				+ "				int foo= mylistiterator.next().intValue();\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";

		String expected1= "" //
				+ "package test;\r\n" //
				+ "import java.util.ArrayList;\r\n" //
				+ "import java.util.List;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		List<List<Integer>> mylistlist=new ArrayList<>();\r\n" //
				+ "		for (List<Integer> list : mylistlist) {\r\n" //
				+ "			for (Integer integer : list) {\r\n" //
				+ "				int foo= integer.intValue();\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		// When
		editCUInEditor(cu1, fileOnEditor);

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}
}
