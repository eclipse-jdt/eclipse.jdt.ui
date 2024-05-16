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
 *     Tom Eicher <eclipse@tom.eicher.name> - [formatting] 'Format Element' in JavaDoc does also format method body - https://bugs.eclipse.org/bugs/show_bug.cgi?id=238746
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class CodeFormatterTest extends CoreTests {

	protected IJavaProject fJProject1;

	protected IPackageFragmentRoot fSourceFolder;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, pts.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	protected static String format(ICompilationUnit cu, int offset, int length) throws PartInitException, JavaModelException {
		return format(cu, offset, length, "Format");
	}

	protected static String formatElement(ICompilationUnit cu, int offset, int length) throws PartInitException, JavaModelException {
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

	@Test
	public void testFormatSelection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
			/**
			*
			 * HEADER
			 */
			package pack;
			
			public final class C {
			    /**\s
			* Bla
			     */
			    public C() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", original, false, null);

		String selection= """
			    /**\s
			* Bla
			     */
			""";

		String formatted= format(cu, original.indexOf(selection), selection.length());

		String expected= """
			/**
			*
			 * HEADER
			 */
			package pack;
			
			public final class C {
			    /**
			     * Bla
			     */
			    public C() {
			    }
			}
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testFormatFieldDeclWithExtraWhitespace() throws Exception {
		String contents= """
			package test1;
			    class A {
			        int i;
			}
			""";
		String formatString1= "    class A {";
		String formatString2= "        int i;";

		IRegion[] regions= new Region[] { new
				Region(contents.indexOf(formatString1), formatString1.length()), new
				Region(contents.indexOf(formatString2), formatString2.length()) };
		TextEdit edit= ToolFactory.createCodeFormatter(null,
				ToolFactory.M_FORMAT_EXISTING).format((CodeFormatter.K_COMPILATION_UNIT |
						CodeFormatter.F_INCLUDE_COMMENTS), contents, regions, 0, "\n");
		assertNotNull(edit);
		Document doc= new Document(contents);
		edit.apply(doc);
		String formatted= doc.get();

		String expected= """
			package test1;
			class A {
			    int i;
			}
			""";
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" formats the surrounding Java Element (including comment) when
	 * invoked in the default (code) partition.
	 */
	@Test
	public void testFormatElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
				  """
			/**
			 *
			 * HEADER
			 */
			package pack;
			
			public final class C {
			    /**\s
			* javadoc
			     */
			    public void method() {
			int local;
			    }
			}
			""";
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
				+ "    public void method() {\n"
				+ "        int local;\n" // local is formatted
				+ "    }\n"
				+ "}\n";
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" only formats the surrounding javadoc, despite its name.
	 */
	@Test
	public void testFormatElementInJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
				  """
			/**
			 *
			 * HEADER
			 */
			package pack;
			
			public final class C {
			    /**\s
			* javadoc
			     */
			    public void method() {
			int local;
			    }
			}
			""";
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
				+ "    public void method() {\n"
				+ "int local;\n" // local does not get formatted
				+ "    }\n"
				+ "}\n";
		assertEqualString(formatted, expected);
	}

	/*
	 * Tests that "Format Element" only formats the surrounding comment, despite its name.
	 */
	@Test
	public void testFormatElementInComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original=
			"""
			/**
			 *
			 * HEADER
			 */
			package pack;
			
			public final class C {
			    /**\s
			* javadoc
			     */
			    public void method() {
			/* a
			comment */
			int local;
			    }
			}
			""";
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
			+ "    public void method() {\n"
			+ "        /*\n" // comment is formatted
			+ "         * a comment\n"
			+ "         */\n"
			+ "int local;\n" // local does not get formatted
			+ "    }\n"
			+ "}\n";
		assertEqualString(formatted, expected);
	}
}
