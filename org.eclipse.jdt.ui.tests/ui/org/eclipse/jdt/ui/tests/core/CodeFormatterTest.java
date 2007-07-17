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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

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
		JavaEditor editorPart= (JavaEditor) EditorUtility.openInEditor(cu);
		try {
			IWorkbenchPartSite editorSite= editorPart.getSite();

			ISelection selection= new TextSelection(offset, length);
			editorSite.getSelectionProvider().setSelection(selection);

			IAction formatAction= editorPart.getAction("Format");
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

}
