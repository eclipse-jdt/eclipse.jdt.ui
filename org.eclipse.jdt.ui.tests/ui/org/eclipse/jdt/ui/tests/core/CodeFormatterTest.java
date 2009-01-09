/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher <eclipse@tom.eicher.name> - [formatting] 'Format Element' in JavaDoc does also format method body - https://bugs.eclipse.org/bugs/show_bug.cgi?id=238746
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class CodeFormatterTest extends CoreTests {

	private static final Class THIS= CodeFormatterTest.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public CodeFormatterTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	private static String format(ICompilationUnit cu, int offset, int length) throws PartInitException, JavaModelException {
		return format(cu, offset, length, "Format");
	}

	private static String formatElement(ICompilationUnit cu, int offset, int length) throws PartInitException, JavaModelException {
		return format(cu, offset, length, "QuickFormat"); // see JavaEditor for the action ids
	}

	private static String format(ICompilationUnit cu, int offset, int length, String actionId) throws PartInitException, JavaModelException {
		JavaEditor editorPart= (JavaEditor) EditorUtility.openInEditor(cu);
		try {
			IWorkbenchPartSite editorSite= editorPart.getSite();

			ISelection selection= new TextSelection(offset, length);
			editorSite.getSelectionProvider().setSelection(selection);

			IAction formatAction= editorPart.getAction(actionId);
			formatAction.run();

			return cu.getBuffer().getContents();
		} finally {
			editorPart.close(false);
		}
	}

	public void testFormatSelection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("*\n");
		buf.append(" * HEADER\n");
		buf.append(" */\n");
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public final class C {\n");
		buf.append("    /** \n");
		buf.append("* Bla\n");
		buf.append("     */\n");
		buf.append("    public C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", original, false, null);

		buf= new StringBuffer();
		buf.append("    /** \n");
		buf.append("* Bla\n");
		buf.append("     */\n");
		String selection= buf.toString();

		String formatted= format(cu, original.indexOf(selection), selection.length());

		buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("*\n");
		buf.append(" * HEADER\n");
		buf.append(" */\n");
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public final class C {\n");
		buf.append("    /**\n");
		buf.append("     * Bla\n");
		buf.append("     */\n");
		buf.append("    public C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" formats the surrounding Java Element (including comment) when
	 * invoked in the default (code) partition.
	 */
	public void testFormatElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
				  "/**\n"
				+ " *\n"
				+ " * HEADER\n"
				+ " */\n"
				+ "package pack;\n"
				+ "\n"
				+ "public final class C {\n"
				+ "    /** \n"
				+ "* javadoc\n"
				+ "     */\n"
				+ "    public method() {\n"
				+ "int local;\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", original, false, null);
		String formatted= formatElement(cu, original.indexOf("method"), 0);

		String expected=
				  "/**\n"
				+ " *\n"
				+ " * HEADER\n"
				+ " */\n"
				+ "package pack;\n"
				+ "\n"
				+ "public final class C {\n"
				+ "    /**\n"
				+ "     * javadoc\n" // javadoc is formatted
				+ "     */\n"
				+ "    public method() {\n"
				+ "        int local;\n" // local is formatted
				+ "    }\n"
				+ "}\n";
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" only formats the surrounding javadoc, despite its name.
	 */
	public void testFormatElementInJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
				  "/**\n"
				+ " *\n"
				+ " * HEADER\n"
				+ " */\n"
				+ "package pack;\n"
				+ "\n"
				+ "public final class C {\n"
				+ "    /** \n"
				+ "* javadoc\n"
				+ "     */\n"
				+ "    public method() {\n"
				+ "int local;\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", original, false, null);
		String formatted= formatElement(cu, original.indexOf("javadoc"), 0);

		String expected=
				  "/**\n"
				+ " *\n"
				+ " * HEADER\n"
				+ " */\n"
				+ "package pack;\n"
				+ "\n"
				+ "public final class C {\n"
				+ "    /**\n"
				+ "     * javadoc\n" // javadoc is formatted
				+ "     */\n"
				+ "    public method() {\n"
				+ "int local;\n" // local does not get formatted
				+ "    }\n"
				+ "}\n";
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" only formats the surrounding comment, despite its name.
	 */
	public void testFormatElementInComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
			"/**\n"
			+ " *\n"
			+ " * HEADER\n"
			+ " */\n"
			+ "package pack;\n"
			+ "\n"
			+ "public final class C {\n"
			+ "    /** \n"
			+ "* javadoc\n"
			+ "     */\n"
			+ "    public method() {\n"
			+ "/* a\n"
			+ "comment */\n"
			+ "int local;\n"
			+ "    }\n"
			+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", original, false, null);
		String formatted= formatElement(cu, original.indexOf("comment"), 0);

		String expected=
			"/**\n"
			+ " *\n"
			+ " * HEADER\n"
			+ " */\n"
			+ "package pack;\n"
			+ "\n"
			+ "public final class C {\n"
			+ "    /** \n"
			+ "* javadoc\n" // javadoc is not formatted
			+ "     */\n"
			+ "    public method() {\n"
			+ "        /*\n" // comment is formatted
			+ "         * a comment\n"
			+ "         */\n"
			+ "int local;\n" // local does not get formatted
			+ "    }\n"
			+ "}\n";
		assertEqualString(formatted, expected);
	}
}
