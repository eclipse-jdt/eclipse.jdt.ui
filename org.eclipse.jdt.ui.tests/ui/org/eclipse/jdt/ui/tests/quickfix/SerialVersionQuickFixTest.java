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

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal;
import org.eclipse.jdt.internal.ui.text.correction.SerialVersionHashProposal;

/**
 *  
 */
public class SerialVersionQuickFixTest extends QuickFixTest {

	private static final String FIELD_COMMENT= "/* Test */";

	private static final Class THIS= SerialVersionQuickFixTest.class;

	/**
	 * @return Test
	 */
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	/*
	 * @see org.eclipse.jdt.ui.tests.quickfix.QuickFixTest#suite()
	 */
	public static Test suite() {
		return allTests();
	}

	private IJavaProject fProject;

	private IPackageFragmentRoot fSourceFolder;

	/**
	 * @param name
	 */
	public SerialVersionQuickFixTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {

		Hashtable options= TestOptions.getFormatterOptions();

		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1"); //$NON-NLS-1$
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4"); //$NON-NLS-1$
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fProject= JavaProjectHelper.createJavaProject("serialIdProject", "bin");
		JavaProjectHelper.addRTJar(fProject);

		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.NEWTYPE).setPattern(""); //$NON-NLS-1$
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.TYPECOMMENT).setPattern(""); //$NON-NLS-1$
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.FIELDCOMMENT).setPattern(FIELD_COMMENT); //$NON-NLS-1$

		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src"); //$NON-NLS-1$
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}

	/**
	 * @throws Exception
	 */
	public void testAnonymousClass() throws Exception {

		IPackageFragment package3= fSourceFolder.createPackageFragment("test3", false, null); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer();

		buffer.append("package test3;\n"); //$NON-NLS-1$
		buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
		buffer.append("public class Test3 {\n"); //$NON-NLS-1$
		buffer.append("    protected int var1;\n"); //$NON-NLS-1$
		buffer.append("    protected int var2;\n"); //$NON-NLS-1$
		buffer.append("    public void test() {\n"); //$NON-NLS-1$
		buffer.append("        Serializable var3= new Serializable() {\n"); //$NON-NLS-1$
		buffer.append("            int var4;\n"); //$NON-NLS-1$
		buffer.append("        };\n"); //$NON-NLS-1$
		buffer.append("    }\n"); //$NON-NLS-1$
		buffer.append("}\n"); //$NON-NLS-1$

		ICompilationUnit unit3= package3.createCompilationUnit("Test3.java", buffer.toString(), false, null); //$NON-NLS-1$
		fProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		CompilationUnit root3= getASTRoot(unit3);
		ArrayList proposals3= collectCorrections(unit3, root3);

		assertNumberOf("proposals3", proposals3.size(), 2); //$NON-NLS-1$
		assertCorrectLabels(proposals3);

		Object current= null;
		for (int index= 0; index < proposals3.size(); index++) {

			current= proposals3.get(index);
			if (current instanceof SerialVersionHashProposal) {

				SerialVersionHashProposal proposal= (SerialVersionHashProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test3;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test3 {\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("    public void test() {\n"); //$NON-NLS-1$
				buffer.append("        Serializable var3= new Serializable() {\n"); //$NON-NLS-1$
				buffer.append("            " + FIELD_COMMENT + "\n");
				buffer.append("            private static final long serialVersionUID = -70923185657280836L;\n"); //$NON-NLS-1$
				buffer.append("            int var4;\n"); //$NON-NLS-1$
				buffer.append("        };\n"); //$NON-NLS-1$
				buffer.append("    }\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());

			} else if (current instanceof SerialVersionDefaultProposal) {

				SerialVersionDefaultProposal proposal= (SerialVersionDefaultProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test3;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test3 {\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("    public void test() {\n"); //$NON-NLS-1$
				buffer.append("        Serializable var3= new Serializable() {\n"); //$NON-NLS-1$
				buffer.append("            " + FIELD_COMMENT + "\n");
				buffer.append("            private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
				buffer.append("            int var4;\n"); //$NON-NLS-1$
				buffer.append("        };\n"); //$NON-NLS-1$
				buffer.append("    }\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());
			}
		}
	}

	/**
	 * @throws Exception
	 */
	public void testInnerClass() throws Exception {

		IPackageFragment package2= fSourceFolder.createPackageFragment("test2", false, null); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer();

		buffer.append("package test2;\n"); //$NON-NLS-1$
		buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
		buffer.append("public class Test2 {\n"); //$NON-NLS-1$
		buffer.append("    protected int var1;\n"); //$NON-NLS-1$
		buffer.append("    protected int var2;\n"); //$NON-NLS-1$
		buffer.append("    protected class Test1 implements Serializable {\n"); //$NON-NLS-1$
		buffer.append("        public long var3;\n"); //$NON-NLS-1$
		buffer.append("    }\n"); //$NON-NLS-1$
		buffer.append("}\n"); //$NON-NLS-1$

		ICompilationUnit unit2= package2.createCompilationUnit("Test2.java", buffer.toString(), false, null); //$NON-NLS-1$
		fProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		CompilationUnit root2= getASTRoot(unit2);
		ArrayList proposals2= collectCorrections(unit2, root2);

		assertNumberOf("proposals2", proposals2.size(), 2); //$NON-NLS-1$
		assertCorrectLabels(proposals2);

		Object current= null;
		for (int index= 0; index < proposals2.size(); index++) {

			current= proposals2.get(index);
			if (current instanceof SerialVersionHashProposal) {

				SerialVersionHashProposal proposal= (SerialVersionHashProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test2;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test2 {\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("    protected class Test1 implements Serializable {\n"); //$NON-NLS-1$
				buffer.append("        " + FIELD_COMMENT + "\n");
				buffer.append("        private static final long serialVersionUID = -4023230086280104302L;\n"); //$NON-NLS-1$
				buffer.append("        public long var3;\n"); //$NON-NLS-1$
				buffer.append("    }\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());

			} else if (current instanceof SerialVersionDefaultProposal) {

				SerialVersionDefaultProposal proposal= (SerialVersionDefaultProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test2;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test2 {\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("    protected class Test1 implements Serializable {\n"); //$NON-NLS-1$
				buffer.append("        " + FIELD_COMMENT + "\n");
				buffer.append("        private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
				buffer.append("        public long var3;\n"); //$NON-NLS-1$
				buffer.append("    }\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());
			}
		}
	}

	public void testOuterClass() throws Exception {

		IPackageFragment package1= fSourceFolder.createPackageFragment("test1", false, null); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer();

		buffer.append("package test1;\n"); //$NON-NLS-1$
		buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
		buffer.append("public class Test1 implements Serializable {\n"); //$NON-NLS-1$
		buffer.append("    protected int var1;\n"); //$NON-NLS-1$
		buffer.append("    protected int var2;\n"); //$NON-NLS-1$
		buffer.append("}\n"); //$NON-NLS-1$

		ICompilationUnit unit1= package1.createCompilationUnit("Test1.java", buffer.toString(), false, null); //$NON-NLS-1$
		fProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		CompilationUnit root1= getASTRoot(unit1);
		ArrayList proposals1= collectCorrections(unit1, root1);

		assertNumberOf("proposals1", proposals1.size(), 2); //$NON-NLS-1$
		assertCorrectLabels(proposals1);

		Object current= null;
		for (int index= 0; index < proposals1.size(); index++) {

			current= proposals1.get(index);
			if (current instanceof SerialVersionHashProposal) {

				SerialVersionHashProposal proposal= (SerialVersionHashProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test1;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test1 implements Serializable {\n"); //$NON-NLS-1$
				buffer.append("    " + FIELD_COMMENT + "\n");
				buffer.append("    private static final long serialVersionUID = -2242798150684569765L;\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());

			} else if (current instanceof SerialVersionDefaultProposal) {

				SerialVersionDefaultProposal proposal= (SerialVersionDefaultProposal) current;
				String preview= getPreviewContent(proposal);

				buffer= new StringBuffer();
				buffer.append("package test1;\n"); //$NON-NLS-1$
				buffer.append("import java.io.Serializable;\n"); //$NON-NLS-1$
				buffer.append("public class Test1 implements Serializable {\n"); //$NON-NLS-1$
				buffer.append("    " + FIELD_COMMENT + "\n");
				buffer.append("    private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
				buffer.append("    protected int var1;\n"); //$NON-NLS-1$
				buffer.append("    protected int var2;\n"); //$NON-NLS-1$
				buffer.append("}\n"); //$NON-NLS-1$
				assertEqualString(preview, buffer.toString());
			}
		}
	}
}
