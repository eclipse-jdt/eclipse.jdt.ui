/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;


public class SaveParticipantTest extends CleanUpTestCase {

	public SaveParticipantTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(SaveParticipantTest.class));
	}

	protected void setUp() throws Exception {
		super.setUp();

		IEclipsePreferences node= new InstanceScope().getNode(JavaUI.ID_PLUGIN);
		node.putBoolean("editor_save_participant_" + CleanUpPostSaveListener.POSTSAVELISTENER_ID, true);
		node.put(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.TRUE);
	}

	private static void editCUInEditor(ICompilationUnit cu, String newContent) throws JavaModelException, PartInitException {
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);

		cu.getBuffer().setContents(newContent);
		editor.doSave(null);
	}

	public void testFormatAll01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        String s= (String)o;\n");
		buf.append("    }\n");
		buf.append("}");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        String s    = (String)o;\n");
		buf.append("    }\n");
		buf.append("}");

		editCUInEditor(cu1, buf.toString());

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        String s = (String) o;\n");
		buf.append("    }\n");
		buf.append("}");
		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	public void testFormatChanges01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        String s= (String)o;\n");
		buf.append("    }\n");
		buf.append("}");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        String s    = (String)o;\n");
		buf.append("    }\n");
		buf.append("}");

		editCUInEditor(cu1, buf.toString());

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        String s = (String) o;\n");
		buf.append("    }\n");
		buf.append("}");
		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	public void testFormatChanges02() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        Object s= (String)o;\n");
		buf.append("}}\n");
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        Object s       = (String)o;\n");
		buf.append("}}\n");

		editCUInEditor(cu1, buf.toString());

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo( Object o ) {\n");
		buf.append("        Object s = o;\n");
		buf.append("}}\n");
		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}

	public void testFormatChangesBug205177() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int        a= 1;\n");
		buf.append("    int        b= 2;\n");
		buf.append("    int        c= 3;\n");
		buf.append("    int        d= 4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int        a= 1;\n");
		buf.append("    int        b= 2;//\n");
		buf.append("    int        c= 3;\n");
		buf.append("    int        d= 4;\n");
		buf.append("}\n");

		editCUInEditor(cu1, buf.toString());

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int        a= 1;\n");
		buf.append("    int b = 2;//\n");
		buf.append("    int        c= 3;\n");
		buf.append("    int        d= 4;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}
	
	public void testFormatChangesBug205308() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int        a= 1;\n");
		buf.append("    int        b= 2;\n");
		buf.append("    int        c= 3;\n");
		buf.append("    int        d= 4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int         a= 1;\n");
		buf.append("    int        b= 2;\n");
		buf.append("}\n");

		editCUInEditor(cu1, buf.toString());

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int a = 1;\n");
		buf.append("    int        b= 2;\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertEquals(expected1, cu1.getBuffer().getContents());
	}
}
