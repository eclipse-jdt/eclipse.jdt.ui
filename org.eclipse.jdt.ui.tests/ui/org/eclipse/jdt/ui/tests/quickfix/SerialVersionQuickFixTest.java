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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class SerialVersionQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private static final String DEFAULT_VALUE= "1L";

	private static final String FIELD_COMMENT= "/* Test */";

	private static final String FIELD_DECLARATION= "private static final long serialVersionUID = ";

	public static void assertEqualPreview(final String preview, final String buffer) {
		final int index= buffer.indexOf(SerialVersionQuickFixTest.FIELD_DECLARATION);
		assertTrue("Could not find the field declaration", index > 0);
		assertTrue("Resulting source should be larger", preview.length() >= buffer.length());
		final int start= index + FIELD_DECLARATION.length();
		assertEqualString(preview.substring(0, start), buffer.substring(0, start));
		final int end= start + DEFAULT_VALUE.length();
		assertEqualString(preview.substring(preview.length() - (buffer.length() - end)), buffer.substring(end));
	}

	private IJavaProject fProject;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		JavaRuntime.getDefaultVMInstall();
		fProject= projectSetup.getProject();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();

		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1"); //$NON-NLS-1$
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4"); //$NON-NLS-1$

		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, FIELD_COMMENT, null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src"); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testLocalClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test5 {\n");
		buf.append("    public void test() {\n");
		buf.append("        class X implements Serializable, Cloneable, Runnable {\n");
		buf.append("            private static final int x= 1;\n");
		buf.append("            private Object y;\n");
		buf.append("            public X() {\n");
		buf.append("            }\n");
		buf.append("            public void run() {}\n");
		buf.append("            public synchronized strictfp void bar() {}\n");
		buf.append("            public String bar(int x, int y) { return null; };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test5.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test5 {\n");
		buf.append("    public void test() {\n");
		buf.append("        class X implements Serializable, Cloneable, Runnable {\n");
		buf.append("            /* Test */\n");
		buf.append("            private static final long serialVersionUID = 1L;\n");
		buf.append("            private static final int x= 1;\n");
		buf.append("            private Object y;\n");
		buf.append("            public X() {\n");
		buf.append("            }\n");
		buf.append("            public void run() {}\n");
		buf.append("            public synchronized strictfp void bar() {}\n");
		buf.append("            public String bar(int x, int y) { return null; };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test5 {\n");
		buf.append("    public void test() {\n");
		buf.append("        class X implements Serializable, Cloneable, Runnable {\n");
		buf.append("            /* Test */\n");
		buf.append("            private static final long serialVersionUID = -4564939359985118485L;\n");
		buf.append("            private static final int x= 1;\n");
		buf.append("            private Object y;\n");
		buf.append("            public X() {\n");
		buf.append("            }\n");
		buf.append("            public void run() {}\n");
		buf.append("            public synchronized strictfp void bar() {}\n");
		buf.append("            public String bar(int x, int y) { return null; };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testAnonymousClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test3 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    public void test() {\n");
		buf.append("        Serializable var3= new Serializable() {\n");
		buf.append("            int var4; \n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test3.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test3 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    public void test() {\n");
		buf.append("        Serializable var3= new Serializable() {\n");
		buf.append("            /* Test */\n");
		buf.append("            private static final long serialVersionUID = 1L;\n");
		buf.append("            int var4; \n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test3 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    public void test() {\n");
		buf.append("        Serializable var3= new Serializable() {\n");
		buf.append("            /* Test */\n");
		buf.append("            private static final long serialVersionUID = -868523843598659436L;\n");
		buf.append("            int var4; \n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class Test2 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    protected class Test1 implements Serializable {\n");
		buf.append("        public long var3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class Test2 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    protected class Test1 implements Serializable {\n");
		buf.append("        /* Test */\n");
		buf.append("        private static final long serialVersionUID = 1L;\n");
		buf.append("        public long var3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class Test2 {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("    protected class Test1 implements Serializable {\n");
		buf.append("        /* Test */\n");
		buf.append("        private static final long serialVersionUID = -4023230086280104302L;\n");
		buf.append("        public long var3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testOuterClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = 1L;\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = -2242798150684569765L;\n");
		buf.append("    protected int var1;\n");
		buf.append("    protected int var2;\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOuterClass2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.util.EventObject;\n");
		buf.append("public class Test4 extends EventObject {\n");
		buf.append("    private static final int x;\n");
		buf.append("    private static Class[] a2;\n");
		buf.append("    private volatile Class a1;\n");
		buf.append("    static {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("    {\n");
		buf.append("        a1= null;\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public Test4(Object source) {\n");
		buf.append("        super(source);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test4.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.util.EventObject;\n");
		buf.append("public class Test4 extends EventObject {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = 1L;\n");
		buf.append("    private static final int x;\n");
		buf.append("    private static Class[] a2;\n");
		buf.append("    private volatile Class a1;\n");
		buf.append("    static {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("    {\n");
		buf.append("        a1= null;\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public Test4(Object source) {\n");
		buf.append("        super(source);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("import java.util.EventObject;\n");
		buf.append("public class Test4 extends EventObject {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = -7476608308201363525L;\n");
		buf.append("    private static final int x;\n");
		buf.append("    private static Class[] a2;\n");
		buf.append("    private volatile Class a1;\n");
		buf.append("    static {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("    {\n");
		buf.append("        a1= null;\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public Test4(Object source) {\n");
		buf.append("        super(source);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOuterClass3() throws Exception {
		// longer package

		IPackageFragment pack1= fSourceFolder.createPackageFragment("a.b.c", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package a.b.c;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    protected int var1;\n");
		buf.append("    class Test1Inner {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package a.b.c;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = 1L;\n");
		buf.append("    protected int var1;\n");
		buf.append("    class Test1Inner {}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package a.b.c;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class Test1 implements Serializable {\n");
		buf.append("    /* Test */\n");
		buf.append("    private static final long serialVersionUID = -3715240305486851194L;\n");
		buf.append("    protected int var1;\n");
		buf.append("    class Test1Inner {}\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
}
