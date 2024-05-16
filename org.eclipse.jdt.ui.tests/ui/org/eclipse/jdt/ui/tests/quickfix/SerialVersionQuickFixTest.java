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
		String str= """
			package test3;
			import java.io.Serializable;
			public class Test5 {
			    public void test() {
			        class X implements Serializable, Cloneable, Runnable {
			            private static final int x= 1;
			            private Object y;
			            public X() {
			            }
			            public void run() {}
			            public synchronized strictfp void bar() {}
			            public String bar(int x, int y) { return null; };
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test5.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test3;
			import java.io.Serializable;
			public class Test5 {
			    public void test() {
			        class X implements Serializable, Cloneable, Runnable {
			            /* Test */
			            private static final long serialVersionUID = 1L;
			            private static final int x= 1;
			            private Object y;
			            public X() {
			            }
			            public void run() {}
			            public synchronized strictfp void bar() {}
			            public String bar(int x, int y) { return null; };
			        }
			    }
			}
			""";

		expected[1]= """
			package test3;
			import java.io.Serializable;
			public class Test5 {
			    public void test() {
			        class X implements Serializable, Cloneable, Runnable {
			            /* Test */
			            private static final long serialVersionUID = -4564939359985118485L;
			            private static final int x= 1;
			            private Object y;
			            public X() {
			            }
			            public void run() {}
			            public synchronized strictfp void bar() {}
			            public String bar(int x, int y) { return null; };
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testAnonymousClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		String str= """
			package test3;
			import java.io.Serializable;
			public class Test3 {
			    protected int var1;
			    protected int var2;
			    public void test() {
			        Serializable var3= new Serializable() {
			            int var4;\s
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test3.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test3;
			import java.io.Serializable;
			public class Test3 {
			    protected int var1;
			    protected int var2;
			    public void test() {
			        Serializable var3= new Serializable() {
			            /* Test */
			            private static final long serialVersionUID = 1L;
			            int var4;\s
			        };
			    }
			}
			""";

		expected[1]= """
			package test3;
			import java.io.Serializable;
			public class Test3 {
			    protected int var1;
			    protected int var2;
			    public void test() {
			        Serializable var3= new Serializable() {
			            /* Test */
			            private static final long serialVersionUID = -868523843598659436L;
			            int var4;\s
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			
			import java.io.Serializable;
			
			public class Test2 {
			    protected int var1;
			    protected int var2;
			    protected class Test1 implements Serializable {
			        public long var3;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test2;
			
			import java.io.Serializable;
			
			public class Test2 {
			    protected int var1;
			    protected int var2;
			    protected class Test1 implements Serializable {
			        /* Test */
			        private static final long serialVersionUID = 1L;
			        public long var3;
			    }
			}
			""";

		expected[1]= """
			package test2;
			
			import java.io.Serializable;
			
			public class Test2 {
			    protected int var1;
			    protected int var2;
			    protected class Test1 implements Serializable {
			        /* Test */
			        private static final long serialVersionUID = -4023230086280104302L;
			        public long var3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testOuterClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    protected int var1;
			    protected int var2;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    /* Test */
			    private static final long serialVersionUID = 1L;
			    protected int var1;
			    protected int var2;
			}
			""";

		expected[1]= """
			package test1;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    /* Test */
			    private static final long serialVersionUID = -2242798150684569765L;
			    protected int var1;
			    protected int var2;
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOuterClass2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		String str= """
			package test3;
			import java.util.EventObject;
			public class Test4 extends EventObject {
			    private static final int x;
			    private static Class[] a2;
			    private volatile Class a1;
			    static {
			        x= 1;
			    }
			    {
			        a1= null;
			    }
			   \s
			    public Test4(Object source) {
			        super(source);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test4.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test3;
			import java.util.EventObject;
			public class Test4 extends EventObject {
			    /* Test */
			    private static final long serialVersionUID = 1L;
			    private static final int x;
			    private static Class[] a2;
			    private volatile Class a1;
			    static {
			        x= 1;
			    }
			    {
			        a1= null;
			    }
			   \s
			    public Test4(Object source) {
			        super(source);
			    }
			}
			""";

		expected[1]= """
			package test3;
			import java.util.EventObject;
			public class Test4 extends EventObject {
			    /* Test */
			    private static final long serialVersionUID = -7476608308201363525L;
			    private static final int x;
			    private static Class[] a2;
			    private volatile Class a1;
			    static {
			        x= 1;
			    }
			    {
			        a1= null;
			    }
			   \s
			    public Test4(Object source) {
			        super(source);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOuterClass3() throws Exception {
		// longer package

		IPackageFragment pack1= fSourceFolder.createPackageFragment("a.b.c", false, null);
		String str= """
			package a.b.c;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    protected int var1;
			    class Test1Inner {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package a.b.c;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    /* Test */
			    private static final long serialVersionUID = 1L;
			    protected int var1;
			    class Test1Inner {}
			}
			""";

		expected[1]= """
			package a.b.c;
			import java.io.Serializable;
			public class Test1 implements Serializable {
			    /* Test */
			    private static final long serialVersionUID = -3715240305486851194L;
			    protected int var1;
			    class Test1Inner {}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
}
