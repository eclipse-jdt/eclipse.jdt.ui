/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *     Chris West (Faux) <eclipse@goeswhere.com> - [clean up] "Use modifier 'final' where possible" can introduce compile errors - https://bugs.eclipse.org/bugs/show_bug.cgi?id=272532
 *     Red Hat Inc. - redundant semicolons test
 *     Fabrice TIERCELIN - Autoboxing and Unboxing test
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.eclipse.jdt.internal.ui.fix.MultiFixMessages.ConstantsCleanUp_description;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.fix.UpdateProperty;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.tests.core.rules.Java13ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.fix.PlainReplacementCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.PrimitiveRatherThanWrapperCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.RedundantModifiersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class CleanUpTest extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java13ProjectTestSetup(false);

	IJavaProject fJProject1= getProject();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		IPath osgiJar= new Path("testresources/org.junit.jupiter.api_stub.jar");
		JavaProjectHelper.addLibrary(fJProject1, JavaProjectHelper.findRtJar(osgiJar)[0]);
	}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	private static class NoChangeRedundantModifiersCleanUp extends RedundantModifiersCleanUp {
		private NoChangeRedundantModifiersCleanUp(Map<String, String> options) {
			super(options);
		}

		@Override
		protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
			return super.createFix(unit);
		}
	}

	@Test
	public void testCleanUpConstantsAreDistinct() throws Exception {
		Field[] allCleanUpConstantsFields= CleanUpConstants.class.getDeclaredFields();

		Map<String, Field> stringFieldsByValue= new HashMap<>();

		for (Field field : allCleanUpConstantsFields) {
			if (String.class.equals(field.getType())
					&& field.getAnnotation(Deprecated.class) == null
					&& !field.getName().startsWith("DEFAULT_")) {
				final String constantValue= (String) field.get(null);

				assertFalse(stringFieldsByValue.containsKey(constantValue),
						() -> CleanUpConstants.class.getCanonicalName()
						+ "."
						+ field.getName()
						+ " and "
						+ CleanUpConstants.class.getCanonicalName()
						+ "."
						+ stringFieldsByValue.get(constantValue).getName()
						+ " should not share the same value: "
						+ constantValue);

				stringFieldsByValue.put(constantValue, field);
			}
		}
	}

	@Test
	public void testAddNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        String s= "";
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public String s1 = "";
			    public void foo() {
			        String s2 = "";
			        String s3 = s2 + "";
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static final String s= "";
			    public static String bar(String s1, String s2) {
			        bar("", "");
			        return "";
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        String s= ""; //$NON-NLS-1$
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public String s1 = ""; //$NON-NLS-1$
			    public void foo() {
			        String s2 = ""; //$NON-NLS-1$
			        String s3 = s2 + ""; //$NON-NLS-1$
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static final String s= ""; //$NON-NLS-1$
			    public static String bar(String s1, String s2) {
			        bar("", ""); //$NON-NLS-1$ //$NON-NLS-2$
			        return ""; //$NON-NLS-1$
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testRemoveNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        String s= null; //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public String s1 = null; //$NON-NLS-1$
			    public void foo() {
			        String s2 = null; //$NON-NLS-1$
			        String s3 = s2 + s2; //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static final String s= null; //$NON-NLS-1$
			    public static String bar(String s1, String s2) {
			        bar(s2, s1); //$NON-NLS-1$ //$NON-NLS-2$
			        return s1; //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		String expected1= """
			package test1;
			public class E1 {
			    public void foo() {
			        String s= null;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E2 {
			    public String s1 = null;
			    public void foo() {
			        String s2 = null;
			        String s3 = s2 + s2;
			    }
			}
			""";

		String expected3= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static final String s= null;
			    public static String bar(String s1, String s2) {
			        bar(s2, s1);
			        return s1;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testRemoveNLSTagWhitespace() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static String bar(String s1, String s2) {
			        bar(s2, s1); //$NON-NLS-1$ //$NON-NLS-2$
			        return s1;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		String expected1= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public static String bar(String s1, String s2) {
			        bar(s2, s1);
			        return s1;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.*;
			public class E2 {
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import java.util.HashMap;
			import test1.E2;
			import java.io.StringReader;
			import java.util.HashMap;
			public class E3 extends E2 {
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private void foo() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private void foo() {}
			    private void bar() {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E3 {
			    private class E3Inner {
			        private void foo() {}
			    }
			    public void foo() {
			        Runnable r= new Runnable() {
			            public void run() {}
			            private void foo() {};
			        };
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS);

		sample= """
			package test1;
			public class E1 {
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			public class E3 {
			    private class E3Inner {
			    }
			    public void foo() {
			        Runnable r= new Runnable() {
			            public void run() {};
			        };
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private E1(int i) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public E2() {}
			    private E2(int i) {}
			    private E2(String s) {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			        private E3Inner(int i) {}
			    }
			    private void foo() {
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS);

		sample= """
			package test1;
			public class E1 {
			    private E1(int i) {}
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public E2() {}
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			        private E3Inner(int i) {}
			    }
			    private void foo() {
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private int i= 10;
			    private int j= 10;
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E3 {
			    private int i;
			    private int j;
			    private void foo() {
			        i= 10;
			        i= 20;
			        i= j;
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= """
			package test1;
			public class E1 {
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			public class E3 {
			    private int j;
			    private void foo() {
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private class E1Inner{}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private class E2Inner1 {}
			    private class E2Inner2 {}
			    public class E2Inner3 {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			        private class E3InnerInner {}
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= """
			package test1;
			public class E1 {
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public class E2Inner3 {}
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public void foo() {
			        int i= 10;
			        int j= 10;
			    }
			    private void bar() {
			        int i= 10;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			        public void foo() {
			            int i= 10;
			        }
			    }
			    public void foo() {
			        int i= 10;
			        int j= i;
			        j= 10;
			        j= 20;
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public void foo() {
			    }
			    private void bar() {
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			public class E3 {
			    public class E3Inner {
			        public void foo() {
			        }
			    }
			    public void foo() {
			        int i= 10;
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCode07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= bar();
			        int j= 1;
			    }
			    public int bar() {return 1;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        bar();
			    }
			    public int bar() {return 1;}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCode08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i= bar();
			    private int j= 1;
			    public int bar() {return 1;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= """
			package test1;
			public class E1 {
			    private int i= bar();
			    public int bar() {return 1;}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCode09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 1;
			        i= bar();
			        int j= 1;
			        j= 1;
			    }
			    public int bar() {return 1;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        bar();
			    }
			    public int bar() {return 1;}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCode10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i= 1;
			    private int j= 1;
			    public void foo() {
			        i= bar();
			        j= 1;
			    }
			    public int bar() {return 1;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= """
			package test1;
			public class E1 {
			    private int i= 1;
			    public void foo() {
			        i= bar();
			    }
			    public int bar() {return 1;}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCode11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1  {
			    private void foo(String s) {
			        String s1= (String)s;
			        Object o= (Object)new Object();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    String s1;
			    String s2= (String)s1;
			    public void foo(Integer i) {
			        Number n= (Number)i;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			public class E1  {
			    private void foo(String s) {
			        String s1= s;
			        Object o= new Object();
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    String s1;
			    String s2= s1;
			    public void foo(Integer i) {
			        Number n= i;
			    }
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2}, null);
	}

	@Test
	public void testUnusedCode12() throws Exception {
		// don't clean up parameters in public methods
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
				   public void bla() {
			        foo(83);
			    }
			    private void foo(int zoz) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		String expected1= """
			package test1;
			public class E1 {
				   public void bla() {
			        foo();
			    }
			    private void foo() {
			    }
			}
			""";

		sample= """
			package test1;
			class E3 {
			    protected void foo(int subu, Object[] gork) {
			        System.out.println(gork.length + subu);
			    }
			}
			public class E2 extends E3 {
				   public void bla() {
			        foo(null, 83, null);
			    }
			    private void foo(String bubu, int zoz, Object... gork) {
			        System.out.println(gork.length + bubu.length());
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		String expected2= """
			package test1;
			class E3 {
			    protected void foo(int subu, Object[] gork) {
			        System.out.println(gork.length + subu);
			    }
			}
			public class E2 extends E3 {
				   public void bla() {
			        foo1(null, null);
			    }
			    private void foo1(String bubu, Object... gork) {
			        System.out.println(gork.length + bubu.length());
			    }
			}
			""";


		sample = """
			package test1;

			public class E4<K> {
			\t
				private <T> void foo(int one, K kay, int two, T tee) {
					System.out.println(one + two);
				}
			}
			""";
		ICompilationUnit cu3= pack1.createCompilationUnit("E4.java", sample, false, null);

		String expected3= """
			package test1;

			public class E4<K> {
			\t
				private <T> void foo(int one, int two) {
					System.out.println(one + two);
				}
			}
			""";

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_METHOD_PARAMETERS);

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testUnusedCodeBug578906_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> resultHints, List<String> results) {
			        Iterator<String> it = results.iterator();
			        for (int j = resultHints.size();it.hasNext();j++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> resultHints, List<String> results) {
			        Iterator<String> it = results.iterator();
			        for (resultHints.size();it.hasNext();) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug578906_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> resultHints, List<String> results) {
			        Iterator<String> it = results.iterator();
			        for (int j = resultHints.size() + resultHints.hashCode();it.hasNext();j++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> resultHints, List<String> results) {
			        Iterator<String> it = results.iterator();
			        for (resultHints.size(), resultHints.hashCode();it.hasNext();) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}


	@Test
	public void testUnusedCodeBug578169() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    static class Point {
			        int x, y;
			        public int getX() {
			            return x;
			        }
			    }

			    static class Rect {
			        Point loc;
			        int w, h;
			        public Point getLoc() {
			            return loc;
			        }
			    }

			    Rect getRect() {
			        return new Rect();
			    }

			    void test() {
			        int x;
			        int y;
			        int z;
			        int k = getRect().loc.getX();
			        x = getRect().getLoc().x;
			        y = getRect().loc.y;
			        System.out.println(y);
			        z = getRect().loc.x;
			        k = getRect().loc.getX();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1 {
			    static class Point {
			        int x, y;
			        public int getX() {
			            return x;
			        }
			    }

			    static class Rect {
			        Point loc;
			        int w, h;
			        public Point getLoc() {
			            return loc;
			        }
			    }

			    Rect getRect() {
			        return new Rect();
			    }

			    void test() {
			        int y;
			        getRect().loc.getX();
			        getRect().getLoc();
			        y = getRect().loc.y;
			        System.out.println(y);
			        getRect();
			        getRect().loc.getX();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug123766() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1  {
			    private int i,j;
			    public void foo() {
			        String s1,s2;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1  {
			    public void foo() {
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug150853() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import foo.Bar;
			public class E1 {}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= """
			package test1;
			public class E1 {}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug173014_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			        void foo() {
			                class Local {}
			        }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= """
			package test1;
			public class E1 {
			        void foo() {
			        }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug173014_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static void main(String[] args) {
			        class Local {}
			        class Local2 {
			            class LMember {}
			            class LMember2 extends Local2 {}
			            LMember m;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= """
			package test1;
			public class E1 {
			    public static void main(String[] args) {
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnusedCodeBug189394() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Random;
			public class E1 {
			    public void foo() {
			        Random ran = new Random();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;
			import java.util.Random;
			public class E1 {
			    public void foo() {
			        new Random();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug578911() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public void foo(Integer o1, Integer o2) {
			        o1 = (Integer)o1;
			        o2 = (((Integer)o2));
			        o1 = (Integer)o2;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;

			public class E {
			    public void foo(Integer o1, Integer o2) {
			        o1 = o2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug335173_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Comparator;

			class IntComp implements Comparator<Integer> {
			    public int compare(Integer o1, Integer o2) {
			        return ((Integer) o1).intValue() - ((Integer) o2).intValue();
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			import java.util.Comparator;

			class IntComp implements Comparator<Integer> {
			    public int compare(Integer o1, Integer o2) {
			        return o1.intValue() - o2.intValue();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug335173_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public void foo(Integer n) {
			        int i = (((Integer) n)).intValue();
			        foo(((Integer) n));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;

			public class E1 {
			    public void foo(Integer n) {
			        int i = ((n)).intValue();
			        foo((n));
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug335173_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public void foo(Integer n) {
			        int i = ((Integer) (n)).intValue();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;

			public class E1 {
			    public void foo(Integer n) {
			        int i = (n).intValue();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug371078_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			class E1 {
			    public static Object create(final int a, final int b) {
			        return (Double) ((double) (a * Math.pow(10, -b)));
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			class E1 {
			    public static Object create(final int a, final int b) {
			        return (a * Math.pow(10, -b));
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testUnusedCodeBug371078_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class NestedCasts {
				void foo(Integer i) {
					Object o= ((((Number) (((Integer) i)))));
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("NestedCasts.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			public class NestedCasts {
				void foo(Integer i) {
					Object o= (((((i)))));
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava5001() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    private int field= 1;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    private int field1= 1;
			    /**
			     * @deprecated
			     */
			    private int field2= 2;
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int field= 1;
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int field1= 1;
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int field2= 2;
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2}, null);
	}

	@Test
	public void testJava5002() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    private int f() {return 1;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    private int f1() {return 1;}
			    /**
			     * @deprecated
			     */
			    private int f2() {return 2;}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int f() {return 1;}
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int f1() {return 1;}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private int f2() {return 2;}
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2}, null);
	}

	@Test
	public void testJava5003() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			/**
			 * @deprecated
			 */
			public class E1 {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    private class E2Sub1 {}
			    /**
			     * @deprecated
			     */
			    private class E2Sub2 {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= """
			package test1;
			/**
			 * @deprecated
			 */
			@Deprecated
			public class E1 {
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1{
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private class E2Sub1 {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    private class E2Sub2 {}
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2}, null);
	}

	@Test
	public void testJava5004() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo1() {}
			    protected void foo2() {}
			    private void foo3() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    public void foo1() {}
			    protected void foo2() {}
			    protected void foo3() {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    public void foo1() {}
			    protected void foo2() {}
			    public void foo3() {}
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		sample= """
			package test1;
			public class E2 extends E1 {
			    @Override
			    public void foo1() {}
			    @Override
			    protected void foo2() {}
			    protected void foo3() {}
			}
			""";
		String expected1= sample;

		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    @Override
			    public void foo1() {}
			    @Override
			    protected void foo2() {}
			    @Override
			    public void foo3() {}
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2}, null);
	}

	@Test
	public void testJava5005() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    protected void foo2() {}
			    private void foo3() {}
			    /**
			     * @deprecated
			     */
			    public int i;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    /**
			     * @deprecated
			     */
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    protected void foo2() {}
			    /**
			     * @deprecated
			     */
			    protected void foo3() {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    /**
			     * @deprecated
			     */
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    protected void foo2() {}
			    /**
			     * @deprecated
			     */
			    public void foo3() {}
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		sample= """
			package test1;
			public class E1 {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    protected void foo2() {}
			    private void foo3() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public int i;
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1 {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    protected void foo2() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    protected void foo3() {}
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    public void foo1() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    protected void foo2() {}
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    public void foo3() {}
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testJava50Bug222257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.ArrayList;
			public class E1 {
			    public void foo() {
			        ArrayList list= new ArrayList<>();
			        ArrayList list2= new ArrayList<>();
			       \s
			        System.out.println(list);
			        System.out.println(list2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		HashMap<String, String> map= new HashMap<>();
		map.put(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES, CleanUpOptions.TRUE);
		Java50CleanUp cleanUp= new Java50CleanUp(map);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(getProject());

		Map<String, String> options= RefactoringASTParser.getCompilerOptions(getProject());
		options.putAll(cleanUp.getRequirements().getCompilerOptions());
		parser.setCompilerOptions(options);

		final CompilationUnit[] roots= new CompilationUnit[1];
		parser.createASTs(new ICompilationUnit[] { cu1 }, new String[0], new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				roots[0]= ast;
			}
		}, null);

		IProblem[] problems= roots[0].getProblems();
		assertEquals(2, problems.length);
		for (IProblem problem : problems) {
			ProblemLocation location= new ProblemLocation(problem);
			assertTrue(cleanUp.canFix(cu1, location));
		}
	}

	@Test
	public void testCodeStyle01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    String s = ""; //$NON-NLS-1$
			    String t = "";  //$NON-NLS-1$
			   \s
			    public void foo() {
			        s = ""; //$NON-NLS-1$
			        s = s + s;
			        s = t + s;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1{
			    int i = 10;
			   \s
			    public class E2Inner {
			        public void bar() {
			            int j = i;
			            String k = s + t;
			        }
			    }
			   \s
			    public void fooBar() {
			        String k = s;
			        int j = i;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    String s = ""; //$NON-NLS-1$
			    String t = "";  //$NON-NLS-1$
			   \s
			    public void foo() {
			        this.s = ""; //$NON-NLS-1$
			        this.s = this.s + this.s;
			        this.s = this.t + this.s;
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1{
			    int i = 10;
			   \s
			    public class E2Inner {
			        public void bar() {
			            int j = E2.this.i;
			            String k = E2.this.s + E2.this.t;
			        }
			    }
			   \s
			    public void fooBar() {
			        String k = this.s;
			        int j = this.i;
			    }
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2}, null);
	}

	@Test
	public void testCodeStyle02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static int i= 0;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private E1 e1;
			   \s
			    public void foo() {
			        e1= new E1();
			        int j= e1.i;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E2 {
			    private E1 e1;
			   \s
			    public void foo() {
			        this.e1= new E1();
			        int j= E1.i;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {cu1.getBuffer().getContents(), expected1}, null);
	}

	@Test
	public void testCodeStyle03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static int f;
			    public void foo() {
			        int i= this.f;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public static String s = "";
			    public void foo() {
			        System.out.println(this.s);
			        E1 e1= new E1();
			        int i= e1.f;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public static int g;
			    {
			        this.g= (new E1()).f;
			    }
			    public static int f= E1.f;
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public static int f;
			    public void foo() {
			        int i= E1.f;
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public static String s = "";
			    public void foo() {
			        System.out.println(E2.s);
			        E1 e1= new E1();
			        int i= E1.f;
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public static int g;
			    {
			        E3.g= E1.f;
			    }
			    public static int f= E1.f;
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);
	}

	@Test
	public void testCodeStyle04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static int f() {return 1;}
			    public void foo() {
			        int i= this.f();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public static String s() {return "";}
			    public void foo() {
			        System.out.println(this.s());
			        E1 e1= new E1();
			        int i= e1.f();
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public static int g;
			    {
			        this.g= (new E1()).f();
			    }
			    public static int f= E1.f();
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public static int f() {return 1;}
			    public void foo() {
			        int i= E1.f();
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 {
			    public static String s() {return "";}
			    public void foo() {
			        System.out.println(E2.s());
			        E1 e1= new E1();
			        int i= E1.f();
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public static int g;
			    {
			        E3.g= E1.f();
			    }
			    public static int f= E1.f();
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);

	}

	@Test
	public void testCodeStyle05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public String s= "";
			    public E2 e2;
			    public static int i= 10;
			    public void foo() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    public int i = 10;
			    public E1 e1;
			    public void fooBar() {}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 {
			    private E1 e1;   \s
			    public void foo() {
			        e1= new E1();
			        int j= e1.i;
			        String s= e1.s;
			        e1.foo();
			        e1.e2.fooBar();
			        int k= e1.e2.e2.e2.i;
			        int h= e1.e2.e2.e1.e2.e1.i;
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 {
			    private E1 e1;   \s
			    public void foo() {
			        this.e1= new E1();
			        int j= E1.i;
			        String s= this.e1.s;
			        this.e1.foo();
			        this.e1.e2.fooBar();
			        int k= this.e1.e2.e2.e2.i;
			        int h= E1.i;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), cu2.getBuffer().getContents(), expected1}, null);
	}

	@Test
	public void testCodeStyle06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public String s= "";
			    public E1 create() {
			        return new E1();
			    }
			    public void foo() {
			        create().s= "";
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static int i = 10;
			    private static int j = i + 10 * i;
			    public void foo() {
			        String s= i + "";
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public final static int i = 1;
			    public final int j = 2;
			    private final int k = 3;
			    public void foo() {
			        switch (3) {
			        case i: break;
			        case j: break;
			        case k: break;
			        default: break;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public abstract class E1Inner1 {
			        protected int n;
			        public abstract void foo();
			    }
			    public abstract class E1Inner2 {
			        public abstract void run();
			    }
			    public void foo() {
			        E1Inner1 inner= new E1Inner1() {
			            public void foo() {
			                E1Inner2 inner2= new E1Inner2() {
			                    public void run() {
			                        System.out.println(n);
			                    }
			                };
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private static final int N;
			    static {N= 10;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {   \s
			    public static int E1N = 10;
			    public void foo() {
			        System.out.println(E1N);
			        E1N = 10;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    public static int E2N = 10;
			    public void foo() {
			        System.out.println(E1N);
			        E1N = 10;
			        System.out.println(E2N);
			        E2N = 10;
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 extends E2 {
			    private static int E3N = 10;
			    public void foo() {
			        System.out.println(E1N);
			        E1N = 10;
			        System.out.println(E2N);
			        E2N = 10;
			        System.out.println(E3N);
			        E3N = 10;
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {   \s
			    public static int E1N = 10;
			    public void foo() {
			        System.out.println(E1.E1N);
			        E1.E1N = 10;
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			public class E2 extends E1 {
			    public static int E2N = 10;
			    public void foo() {
			        System.out.println(E1.E1N);
			        E1.E1N = 10;
			        System.out.println(E2.E2N);
			        E2.E2N = 10;
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    private static int E3N = 10;
			    public void foo() {
			        System.out.println(E1.E1N);
			        E1.E1N = 10;
			        System.out.println(E2.E2N);
			        E2.E2N = 10;
			        System.out.println(E3.E3N);
			        E3.E3N = 10;
			    }
			}
			""";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3}, null);

	}

	@Test
	public void testCodeStyle12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public final static int N1 = 10;
			    public static int N2 = N1;
			    {
			        System.out.println(N1);
			        N2 = 10;
			        System.out.println(N2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    public final static int N1 = 10;
			    public static int N2 = E1.N1;
			    {
			        System.out.println(E1.N1);
			        E1.N2 = 10;
			        System.out.println(E1.N2);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private static class E1Inner {
			        private static class E1InnerInner {
			            public static int N = 10;
			            static {
			                System.out.println(N);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    private static class E1Inner {
			        private static class E1InnerInner {
			            public static int N = 10;
			            static {
			                System.out.println(E1InnerInner.N);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    static class E1Inner {
			        public static class E1InnerInner {
			            public static int N = 10;
			            public void foo() {
			                System.out.println(N);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    static class E1Inner {
			        public static class E1InnerInner {
			            public static int N = 10;
			            public void foo() {
			                System.out.println(E1InnerInner.N);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    static class E1Inner {
			        public static class E1InnerInner {
			            public static int N = 10;
			            public void foo() {
			                System.out.println((new E1InnerInner()).N);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    static class E1Inner {
			        public static class E1InnerInner {
			            public static int N = 10;
			            public void foo() {
			                System.out.println(E1InnerInner.N);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static int E1N;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    public static int E2N = 10;
			    public void foo() {
			        System.out.println(E1.E1N);
			        System.out.println(E2.E1N);
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public void foo() {
			        System.out.println(E1.E1N);
			        System.out.println(E2.E1N);
			        System.out.println(E3.E1N);
			        System.out.println(E2.E2N);
			        System.out.println(E3.E2N);
			    }
			}
			""";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		sample= """
			package test1;
			public class E2 extends E1 {
			    public static int E2N = 10;
			    public void foo() {
			        System.out.println(E1.E1N);
			        System.out.println(E1.E1N);
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test2;
			import test1.E1;
			import test1.E2;
			public class E3 extends E2 {
			    public void foo() {
			        System.out.println(E1.E1N);
			        System.out.println(E1.E1N);
			        System.out.println(E1.E1N);
			        System.out.println(E2.E2N);
			        System.out.println(E2.E2N);
			    }
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2}, null);

	}

	@Test
	public void testCodeStyle17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public boolean b= true;
			    public void foo() {
			        if (b)
			            System.out.println(10);
			        if (b) {
			            System.out.println(10);
			        } else
			            System.out.println(10);
			        if (b)
			            System.out.println(10);
			        else
			            System.out.println(10);
			        while (b)
			            System.out.println(10);
			        do
			            System.out.println(10);
			        while (b);
			        for(;;)
			            System.out.println(10);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public boolean b= true;
			    public void foo() {
			        if (b) {
			            System.out.println(10);
			        }
			        if (b) {
			            System.out.println(10);
			        } else {
			            System.out.println(10);
			        }
			        if (b) {
			            System.out.println(10);
			        } else {
			            System.out.println(10);
			        }
			        while (b) {
			            System.out.println(10);
			        }
			        do {
			            System.out.println(10);
			        } while (b);
			        for(;;) {
			            System.out.println(10);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        if (b)
			            System.out.println(1);
			        else if (q)
			            System.out.println(1);
			        else
			            if (b && q)
			                System.out.println(1);
			            else
			                System.out.println(2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        if (b) {
			            System.out.println(1);
			        } else if (q) {
			            System.out.println(1);
			        } else
			            if (b && q) {
			                System.out.println(1);
			            } else {
			                System.out.println(2);
			            }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        for (;b;)
			            for (;q;)
			                if (b)
			                    System.out.println(1);
			                else if (q)
			                    System.out.println(2);
			                else
			                    System.out.println(3);
			        for (;b;)
			            for (;q;) {
			               \s
			            }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        for (;b;) {
			            for (;q;) {
			                if (b) {
			                    System.out.println(1);
			                } else if (q) {
			                    System.out.println(2);
			                } else {
			                    System.out.println(3);
			                }
			            }
			        }
			        for (;b;) {
			            for (;q;) {
			               \s
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle20() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        while (b)
			            while (q)
			                if (b)
			                    System.out.println(1);
			                else if (q)
			                    System.out.println(2);
			                else
			                    System.out.println(3);
			        while (b)
			            while (q) {
			               \s
			            }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        while (b) {
			            while (q) {
			                if (b) {
			                    System.out.println(1);
			                } else if (q) {
			                    System.out.println(2);
			                } else {
			                    System.out.println(3);
			                }
			            }
			        }
			        while (b) {
			            while (q) {
			               \s
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle21() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        do
			            do
			                if (b)
			                    System.out.println(1);
			                else if (q)
			                    System.out.println(2);
			                else
			                    System.out.println(3);
			            while (q);
			        while (b);
			        do
			            do {
			               \s
			            } while (q);
			        while (b);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public boolean b, q;
			    public void foo() {
			        do {
			            do {
			                if (b) {
			                    System.out.println(1);
			                } else if (q) {
			                    System.out.println(2);
			                } else {
			                    System.out.println(3);
			                }
			            } while (q);
			        } while (b);
			        do {
			            do {
			               \s
			            } while (q);
			        } while (b);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle22() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import test2.I1;
			public class E1 {
			    public void foo() {
			        I1 i1= new I1() {
			            private static final int N= 10;
			            public void foo() {
			                System.out.println(N);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public interface I1 {}
			""";
		pack2.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle23() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int fNb= 0;
			    public void foo() {
			        if (true)
			            fNb++;
			        String s; //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		sample= """
			package test1;
			public class E1 {
			    private int fNb= 0;
			    public void foo() {
			        if (true) {
			            this.fNb++;
			        }
			        String s;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle24() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            System.out.println("");
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true) {
			            System.out.println(""); //$NON-NLS-1$
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle25() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        System.out.println(I1Impl.N);
			        I1 i1= new I1();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			import test2.I1;
			public class I1Impl implements I1 {}
			""";
		pack1.createCompilationUnit("I1Impl.java", sample, false, null);

		sample= """
			package test1;
			public class I1 {}
			""";
		pack1.createCompilationUnit("I1.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public interface I1 {
			    public static int N= 10;
			}
			""";
		pack2.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        System.out.println(test2.I1.N);
			        I1 i1= new I1();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle26() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {}
			    private void bar() {
			        foo();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {}
			    private void bar() {
			        this.foo();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyle27() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static void foo() {}
			    private void bar() {
			        foo();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= """
			package test1;
			public class E1 {
			    public static void foo() {}
			    private void bar() {
			        E1.foo();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug118204() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    static String s;
			    public void foo() {
			        System.out.println(s);
			    }
			    E1(){}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    static String s;
			    public void foo() {
			        System.out.println(E1.s);
			    }
			    E1(){}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug114544() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        System.out.println(new E1().i);
			    }
			    public static int i= 10;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        new E1();
			        System.out.println(E1.i);
			    }
			    public static int i= 10;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug119170_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static void foo() {}
			}
			""";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private static class E1 {}
			    public void bar() {
			        test1.E1 e1= new test1.E1();
			        e1.foo();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E2 {
			    private static class E1 {}
			    public void bar() {
			        test1.E1 e1= new test1.E1();
			        test1.E1.foo();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug119170_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static void foo() {}
			}
			""";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    private static String E1= "";
			    public void foo() {
			        test1.E1 e1= new test1.E1();
			        e1.foo();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E2 {
			    private static String E1= "";
			    public void foo() {
			        test1.E1 e1= new test1.E1();
			        test1.E1.foo();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug123468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    protected int field;
			}
			""";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {
			    private int field;
			    public void foo() {
			        super.field= 10;
			        field= 10;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= """
			package test1;
			public class E2 extends E1 {
			    private int field;
			    public void foo() {
			        super.field= 10;
			        this.field= 10;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug129115() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private static int NUMBER;
			    public void reset() {
			        NUMBER= 0;
			    }
			    enum MyEnum {
			        STATE_1, STATE_2, STATE_3
			      };
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    private static int NUMBER;
			    public void reset() {
			        E1.NUMBER= 0;
			    }
			    enum MyEnum {
			        STATE_1, STATE_2, STATE_3
			      };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug135219() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {\s
			    public int i;
			    public void print(int j) {}
			    public void foo() {
			        print(i);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= """
			package test;
			public class E {\s
			    public int i;
			    public void print(int j) {}
			    public void foo() {
			        this.print(this.i);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug_138318() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E<I> {
			    private static int I;
			    private static String STR() {return "";}
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(I);
			                System.out.println(STR());
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= """
			package test;
			public class E<I> {
			    private static int I;
			    private static String STR() {return "";}
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(E.I);
			                System.out.println(E.STR());
			            }
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug138325_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E<I> {
			    private int i;
			    private String str() {return "";}
			    public void foo() {
			        System.out.println(i);
			        System.out.println(str());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= """
			package test;
			public class E<I> {
			    private int i;
			    private String str() {return "";}
			    public void foo() {
			        System.out.println(this.i);
			        System.out.println(this.str());
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug138325_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E<I> {
			    private int i;
			    private String str() {return "";}
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			                System.out.println(str());
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= """
			package test;
			public class E<I> {
			    private int i;
			    private String str() {return "";}
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(E.this.i);
			                System.out.println(E.this.str());
			            }
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleQualifyMethodAccessesImportConflictBug_552461() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;

			import static java.util.Date.parse;

			import java.sql.Date;

			public class E {
			    public Object addFullyQualifiedName(String dateText, Date sqlDate) {
			        return parse(dateText);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= """
			package test;

			import static java.util.Date.parse;

			import java.sql.Date;

			public class E {
			    public Object addFullyQualifiedName(String dateText, Date sqlDate) {
			        return java.util.Date.parse(dateText);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCodeStyleQualifyMethodAccessesAlreadyImportedBug_552461() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;

			import static java.util.Date.parse;

			import java.util.Date;

			public class E {
			    public Object addFullyQualifiedName(String dateText, Date sqlDate) {
			        return parse(dateText);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= """
			package test;

			import static java.util.Date.parse;

			import java.util.Date;

			public class E {
			    public Object addFullyQualifiedName(String dateText, Date sqlDate) {
			        return Date.parse(dateText);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCodeStyle_Bug140565() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.io.*;
			public class E1 {
			        static class ClassA {static ClassB B;}
			        static class ClassB {static ClassC C;}
			        static class ClassC {static ClassD D;}
			        static class ClassD {}

			        public void foo() {
			                ClassA.B.C.D.toString();
			        }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= """
			package test1;
			public class E1 {
			        static class ClassA {static ClassB B;}
			        static class ClassB {static ClassC C;}
			        static class ClassC {static ClassD D;}
			        static class ClassD {}

			        public void foo() {
			                ClassC.D.toString();
			        }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug157480() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 extends ETop {
			    public void bar(boolean b) {
			        if (b == true && b || b) {}
			    }
			}
			class ETop {
			    public void bar(boolean b) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

		sample= """
			package test1;
			public class E1 extends ETop {
			    @Override
			    public void bar(boolean b) {
			        if (((b == true) && b) || b) {}
			    }
			}
			class ETop {
			    public void bar(boolean b) {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCodeStyleBug154787() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			interface E1 {String FOO = "FOO";}
			""";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 implements E1 {}
			""";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 {
			    public String foo() {
			        return E2.FOO;
			    }
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyleBug579044() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			class E1 {public static String FOO = "FOO";}
			""";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 extends E1 {}
			""";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			import test1.E2;
			public class E3 {
			    public String foo() {
			        return E2.FOO;
			    }
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyleBug189398() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(Object o) {
			        if (o != null)
			            System.out.println(o);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= """
			package test1;
			public class E1 {
			    public void foo(Object o) {
			        if (o != null) {
			            System.out.println(o);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCodeStyleBug238828_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;

			    public String foo() {
			        return "Foo" + field //MyComment
			                    + field;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    private int field;

			    public String foo() {
			        return "Foo" + this.field //MyComment
			                    + this.field;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCodeStyleBug238828_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private static int FIELD;

			    public String foo() {
			        return "Foo" + FIELD //MyComment
			                    + FIELD;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= """
			package test1;
			public class E1 {
			    private static int FIELD;

			    public String foo() {
			        return "Foo" + E1.FIELD //MyComment
			                    + E1.FIELD;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCodeStyleBug346230() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);

			String sample= """
				package test1;
				interface CinematicEvent {
				    public void stop();
				    public boolean internalUpdate();
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("CinematicEvent.java", sample, false, null);

			sample= """
				package test1;
				abstract class E1 implements CinematicEvent {

				    protected PlayState playState = PlayState.Stopped;
				    protected LoopMode loopMode = LoopMode.DontLoop;

				    public boolean internalUpdate() {
				        return loopMode == loopMode.DontLoop;
				    }

				    public void stop() {
				    }

				    public void read() {
				        Object ic= new Object();
				        playState.toString();
				    }

				    enum PlayState {
				        Stopped
				    }
				    enum LoopMode {
				        DontLoop
				    }
				}
				""";
			ICompilationUnit cu2= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			sample= """
				package test1;
				abstract class E1 implements CinematicEvent {

				    protected PlayState playState = PlayState.Stopped;
				    protected LoopMode loopMode = LoopMode.DontLoop;

				    @Override
				    public boolean internalUpdate() {
				        return this.loopMode == LoopMode.DontLoop;
				    }

				    @Override
				    public void stop() {
				    }

				    public void read() {
				        final Object ic= new Object();
				        this.playState.toString();
				    }

				    enum PlayState {
				        Stopped
				    }
				    enum LoopMode {
				        DontLoop
				    }
				}
				""";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { cu1.getBuffer().getContents(), expected1 }, null);
		} finally {
			JavaProjectHelper.delete(project);
		}

	}

	@Test
	public void testCodeStyle_StaticAccessThroughInstance_Bug307407() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private final String localString = new MyClass().getMyString();
			    public static class MyClass {
			        public static String getMyString() {
			            return "a";
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveNonStaticQualifier_Bug219204_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void test() {
			        E1 t = E1.bar().g().g().foo(E1.foo(null).bar()).bar();
			    }

			    private static E1 foo(E1 t) {
			        return null;
			    }

			    private static E1 bar() {
			        return null;
			    }

			    private E1 g() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public void test() {
			        E1.bar().g().g();
			        E1.foo(null);
			        E1.foo(E1.bar());
			        E1 t = E1.bar();
			    }

			    private static E1 foo(E1 t) {
			        return null;
			    }

			    private static E1 bar() {
			        return null;
			    }

			    private E1 g() {
			        return null;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveNonStaticQualifier_Bug219204_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void test() {
			        while (true)
			            new E1().bar1().bar2().bar3();
			    }
			    private static E1 bar1() {
			        return null;
			    }
			    private static E1 bar2() {
			        return null;
			    }
			    private static E1 bar3() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			public class E1 {
			    public void test() {
			        while (true) {
			            new E1();
			            E1.bar1();
			            E1.bar2();
			            E1.bar3();
			        }
			    }
			    private static E1 bar1() {
			        return null;
			    }
			    private static E1 bar2() {
			        return null;
			    }
			    private static E1 bar3() {
			        return null;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testChangeNonstaticAccessToStatic_Bug439733() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			class Singleton {
			    public static String name = "The Singleton";
			    public static Singleton instance = new Singleton();
			    public static Singleton getInstance() {
			        return instance;
			    }
			}

			public class E1 {
			    public static void main(String[] args) {
			        System.out.println(Singleton.instance.name);
			        System.out.println(Singleton.getInstance().name);
			        System.out.println(Singleton.getInstance().getInstance().name);
			        System.out.println(new Singleton().name);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= """
			package test1;
			class Singleton {
			    public static String name = "The Singleton";
			    public static Singleton instance = new Singleton();
			    public static Singleton getInstance() {
			        return instance;
			    }
			}

			public class E1 {
			    public static void main(String[] args) {
			        System.out.println(Singleton.name);
			        Singleton.getInstance();
			        System.out.println(Singleton.name);
			        Singleton.getInstance();
			        Singleton.getInstance();
			        System.out.println(Singleton.name);
			        new Singleton();
			        System.out.println(Singleton.name);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCombination01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    private int i= 10;
			    private int j= 20;
			   \s
			    public void foo() {
			        i= j;
			        i= 20;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= """
			package test1;

			public class E1 {
			    private int j= 20;
			   \s
			    public void foo() {
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCombination02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            System.out.println("");
			        if (true)
			            System.out.println("");
			        if (true)
			            System.out.println("");
			        System.out.println("");
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true) {
			            System.out.println(""); //$NON-NLS-1$
			        }
			        if (true) {
			            System.out.println(""); //$NON-NLS-1$
			        }
			        if (true) {
			            System.out.println(""); //$NON-NLS-1$
			        }
			        System.out.println(""); //$NON-NLS-1$
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCombination03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1; \s
			import java.util.Iterator;
			import java.util.List;
			public class E1  {
			    private List<String> fList;
			    public void foo() {
			        for (Iterator<String> iter = fList.iterator(); iter.hasNext();) {
			            String element = (String) iter.next();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1; \s
			import java.util.List;
			public class E1  {
			    private List<String> fList;
			    public void foo() {
			        for (String string : this.fList) {
			            String element = (String) string;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testBug245254() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i= 0;
			    void method() {
			        if (true
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    private int i= 0;
			    void method() {
			        if (true
			    }
			}
			""";
		String expected= sample;
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testCombinationBug120585() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i= 0;
			    void method() {
			        int[] array= null;
			        for (int i= 0; i < array.length; i++)
			            System.out.println(array[i]);
			        i= 12;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= """
			package test1;
			public class E1 {
			    void method() {
			        int[] array= null;
			        for (int element : array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCombinationBug125455() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1  {
			    private void bar(boolean wait) {
			        if (!wait)\s
			            return;
			    }
			    private void foo(String s) {
			        String s1= "";
			        if (s.equals(""))
			            System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= """
			package test1;
			public class E1  {
			    private void bar(boolean wait) {
			        if (!wait) {
			            return;
			        }
			    }
			    private void foo(String s) {
			        String s1= ""; //$NON-NLS-1$
			        if (s.equals("")) { //$NON-NLS-1$
			            System.out.println();
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCombinationBug157468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {
			        if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g h i j k
			            final String s = "";
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		Hashtable<String, String> options= TestOptions.getDefaultOptions();

		Map<String, String> formatterSettings= DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		formatterSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION,
				DefaultCodeFormatterConstants.FALSE);
		options.putAll(formatterSettings);

		JavaCore.setOptions(options);

		sample= """
			package test1;

			public class E1 {
				private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {
					if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g
																			// h i j k
						final String s = "";
					}
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCombinationBug234984_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void method(String[] arr) {
			        for (int i = 0; i < arr.length; i++) {
			            String item = arr[i];
			            String item2 = item + "a";
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E1 {
			    public void method(String[] arr) {
			        for (final String item : arr) {
			            final String item2 = item + "a";
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCombinationBug234984_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void method(List<E1> es) {
			        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {
			            E1 next = iterator.next();
			            next= new E1();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void method(List<E1> es) {
			        for (E1 next : es) {
			            next= new E1();
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testKeepCommentOnReplacement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean[] refactorBooleanArray() {
			        boolean[] array = new boolean[10];

			        // Keep this comment
			        for (int i = 0; i < array.length; i++) {
			            array[i] = true;
			        }

			        return array;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {
			    public boolean[] refactorBooleanArray() {
			        boolean[] array = new boolean[10];

			        // Keep this comment
			        Arrays.fill(array, true);

			        return array;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testKeepCommentOnRemoval() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    class A {
			        A(int a) {}

			        A() {
			            // Keep this comment
			            super();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		sample= """
			package test1;

			public class E1 {
			    class A {
			        A(int a) {}

			        A() {
			            // Keep this comment
			           \s
			        }
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantSuperCallCleanup_description)));
	}

	@Test
	public void testSubstring() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private String textInInstance = "foo";

			    public String reduceSubstring(String text) {
			        // Keep this comment
			        return text.substring(2, text.length());
			    }

			    public String reduceSubstringOnField() {
			        // Keep this comment
			        return textInInstance.substring(3, textInInstance.length());
			    }

			    public String reduceSubstringOnExpression(String text) {
			        // Keep this comment
			        return (textInInstance + text).substring(4, (textInInstance + text).length());
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.SUBSTRING);

		String expected= """
			package test1;

			public class E {
			    private String textInInstance = "foo";

			    public String reduceSubstring(String text) {
			        // Keep this comment
			        return text.substring(2);
			    }

			    public String reduceSubstringOnField() {
			        // Keep this comment
			        return textInInstance.substring(3);
			    }

			    public String reduceSubstringOnExpression(String text) {
			        // Keep this comment
			        return (textInInstance + text).substring(4);
			    }
			}
			""";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SubstringCleanUp_description)));
	}

	@Test
	public void testKeepSubstring() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    public String doNotReduceSubstringOnOtherExpression(String text) {
			        return text.substring(5, text.hashCode());
			    }

			    public String doNotReduceSubstringOnConstant(String text) {
			        return text.substring(6, 123);
			    }

			    public String doNotReduceSubstringOnDifferentVariable(String text1, String text2) {
			        return text1.substring(7, text2.length());
			    }

			    public String doNotReduceSubstringOnActiveExpression(List<String> texts) {
			        return texts.remove(0).substring(7, texts.remove(0).length());
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SUBSTRING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testUseArraysFill() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    private static final boolean CONSTANT = true;
			    private boolean[] booleanArray = new boolean[10];

			    public boolean[] refactorBooleanArray() {
			        boolean[] array = new boolean[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = true;
			        }
			        for (int i = 0; i < array.length; ++i) {
			            array[i] = false;
			        }

			        return array;
			    }

			    public boolean[] refactorWithConstant() {
			        boolean[] array = new boolean[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = Boolean.TRUE;
			        }

			        return array;
			    }

			    public int[] refactorNumberArray() {
			        int[] array = new int[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = 123;
			        }
			        for (int i = 0; i < array.length; i++) {
			            array[i] = Integer.MAX_VALUE;
			        }

			        return array;
			    }

			    public char[] refactorCharacterArray() {
			        char[] array = new char[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = '!';
			        }
			        for (int j = 0; array.length > j; j++) {
			            array[j] = 'z';
			        }

			        return array;
			    }

			    public String[] refactorStringArray() {
			        String[] array = new String[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = "foo";
			        }
			        for (int j = 0; array.length > j; j++) {
			            array[j] = null;
			        }

			        return array;
			    }

			    public String[] refactorStringArrayWithLocalVar(String s) {
			        String[] array = new String[10];

			        String var = "foo";
			        for (int i = 0; i < array.length; i++) {
			            array[i] = var;
			        }

			        for (int i = 0; i < array.length; i++) {
			            array[i] = s;
			        }

			        return array;
			    }

			    public String[] refactorArrayWithFinalField() {
			        Boolean[] array = new Boolean[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = CONSTANT;
			        }

			        return array;
			    }

			    public String[] refactorBackwardLoopOnArrary() {
			        String[] array = new String[10];

			        for (int i = array.length - 1; i >= 0; i--) {
			            array[i] = "foo";
			        }
			        for (int i = array.length - 1; 0 <= i; --i) {
			            array[i] = "foo";
			        }

			        return array;
			    }

			    public void refactorExternalArray() {
			        for (int i = 0; i < booleanArray.length; i++) {
			            booleanArray[i] = true;
			        }
			        for (int i = 0; i < this.booleanArray.length; i++) {
			            this.booleanArray[i] = false;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		String output= """
			package test1;

			import java.util.Arrays;

			public class E {
			    private static final boolean CONSTANT = true;
			    private boolean[] booleanArray = new boolean[10];

			    public boolean[] refactorBooleanArray() {
			        boolean[] array = new boolean[10];

			        Arrays.fill(array, true);
			        Arrays.fill(array, false);

			        return array;
			    }

			    public boolean[] refactorWithConstant() {
			        boolean[] array = new boolean[10];

			        Arrays.fill(array, Boolean.TRUE);

			        return array;
			    }

			    public int[] refactorNumberArray() {
			        int[] array = new int[10];

			        Arrays.fill(array, 123);
			        Arrays.fill(array, Integer.MAX_VALUE);

			        return array;
			    }

			    public char[] refactorCharacterArray() {
			        char[] array = new char[10];

			        Arrays.fill(array, '!');
			        Arrays.fill(array, 'z');

			        return array;
			    }

			    public String[] refactorStringArray() {
			        String[] array = new String[10];

			        Arrays.fill(array, "foo");
			        Arrays.fill(array, null);

			        return array;
			    }

			    public String[] refactorStringArrayWithLocalVar(String s) {
			        String[] array = new String[10];

			        String var = "foo";
			        Arrays.fill(array, var);

			        Arrays.fill(array, s);

			        return array;
			    }

			    public String[] refactorArrayWithFinalField() {
			        Boolean[] array = new Boolean[10];

			        Arrays.fill(array, CONSTANT);

			        return array;
			    }

			    public String[] refactorBackwardLoopOnArrary() {
			        String[] array = new String[10];

			        Arrays.fill(array, "foo");
			        Arrays.fill(array, "foo");

			        return array;
			    }

			    public void refactorExternalArray() {
			        Arrays.fill(booleanArray, true);
			        Arrays.fill(this.booleanArray, false);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { output }, null);
	}

	@Test
	public void testDoNotUseArraysFill() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public int field = 4;
			    private static int changingValue = 0;
			    private final int CONSTANT = changingValue++;
			    private E1[] arrayOfE1 = null;

			    public boolean[] doNotReplaceNonForEachLoop() {
			        boolean[] array = new boolean[10];

			        for (int i = 1; i < array.length; i++) {
			            array[i] = true;
			        }
			        for (int i = 0; i < array.length - 1; i++) {
			            array[i] = false;
			        }

			        return array;
			    }

			    public boolean[] doNotReplaceWierdLoop(int k) {
			        boolean[] array = new boolean[10];

			        for (int m = 0; k++ < array.length; m++) {
			            array[m] = true;
			        }

			        return array;
			    }

			    public int[] doNotRefactorInitWithoutConstant(int n) {
			        int[] array = new int[10];

			        for (int p = 0; p < array.length; p++) {
			            array[p] = p*p;
			        }
			        for (int p = array.length - 1; p >= 0; p--) {
			            array[p] = n++;
			        }

			        return array;
			    }

			    public int[] doNotRefactorInitWithIndexVarOrNonFinalField(int q) {
			        int[] array = new int[10];

			        for (int r = 0; r < array.length; r++) {
			            array[r] = r;
			        }
			        for (int r = 0; r < array.length; r++) {
			            array[r] = field;
			        }

			        return array;
			    }

			    public int[] doNotRefactorCodeThatUsesIndex() {
			        int[] array = new int[10];

			        for (int s = 0; s < array.length; s++) {
			            array[s] = arrayOfE1[s].CONSTANT;
			        }

			        return array;
			    }

			    public int[] doNotRefactorWithAnotherStatement() {
			        int[] array = new int[10];

			        for (int i = 0; i < array.length; i++) {
			            array[i] = 123;
			            System.out.println("Do not forget me!");
			        }
			        for (int i = array.length - 1; i >= 0; i--) {
			            System.out.println("Do not forget me!");
			            array[i] = 123;
			        }

			        return array;
			    }

			    public int[] doNotRefactorWithSpecificIndex() {
			        int[] array = new int[10];

			        for (int i = 0; i < array.length; i++) {
			            array[0] = 123;
			        }
			        for (int i = 0; i < array.length; i++) {
			            array[array.length - i] = 123;
			        }

			        return array;
			    }

			    public int[] doNotRefactorAnotherArray(int[] array3) {
			        int[] array = new int[10];
			        int[] array2 = new int[10];

			        for (int i = 0; i < array.length; i++) {
			            array2[i] = 123;
			        }
			        for (int i = 0; i < array.length; i++) {
			            array3[i] = 123;
			        }

			        return array;
			    }

			    public int[] doNotRefactorSpecialAssignment(int[] array) {
			        for (int i = 0; i < array.length; i++) {
			            array[i] += 123;
			        }

			        return array;
			    }

			    public char[] doNotRefactorIntIntoCharArray(char[] array) {
			        for (int i = 0; i < array.length; i++) {
			            array[i] = 123;
			        }

			        return array;
			    }

			    public byte[] doNotRefactorIntIntoByteArray(byte[] array) {
			        for (int i = 0; i < array.length; i++) {
			            array[i] = 123;
			        }

			        return array;
			    }

			    public short[] doNotRefactorIntIntoShortArray(short[] array) {
			        for (int i = 0; i < array.length; i++) {
			            array[i] = 123;
			        }

			        return array;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLazyLogicalOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E1 {
			    private static int staticField = 0;

			    public void replaceOperatorWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 & b2;
			        boolean newBoolean2 = b1 | b2;
			    }

			    public void replaceOperatorWithExtendedOperands(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = b1 & b2 & b3;
			        boolean newBoolean2 = b1 | b2 | b3;
			    }

			    public void replaceOperatorWithWrappers(Boolean b1, Boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 & b2;
			        boolean newBoolean2 = b1 | b2;
			    }

			    public void doNotReplaceOperatorWithIntegers(int i1, int i2) {
			        int newInteger1 = i1 & i2;
			        int newInteger2 = i1 | i2;
			    }

			    public void replaceOperatorWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) & (i3 != i4);
			        boolean newBoolean2 = (i1 == i2) | (i3 != i4);
			    }

			    public void replaceOperatorWithUnparentherizedExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = i1 == i2 & i3 != i4 & i5 != i6;
			        boolean newBoolean2 = i1 == i2 | i3 != i4 | i5 != i6;
			    }

			    public void doNotReplaceOperatorWithMethods(List<String> myList) {
			        boolean newBoolean1 = myList.remove("lorem") & myList.remove("ipsum");
			        boolean newBoolean2 = myList.remove("lorem") | myList.remove("ipsum");
			    }

			    public void doNotReplaceOperatorWithArrayAccess() {
			        boolean[] booleans = new boolean[] {true, true};
			        boolean newBoolean1 = booleans[0] & booleans[1] & booleans[2];
			        boolean newBoolean2 = booleans[0] | booleans[1] | booleans[2];
			    }

			    public void doNotReplaceOperatorWithDivision(int i1, int i2) {
			        boolean newBoolean1 = (i1 == 123) & ((10 / i1) == i2);
			        boolean newBoolean2 = (i1 == 123) | ((10 / i1) == i2);
			    }

			    public void replaceOperatorWithMethodOnLeftOperand(List<String> myList, boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = myList.remove("lorem") & b1 & b2;
			        boolean newBoolean2 = myList.remove("lorem") | b1 | b2;
			    }

			    public void doNotReplaceOperatorWithIncrements(int i1, int i2, int i3, int i4) {
			        boolean newBoolean1 = (i1 == i2) & (i3 != i4++);
			        boolean newBoolean2 = (i1 == i2) & (i3 != ++i4);
			        boolean newBoolean3 = (i1 == i2) & (i3 != i4--);
			        boolean newBoolean4 = (i1 == i2) & (i3 != --i4);

			        boolean newBoolean5 = (i1 == i2) | (i3 != i4++);
			        boolean newBoolean6 = (i1 == i2) | (i3 != ++i4);
			        boolean newBoolean7 = (i1 == i2) | (i3 != i4--);
			        boolean newBoolean8 = (i1 == i2) | (i3 != --i4);
			    }

			    public void doNotReplaceOperatorWithAssignments(int i1, int i2, boolean b1, boolean b2) {
			        boolean newBoolean1 = (i1 == i2) & (b1 = b2);
			        boolean newBoolean2 = (i1 == i2) | (b1 = b2);
			    }

			    private class SideEffect {
			        private SideEffect() {
			            staticField++;
			        }
			    }

			    public void doNotReplaceOperatorWithInstanciations(Boolean b1) {
			        boolean newBoolean1 = b1 & new SideEffect() instanceof SideEffect;
			        boolean newBoolean2 = b1 | new SideEffect() instanceof SideEffect;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR);

		sample= """
			package test1;

			import java.util.List;

			public class E1 {
			    private static int staticField = 0;

			    public void replaceOperatorWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && b2;
			        boolean newBoolean2 = b1 || b2;
			    }

			    public void replaceOperatorWithExtendedOperands(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && b2 && b3;
			        boolean newBoolean2 = b1 || b2 || b3;
			    }

			    public void replaceOperatorWithWrappers(Boolean b1, Boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && b2;
			        boolean newBoolean2 = b1 || b2;
			    }

			    public void doNotReplaceOperatorWithIntegers(int i1, int i2) {
			        int newInteger1 = i1 & i2;
			        int newInteger2 = i1 | i2;
			    }

			    public void replaceOperatorWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) && (i3 != i4);
			        boolean newBoolean2 = (i1 == i2) || (i3 != i4);
			    }

			    public void replaceOperatorWithUnparentherizedExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = i1 == i2 && i3 != i4 && i5 != i6;
			        boolean newBoolean2 = i1 == i2 || i3 != i4 || i5 != i6;
			    }

			    public void doNotReplaceOperatorWithMethods(List<String> myList) {
			        boolean newBoolean1 = myList.remove("lorem") & myList.remove("ipsum");
			        boolean newBoolean2 = myList.remove("lorem") | myList.remove("ipsum");
			    }

			    public void doNotReplaceOperatorWithArrayAccess() {
			        boolean[] booleans = new boolean[] {true, true};
			        boolean newBoolean1 = booleans[0] & booleans[1] & booleans[2];
			        boolean newBoolean2 = booleans[0] | booleans[1] | booleans[2];
			    }

			    public void doNotReplaceOperatorWithDivision(int i1, int i2) {
			        boolean newBoolean1 = (i1 == 123) & ((10 / i1) == i2);
			        boolean newBoolean2 = (i1 == 123) | ((10 / i1) == i2);
			    }

			    public void replaceOperatorWithMethodOnLeftOperand(List<String> myList, boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = myList.remove("lorem") && b1 && b2;
			        boolean newBoolean2 = myList.remove("lorem") || b1 || b2;
			    }

			    public void doNotReplaceOperatorWithIncrements(int i1, int i2, int i3, int i4) {
			        boolean newBoolean1 = (i1 == i2) & (i3 != i4++);
			        boolean newBoolean2 = (i1 == i2) & (i3 != ++i4);
			        boolean newBoolean3 = (i1 == i2) & (i3 != i4--);
			        boolean newBoolean4 = (i1 == i2) & (i3 != --i4);

			        boolean newBoolean5 = (i1 == i2) | (i3 != i4++);
			        boolean newBoolean6 = (i1 == i2) | (i3 != ++i4);
			        boolean newBoolean7 = (i1 == i2) | (i3 != i4--);
			        boolean newBoolean8 = (i1 == i2) | (i3 != --i4);
			    }

			    public void doNotReplaceOperatorWithAssignments(int i1, int i2, boolean b1, boolean b2) {
			        boolean newBoolean1 = (i1 == i2) & (b1 = b2);
			        boolean newBoolean2 = (i1 == i2) | (b1 = b2);
			    }

			    private class SideEffect {
			        private SideEffect() {
			            staticField++;
			        }
			    }

			    public void doNotReplaceOperatorWithInstanciations(Boolean b1) {
			        boolean newBoolean1 = b1 & new SideEffect() instanceof SideEffect;
			        boolean newBoolean2 = b1 | new SideEffect() instanceof SideEffect;
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_LazyLogical_description)));
	}

	@Test
	public void testValueOfRatherThanInstantiation() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public static void replaceWrapperConstructorsWithValueOf() {
			        // Replace all calls to wrapper constructors with calls to .valueOf() methods
			        byte byPrimitive = 4;
			        boolean boPrimitive = true;
			        char cPrimitive = 'c';
			        double dPrimitive = 1;
			        Double dObject = Double.valueOf(1d);
			        float fPrimitive = 1f;
			        long lPrimitive = 1;
			        short shPrimitive = 1;
			        int iPrimitive = 1;

			        // Primitive literals
			        Byte by = new Byte((byte) 4);
			        Boolean bo = new Boolean(true);
			        Character c = new Character('c');
			        Double d = new Double(1);
			        Float f1 = new Float(1f);
			        Float f2 = new Float(1d);
			        Long l = new Long(1);
			        Short s = new Short((short) 1);
			        Integer i = new Integer(1);

			        // Primitive variables
			        by = new Byte(byPrimitive);
			        bo = new Boolean(boPrimitive);
			        c = new Character(cPrimitive);
			        d = new Double(dPrimitive);
			        f1 = new Float(fPrimitive);
			        f2 = new Float(dPrimitive);
			        l = new Long(lPrimitive);
			        s = new Short(shPrimitive);
			        i = new Integer(iPrimitive);

			        // Implicit object narrowing
			        Float f3 = new Float(dObject);
			    }

			    public static void parsedByStringAutoboxedToPrimitive() {
			        // Keep this comment
			        byte by = new Byte("42");
			        boolean bo = new Boolean("true");
			        double d = new Double("42");
			        float f = new Float("42");
			        long l = new Long("42");
			        short s = new Short("42");
			        int i = new Integer("42");
			    }

			    public static void removeUnnecessaryObjectCreation() {
			        // Keep this comment
			        new Byte("0").byteValue();
			        new Boolean("true").booleanValue();
			        new Integer("42").intValue();
			        new Short("42").shortValue();
			        new Long("42").longValue();
			        new Float("42.42").floatValue();
			        new Double("42.42").doubleValue();
			    }

			    public static void removeUnnecessaryConstructorInvocationsInPrimitiveContext() {
			        // Keep this comment
			        byte by = new Byte((byte) 0);
			        boolean bo = new Boolean(true);
			        int i = new Integer(42);
			        long l = new Long(42);
			        short s = new Short((short) 42);
			        float f = new Float(42.42F);
			        double d = new Double(42.42);
			    }

			    public static void removeUnnecessaryConstructorInvocationsInSwitch() {
			        byte by = (byte) 4;
			        char c = 'c';
			        short s = (short) 1;
			        int i = 1;

			        // Keep this comment
			        switch (new Byte(by)) {
			        // Keep this comment too
			        default:
			        }
			        switch (new Character(c)) {
			        default:
			        }
			        switch (new Short(s)) {
			        default:
			        }
			        switch (new Integer(i)) {
			        default:
			        }
			    }

			    public static String removeUnnecessaryConstructorInvocationsInArrayAccess(String[] strings, int i) {
			        return strings[new Integer(i)];
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public static void replaceWrapperConstructorsWithValueOf() {
			        // Replace all calls to wrapper constructors with calls to .valueOf() methods
			        byte byPrimitive = 4;
			        boolean boPrimitive = true;
			        char cPrimitive = 'c';
			        double dPrimitive = 1;
			        Double dObject = Double.valueOf(1d);
			        float fPrimitive = 1f;
			        long lPrimitive = 1;
			        short shPrimitive = 1;
			        int iPrimitive = 1;

			        // Primitive literals
			        Byte by = Byte.valueOf((byte) 4);
			        Boolean bo = Boolean.valueOf(true);
			        Character c = Character.valueOf('c');
			        Double d = Double.valueOf(1);
			        Float f1 = Float.valueOf(1f);
			        Float f2 = Float.valueOf((float) 1d);
			        Long l = Long.valueOf(1);
			        Short s = Short.valueOf((short) 1);
			        Integer i = Integer.valueOf(1);

			        // Primitive variables
			        by = Byte.valueOf(byPrimitive);
			        bo = Boolean.valueOf(boPrimitive);
			        c = Character.valueOf(cPrimitive);
			        d = Double.valueOf(dPrimitive);
			        f1 = Float.valueOf(fPrimitive);
			        f2 = Float.valueOf((float) dPrimitive);
			        l = Long.valueOf(lPrimitive);
			        s = Short.valueOf(shPrimitive);
			        i = Integer.valueOf(iPrimitive);

			        // Implicit object narrowing
			        Float f3 = dObject.floatValue();
			    }

			    public static void parsedByStringAutoboxedToPrimitive() {
			        // Keep this comment
			        byte by = Byte.valueOf("42");
			        boolean bo = Boolean.valueOf("true");
			        double d = Double.valueOf("42");
			        float f = Float.valueOf("42");
			        long l = Long.valueOf("42");
			        short s = Short.valueOf("42");
			        int i = Integer.valueOf("42");
			    }

			    public static void removeUnnecessaryObjectCreation() {
			        // Keep this comment
			        Byte.valueOf("0").byteValue();
			        Boolean.valueOf("true").booleanValue();
			        Integer.valueOf("42").intValue();
			        Short.valueOf("42").shortValue();
			        Long.valueOf("42").longValue();
			        Float.valueOf("42.42").floatValue();
			        Double.valueOf("42.42").doubleValue();
			    }

			    public static void removeUnnecessaryConstructorInvocationsInPrimitiveContext() {
			        // Keep this comment
			        byte by = (byte) 0;
			        boolean bo = true;
			        int i = 42;
			        long l = 42;
			        short s = (short) 42;
			        float f = 42.42F;
			        double d = 42.42;
			    }

			    public static void removeUnnecessaryConstructorInvocationsInSwitch() {
			        byte by = (byte) 4;
			        char c = 'c';
			        short s = (short) 1;
			        int i = 1;

			        // Keep this comment
			        switch (by) {
			        // Keep this comment too
			        default:
			        }
			        switch (c) {
			        default:
			        }
			        switch (s) {
			        default:
			        }
			        switch (i) {
			        default:
			        }
			    }

			    public static String removeUnnecessaryConstructorInvocationsInArrayAccess(String[] strings, int i) {
			        return strings[i];
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(
						MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_float_with_valueof,
						MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_float_with_float_value,
						MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_single_argument,
						MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_valueof)));
	}



	@Test
	public void testDoNotUseValueOfRatherThanInstantiation() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.math.BigInteger;

			public class E {
			    public static void doNotRefactorBigInteger() {
			        BigInteger bi = new BigInteger("42");
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testValueOfRatherThanInstantiationBug578917() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public static void replaceWrapperConstructorsWithValueOf() {
			        double k= 33;
			        Float f= new Float(((k= (4 * 3f / 72d))))\
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public static void replaceWrapperConstructorsWithValueOf() {
			        double k= 33;
			        Float f= Float.valueOf((float) (k= (4 * 3f / 72d)))\
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_float_with_float_value)));
	}

	@Test
	public void testPrimitiveComparison() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public int simplifyIntegerComparison(int number, int anotherNumber) {
			        // Keep this comment
			        return Integer.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyDoubleComparison(double number, double anotherNumber) {
			        // Keep this comment
			        return Double.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyFloatComparison(float number, float anotherNumber) {
			        // Keep this comment
			        return Float.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyShortComparison(short number, short anotherNumber) {
			        // Keep this comment
			        return Short.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyLongComparison(long number, long anotherNumber) {
			        // Keep this comment
			        return Long.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyCharacterComparison(char number, char anotherNumber) {
			        // Keep this comment
			        return Character.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyByteComparison(byte number, byte anotherNumber) {
			        // Keep this comment
			        return Byte.valueOf(number).compareTo(anotherNumber);
			    }

			    public int simplifyBooleanComparison(boolean number, boolean anotherNumber) {
			        // Keep this comment
			        return Boolean.valueOf(number).compareTo(anotherNumber);
			    }

			    public int refactorIntegerInstantiation(int number, int anotherNumber) {
			        // Keep this comment
			        return new Integer(number).compareTo(anotherNumber);
			    }

			    public int refactorIntegerCast(int number, int anotherNumber) {
			        // Keep this comment
			        return ((Integer) number).compareTo(anotherNumber);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public int simplifyIntegerComparison(int number, int anotherNumber) {
			        // Keep this comment
			        return Integer.compare(number, anotherNumber);
			    }

			    public int simplifyDoubleComparison(double number, double anotherNumber) {
			        // Keep this comment
			        return Double.compare(number, anotherNumber);
			    }

			    public int simplifyFloatComparison(float number, float anotherNumber) {
			        // Keep this comment
			        return Float.compare(number, anotherNumber);
			    }

			    public int simplifyShortComparison(short number, short anotherNumber) {
			        // Keep this comment
			        return Short.compare(number, anotherNumber);
			    }

			    public int simplifyLongComparison(long number, long anotherNumber) {
			        // Keep this comment
			        return Long.compare(number, anotherNumber);
			    }

			    public int simplifyCharacterComparison(char number, char anotherNumber) {
			        // Keep this comment
			        return Character.compare(number, anotherNumber);
			    }

			    public int simplifyByteComparison(byte number, byte anotherNumber) {
			        // Keep this comment
			        return Byte.compare(number, anotherNumber);
			    }

			    public int simplifyBooleanComparison(boolean number, boolean anotherNumber) {
			        // Keep this comment
			        return Boolean.compare(number, anotherNumber);
			    }

			    public int refactorIntegerInstantiation(int number, int anotherNumber) {
			        // Keep this comment
			        return Integer.compare(number, anotherNumber);
			    }

			    public int refactorIntegerCast(int number, int anotherNumber) {
			        // Keep this comment
			        return Integer.compare(number, anotherNumber);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_COMPARISON);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveComparisonCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveComparison() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public int doNotRefactorWrapper(Integer number, int anotherNumber) {
			        return Integer.valueOf(number).compareTo(anotherNumber);
			    }

			    public int doNotRefactorWrapperComparator(int number, Integer anotherNumber) {
			        return Integer.valueOf(number).compareTo(anotherNumber);
			    }

			    public int doNotRefactorString(String number, int anotherNumber) {
			        return Integer.valueOf(number).compareTo(anotherNumber);
			    }

			    public int doNotRefactorBadMethod(int number, int anotherNumber) {
			        return Integer.valueOf(number).valueOf(anotherNumber);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_COMPARISON);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveParsing() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public static void convertValueOfCallsToParseCallsInPrimitiveContext() {
			        // Keep this comment
			        byte by1 = Byte.valueOf("0");
			        byte by2 = Byte.valueOf("0", 10);
			        boolean bo = Boolean.valueOf("true");
			        int i1 = Integer.valueOf("42");
			        int i2 = Integer.valueOf("42", 10);
			        long l1 = Long.valueOf("42");
			        long l2 = Long.valueOf("42", 10);
			        short s1 = Short.valueOf("42");
			        short s2 = Short.valueOf("42", 10);
			        float f = Float.valueOf("42.42");
			        double d = Double.valueOf("42.42");
			    }

			    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {
			        // Keep this comment
			        char c = Character.valueOf('&');
			        byte by = Byte.valueOf((byte) 0);
			        boolean bo = Boolean.valueOf(true);
			        int i = Integer.valueOf(42);
			        long l = Long.valueOf(42);
			        short s = Short.valueOf((short) 42);
			        float f = Float.valueOf(42.42F);
			        double d = Double.valueOf(42.42);
			    }

			    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {
			        // Keep this comment
			        char c;
			        c = Character.valueOf('&');
			        byte by;
			        by = Byte.valueOf((byte) 0);
			        boolean bo1;
			        bo1 = Boolean.valueOf(true);
			        int i;
			        i = Integer.valueOf(42);
			        long l;
			        l = Long.valueOf(42);
			        short s;
			        s = Short.valueOf((short) 42);
			        float f;
			        f = Float.valueOf(42.42F);
			        double d;
			        d = Double.valueOf(42.42);
			    }

			    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {
			        // Keep this comment
			        return Character.valueOf('&');
			    }

			    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {
			        // Keep this comment
			        return Byte.valueOf((byte) 0);
			    }

			    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {
			        // Keep this comment
			        return Boolean.valueOf(true);
			    }

			    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {
			        // Keep this comment
			        return Integer.valueOf(42);
			    }

			    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {
			        // Keep this comment
			        return Long.valueOf(42);
			    }

			    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {
			        // Keep this comment
			        return Short.valueOf((short) 42);
			    }

			    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {
			        // Keep this comment
			        return Float.valueOf(42.42F);
			    }

			    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {
			        // Keep this comment
			        return Double.valueOf(42.42);
			    }

			    public static void removeUnnecessaryObjectCreation() {
			        // Keep this comment
			        new Byte("0").byteValue();
			        new Boolean("true").booleanValue();
			        new Integer("42").intValue();
			        new Short("42").shortValue();
			        new Long("42").longValue();
			        new Float("42.42").floatValue();
			        new Double("42.42").doubleValue();
			    }

			    public static void removeUnnecessaryValueOfCalls() {
			        // Keep this comment
			        Byte.valueOf("0").byteValue();
			        Byte.valueOf("0", 8).byteValue();
			        Byte.valueOf("0", 10).byteValue();
			        Boolean.valueOf("true").booleanValue();
			        Integer.valueOf("42").intValue();
			        Integer.valueOf("42", 8).intValue();
			        Integer.valueOf("42", 10).intValue();
			        Short.valueOf("42").shortValue();
			        Short.valueOf("42", 8).shortValue();
			        Short.valueOf("42", 10).shortValue();
			        Long.valueOf("42").longValue();
			        Long.valueOf("42", 8).longValue();
			        Long.valueOf("42", 10).longValue();
			        Float.valueOf("42.42").floatValue();
			        Double.valueOf("42.42").doubleValue();
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public static void convertValueOfCallsToParseCallsInPrimitiveContext() {
			        // Keep this comment
			        byte by1 = Byte.parseByte("0");
			        byte by2 = Byte.parseByte("0", 10);
			        boolean bo = Boolean.parseBoolean("true");
			        int i1 = Integer.parseInt("42");
			        int i2 = Integer.parseInt("42", 10);
			        long l1 = Long.parseLong("42");
			        long l2 = Long.parseLong("42", 10);
			        short s1 = Short.parseShort("42");
			        short s2 = Short.parseShort("42", 10);
			        float f = Float.parseFloat("42.42");
			        double d = Double.parseDouble("42.42");
			    }

			    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {
			        // Keep this comment
			        char c = '&';
			        byte by = (byte) 0;
			        boolean bo = true;
			        int i = 42;
			        long l = 42;
			        short s = (short) 42;
			        float f = 42.42F;
			        double d = 42.42;
			    }

			    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {
			        // Keep this comment
			        char c;
			        c = '&';
			        byte by;
			        by = (byte) 0;
			        boolean bo1;
			        bo1 = true;
			        int i;
			        i = 42;
			        long l;
			        l = 42;
			        short s;
			        s = (short) 42;
			        float f;
			        f = 42.42F;
			        double d;
			        d = 42.42;
			    }

			    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {
			        // Keep this comment
			        return '&';
			    }

			    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {
			        // Keep this comment
			        return (byte) 0;
			    }

			    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {
			        // Keep this comment
			        return true;
			    }

			    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {
			        // Keep this comment
			        return 42;
			    }

			    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {
			        // Keep this comment
			        return 42;
			    }

			    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {
			        // Keep this comment
			        return (short) 42;
			    }

			    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {
			        // Keep this comment
			        return 42.42F;
			    }

			    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {
			        // Keep this comment
			        return 42.42;
			    }

			    public static void removeUnnecessaryObjectCreation() {
			        // Keep this comment
			        Byte.parseByte("0");
			        Boolean.parseBoolean("true");
			        Integer.parseInt("42");
			        Short.parseShort("42");
			        Long.parseLong("42");
			        Float.parseFloat("42.42");
			        Double.parseDouble("42.42");
			    }

			    public static void removeUnnecessaryValueOfCalls() {
			        // Keep this comment
			        Byte.parseByte("0");
			        Byte.parseByte("0", 8);
			        Byte.parseByte("0", 10);
			        Boolean.parseBoolean("true");
			        Integer.parseInt("42");
			        Integer.parseInt("42", 8);
			        Integer.parseInt("42", 10);
			        Short.parseShort("42");
			        Short.parseShort("42", 8);
			        Short.parseShort("42", 10);
			        Long.parseLong("42");
			        Long.parseLong("42", 8);
			        Long.parseLong("42", 10);
			        Float.parseFloat("42.42");
			        Double.parseDouble("42.42");
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_PARSING);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveParsingCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveParsing() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public static void doNotConvertToPrimitiveWithObjectUse() {
			        Byte by1 = Byte.valueOf("0");
			        Byte by2 = Byte.valueOf("0", 10);
			        Boolean bo = Boolean.valueOf("true");
			        Integer i1 = Integer.valueOf("42");
			        Integer i2 = Integer.valueOf("42", 10);
			        Long l1 = Long.valueOf("42");
			        Long l2 = Long.valueOf("42", 10);
			        Short s1 = Short.valueOf("42");
			        Short s2 = Short.valueOf("42", 10);
			        Float f = Float.valueOf("42.42");
			        Double d = Double.valueOf("42.42");
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_PARSING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveSerialization() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    public String simplifyIntegerSerialization(int number) {
			        // Keep this comment
			        return Integer.valueOf(number).toString();
			    }

			    public String simplifyDoubleSerialization(double number) {
			        // Keep this comment
			        return Double.valueOf(number).toString();
			    }

			    public String simplifyFloatSerialization(float number) {
			        // Keep this comment
			        return Float.valueOf(number).toString();
			    }

			    public String simplifyShortSerialization(short number) {
			        // Keep this comment
			        return Short.valueOf(number).toString();
			    }

			    public String simplifyLongSerialization(long number) {
			        // Keep this comment
			        return Long.valueOf(number).toString();
			    }

			    public String simplifyCharacterSerialization(char number) {
			        // Keep this comment
			        return Character.valueOf(number).toString();
			    }

			    public String simplifyByteSerialization(byte number) {
			        // Keep this comment
			        return Byte.valueOf(number).toString();
			    }

			    public String simplifyBooleanSerialization(boolean number) {
			        // Keep this comment
			        return Boolean.valueOf(number).toString();
			    }

			    public String refactorIntegerInstantiation(int number) {
			        // Keep this comment
			        return new Integer(number).toString();
			    }

			    public String refactorIntegerCast(int number) {
			        // Keep this comment
			        return ((Integer) number).toString();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.PRIMITIVE_SERIALIZATION);

		String output= """
			package test1;

			public class E {
			    public String simplifyIntegerSerialization(int number) {
			        // Keep this comment
			        return Integer.toString(number);
			    }

			    public String simplifyDoubleSerialization(double number) {
			        // Keep this comment
			        return Double.toString(number);
			    }

			    public String simplifyFloatSerialization(float number) {
			        // Keep this comment
			        return Float.toString(number);
			    }

			    public String simplifyShortSerialization(short number) {
			        // Keep this comment
			        return Short.toString(number);
			    }

			    public String simplifyLongSerialization(long number) {
			        // Keep this comment
			        return Long.toString(number);
			    }

			    public String simplifyCharacterSerialization(char number) {
			        // Keep this comment
			        return Character.toString(number);
			    }

			    public String simplifyByteSerialization(byte number) {
			        // Keep this comment
			        return Byte.toString(number);
			    }

			    public String simplifyBooleanSerialization(boolean number) {
			        // Keep this comment
			        return Boolean.toString(number);
			    }

			    public String refactorIntegerInstantiation(int number) {
			        // Keep this comment
			        return Integer.toString(number);
			    }

			    public String refactorIntegerCast(int number) {
			        // Keep this comment
			        return Integer.toString(number);
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveSerializationCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveSerialization() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public String doNotRefactorWrapper(Integer number) {
			        return Integer.valueOf(number).toString();
			    }

			    public String doNotRefactorString(String number) {
			        return Integer.valueOf(number).toString();
			    }

			    public String doNotRefactorBadMethod(int number) {
			        return Integer.valueOf(number).toBinaryString(0);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_SERIALIZATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveBooleanRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public boolean booleanField;
			    public Boolean wrapperField;

			    public void replaceWrapper(boolean b) {
			        // Keep this comment
			        /* c1 */ Boolean /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ true /* c5 */;
			        if (alwaysInitializedVar && b) {
			            System.out.println("True!");
			        }
			    }
			    public void replaceWrapperAndUseParsing(boolean b) {
			        // Keep this comment
			        Boolean alwaysInitializedVar = Boolean.valueOf("true");
			        if (alwaysInitializedVar && b) {
			            System.out.println("True!");
			        }
			    }
			    public String replaceWrapperAndToStringMethod(boolean b) {
			        // Keep this comment
			        Boolean alwaysInitializedVar = new Boolean(true);
			        if (alwaysInitializedVar) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }
			    public int replaceWrapperAndCompareToMethod(boolean b) {
			        // Keep this comment
			        Boolean alwaysInitializedVar = new Boolean("true");
			        if (alwaysInitializedVar) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(b);
			    }
			    public boolean replaceWrapperAndPrimitiveValueMethod() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = true;
			        if (alwaysInitializedVar) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.booleanValue();
			    }

			    public void replaceFullyQualifiedWrapper(boolean b) {
			        // Keep this comment
			        java.lang.Boolean alwaysInitializedVar = false;
			        if (alwaysInitializedVar && b) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInCast() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = false;
			        if ((boolean) alwaysInitializedVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInParenthesis() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = false;
			        if ((alwaysInitializedVar)) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceGreaterWrapper(int i) {
			        // Keep this comment
			        Boolean greaterVar = i > 0;
			        if (greaterVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceLesserWrapper(int i) {
			        // Keep this comment
			        Boolean lesserVar = i < 0;
			        if (lesserVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceAndWrapper(boolean b1, boolean b2) {
			        // Keep this comment
			        Boolean andVar = b1 && b2;
			        if (andVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceOrWrapper(boolean b1, boolean b2) {
			        // Keep this comment
			        Boolean orVar = b1 || b2;
			        if (orVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceOppositeWrapper(boolean b) {
			        // Keep this comment
			        Boolean oppositeVar = !b;
			        if (oppositeVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperFromValueOf(boolean b1) {
			        // Keep this comment
			        Boolean varFromValueOf = Boolean.valueOf(b1);
			        if (varFromValueOf) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceParentherizedWrapper(boolean b1, boolean b2) {
			        // Keep this comment
			        Boolean parentherizedVar = (b1 || b2);
			        if (parentherizedVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceComplexExprWrapper(boolean b1, boolean b2, boolean b3, boolean b4) {
			        // Keep this comment
			        Boolean complexVar = b1 ? !b2 : (b3 || b4);
			        if (complexVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceCastWrapper(Boolean b) {
			        // Keep this comment
			        Boolean castVar = (boolean) b;
			        if (castVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInPrefixExpression() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = true;
			        if (!alwaysInitializedVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInIf() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = true;
			        if (alwaysInitializedVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInWhile() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = true;
			        while (alwaysInitializedVar) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperInDoWhile() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = false;
			        do {
			            System.out.println("True!");
			        } while (alwaysInitializedVar);
			    }

			    public String replaceWrapperInConditionalExpression() {
			        // Keep this comment
			        Boolean alwaysInitializedVar = true;
			        return alwaysInitializedVar ? "foo" : "bar";
			    }

			    public boolean replaceReturnedWrapper() {
			        // Keep this comment
			        Boolean returnedBoolean = true;
			        return returnedBoolean;
			    }

			    public boolean replaceMultiReturnedWrapper(int i) {
			        // Keep this comment
			        Boolean returnedBoolean = true;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedBoolean;
			        } else {
			            System.out.println("Negative");
			            return returnedBoolean;
			        }
			    }

			    public Boolean replaceReturnedAutoBoxedWrapper(int i) {
			        // Keep this comment
			        Boolean returnedBoolean = false;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedBoolean;
			        } else {
			            System.out.println("Negative");
			            return returnedBoolean;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Boolean reassignedBoolean = true;
			        reassignedBoolean = false;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Boolean multiReassignedBoolean = true;
			        multiReassignedBoolean = false;
			        multiReassignedBoolean = true;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Boolean assignedBoolean = true;
			        Boolean anotherBoolean = assignedBoolean;
			    }

			    public void replaceWrapperAssignedOnBooleanField() {
			        // Keep this comment
			        Boolean assignedBoolean = true;
			        booleanField = assignedBoolean;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Boolean assignedBoolean = true;
			        wrapperField = assignedBoolean;
			    }

			    public void replaceBitAssignedWrapper(Boolean aBoolean, Boolean anotherBoolean,
			            Boolean yetAnotherBoolean) {
			        // Keep this comment
			        Boolean assignedBoolean = true;
			        aBoolean &= assignedBoolean;
			        anotherBoolean |= assignedBoolean;
			        yetAnotherBoolean ^= assignedBoolean;
			    }
			}
			""";

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public boolean booleanField;\n" //
				+ "    public Boolean wrapperField;\n" //
				+ "\n" //
				+ "    public void replaceWrapper(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        /* c1 */ boolean /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ true /* c5 */;\n" //
				+ "        if (alwaysInitializedVar && b) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void replaceWrapperAndUseParsing(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = Boolean.parseBoolean(\"true\");\n" //
				+ "        if (alwaysInitializedVar && b) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public String replaceWrapperAndToStringMethod(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        if (alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return Boolean.toString(alwaysInitializedVar);\n" //
				+ "    }\n" //
				+ "    public int replaceWrapperAndCompareToMethod(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = Boolean.parseBoolean(\"true\");\n" //'				+ "        if (alwaysInitializedVar) {\n" //
				+ "        if (alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return Boolean.compare(alwaysInitializedVar, b);\n" //
				+ "    }\n" //
				+ "    public boolean replaceWrapperAndPrimitiveValueMethod() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        if (alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return alwaysInitializedVar;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceFullyQualifiedWrapper(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = false;\n" //
				+ "        if (alwaysInitializedVar && b) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInCast() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = false;\n" //
				+ "        if ((boolean) alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInParenthesis() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = false;\n" //
				+ "        if ((alwaysInitializedVar)) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceGreaterWrapper(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean greaterVar = i > 0;\n" //
				+ "        if (greaterVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceLesserWrapper(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean lesserVar = i < 0;\n" //
				+ "        if (lesserVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceAndWrapper(boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean andVar = b1 && b2;\n" //
				+ "        if (andVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOrWrapper(boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean orVar = b1 || b2;\n" //
				+ "        if (orVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOppositeWrapper(boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean oppositeVar = !b;\n" //
				+ "        if (oppositeVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperFromValueOf(boolean b1) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean varFromValueOf = b1;\n" //
				+ "        if (varFromValueOf) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceParentherizedWrapper(boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean parentherizedVar = (b1 || b2);\n" //
				+ "        if (parentherizedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceComplexExprWrapper(boolean b1, boolean b2, boolean b3, boolean b4) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean complexVar = b1 ? !b2 : (b3 || b4);\n" //
				+ "        if (complexVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceCastWrapper(Boolean b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean castVar = (boolean) b;\n" //
				+ "        if (castVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInPrefixExpression() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        if (!alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInIf() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        if (alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInWhile() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        while (alwaysInitializedVar) {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperInDoWhile() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = false;\n" //
				+ "        do {\n" //
				+ "            System.out.println(\"True!\");\n" //
				+ "        } while (alwaysInitializedVar);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String replaceWrapperInConditionalExpression() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean alwaysInitializedVar = true;\n" //
				+ "        return alwaysInitializedVar ? \"foo\" : \"bar\";\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean replaceReturnedWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean returnedBoolean = true;\n" //
				+ "        return returnedBoolean;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean replaceMultiReturnedWrapper(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean returnedBoolean = true;\n" //
				+ "        if (i > 0) {\n" //
				+ "            System.out.println(\"Positive\");\n" //
				+ "            return returnedBoolean;\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Negative\");\n" //
				+ "            return returnedBoolean;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Boolean replaceReturnedAutoBoxedWrapper(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean returnedBoolean = false;\n" //
				+ "        if (i > 0) {\n" //
				+ "            System.out.println(\"Positive\");\n" //
				+ "            return returnedBoolean;\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Negative\");\n" //
				+ "            return returnedBoolean;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceReassignedWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean reassignedBoolean = true;\n" //
				+ "        reassignedBoolean = false;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceMultiReassignedWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean multiReassignedBoolean = true;\n" //
				+ "        multiReassignedBoolean = false;\n" //
				+ "        multiReassignedBoolean = true;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceAssignedWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean assignedBoolean = true;\n" //
				+ "        Boolean anotherBoolean = assignedBoolean;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperAssignedOnBooleanField() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean assignedBoolean = true;\n" //
				+ "        booleanField = assignedBoolean;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWrapperAssignedOnWrapperField() {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean assignedBoolean = true;\n" //
				+ "        wrapperField = assignedBoolean;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceBitAssignedWrapper(Boolean aBoolean, Boolean anotherBoolean,\n" //
				+ "            Boolean yetAnotherBoolean) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean assignedBoolean = true;\n" //
				+ "        aBoolean &= assignedBoolean;\n" //
				+ "        anotherBoolean |= assignedBoolean;\n" //
				+ "        yetAnotherBoolean ^= assignedBoolean;\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveBooleanRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Boolean doNotRefactorFields = true;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Boolean returnedObject = true;
			        return returnedObject;
			    }

			    public Boolean doNotUsePrimitiveWithWrappedInitializer() {
			        Boolean returnedObject = Boolean.TRUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Boolean reassignedBoolean = true;
			        reassignedBoolean = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Boolean, Observable> obsByBoolean) {
			        Boolean reassignedBoolean = true;
			        obsByBoolean.get(reassignedBoolean).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Boolean assignedBoolean = true;
			        objectField = assignedBoolean;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Boolean assignedBoolean = true;
			        Boolean anotherBoolean = assignedBoolean;
			        Boolean yetAnotherBoolean = assignedBoolean;
			    }

			    public Boolean doNotReplaceMultiAutoBoxedWrapper() {
			        Boolean assignedBoolean = true;
			        Boolean anotherBoolean = assignedBoolean;
			        return assignedBoolean;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Boolean returnedObject = true;
			        Object anotherObject = returnedObject;
			    }

			    public void doNotReplaceRessignedWrapper(Boolean b) {
			        Boolean returnedObject = true;
			        try {
			            returnedObject = b;
			        } catch (Exception e) {
			            System.out.println("Error!");
			        }
			    }

			    public Boolean doNotReplaceAssignedAndReturnedWrapper(Boolean b) {
			        Boolean returnedObject = false;
			        returnedObject = b;
			        return returnedObject;
			    }

			    public void doNotRefactorMultiDeclaration(boolean isValid) {
			        Boolean alwaysInitializedVar = true, otherVar;
			        if (alwaysInitializedVar && isValid) {
			            System.out.println("True!");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveCharRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public char charField;
			    public Character wrapperField;

			    public void replaceWrapper(char c) {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(char c) {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(char c) {
			        // Keep this comment
			        Character alwaysInitializedVar = new Character(Character.MIN_VALUE);
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(c);
			    }

			    public char replaceWrapperAndPrimitiveValueMethod(char c) {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.charValue();
			    }

			    public void replaceFullyQualifiedWrapper(char c) {
			        // Keep this comment
			        java.lang.Character alwaysInitializedVar = Character.MAX_VALUE;
			        if (alwaysInitializedVar < c) {
			            System.out.println("True!");
			        }
			    }

			    public int replacePreDecrementWrapper(char c) {
			        // Keep this comment
			        Character preDecrementVar = --c;
			        return preDecrementVar - 1;
			    }

			    public int replacePreIncrementWrapper(char c) {
			        // Keep this comment
			        Character preDecrementVar = ++c;
			        return preDecrementVar + 1;
			    }

			    public int replacePostDecrementWrapper(char c) {
			        // Keep this comment
			        Character postDecrementVar = c--;
			        return -postDecrementVar;
			    }

			    public char replacePostIncrementWrapper(char c) {
			        // Keep this comment
			        Character postIncrementVar = c++;
			        return postIncrementVar++;
			    }

			    public int replaceWrapperFromValueOf(char c1) {
			        // Keep this comment
			        Character varFromValueOf = Character.valueOf(c1);
			        return +varFromValueOf;
			    }

			    public char replaceCastWrapper(Character c) {
			        // Keep this comment
			        Character castVar = (char) c;
			        return castVar++;
			    }

			    public char replaceObjectCastWrapper() {
			        // Keep this comment
			        Character castVar = (Character) Character.MAX_HIGH_SURROGATE;
			        return castVar++;
			    }

			    public char replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MAX_LOW_SURROGATE;
			        return ++alwaysInitializedVar;
			    }

			    public char replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MAX_SURROGATE;
			        return --alwaysInitializedVar;
			    }

			    public char replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MIN_HIGH_SURROGATE;
			        return alwaysInitializedVar--;
			    }

			    public char replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Character alwaysInitializedVar = Character.MIN_LOW_SURROGATE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        Character charInSwitch = Character.MIN_SURROGATE;
			        switch (charInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        Character charInArrayAccess = Character.MIN_VALUE;
			        return strings[charInArrayAccess];
			    }

			    public char replaceReturnedWrapper() {
			        // Keep this comment
			        Character returnedCharacter = Character.MIN_VALUE;
			        return returnedCharacter;
			    }

			    public char replaceMultiReturnedWrapper(char c) {
			        // Keep this comment
			        Character returnedCharacter = Character.MIN_VALUE;
			        if (c > 0) {
			            System.out.println("Positive");
			            return returnedCharacter;
			        } else {
			            System.out.println("Negative");
			            return returnedCharacter;
			        }
			    }

			    public Character replaceReturnedAutoBoxedWrapper(char c) {
			        // Keep this comment
			        Character returnedCharacter = Character.MIN_VALUE;
			        if (c > 0) {
			            System.out.println("Positive");
			            return returnedCharacter;
			        } else {
			            System.out.println("Negative");
			            return returnedCharacter;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Character reassignedCharacter = Character.MIN_VALUE;
			        reassignedCharacter = 'a';
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Character multiReassignedCharacter = Character.MIN_VALUE;
			        multiReassignedCharacter = 'a';
			        multiReassignedCharacter = 'b';
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Character assignedCharacter = Character.MIN_VALUE;
			        Character anotherCharacter = assignedCharacter;
			    }

			    public void replaceWrapperAssignedOnCharacterField() {
			        // Keep this comment
			        Character assignedCharacter = Character.MIN_VALUE;
			        charField = assignedCharacter;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Character assignedCharacter = Character.MIN_VALUE;
			        wrapperField = assignedCharacter;
			    }

			    public void replaceBitAssignedWrapper(int anInteger, int anotherInteger,
			            int yetAnotherInteger) {
			        // Keep this comment
			        Character assignedCharacter = Character.MIN_VALUE;
			        anInteger &= assignedCharacter;
			        anotherInteger += assignedCharacter;
			        yetAnotherInteger ^= assignedCharacter;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public char charField;
			    public Character wrapperField;

			    public void replaceWrapper(char c) {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(char c) {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Character.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(char c) {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Character.compare(alwaysInitializedVar, c);
			    }

			    public char replaceWrapperAndPrimitiveValueMethod(char c) {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_VALUE;
			        if (alwaysInitializedVar > c) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(char c) {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MAX_VALUE;
			        if (alwaysInitializedVar < c) {
			            System.out.println("True!");
			        }
			    }

			    public int replacePreDecrementWrapper(char c) {
			        // Keep this comment
			        char preDecrementVar = --c;
			        return preDecrementVar - 1;
			    }

			    public int replacePreIncrementWrapper(char c) {
			        // Keep this comment
			        char preDecrementVar = ++c;
			        return preDecrementVar + 1;
			    }

			    public int replacePostDecrementWrapper(char c) {
			        // Keep this comment
			        char postDecrementVar = c--;
			        return -postDecrementVar;
			    }

			    public char replacePostIncrementWrapper(char c) {
			        // Keep this comment
			        char postIncrementVar = c++;
			        return postIncrementVar++;
			    }

			    public int replaceWrapperFromValueOf(char c1) {
			        // Keep this comment
			        char varFromValueOf = c1;
			        return +varFromValueOf;
			    }

			    public char replaceCastWrapper(Character c) {
			        // Keep this comment
			        char castVar = (char) c;
			        return castVar++;
			    }

			    public char replaceObjectCastWrapper() {
			        // Keep this comment
			        char castVar = (Character) Character.MAX_HIGH_SURROGATE;
			        return castVar++;
			    }

			    public char replaceWrapperInPreIncrement() {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MAX_LOW_SURROGATE;
			        return ++alwaysInitializedVar;
			    }

			    public char replaceWrapperInPreDecrement() {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MAX_SURROGATE;
			        return --alwaysInitializedVar;
			    }

			    public char replaceWrapperInPostDecrement() {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_HIGH_SURROGATE;
			        return alwaysInitializedVar--;
			    }

			    public char replaceWrapperInPostIncrement() {
			        // Keep this comment
			        char alwaysInitializedVar = Character.MIN_LOW_SURROGATE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        char charInSwitch = Character.MIN_SURROGATE;
			        switch (charInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        char charInArrayAccess = Character.MIN_VALUE;
			        return strings[charInArrayAccess];
			    }

			    public char replaceReturnedWrapper() {
			        // Keep this comment
			        char returnedCharacter = Character.MIN_VALUE;
			        return returnedCharacter;
			    }

			    public char replaceMultiReturnedWrapper(char c) {
			        // Keep this comment
			        char returnedCharacter = Character.MIN_VALUE;
			        if (c > 0) {
			            System.out.println("Positive");
			            return returnedCharacter;
			        } else {
			            System.out.println("Negative");
			            return returnedCharacter;
			        }
			    }

			    public Character replaceReturnedAutoBoxedWrapper(char c) {
			        // Keep this comment
			        char returnedCharacter = Character.MIN_VALUE;
			        if (c > 0) {
			            System.out.println("Positive");
			            return returnedCharacter;
			        } else {
			            System.out.println("Negative");
			            return returnedCharacter;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        char reassignedCharacter = Character.MIN_VALUE;
			        reassignedCharacter = 'a';
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        char multiReassignedCharacter = Character.MIN_VALUE;
			        multiReassignedCharacter = 'a';
			        multiReassignedCharacter = 'b';
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        char assignedCharacter = Character.MIN_VALUE;
			        Character anotherCharacter = assignedCharacter;
			    }

			    public void replaceWrapperAssignedOnCharacterField() {
			        // Keep this comment
			        char assignedCharacter = Character.MIN_VALUE;
			        charField = assignedCharacter;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        char assignedCharacter = Character.MIN_VALUE;
			        wrapperField = assignedCharacter;
			    }

			    public void replaceBitAssignedWrapper(int anInteger, int anotherInteger,
			            int yetAnotherInteger) {
			        // Keep this comment
			        char assignedCharacter = Character.MIN_VALUE;
			        anInteger &= assignedCharacter;
			        anotherInteger += assignedCharacter;
			        yetAnotherInteger ^= assignedCharacter;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveCharRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Character doNotRefactorFields = Character.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Character returnedObject = Character.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Character reassignedCharacter = Character.MIN_VALUE;
			        reassignedCharacter = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Character, Observable> obsByCharacter) {
			        Character reassignedCharacter = Character.MIN_VALUE;
			        obsByCharacter.get(reassignedCharacter).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Character assignedCharacter = Character.MIN_VALUE;
			        objectField = assignedCharacter;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Character assignedCharacter = Character.MIN_VALUE;
			        Character anotherCharacter = assignedCharacter;
			        Character yetAnotherCharacter = assignedCharacter;
			    }

			    public Character doNotReplaceMultiAutoBoxedWrapper() {
			        Character assignedCharacter = Character.MIN_VALUE;
			        Character anotherCharacter = assignedCharacter;
			        return assignedCharacter;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Character returnedObject = Character.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Character doNotReplaceAssignedAndReturnedWrapper(Character c) {
			        Character returnedObject = Character.MIN_VALUE;
			        returnedObject = c;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveByteRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public byte byteField;
			    public Byte wrapperField;

			    public void replaceWrapper(byte b) {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(byte b) {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.valueOf("0");
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(byte b) {
			        // Keep this comment
			        Byte alwaysInitializedVar = new Byte(Byte.MIN_VALUE);
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(byte b) {
			        // Keep this comment
			        Byte alwaysInitializedVar = new Byte("0");
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(b);
			    }

			    public byte replaceWrapperAndPrimitiveValueMethod(byte b) {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.byteValue();
			    }

			    public void replaceFullyQualifiedWrapper(byte b) {
			        // Keep this comment
			        java.lang.Byte alwaysInitializedVar = Byte.MAX_VALUE;
			        if (alwaysInitializedVar < b) {
			            System.out.println("True!");
			        }
			    }

			    public int replacePreDecrementWrapper(byte b) {
			        // Keep this comment
			        Byte preDecrementVar = --b;
			        return preDecrementVar - 1;
			    }

			    public int replacePreIncrementWrapper(byte b) {
			        // Keep this comment
			        Byte preDecrementVar = ++b;
			        return preDecrementVar + 1;
			    }

			    public int replacePostDecrementWrapper(byte b) {
			        // Keep this comment
			        Byte postDecrementVar = b--;
			        return +postDecrementVar;
			    }

			    public int replacePostIncrementWrapper(byte b) {
			        // Keep this comment
			        Byte postIncrementVar = b++;
			        return -postIncrementVar;
			    }

			    public byte replaceWrapperFromValueOf(byte b1) {
			        // Keep this comment
			        Byte varFromValueOf = Byte.valueOf(b1);
			        return varFromValueOf++;
			    }

			    public byte replaceParentherizedWrapper(byte b1) {
			        // Keep this comment
			        Byte parentherizedVar = (Byte.MIN_VALUE);
			        return parentherizedVar++;
			    }

			    public byte replaceCastWrapper(Byte b) {
			        // Keep this comment
			        Byte castVar = (byte) b;
			        return castVar++;
			    }

			    public byte replaceObjectCastWrapper() {
			        // Keep this comment
			        Byte castVar = (Byte) Byte.MIN_VALUE;
			        return castVar++;
			    }

			    public byte replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return ++alwaysInitializedVar;
			    }

			    public byte replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return --alwaysInitializedVar;
			    }

			    public byte replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return alwaysInitializedVar--;
			    }

			    public byte replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        Byte byteInSwitch = Byte.MIN_VALUE;
			        switch (byteInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        Byte byteInArrayAccess = Byte.MIN_VALUE;
			        return strings[byteInArrayAccess];
			    }

			    public byte replaceReturnedWrapper() {
			        // Keep this comment
			        Byte returnedByte = Byte.MIN_VALUE;
			        return returnedByte;
			    }

			    public byte replaceMultiReturnedWrapper(byte b) {
			        // Keep this comment
			        Byte returnedByte = Byte.MIN_VALUE;
			        if (b > 0) {
			            System.out.println("Positive");
			            return returnedByte;
			        } else {
			            System.out.println("Negative");
			            return returnedByte;
			        }
			    }

			    public Byte replaceReturnedAutoBoxedWrapper(byte b) {
			        // Keep this comment
			        Byte returnedByte = Byte.MIN_VALUE;
			        if (b > 0) {
			            System.out.println("Positive");
			            return returnedByte;
			        } else {
			            System.out.println("Negative");
			            return returnedByte;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Byte reassignedByte = Byte.MIN_VALUE;
			        reassignedByte = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Byte multiReassignedByte = Byte.MIN_VALUE;
			        multiReassignedByte = 1;
			        multiReassignedByte = 2;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Byte assignedByte = Byte.MIN_VALUE;
			        Byte anotherByte = assignedByte;
			    }

			    public void replaceWrapperAssignedOnByteField() {
			        // Keep this comment
			        Byte assignedByte = Byte.MIN_VALUE;
			        byteField = assignedByte;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Byte assignedByte = Byte.MIN_VALUE;
			        wrapperField = assignedByte;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public byte byteField;
			    public Byte wrapperField;

			    public void replaceWrapper(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.parseByte("0");
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Byte.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.parseByte("0");
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Byte.compare(alwaysInitializedVar, b);
			    }

			    public byte replaceWrapperAndPrimitiveValueMethod(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        if (alwaysInitializedVar > b) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(byte b) {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MAX_VALUE;
			        if (alwaysInitializedVar < b) {
			            System.out.println("True!");
			        }
			    }

			    public int replacePreDecrementWrapper(byte b) {
			        // Keep this comment
			        byte preDecrementVar = --b;
			        return preDecrementVar - 1;
			    }

			    public int replacePreIncrementWrapper(byte b) {
			        // Keep this comment
			        byte preDecrementVar = ++b;
			        return preDecrementVar + 1;
			    }

			    public int replacePostDecrementWrapper(byte b) {
			        // Keep this comment
			        byte postDecrementVar = b--;
			        return +postDecrementVar;
			    }

			    public int replacePostIncrementWrapper(byte b) {
			        // Keep this comment
			        byte postIncrementVar = b++;
			        return -postIncrementVar;
			    }

			    public byte replaceWrapperFromValueOf(byte b1) {
			        // Keep this comment
			        byte varFromValueOf = b1;
			        return varFromValueOf++;
			    }

			    public byte replaceParentherizedWrapper(byte b1) {
			        // Keep this comment
			        byte parentherizedVar = (Byte.MIN_VALUE);
			        return parentherizedVar++;
			    }

			    public byte replaceCastWrapper(Byte b) {
			        // Keep this comment
			        byte castVar = (byte) b;
			        return castVar++;
			    }

			    public byte replaceObjectCastWrapper() {
			        // Keep this comment
			        byte castVar = (Byte) Byte.MIN_VALUE;
			        return castVar++;
			    }

			    public byte replaceWrapperInPreIncrement() {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return ++alwaysInitializedVar;
			    }

			    public byte replaceWrapperInPreDecrement() {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return --alwaysInitializedVar;
			    }

			    public byte replaceWrapperInPostDecrement() {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return alwaysInitializedVar--;
			    }

			    public byte replaceWrapperInPostIncrement() {
			        // Keep this comment
			        byte alwaysInitializedVar = Byte.MIN_VALUE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        byte byteInSwitch = Byte.MIN_VALUE;
			        switch (byteInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        byte byteInArrayAccess = Byte.MIN_VALUE;
			        return strings[byteInArrayAccess];
			    }

			    public byte replaceReturnedWrapper() {
			        // Keep this comment
			        byte returnedByte = Byte.MIN_VALUE;
			        return returnedByte;
			    }

			    public byte replaceMultiReturnedWrapper(byte b) {
			        // Keep this comment
			        byte returnedByte = Byte.MIN_VALUE;
			        if (b > 0) {
			            System.out.println("Positive");
			            return returnedByte;
			        } else {
			            System.out.println("Negative");
			            return returnedByte;
			        }
			    }

			    public Byte replaceReturnedAutoBoxedWrapper(byte b) {
			        // Keep this comment
			        byte returnedByte = Byte.MIN_VALUE;
			        if (b > 0) {
			            System.out.println("Positive");
			            return returnedByte;
			        } else {
			            System.out.println("Negative");
			            return returnedByte;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        byte reassignedByte = Byte.MIN_VALUE;
			        reassignedByte = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        byte multiReassignedByte = Byte.MIN_VALUE;
			        multiReassignedByte = 1;
			        multiReassignedByte = 2;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        byte assignedByte = Byte.MIN_VALUE;
			        Byte anotherByte = assignedByte;
			    }

			    public void replaceWrapperAssignedOnByteField() {
			        // Keep this comment
			        byte assignedByte = Byte.MIN_VALUE;
			        byteField = assignedByte;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        byte assignedByte = Byte.MIN_VALUE;
			        wrapperField = assignedByte;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveByteRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Byte doNotRefactorFields = Byte.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Byte returnedObject = Byte.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Byte reassignedByte = Byte.MIN_VALUE;
			        reassignedByte = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Byte, Observable> obsByByte) {
			        Byte reassignedByte = Byte.MIN_VALUE;
			        obsByByte.get(reassignedByte).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Byte assignedByte = Byte.MIN_VALUE;
			        objectField = assignedByte;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Byte assignedByte = Byte.MIN_VALUE;
			        Byte anotherByte = assignedByte;
			        Byte yetAnotherByte = assignedByte;
			    }

			    public Byte doNotReplaceMultiAutoBoxedWrapper() {
			        Byte assignedByte = Byte.MIN_VALUE;
			        Byte anotherByte = assignedByte;
			        return assignedByte;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Byte returnedObject = Byte.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Byte doNotReplaceAssignedAndReturnedWrapper(Byte b) {
			        Byte returnedObject = Byte.MIN_VALUE;
			        returnedObject = b;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveShortRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public short shortField;
			    public Short wrapperField;

			    public void replaceWrapper(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = Short.valueOf("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = new Short("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = new Short(Short.MIN_VALUE);
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = new Short("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(s);
			    }

			    public short replaceWrapperAndPrimitiveValueMethod(short s) {
			        // Keep this comment
			        Short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.shortValue();
			    }

			    public void replaceFullyQualifiedWrapper(short s) {
			        // Keep this comment
			        java.lang.Short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar < s) {
			            System.out.println("True!");
			        }
			    }

			    public short replacePreDecrementWrapper(short s) {
			        // Keep this comment
			        Short preDecrementVar = --s;
			        if (preDecrementVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public short replacePreIncrementWrapper(short s) {
			        // Keep this comment
			        Short preDecrementVar = ++s;
			        return preDecrementVar++;
			    }

			    public short replacePostDecrementWrapper(short s) {
			        // Keep this comment
			        Short postDecrementVar = s--;
			        return postDecrementVar++;
			    }

			    public short replacePostIncrementWrapper(short s) {
			        // Keep this comment
			        Short postIncrementVar = s++;
			        return postIncrementVar++;
			    }

			    public short replaceWrapperFromValueOf(short s1) {
			        // Keep this comment
			        Short varFromValueOf = Short.valueOf(s1);
			        return varFromValueOf++;
			    }

			    public short replaceParentherizedWrapper(short s1, short s2) {
			        // Keep this comment
			        Short parentherizedVar = ((short)(s1 + s2));
			        return parentherizedVar++;
			    }

			    public short replaceCastWrapper(Short s) {
			        // Keep this comment
			        Short castVar = (short) s;
			        return castVar++;
			    }

			    public short replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Short shortInPreIncrement = Short.MIN_VALUE;
			        return ++shortInPreIncrement;
			    }

			    public short replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Short shortInPreDecrement = Short.MIN_VALUE;
			        return --shortInPreDecrement;
			    }

			    public short replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Short shortInPostDecrement = Short.MIN_VALUE;
			        return shortInPostDecrement--;
			    }

			    public short replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Short shortInPostIncrement = Short.MIN_VALUE;
			        return shortInPostIncrement++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        Short shortInSwitch = Short.MIN_VALUE;
			        switch (shortInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        Short shortInArrayAccess = Short.MAX_VALUE;
			        return strings[shortInArrayAccess];
			    }

			    public short replaceReturnedWrapper() {
			        // Keep this comment
			        Short returnedShort = Short.MIN_VALUE;
			        return returnedShort;
			    }

			    public short replaceMultiReturnedWrapper(short s) {
			        // Keep this comment
			        Short returnedShort = Short.MIN_VALUE;
			        if (s > 0) {
			            System.out.println("Positive");
			            return returnedShort;
			        } else {
			            System.out.println("Negative");
			            return returnedShort;
			        }
			    }

			    public Short replaceReturnedAutoBoxedWrapper(short s) {
			        // Keep this comment
			        Short returnedShort = Short.MIN_VALUE;
			        if (s > 0) {
			            System.out.println("Positive");
			            return returnedShort;
			        } else {
			            System.out.println("Negative");
			            return returnedShort;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Short reassignedShort = Short.MIN_VALUE;
			        reassignedShort = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Short multiReassignedShort = Short.MIN_VALUE;
			        multiReassignedShort = 123;
			        multiReassignedShort = 456;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Short assignedShort = Short.MIN_VALUE;
			        Short anotherShort = assignedShort;
			    }

			    public void replaceWrapperAssignedOnShortField() {
			        // Keep this comment
			        Short assignedShort = Short.MIN_VALUE;
			        shortField = assignedShort;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Short assignedShort = Short.MIN_VALUE;
			        wrapperField = assignedShort;
			    }

			    public void replaceBitAssignedWrapper(Integer anInteger, Integer anotherInteger,
			            Integer yetAnotherInteger) {
			        // Keep this comment
			        Short assignedShort = Short.MIN_VALUE;
			        anInteger |= assignedShort;
			        anotherInteger += assignedShort;
			        yetAnotherInteger ^= assignedShort;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public short shortField;
			    public Short wrapperField;

			    public void replaceWrapper(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.parseShort("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.parseShort("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Short.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.parseShort("0");
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Short.compare(alwaysInitializedVar, s);
			    }

			    public short replaceWrapperAndPrimitiveValueMethod(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar > s) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(short s) {
			        // Keep this comment
			        short alwaysInitializedVar = Short.MIN_VALUE;
			        if (alwaysInitializedVar < s) {
			            System.out.println("True!");
			        }
			    }

			    public short replacePreDecrementWrapper(short s) {
			        // Keep this comment
			        short preDecrementVar = --s;
			        if (preDecrementVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public short replacePreIncrementWrapper(short s) {
			        // Keep this comment
			        short preDecrementVar = ++s;
			        return preDecrementVar++;
			    }

			    public short replacePostDecrementWrapper(short s) {
			        // Keep this comment
			        short postDecrementVar = s--;
			        return postDecrementVar++;
			    }

			    public short replacePostIncrementWrapper(short s) {
			        // Keep this comment
			        short postIncrementVar = s++;
			        return postIncrementVar++;
			    }

			    public short replaceWrapperFromValueOf(short s1) {
			        // Keep this comment
			        short varFromValueOf = s1;
			        return varFromValueOf++;
			    }

			    public short replaceParentherizedWrapper(short s1, short s2) {
			        // Keep this comment
			        short parentherizedVar = ((short)(s1 + s2));
			        return parentherizedVar++;
			    }

			    public short replaceCastWrapper(Short s) {
			        // Keep this comment
			        short castVar = (short) s;
			        return castVar++;
			    }

			    public short replaceWrapperInPreIncrement() {
			        // Keep this comment
			        short shortInPreIncrement = Short.MIN_VALUE;
			        return ++shortInPreIncrement;
			    }

			    public short replaceWrapperInPreDecrement() {
			        // Keep this comment
			        short shortInPreDecrement = Short.MIN_VALUE;
			        return --shortInPreDecrement;
			    }

			    public short replaceWrapperInPostDecrement() {
			        // Keep this comment
			        short shortInPostDecrement = Short.MIN_VALUE;
			        return shortInPostDecrement--;
			    }

			    public short replaceWrapperInPostIncrement() {
			        // Keep this comment
			        short shortInPostIncrement = Short.MIN_VALUE;
			        return shortInPostIncrement++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        short shortInSwitch = Short.MIN_VALUE;
			        switch (shortInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        short shortInArrayAccess = Short.MAX_VALUE;
			        return strings[shortInArrayAccess];
			    }

			    public short replaceReturnedWrapper() {
			        // Keep this comment
			        short returnedShort = Short.MIN_VALUE;
			        return returnedShort;
			    }

			    public short replaceMultiReturnedWrapper(short s) {
			        // Keep this comment
			        short returnedShort = Short.MIN_VALUE;
			        if (s > 0) {
			            System.out.println("Positive");
			            return returnedShort;
			        } else {
			            System.out.println("Negative");
			            return returnedShort;
			        }
			    }

			    public Short replaceReturnedAutoBoxedWrapper(short s) {
			        // Keep this comment
			        short returnedShort = Short.MIN_VALUE;
			        if (s > 0) {
			            System.out.println("Positive");
			            return returnedShort;
			        } else {
			            System.out.println("Negative");
			            return returnedShort;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        short reassignedShort = Short.MIN_VALUE;
			        reassignedShort = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        short multiReassignedShort = Short.MIN_VALUE;
			        multiReassignedShort = 123;
			        multiReassignedShort = 456;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        short assignedShort = Short.MIN_VALUE;
			        Short anotherShort = assignedShort;
			    }

			    public void replaceWrapperAssignedOnShortField() {
			        // Keep this comment
			        short assignedShort = Short.MIN_VALUE;
			        shortField = assignedShort;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        short assignedShort = Short.MIN_VALUE;
			        wrapperField = assignedShort;
			    }

			    public void replaceBitAssignedWrapper(Integer anInteger, Integer anotherInteger,
			            Integer yetAnotherInteger) {
			        // Keep this comment
			        short assignedShort = Short.MIN_VALUE;
			        anInteger |= assignedShort;
			        anotherInteger += assignedShort;
			        yetAnotherInteger ^= assignedShort;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveShortRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Short doNotRefactorFields = Short.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Short returnedObject = Short.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Short reassignedShort = Short.MIN_VALUE;
			        reassignedShort = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Short, Observable> obsByShort) {
			        Short reassignedShort = Short.MIN_VALUE;
			        obsByShort.get(reassignedShort).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Short assignedShort = Short.MIN_VALUE;
			        objectField = assignedShort;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Short assignedShort = Short.MIN_VALUE;
			        Short anotherShort = assignedShort;
			        Short yetAnotherShort = assignedShort;
			    }

			    public Short doNotReplaceMultiAutoBoxedWrapper() {
			        Short assignedShort = Short.MIN_VALUE;
			        Short anotherShort = assignedShort;
			        return assignedShort;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Short returnedObject = Short.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Short doNotReplaceAssignedAndReturnedWrapper(Short s) {
			        Short returnedObject = Short.MIN_VALUE;
			        returnedObject = s;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveIntRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public int intField;
			    public Integer wrapperField;

			    public void replaceWrapper(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.MIN_VALUE;
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.valueOf("0");
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsingWithRadix(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.valueOf("0", 10);
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.MIN_VALUE;
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = new Integer("0");
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(i);
			    }

			    public int replaceWrapperAndPrimitiveValueMethod(int i) {
			        // Keep this comment
			        Integer alwaysInitializedVar = new Integer(Integer.MIN_VALUE);
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.intValue();
			    }

			    public void replaceFullyQualifiedWrapper(int i) {
			        // Keep this comment
			        java.lang.Integer alwaysInitializedVar = Integer.MAX_VALUE;
			        if (alwaysInitializedVar < i) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer plusVar = i1 + i2;
			        return plusVar > 0;
			    }

			    public int replaceLessWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer lessVar = i1 - i2;
			        return -lessVar;
			    }

			    public int replaceTimesWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer timesVar = i1 * i2;
			        return timesVar + 100;
			    }

			    public int replaceDivideWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer divideVar = i1 / i2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public int replaceAndMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer divideVar = i1 & i2;
			        return divideVar++;
			    }

			    public int replaceOrMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer divideVar = i1 | i2;
			        return divideVar++;
			    }

			    public int replaceShiftMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer divideVar = i1 ^ i2;
			        return divideVar++;
			    }

			    public int replaceMinusWrapper(int i) {
			        // Keep this comment
			        Integer minusVar = -i;
			        return minusVar++;
			    }

			    public int replacePreDecrementWrapper(int i) {
			        // Keep this comment
			        Integer preDecrementVar = --i;
			        return preDecrementVar++;
			    }

			    public int replacePreIncrementWrapper(int i) {
			        // Keep this comment
			        Integer preDecrementVar = ++i;
			        return preDecrementVar++;
			    }

			    public int replacePostDecrementWrapper(int i) {
			        // Keep this comment
			        Integer postDecrementVar = i--;
			        return postDecrementVar++;
			    }

			    public int replacePostIncrementWrapper(int i) {
			        // Keep this comment
			        Integer postIncrementVar = i++;
			        return postIncrementVar++;
			    }

			    public int replaceWrapperFromValueOf(int i1) {
			        // Keep this comment
			        Integer varFromValueOf = Integer.valueOf(i1);
			        return varFromValueOf++;
			    }

			    public int replaceParentherizedWrapper(int i1, int i2) {
			        // Keep this comment
			        Integer parentherizedVar = (i1 + i2);
			        return parentherizedVar++;
			    }

			    public int replaceComplexExprWrapper(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        Integer complexVar = i1 + i2 / (i3 - i4);
			        return complexVar++;
			    }

			    public int replaceCastWrapper(Integer i) {
			        // Keep this comment
			        Integer castVar = (int) i;
			        return castVar++;
			    }

			    public int replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.SIZE;
			        return ++alwaysInitializedVar;
			    }

			    public int replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.MIN_VALUE;
			        return --alwaysInitializedVar;
			    }

			    public int replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.MIN_VALUE;
			        return alwaysInitializedVar--;
			    }

			    public int replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Integer alwaysInitializedVar = Integer.MIN_VALUE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        Integer intInSwitch = Integer.MIN_VALUE;
			        switch (intInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        Integer intInArrayAccess = Integer.MIN_VALUE;
			        return strings[intInArrayAccess];
			    }

			    public int replaceReturnedWrapper() {
			        // Keep this comment
			        Integer returnedInteger = Integer.MIN_VALUE;
			        return returnedInteger;
			    }

			    public int replaceMultiReturnedWrapper(int i) {
			        // Keep this comment
			        Integer returnedInteger = Integer.MIN_VALUE;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedInteger;
			        } else {
			            System.out.println("Negative");
			            return returnedInteger;
			        }
			    }

			    public Integer replaceReturnedAutoBoxedWrapper(int i) {
			        // Keep this comment
			        Integer returnedInteger = Integer.MIN_VALUE;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedInteger;
			        } else {
			            System.out.println("Negative");
			            return returnedInteger;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Integer reassignedInteger = Integer.MIN_VALUE;
			        reassignedInteger = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Integer multiReassignedInteger = Integer.MIN_VALUE;
			        multiReassignedInteger = 123;
			        multiReassignedInteger = 456;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Integer assignedInteger = Integer.MIN_VALUE;
			        Integer anotherInteger = assignedInteger;
			    }

			    public void replaceWrapperAssignedOnIntegerField() {
			        // Keep this comment
			        Integer assignedInteger = Integer.MIN_VALUE;
			        intField = assignedInteger;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Integer assignedInteger = Integer.MIN_VALUE;
			        wrapperField = assignedInteger;
			    }

			    public void replaceBitAssignedWrapper(Integer aInteger, Integer anotherInteger,
			            Integer yetAnotherInteger) {
			        // Keep this comment
			        Integer assignedInteger = Integer.MIN_VALUE;
			        aInteger &= assignedInteger;
			        anotherInteger += assignedInteger;
			        yetAnotherInteger ^= assignedInteger;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public int intField;
			    public Integer wrapperField;

			    public void replaceWrapper(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.parseInt("0");
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsingWithRadix(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.parseInt("0", 10);
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Integer.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.parseInt("0");
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Integer.compare(alwaysInitializedVar, i);
			    }

			    public int replaceWrapperAndPrimitiveValueMethod(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        if (alwaysInitializedVar > i) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(int i) {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MAX_VALUE;
			        if (alwaysInitializedVar < i) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(int i1, int i2) {
			        // Keep this comment
			        int plusVar = i1 + i2;
			        return plusVar > 0;
			    }

			    public int replaceLessWrapper(int i1, int i2) {
			        // Keep this comment
			        int lessVar = i1 - i2;
			        return -lessVar;
			    }

			    public int replaceTimesWrapper(int i1, int i2) {
			        // Keep this comment
			        int timesVar = i1 * i2;
			        return timesVar + 100;
			    }

			    public int replaceDivideWrapper(int i1, int i2) {
			        // Keep this comment
			        int divideVar = i1 / i2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public int replaceAndMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        int divideVar = i1 & i2;
			        return divideVar++;
			    }

			    public int replaceOrMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        int divideVar = i1 | i2;
			        return divideVar++;
			    }

			    public int replaceShiftMaskWrapper(int i1, int i2) {
			        // Keep this comment
			        int divideVar = i1 ^ i2;
			        return divideVar++;
			    }

			    public int replaceMinusWrapper(int i) {
			        // Keep this comment
			        int minusVar = -i;
			        return minusVar++;
			    }

			    public int replacePreDecrementWrapper(int i) {
			        // Keep this comment
			        int preDecrementVar = --i;
			        return preDecrementVar++;
			    }

			    public int replacePreIncrementWrapper(int i) {
			        // Keep this comment
			        int preDecrementVar = ++i;
			        return preDecrementVar++;
			    }

			    public int replacePostDecrementWrapper(int i) {
			        // Keep this comment
			        int postDecrementVar = i--;
			        return postDecrementVar++;
			    }

			    public int replacePostIncrementWrapper(int i) {
			        // Keep this comment
			        int postIncrementVar = i++;
			        return postIncrementVar++;
			    }

			    public int replaceWrapperFromValueOf(int i1) {
			        // Keep this comment
			        int varFromValueOf = i1;
			        return varFromValueOf++;
			    }

			    public int replaceParentherizedWrapper(int i1, int i2) {
			        // Keep this comment
			        int parentherizedVar = (i1 + i2);
			        return parentherizedVar++;
			    }

			    public int replaceComplexExprWrapper(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        int complexVar = i1 + i2 / (i3 - i4);
			        return complexVar++;
			    }

			    public int replaceCastWrapper(Integer i) {
			        // Keep this comment
			        int castVar = (int) i;
			        return castVar++;
			    }

			    public int replaceWrapperInPreIncrement() {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.SIZE;
			        return ++alwaysInitializedVar;
			    }

			    public int replaceWrapperInPreDecrement() {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        return --alwaysInitializedVar;
			    }

			    public int replaceWrapperInPostDecrement() {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        return alwaysInitializedVar--;
			    }

			    public int replaceWrapperInPostIncrement() {
			        // Keep this comment
			        int alwaysInitializedVar = Integer.MIN_VALUE;
			        return alwaysInitializedVar++;
			    }

			    public void replaceWrapperInSwitch() {
			        // Keep this comment
			        int intInSwitch = Integer.MIN_VALUE;
			        switch (intInSwitch) {
			        case 1:
			            System.out.println("One");
			            break;

			        case 2:
			            System.out.println("Two");
			            break;

			        default:
			            break;
			        }
			    }

			    public String replaceWrapperInArrayAccess(String[] strings) {
			        // Keep this comment
			        int intInArrayAccess = Integer.MIN_VALUE;
			        return strings[intInArrayAccess];
			    }

			    public int replaceReturnedWrapper() {
			        // Keep this comment
			        int returnedInteger = Integer.MIN_VALUE;
			        return returnedInteger;
			    }

			    public int replaceMultiReturnedWrapper(int i) {
			        // Keep this comment
			        int returnedInteger = Integer.MIN_VALUE;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedInteger;
			        } else {
			            System.out.println("Negative");
			            return returnedInteger;
			        }
			    }

			    public Integer replaceReturnedAutoBoxedWrapper(int i) {
			        // Keep this comment
			        int returnedInteger = Integer.MIN_VALUE;
			        if (i > 0) {
			            System.out.println("Positive");
			            return returnedInteger;
			        } else {
			            System.out.println("Negative");
			            return returnedInteger;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        int reassignedInteger = Integer.MIN_VALUE;
			        reassignedInteger = 123;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        int multiReassignedInteger = Integer.MIN_VALUE;
			        multiReassignedInteger = 123;
			        multiReassignedInteger = 456;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        int assignedInteger = Integer.MIN_VALUE;
			        Integer anotherInteger = assignedInteger;
			    }

			    public void replaceWrapperAssignedOnIntegerField() {
			        // Keep this comment
			        int assignedInteger = Integer.MIN_VALUE;
			        intField = assignedInteger;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        int assignedInteger = Integer.MIN_VALUE;
			        wrapperField = assignedInteger;
			    }

			    public void replaceBitAssignedWrapper(Integer aInteger, Integer anotherInteger,
			            Integer yetAnotherInteger) {
			        // Keep this comment
			        int assignedInteger = Integer.MIN_VALUE;
			        aInteger &= assignedInteger;
			        anotherInteger += assignedInteger;
			        yetAnotherInteger ^= assignedInteger;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveIntRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Integer doNotRefactorFields = Integer.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Integer returnedObject = Integer.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Integer reassignedInteger = Integer.MIN_VALUE;
			        reassignedInteger = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Integer, Observable> obsByInteger) {
			        Integer reassignedInteger = Integer.MIN_VALUE;
			        obsByInteger.get(reassignedInteger).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Integer assignedInteger = Integer.MIN_VALUE;
			        objectField = assignedInteger;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Integer assignedInteger = Integer.MIN_VALUE;
			        Integer anotherInteger = assignedInteger;
			        Integer yetAnotherInteger = assignedInteger;
			    }

			    public Integer doNotReplaceMultiAutoBoxedWrapper() {
			        Integer assignedInteger = Integer.MIN_VALUE;
			        Integer anotherInteger = assignedInteger;
			        return assignedInteger;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Integer returnedObject = Integer.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Integer doNotReplaceAssignedAndReturnedWrapper(Integer i) {
			        Integer returnedObject = Integer.MIN_VALUE;
			        returnedObject = i;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveLongRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public long longField;
			    public Long wrapperField;

			    public void replaceWrapper(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = Long.valueOf("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsingWithRadix(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = Long.valueOf("0", 10);
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = new Long("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = new Long(Long.MIN_VALUE);
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = new Long("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(l);
			    }

			    public long replaceWrapperAndPrimitiveValueMethod(long l) {
			        // Keep this comment
			        Long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.longValue();
			    }

			    public void replaceFullyQualifiedWrapper(long l) {
			        // Keep this comment
			        java.lang.Long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar < l) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(long l1, long l2) {
			        // Keep this comment
			        Long plusVar = l1 + l2;
			        return plusVar > 0;
			    }

			    public long replaceLessWrapper(long l1, long l2) {
			        // Keep this comment
			        Long lessVar = l1 - l2;
			        return -lessVar;
			    }

			    public long replaceTimesWrapper(long l1, long l2) {
			        // Keep this comment
			        Long timesVar = l1 * l2;
			        return timesVar + 100;
			    }

			    public long replaceDivideWrapper(long l1, long l2) {
			        // Keep this comment
			        Long divideVar = l1 / l2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public long replaceAndMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        Long divideVar = l1 & l2;
			        return divideVar++;
			    }

			    public long replaceOrMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        Long divideVar = l1 | l2;
			        return divideVar++;
			    }

			    public long replaceShiftMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        Long divideVar = l1 ^ l2;
			        return divideVar++;
			    }

			    public long replaceMinusWrapper(long l) {
			        // Keep this comment
			        Long minusVar = -l;
			        return minusVar++;
			    }

			    public long replacePreDecrementWrapper(long l) {
			        // Keep this comment
			        Long preDecrementVar = --l;
			        return preDecrementVar++;
			    }

			    public long replacePreIncrementWrapper(long l) {
			        // Keep this comment
			        Long preDecrementVar = ++l;
			        return preDecrementVar++;
			    }

			    public long replacePostDecrementWrapper(long l) {
			        // Keep this comment
			        Long postDecrementVar = l--;
			        return postDecrementVar++;
			    }

			    public long replacePostIncrementWrapper(long l) {
			        // Keep this comment
			        Long postIncrementVar = l++;
			        return postIncrementVar++;
			    }

			    public long replaceWrapperFromValueOf(long l1) {
			        // Keep this comment
			        Long varFromValueOf = Long.valueOf(l1);
			        return varFromValueOf++;
			    }

			    public long replaceParentherizedWrapper(long l1, long l2) {
			        // Keep this comment
			        Long parentherizedVar = (l1 + l2);
			        return parentherizedVar++;
			    }

			    public long replaceComplexExprWrapper(long l1, long l2, long l3, long l4) {
			        // Keep this comment
			        Long complexVar = l1 + l2 / (l3 - l4);
			        return complexVar++;
			    }

			    public long replaceCastWrapper(Long l) {
			        // Keep this comment
			        Long castVar = (long) l;
			        return castVar++;
			    }

			    public long replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Long longInPreIncrement = Long.MIN_VALUE;
			        return ++longInPreIncrement;
			    }

			    public long replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Long longInPreDecrement = Long.MIN_VALUE;
			        return --longInPreDecrement;
			    }

			    public long replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Long longInPostDecrement = Long.MIN_VALUE;
			        return longInPostDecrement--;
			    }

			    public long replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Long longInPostIncrement = Long.MIN_VALUE;
			        return longInPostIncrement++;
			    }

			    public long replaceReturnedWrapper() {
			        // Keep this comment
			        Long returnedLong = Long.MIN_VALUE;
			        return returnedLong;
			    }

			    public long replaceMultiReturnedWrapper(long l) {
			        // Keep this comment
			        Long returnedLong = Long.MIN_VALUE;
			        if (l > 0) {
			            System.out.println("Positive");
			            return returnedLong;
			        } else {
			            System.out.println("Negative");
			            return returnedLong;
			        }
			    }

			    public Long replaceReturnedAutoBoxedWrapper(long l) {
			        // Keep this comment
			        Long returnedLong = Long.MIN_VALUE;
			        if (l > 0) {
			            System.out.println("Positive");
			            return returnedLong;
			        } else {
			            System.out.println("Negative");
			            return returnedLong;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Long reassignedLong = Long.MIN_VALUE;
			        reassignedLong = 123L;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Long multiReassignedLong = Long.MIN_VALUE;
			        multiReassignedLong = 123L;
			        multiReassignedLong = 456L;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Long assignedLong = Long.MIN_VALUE;
			        Long anotherLong = assignedLong;
			    }

			    public void replaceWrapperAssignedOnLongField() {
			        // Keep this comment
			        Long assignedLong = Long.MIN_VALUE;
			        longField = assignedLong;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Long assignedLong = Long.MIN_VALUE;
			        wrapperField = assignedLong;
			    }

			    public void replaceBitAssignedWrapper(Long aLong, Long anotherLong,
			            Long yetAnotherLong) {
			        // Keep this comment
			        Long assignedLong = Long.MIN_VALUE;
			        aLong &= assignedLong;
			        anotherLong += assignedLong;
			        yetAnotherLong ^= assignedLong;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public long longField;
			    public Long wrapperField;

			    public void replaceWrapper(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.parseLong("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsingWithRadix(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.parseLong("0", 10);
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.parseLong("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Long.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.parseLong("0");
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Long.compare(alwaysInitializedVar, l);
			    }

			    public long replaceWrapperAndPrimitiveValueMethod(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar > l) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(long l) {
			        // Keep this comment
			        long alwaysInitializedVar = Long.MIN_VALUE;
			        if (alwaysInitializedVar < l) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(long l1, long l2) {
			        // Keep this comment
			        long plusVar = l1 + l2;
			        return plusVar > 0;
			    }

			    public long replaceLessWrapper(long l1, long l2) {
			        // Keep this comment
			        long lessVar = l1 - l2;
			        return -lessVar;
			    }

			    public long replaceTimesWrapper(long l1, long l2) {
			        // Keep this comment
			        long timesVar = l1 * l2;
			        return timesVar + 100;
			    }

			    public long replaceDivideWrapper(long l1, long l2) {
			        // Keep this comment
			        long divideVar = l1 / l2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public long replaceAndMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        long divideVar = l1 & l2;
			        return divideVar++;
			    }

			    public long replaceOrMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        long divideVar = l1 | l2;
			        return divideVar++;
			    }

			    public long replaceShiftMaskWrapper(long l1, long l2) {
			        // Keep this comment
			        long divideVar = l1 ^ l2;
			        return divideVar++;
			    }

			    public long replaceMinusWrapper(long l) {
			        // Keep this comment
			        long minusVar = -l;
			        return minusVar++;
			    }

			    public long replacePreDecrementWrapper(long l) {
			        // Keep this comment
			        long preDecrementVar = --l;
			        return preDecrementVar++;
			    }

			    public long replacePreIncrementWrapper(long l) {
			        // Keep this comment
			        long preDecrementVar = ++l;
			        return preDecrementVar++;
			    }

			    public long replacePostDecrementWrapper(long l) {
			        // Keep this comment
			        long postDecrementVar = l--;
			        return postDecrementVar++;
			    }

			    public long replacePostIncrementWrapper(long l) {
			        // Keep this comment
			        long postIncrementVar = l++;
			        return postIncrementVar++;
			    }

			    public long replaceWrapperFromValueOf(long l1) {
			        // Keep this comment
			        long varFromValueOf = l1;
			        return varFromValueOf++;
			    }

			    public long replaceParentherizedWrapper(long l1, long l2) {
			        // Keep this comment
			        long parentherizedVar = (l1 + l2);
			        return parentherizedVar++;
			    }

			    public long replaceComplexExprWrapper(long l1, long l2, long l3, long l4) {
			        // Keep this comment
			        long complexVar = l1 + l2 / (l3 - l4);
			        return complexVar++;
			    }

			    public long replaceCastWrapper(Long l) {
			        // Keep this comment
			        long castVar = (long) l;
			        return castVar++;
			    }

			    public long replaceWrapperInPreIncrement() {
			        // Keep this comment
			        long longInPreIncrement = Long.MIN_VALUE;
			        return ++longInPreIncrement;
			    }

			    public long replaceWrapperInPreDecrement() {
			        // Keep this comment
			        long longInPreDecrement = Long.MIN_VALUE;
			        return --longInPreDecrement;
			    }

			    public long replaceWrapperInPostDecrement() {
			        // Keep this comment
			        long longInPostDecrement = Long.MIN_VALUE;
			        return longInPostDecrement--;
			    }

			    public long replaceWrapperInPostIncrement() {
			        // Keep this comment
			        long longInPostIncrement = Long.MIN_VALUE;
			        return longInPostIncrement++;
			    }

			    public long replaceReturnedWrapper() {
			        // Keep this comment
			        long returnedLong = Long.MIN_VALUE;
			        return returnedLong;
			    }

			    public long replaceMultiReturnedWrapper(long l) {
			        // Keep this comment
			        long returnedLong = Long.MIN_VALUE;
			        if (l > 0) {
			            System.out.println("Positive");
			            return returnedLong;
			        } else {
			            System.out.println("Negative");
			            return returnedLong;
			        }
			    }

			    public Long replaceReturnedAutoBoxedWrapper(long l) {
			        // Keep this comment
			        long returnedLong = Long.MIN_VALUE;
			        if (l > 0) {
			            System.out.println("Positive");
			            return returnedLong;
			        } else {
			            System.out.println("Negative");
			            return returnedLong;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        long reassignedLong = Long.MIN_VALUE;
			        reassignedLong = 123L;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        long multiReassignedLong = Long.MIN_VALUE;
			        multiReassignedLong = 123L;
			        multiReassignedLong = 456L;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        long assignedLong = Long.MIN_VALUE;
			        Long anotherLong = assignedLong;
			    }

			    public void replaceWrapperAssignedOnLongField() {
			        // Keep this comment
			        long assignedLong = Long.MIN_VALUE;
			        longField = assignedLong;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        long assignedLong = Long.MIN_VALUE;
			        wrapperField = assignedLong;
			    }

			    public void replaceBitAssignedWrapper(Long aLong, Long anotherLong,
			            Long yetAnotherLong) {
			        // Keep this comment
			        long assignedLong = Long.MIN_VALUE;
			        aLong &= assignedLong;
			        anotherLong += assignedLong;
			        yetAnotherLong ^= assignedLong;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveLongRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Long doNotRefactorFields = Long.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Long returnedObject = Long.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Long reassignedLong = Long.MIN_VALUE;
			        reassignedLong = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Long, Observable> obsByLong) {
			        Long reassignedLong = Long.MIN_VALUE;
			        obsByLong.get(reassignedLong).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Long assignedLong = Long.MIN_VALUE;
			        objectField = assignedLong;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Long assignedLong = Long.MIN_VALUE;
			        Long anotherLong = assignedLong;
			        Long yetAnotherLong = assignedLong;
			    }

			    public Long doNotReplaceMultiAutoBoxedWrapper() {
			        Long assignedLong = Long.MIN_VALUE;
			        Long anotherLong = assignedLong;
			        return assignedLong;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Long returnedObject = Long.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Long doNotReplaceAssignedAndReturnedWrapper(Long l) {
			        Long returnedObject = Long.MIN_VALUE;
			        returnedObject = l;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveFloatRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public float floatField;
			    public Float wrapperField;

			    public void replaceWrapper(float f) {
			        // Keep this comment
			        /* c1 */ Float /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ Float.MIN_VALUE /* c5 */;
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(float f) {
			        // Keep this comment
			        Float alwaysInitializedVar = Float.valueOf("0");
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(float f) {
			        // Keep this comment
			        Float alwaysInitializedVar = Float.MIN_VALUE;
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(float f) {
			        // Keep this comment
			        Float alwaysInitializedVar = new Float("0.0");
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(f);
			    }

			    public float replaceWrapperAndPrimitiveValueMethod(float f) {
			        // Keep this comment
			        Float alwaysInitializedVar = new Float(Float.MIN_VALUE);
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.floatValue();
			    }

			    public void replaceFullyQualifiedWrapper(float f) {
			        // Keep this comment
			        java.lang.Float alwaysInitializedVar = Float.MIN_VALUE;
			        if (alwaysInitializedVar < f) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(float f1, float f2) {
			        // Keep this comment
			        Float plusVar = f1 + f2;
			        return plusVar > 0;
			    }

			    public float replaceLessWrapper(float f1, float f2) {
			        // Keep this comment
			        Float lessVar = f1 - f2;
			        return -lessVar;
			    }

			    public float replaceTimesWrapper(float f1, float f2) {
			        // Keep this comment
			        Float timesVar = f1 * f2;
			        return timesVar + 100;
			    }

			    public float replaceDivideWrapper(float f1, float f2) {
			        // Keep this comment
			        Float divideVar = f1 / f2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public float replaceMinusWrapper(float f) {
			        // Keep this comment
			        Float minusVar = -f;
			        return minusVar++;
			    }

			    public float replacePreDecrementWrapper(float f) {
			        // Keep this comment
			        Float preDecrementVar = --f;
			        return preDecrementVar++;
			    }

			    public float replacePreIncrementWrapper(float f) {
			        // Keep this comment
			        Float preDecrementVar = ++f;
			        return preDecrementVar++;
			    }

			    public float replacePostDecrementWrapper(float f) {
			        // Keep this comment
			        Float postDecrementVar = f--;
			        return postDecrementVar++;
			    }

			    public float replacePostIncrementWrapper(float f) {
			        // Keep this comment
			        Float postIncrementVar = f++;
			        return postIncrementVar++;
			    }

			    public float replaceWrapperFromValueOf(float f1) {
			        // Keep this comment
			        Float varFromValueOf = Float.valueOf(f1);
			        return varFromValueOf++;
			    }

			    public float replaceParentherizedWrapper(float f1, float f2) {
			        // Keep this comment
			        Float parentherizedVar = (f1 + f2);
			        return parentherizedVar++;
			    }

			    public float replaceComplexExprWrapper(float f1, float f2, float f3, float f4) {
			        // Keep this comment
			        Float complexVar = f1 + f2 / (f3 - f4);
			        return complexVar++;
			    }

			    public float replaceCastWrapper(Float f) {
			        // Keep this comment
			        Float castVar = (float) f;
			        return castVar++;
			    }

			    public float replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Float floatInPreIncrement = Float.MIN_VALUE;
			        return ++floatInPreIncrement;
			    }

			    public float replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Float floatInPreDecrement = Float.MAX_VALUE;
			        return --floatInPreDecrement;
			    }

			    public float replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Float floatInPostDecrement = Float.MIN_NORMAL;
			        return floatInPostDecrement--;
			    }

			    public float replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Float floatInPostIncrement = Float.NaN;
			        return floatInPostIncrement++;
			    }

			    public float replaceReturnedWrapper() {
			        // Keep this comment
			        float returnedFloat = Float.NEGATIVE_INFINITY;
			        return returnedFloat;
			    }

			    public float replaceMultiReturnedWrapper(float f) {
			        // Keep this comment
			        Float returnedFloat = Float.POSITIVE_INFINITY;
			        if (f > 0) {
			            System.out.println("Positive");
			            return returnedFloat;
			        } else {
			            System.out.println("Negative");
			            return returnedFloat;
			        }
			    }

			    public Float replaceReturnedAutoBoxedWrapper(float f) {
			        // Keep this comment
			        Float returnedFloat = Float.MIN_VALUE;
			        if (f > 0) {
			            System.out.println("Positive");
			            return returnedFloat;
			        } else {
			            System.out.println("Negative");
			            return returnedFloat;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Float reassignedFloat = Float.MIN_VALUE;
			        reassignedFloat = 123f;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Float multiReassignedFloat = Float.MIN_VALUE;
			        multiReassignedFloat = 123f;
			        multiReassignedFloat = 456f;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Float assignedLocal = Float.MIN_VALUE;
			        Float anotherFloat = assignedLocal;
			    }

			    public void replaceWrapperAssignedOnFloatField() {
			        // Keep this comment
			        Float assignedFloat = Float.MIN_VALUE;
			        floatField = assignedFloat;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Float assignedWrapper = Float.MIN_VALUE;
			        wrapperField = assignedWrapper;
			    }

			    public void replaceBitAssignedWrapper(Float aFloat, Float anotherFloat,
			            Float yetAnotherFloat, Float evenAnotherFloat) {
			        // Keep this comment
			        Float assignedFloat = Float.MIN_VALUE;
			        aFloat += assignedFloat;
			        anotherFloat -= assignedFloat;
			        yetAnotherFloat *= assignedFloat;
			        evenAnotherFloat /= assignedFloat;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public float floatField;
			    public Float wrapperField;

			    public void replaceWrapper(float f) {
			        // Keep this comment
			        /* c1 */ float /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ Float.MIN_VALUE /* c5 */;
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(float f) {
			        // Keep this comment
			        float alwaysInitializedVar = Float.parseFloat("0");
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(float f) {
			        // Keep this comment
			        float alwaysInitializedVar = Float.MIN_VALUE;
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Float.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(float f) {
			        // Keep this comment
			        float alwaysInitializedVar = Float.parseFloat("0.0");
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Float.compare(alwaysInitializedVar, f);
			    }

			    public float replaceWrapperAndPrimitiveValueMethod(float f) {
			        // Keep this comment
			        float alwaysInitializedVar = Float.MIN_VALUE;
			        if (alwaysInitializedVar > f) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(float f) {
			        // Keep this comment
			        float alwaysInitializedVar = Float.MIN_VALUE;
			        if (alwaysInitializedVar < f) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(float f1, float f2) {
			        // Keep this comment
			        float plusVar = f1 + f2;
			        return plusVar > 0;
			    }

			    public float replaceLessWrapper(float f1, float f2) {
			        // Keep this comment
			        float lessVar = f1 - f2;
			        return -lessVar;
			    }

			    public float replaceTimesWrapper(float f1, float f2) {
			        // Keep this comment
			        float timesVar = f1 * f2;
			        return timesVar + 100;
			    }

			    public float replaceDivideWrapper(float f1, float f2) {
			        // Keep this comment
			        float divideVar = f1 / f2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public float replaceMinusWrapper(float f) {
			        // Keep this comment
			        float minusVar = -f;
			        return minusVar++;
			    }

			    public float replacePreDecrementWrapper(float f) {
			        // Keep this comment
			        float preDecrementVar = --f;
			        return preDecrementVar++;
			    }

			    public float replacePreIncrementWrapper(float f) {
			        // Keep this comment
			        float preDecrementVar = ++f;
			        return preDecrementVar++;
			    }

			    public float replacePostDecrementWrapper(float f) {
			        // Keep this comment
			        float postDecrementVar = f--;
			        return postDecrementVar++;
			    }

			    public float replacePostIncrementWrapper(float f) {
			        // Keep this comment
			        float postIncrementVar = f++;
			        return postIncrementVar++;
			    }

			    public float replaceWrapperFromValueOf(float f1) {
			        // Keep this comment
			        float varFromValueOf = f1;
			        return varFromValueOf++;
			    }

			    public float replaceParentherizedWrapper(float f1, float f2) {
			        // Keep this comment
			        float parentherizedVar = (f1 + f2);
			        return parentherizedVar++;
			    }

			    public float replaceComplexExprWrapper(float f1, float f2, float f3, float f4) {
			        // Keep this comment
			        float complexVar = f1 + f2 / (f3 - f4);
			        return complexVar++;
			    }

			    public float replaceCastWrapper(Float f) {
			        // Keep this comment
			        float castVar = (float) f;
			        return castVar++;
			    }

			    public float replaceWrapperInPreIncrement() {
			        // Keep this comment
			        float floatInPreIncrement = Float.MIN_VALUE;
			        return ++floatInPreIncrement;
			    }

			    public float replaceWrapperInPreDecrement() {
			        // Keep this comment
			        float floatInPreDecrement = Float.MAX_VALUE;
			        return --floatInPreDecrement;
			    }

			    public float replaceWrapperInPostDecrement() {
			        // Keep this comment
			        float floatInPostDecrement = Float.MIN_NORMAL;
			        return floatInPostDecrement--;
			    }

			    public float replaceWrapperInPostIncrement() {
			        // Keep this comment
			        float floatInPostIncrement = Float.NaN;
			        return floatInPostIncrement++;
			    }

			    public float replaceReturnedWrapper() {
			        // Keep this comment
			        float returnedFloat = Float.NEGATIVE_INFINITY;
			        return returnedFloat;
			    }

			    public float replaceMultiReturnedWrapper(float f) {
			        // Keep this comment
			        float returnedFloat = Float.POSITIVE_INFINITY;
			        if (f > 0) {
			            System.out.println("Positive");
			            return returnedFloat;
			        } else {
			            System.out.println("Negative");
			            return returnedFloat;
			        }
			    }

			    public Float replaceReturnedAutoBoxedWrapper(float f) {
			        // Keep this comment
			        float returnedFloat = Float.MIN_VALUE;
			        if (f > 0) {
			            System.out.println("Positive");
			            return returnedFloat;
			        } else {
			            System.out.println("Negative");
			            return returnedFloat;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        float reassignedFloat = Float.MIN_VALUE;
			        reassignedFloat = 123f;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        float multiReassignedFloat = Float.MIN_VALUE;
			        multiReassignedFloat = 123f;
			        multiReassignedFloat = 456f;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        float assignedLocal = Float.MIN_VALUE;
			        Float anotherFloat = assignedLocal;
			    }

			    public void replaceWrapperAssignedOnFloatField() {
			        // Keep this comment
			        float assignedFloat = Float.MIN_VALUE;
			        floatField = assignedFloat;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        float assignedWrapper = Float.MIN_VALUE;
			        wrapperField = assignedWrapper;
			    }

			    public void replaceBitAssignedWrapper(Float aFloat, Float anotherFloat,
			            Float yetAnotherFloat, Float evenAnotherFloat) {
			        // Keep this comment
			        float assignedFloat = Float.MIN_VALUE;
			        aFloat += assignedFloat;
			        anotherFloat -= assignedFloat;
			        yetAnotherFloat *= assignedFloat;
			        evenAnotherFloat /= assignedFloat;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveFloatRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Float doNotRefactorFields = Float.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Float returnedObject = Float.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Float reassignedFloat = Float.MIN_VALUE;
			        reassignedFloat = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Float, Observable> obsByFloat) {
			        Float reassignedFloat = Float.MIN_VALUE;
			        obsByFloat.get(reassignedFloat).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Float assignedObject = Float.MIN_VALUE;
			        objectField = assignedObject;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Float assignedFloat = Float.MIN_VALUE;
			        Float anotherFloat = assignedFloat;
			        Float yetAnotherFloat = assignedFloat;
			    }

			    public Float doNotReplaceMultiAutoBoxedWrapper() {
			        Float assignedFloat = Float.MIN_VALUE;
			        Float anotherFloat = assignedFloat;
			        return assignedFloat;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Float returnedObject = Float.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Float doNotReplaceAssignedAndReturnedWrapper(Float f) {
			        Float returnedObject = Float.MIN_VALUE;
			        returnedObject = f;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveDoubleRatherThanWrapper() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public double doubleField;
			    public Double wrapperField;

			    public void replaceWrapper(double d) {
			        // Keep this comment
			        /* c1 */ Double /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ Double.MIN_VALUE /* c5 */;
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(double d) {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.valueOf("0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(double d) {
			        // Keep this comment
			        Double alwaysInitializedVar = new Double("0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(double d) {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.MIN_VALUE;
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.toString();
			    }

			    public int replaceWrapperAndCompareToMethod(double d) {
			        // Keep this comment
			        Double alwaysInitializedVar = new Double("0.0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.compareTo(d);
			    }

			    public double replaceWrapperAndPrimitiveValueMethod(double d) {
			        // Keep this comment
			        Double alwaysInitializedVar = new Double(Double.MIN_VALUE);
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar.doubleValue();
			    }

			    public void replaceFullyQualifiedWrapper(double d) {
			        // Keep this comment
			        java.lang.Double alwaysInitializedVar = Double.MAX_VALUE;
			        if (alwaysInitializedVar < d) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(double d1, double d2) {
			        // Keep this comment
			        Double plusVar = d1 + d2;
			        return plusVar > 0;
			    }

			    public double replaceLessWrapper(double d1, double d2) {
			        // Keep this comment
			        Double lessVar = d1 - d2;
			        return -lessVar;
			    }

			    public double replaceTimesWrapper(double d1, double d2) {
			        // Keep this comment
			        Double timesVar = d1 * d2;
			        return timesVar + 100;
			    }

			    public double replaceDivideWrapper(double d1, double d2) {
			        // Keep this comment
			        Double divideVar = d1 / d2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public double replaceMinusWrapper(double d) {
			        // Keep this comment
			        Double minusVar = -d;
			        return minusVar++;
			    }

			    public double replacePreDecrementWrapper(double d) {
			        // Keep this comment
			        Double preDecrementVar = --d;
			        return preDecrementVar++;
			    }

			    public double replacePreIncrementWrapper(double d) {
			        // Keep this comment
			        Double preDecrementVar = ++d;
			        return preDecrementVar++;
			    }

			    public double replacePostDecrementWrapper(double d) {
			        // Keep this comment
			        Double postDecrementVar = d--;
			        return postDecrementVar++;
			    }

			    public double replacePostIncrementWrapper(double d) {
			        // Keep this comment
			        Double postIncrementVar = d++;
			        return postIncrementVar++;
			    }

			    public double replaceWrapperFromValueOf(double d1) {
			        // Keep this comment
			        Double varFromValueOf = Double.valueOf(d1);
			        return varFromValueOf++;
			    }

			    public double replaceParentherizedWrapper(double d1, double d2) {
			        // Keep this comment
			        Double parentherizedVar = (d1 + d2);
			        return parentherizedVar++;
			    }

			    public double replaceComplexExprWrapper(double d1, double d2, double d3, double d4) {
			        // Keep this comment
			        Double complexVar = d1 + d2 / (d3 - d4);
			        return complexVar++;
			    }

			    public double replaceCastWrapper(Double d) {
			        // Keep this comment
			        Double castVar = (double) d;
			        return castVar++;
			    }

			    public double replaceWrapperInPreIncrement() {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.MIN_NORMAL;
			        return ++alwaysInitializedVar;
			    }

			    public double replaceWrapperInPreDecrement() {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.NaN;
			        return --alwaysInitializedVar;
			    }

			    public double replaceWrapperInPostDecrement() {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.NEGATIVE_INFINITY;
			        return alwaysInitializedVar--;
			    }

			    public double replaceWrapperInPostIncrement() {
			        // Keep this comment
			        Double alwaysInitializedVar = Double.POSITIVE_INFINITY;
			        return alwaysInitializedVar++;
			    }

			    public double replaceReturnedWrapper() {
			        // Keep this comment
			        Double returnedDouble = Double.MIN_VALUE;
			        return returnedDouble;
			    }

			    public double replaceMultiReturnedWrapper(double d) {
			        // Keep this comment
			        Double returnedDouble = Double.MIN_VALUE;
			        if (d > 0) {
			            System.out.println("Positive");
			            return returnedDouble;
			        } else {
			            System.out.println("Negative");
			            return returnedDouble;
			        }
			    }

			    public Double replaceReturnedAutoBoxedWrapper(double d) {
			        // Keep this comment
			        Double returnedDouble = Double.MIN_VALUE;
			        if (d > 0) {
			            System.out.println("Positive");
			            return returnedDouble;
			        } else {
			            System.out.println("Negative");
			            return returnedDouble;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        Double reassignedDouble = Double.MIN_VALUE;
			        reassignedDouble = 123.0;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        Double multiReassignedDouble = Double.MIN_VALUE;
			        multiReassignedDouble = 123.0;
			        multiReassignedDouble = 456.0;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        Double anotherDouble = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnDoubleVariable() {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        doubleField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnWrapperVariable() {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        wrapperField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnDoubleField() {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        this.doubleField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        this.wrapperField = assignedDouble;
			    }

			    public void replaceBitAssignedWrapper(Double aDouble, Double anotherDouble,
			            Double yetAnotherDouble) {
			        // Keep this comment
			        Double assignedDouble = Double.MIN_VALUE;
			        aDouble -= assignedDouble;
			        anotherDouble += assignedDouble;
			        yetAnotherDouble *= assignedDouble;
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public double doubleField;
			    public Double wrapperField;

			    public void replaceWrapper(double d) {
			        // Keep this comment
			        /* c1 */ double /* c2 */ alwaysInitializedVar /* c3 */ = /* c4 */ Double.MIN_VALUE /* c5 */;
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndUseParsing(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.parseDouble("0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public void replaceWrapperAndConstructor(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.parseDouble("0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }
			    }

			    public String replaceWrapperAndToStringMethod(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.MIN_VALUE;
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Double.toString(alwaysInitializedVar);
			    }

			    public int replaceWrapperAndCompareToMethod(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.parseDouble("0.0");
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return Double.compare(alwaysInitializedVar, d);
			    }

			    public double replaceWrapperAndPrimitiveValueMethod(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.MIN_VALUE;
			        if (alwaysInitializedVar > d) {
			            System.out.println("True!");
			        }

			        // Keep this comment too
			        return alwaysInitializedVar;
			    }

			    public void replaceFullyQualifiedWrapper(double d) {
			        // Keep this comment
			        double alwaysInitializedVar = Double.MAX_VALUE;
			        if (alwaysInitializedVar < d) {
			            System.out.println("True!");
			        }
			    }

			    public boolean replacePlusWrapper(double d1, double d2) {
			        // Keep this comment
			        double plusVar = d1 + d2;
			        return plusVar > 0;
			    }

			    public double replaceLessWrapper(double d1, double d2) {
			        // Keep this comment
			        double lessVar = d1 - d2;
			        return -lessVar;
			    }

			    public double replaceTimesWrapper(double d1, double d2) {
			        // Keep this comment
			        double timesVar = d1 * d2;
			        return timesVar + 100;
			    }

			    public double replaceDivideWrapper(double d1, double d2) {
			        // Keep this comment
			        double divideVar = d1 / d2;
			        if (divideVar <= 0) {
			            return -1;
			        }
			        return 1;
			    }

			    public double replaceMinusWrapper(double d) {
			        // Keep this comment
			        double minusVar = -d;
			        return minusVar++;
			    }

			    public double replacePreDecrementWrapper(double d) {
			        // Keep this comment
			        double preDecrementVar = --d;
			        return preDecrementVar++;
			    }

			    public double replacePreIncrementWrapper(double d) {
			        // Keep this comment
			        double preDecrementVar = ++d;
			        return preDecrementVar++;
			    }

			    public double replacePostDecrementWrapper(double d) {
			        // Keep this comment
			        double postDecrementVar = d--;
			        return postDecrementVar++;
			    }

			    public double replacePostIncrementWrapper(double d) {
			        // Keep this comment
			        double postIncrementVar = d++;
			        return postIncrementVar++;
			    }

			    public double replaceWrapperFromValueOf(double d1) {
			        // Keep this comment
			        double varFromValueOf = d1;
			        return varFromValueOf++;
			    }

			    public double replaceParentherizedWrapper(double d1, double d2) {
			        // Keep this comment
			        double parentherizedVar = (d1 + d2);
			        return parentherizedVar++;
			    }

			    public double replaceComplexExprWrapper(double d1, double d2, double d3, double d4) {
			        // Keep this comment
			        double complexVar = d1 + d2 / (d3 - d4);
			        return complexVar++;
			    }

			    public double replaceCastWrapper(Double d) {
			        // Keep this comment
			        double castVar = (double) d;
			        return castVar++;
			    }

			    public double replaceWrapperInPreIncrement() {
			        // Keep this comment
			        double alwaysInitializedVar = Double.MIN_NORMAL;
			        return ++alwaysInitializedVar;
			    }

			    public double replaceWrapperInPreDecrement() {
			        // Keep this comment
			        double alwaysInitializedVar = Double.NaN;
			        return --alwaysInitializedVar;
			    }

			    public double replaceWrapperInPostDecrement() {
			        // Keep this comment
			        double alwaysInitializedVar = Double.NEGATIVE_INFINITY;
			        return alwaysInitializedVar--;
			    }

			    public double replaceWrapperInPostIncrement() {
			        // Keep this comment
			        double alwaysInitializedVar = Double.POSITIVE_INFINITY;
			        return alwaysInitializedVar++;
			    }

			    public double replaceReturnedWrapper() {
			        // Keep this comment
			        double returnedDouble = Double.MIN_VALUE;
			        return returnedDouble;
			    }

			    public double replaceMultiReturnedWrapper(double d) {
			        // Keep this comment
			        double returnedDouble = Double.MIN_VALUE;
			        if (d > 0) {
			            System.out.println("Positive");
			            return returnedDouble;
			        } else {
			            System.out.println("Negative");
			            return returnedDouble;
			        }
			    }

			    public Double replaceReturnedAutoBoxedWrapper(double d) {
			        // Keep this comment
			        double returnedDouble = Double.MIN_VALUE;
			        if (d > 0) {
			            System.out.println("Positive");
			            return returnedDouble;
			        } else {
			            System.out.println("Negative");
			            return returnedDouble;
			        }
			    }

			    public void replaceReassignedWrapper() {
			        // Keep this comment
			        double reassignedDouble = Double.MIN_VALUE;
			        reassignedDouble = 123.0;
			    }

			    public void replaceMultiReassignedWrapper() {
			        // Keep this comment
			        double multiReassignedDouble = Double.MIN_VALUE;
			        multiReassignedDouble = 123.0;
			        multiReassignedDouble = 456.0;
			    }

			    public void replaceAssignedWrapper() {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        Double anotherDouble = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnDoubleVariable() {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        doubleField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnWrapperVariable() {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        wrapperField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnDoubleField() {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        this.doubleField = assignedDouble;
			    }

			    public void replaceWrapperAssignedOnWrapperField() {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        this.wrapperField = assignedDouble;
			    }

			    public void replaceBitAssignedWrapper(Double aDouble, Double anotherDouble,
			            Double yetAnotherDouble) {
			        // Keep this comment
			        double assignedDouble = Double.MIN_VALUE;
			        aDouble -= assignedDouble;
			        anotherDouble += assignedDouble;
			        yetAnotherDouble *= assignedDouble;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testDoNotUsePrimitiveDoubleRatherThanWrapper() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;
			import java.util.Observable;

			public class E {
			    public Double doNotRefactorFields = Double.MIN_VALUE;
			    public Object objectField;

			    public Object doNotBreakAutoboxing() {
			        Double returnedObject = Double.MIN_VALUE;
			        return returnedObject;
			    }

			    public void doNotReplaceNullWrapper() {
			        Double reassignedDouble = Double.MIN_VALUE;
			        reassignedDouble = null;
			    }

			    public void doNotReplaceWrapperPassedAsObject(Map<Double, Observable> obsByDouble) {
			        Double reassignedDouble = Double.MIN_VALUE;
			        obsByDouble.get(reassignedDouble).notifyObservers();
			    }

			    public void doNotReplaceWrapperAssignedOnObjectField() {
			        Double assignedDouble = Double.MIN_VALUE;
			        objectField = assignedDouble;
			    }

			    public void doNotReplaceMultiAssignedWrapper() {
			        Double assignedDouble = Double.MIN_VALUE;
			        Double anotherDouble = assignedDouble;
			        Double yetAnotherDouble = assignedDouble;
			    }

			    public Double doNotReplaceMultiAutoBoxedWrapper() {
			        Double assignedDouble = Double.MIN_VALUE;
			        Double anotherDouble = assignedDouble;
			        return assignedDouble;
			    }

			    public void doNotBreakAutoboxingOnAssignment() {
			        Double returnedObject = Double.MIN_VALUE;
			        Object anotherObject = returnedObject;
			    }

			    public Double doNotReplaceAssignedAndReturnedWrapper(Double d) {
			        Double returnedObject = Double.MIN_VALUE;
			        returnedObject = d;
			        return returnedObject;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPrimitiveRatherThanWrapperPreview() throws Exception {
		String previewHeader= """
			package test1;

			public class E {
			    public void preview(int i) {
			""";
		String previewFooter= """
			    }
			}
			""";
		AbstractCleanUp cleanUp= new PrimitiveRatherThanWrapperCleanUpCore() {
			@Override
			public boolean isEnabled(String key) {
				return false;
			}
		};
		String given= previewHeader + cleanUp.getPreview() + previewFooter;
		cleanUp= new PrimitiveRatherThanWrapperCleanUpCore() {
			@Override
			public boolean isEnabled(String key) {
				return true;
			}
		};
		String expected= previewHeader + cleanUp.getPreview() + previewFooter;

		// When
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description)));
	}

	@Test
	public void testEvaluateNullable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    public void removeUselessNullCheck(String s) {
			        // Remove redundant null checks
			        boolean b1 = s != null && "".equals(s);
			        boolean b2 = s != null && "".equalsIgnoreCase(s);
			        boolean b3 = s != null && s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = null != s && "".equals(s);
			        boolean b5 = null != s && "".equalsIgnoreCase(s);
			        boolean b6 = null != s && s instanceof String;
			    }

			    public boolean removeExtendedNullCheck(boolean enabled, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && s != null && "".equals(s);
			        boolean b2 = enabled && s != null && "".equalsIgnoreCase(s);
			        boolean b3 = enabled && s != null && s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = enabled && null != s && "".equals(s);
			        boolean b5 = enabled && null != s && "".equalsIgnoreCase(s);
			        boolean b6 = enabled && null != s && s instanceof String;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }

			    public boolean removeExtendedNullCheck(boolean enabled, boolean isValid, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && isValid && s != null && "".equals(s);
			        boolean b2 = enabled && isValid && s != null && "".equalsIgnoreCase(s);
			        boolean b3 = enabled && isValid && s != null && s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = enabled && isValid && null != s && "".equals(s);
			        boolean b5 = enabled && isValid && null != s && "".equalsIgnoreCase(s);
			        boolean b6 = enabled && isValid && null != s && s instanceof String;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }

			    public boolean removeNullCheckInTheMiddle(boolean enabled, boolean isValid, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && s != null && "".equals(s) && isValid;
			        boolean b2 = enabled && s != null && "".equalsIgnoreCase(s) && isValid;
			        boolean b3 = enabled && s != null && s instanceof String && isValid;

			        // Remove redundant null checks
			        boolean b4 = enabled && null != s && "".equals(s) && isValid;
			        boolean b5 = enabled && null != s && "".equalsIgnoreCase(s) && isValid;
			        boolean b6 = enabled && null != s && s instanceof String && isValid;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.EVALUATE_NULLABLE);

		String output= """
			package test1;

			public class E {
			    public void removeUselessNullCheck(String s) {
			        // Remove redundant null checks
			        boolean b1 = "".equals(s);
			        boolean b2 = "".equalsIgnoreCase(s);
			        boolean b3 = s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = "".equals(s);
			        boolean b5 = "".equalsIgnoreCase(s);
			        boolean b6 = s instanceof String;
			    }

			    public boolean removeExtendedNullCheck(boolean enabled, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && "".equals(s);
			        boolean b2 = enabled && "".equalsIgnoreCase(s);
			        boolean b3 = enabled && s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = enabled && "".equals(s);
			        boolean b5 = enabled && "".equalsIgnoreCase(s);
			        boolean b6 = enabled && s instanceof String;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }

			    public boolean removeExtendedNullCheck(boolean enabled, boolean isValid, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && isValid && "".equals(s);
			        boolean b2 = enabled && isValid && "".equalsIgnoreCase(s);
			        boolean b3 = enabled && isValid && s instanceof String;

			        // Remove redundant null checks
			        boolean b4 = enabled && isValid && "".equals(s);
			        boolean b5 = enabled && isValid && "".equalsIgnoreCase(s);
			        boolean b6 = enabled && isValid && s instanceof String;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }

			    public boolean removeNullCheckInTheMiddle(boolean enabled, boolean isValid, String s) {
			        // Remove redundant null checks
			        boolean b1 = enabled && "".equals(s) && isValid;
			        boolean b2 = enabled && "".equalsIgnoreCase(s) && isValid;
			        boolean b3 = enabled && s instanceof String && isValid;

			        // Remove redundant null checks
			        boolean b4 = enabled && "".equals(s) && isValid;
			        boolean b5 = enabled && "".equalsIgnoreCase(s) && isValid;
			        boolean b6 = enabled && s instanceof String && isValid;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.EvaluateNullableCleanUp_description)));
	}

	@Test
	public void testDoNotEvaluateNullable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    private static final String NULL_CONSTANT = null;

			    public boolean doNotRemoveUselessNullCheckOnInstance(Object o) {
			        return o != null && equals(o);
			    }

			    public boolean doNotRemoveUselessNullCheckOnThis(Object o) {
			        return o != null && this.equals(o);
			    }

			    public boolean doNotRemoveNullCheck(String s) {
			        // Do not remove non redundant null checks
			        boolean b1 = s != null && s.equals(NULL_CONSTANT);
			        boolean b2 = s != null && s.equalsIgnoreCase(NULL_CONSTANT);

			        // Do not remove non redundant null checks
			        boolean b3 = null != s && s.equals(NULL_CONSTANT);
			        boolean b4 = null != s && s.equalsIgnoreCase(NULL_CONSTANT);

			        return b1 && b2 && b3 && b4;
			    }

			    public boolean doNotRemoveNullCheckOnActiveExpression(List<String> texts) {
			        boolean b1 = texts.remove(0) != null && "foo".equals(texts.remove(0));
			        boolean b2 = texts.remove(0) != null && "foo".equalsIgnoreCase(texts.remove(0));
			        boolean b3 = null != texts.remove(0) && "foo".equals(texts.remove(0));
			        boolean b4 = null != texts.remove(0) && "foo".equalsIgnoreCase(texts.remove(0));

			        boolean b5 = texts.remove(0) != null && texts.remove(0) instanceof String;
			        boolean b6 = null != texts.remove(0) && texts.remove(0) instanceof String;

			        return b1 && b2 && b3 && b4 && b5 && b6;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EVALUATE_NULLABLE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPushDownNegationReplaceDoubleNegation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public void replaceDoubleNegation(boolean b) {
			        boolean b1 = !!b;
			        boolean b2 = !Boolean.TRUE;
			        boolean b3 = !Boolean.FALSE;
			        boolean b4 = !true;
			        boolean b5 = !false;
			        boolean b6 = !!!!b;
			        boolean b7 = !!!!!Boolean.TRUE;
			        boolean b8 = !!!!!!!Boolean.FALSE;
			        boolean b9 = !!!!!!!!!true;
			        boolean b10 = !!!false;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public void replaceDoubleNegation(boolean b) {
			        boolean b1 = b;
			        boolean b2 = false;
			        boolean b3 = true;
			        boolean b4 = false;
			        boolean b5 = true;
			        boolean b6 = b;
			        boolean b7 = false;
			        boolean b8 = true;
			        boolean b9 = false;
			        boolean b10 = true;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceDoubleNegationWithParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceDoubleNegationWithParentheses(boolean b) {
			        return !!!(!(b /* another refactoring removes the parentheses */));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceDoubleNegationWithParentheses(boolean b) {
			        return (b /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.PushDownNegationCleanup_description)));
	}

	@Test
	public void testPushDownNegationReplaceNegationWithInfixAndOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2, boolean b3) {
			        return !(b1 && b2 && b3); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2, boolean b3) {
			        return (!b1 || !b2 || !b3); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationWithInfixOrOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2, boolean b3) {
			        return !(b1 || b2 || b3); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2, boolean b3) {
			        return (!b1 && !b2 && !b3); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceInstanceofNegationWithInfixAndOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2) {
			        return !(b1 && b2 instanceof String); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2) {
			        return (!b1 || !(b2 instanceof String)); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceInstanceofNegationWithInfixOrOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2) {
			        return !(b1 instanceof String || b2); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2) {
			        return (!(b1 instanceof String) && !b2); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationWithEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithEqualOperator(boolean b1, boolean b2) {
			        return !(b1 == b2); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithEqualOperator(boolean b1, boolean b2) {
			        return (b1 != b2); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationWithNotEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithNotEqualOperator(boolean b1, boolean b2) {
			        return !(b1 != b2); // another refactoring removes the parentheses
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationWithNotEqualOperator(boolean b1, boolean b2) {
			        return (b1 == b2); // another refactoring removes the parentheses
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationRevertInnerExpressions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationRevertInnerExpressions(boolean b1, boolean b2) {
			        return !(!b1 && !b2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationRevertInnerExpressions(boolean b1, boolean b2) {
			        return (b1 || b2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationLeaveParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationLeaveParentheses(boolean b1, boolean b2) {
			        return !(!(b1 && b2 /* another refactoring removes the parentheses */));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationLeaveParentheses(boolean b1, boolean b2) {
			        return (b1 && b2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationRemoveParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationRemoveParentheses(boolean b1, boolean b2) {
			        return !((!b1) && (!b2));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationRemoveParentheses(boolean b1, boolean b2) {
			        return (b1 || b2);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegateNonBooleanExprs() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegateNonBooleanExprs(Object o) {
			        return !(o != null /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegateNonBooleanExprs(Object o) {
			        return (o == null /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegateNonBooleanPrimitiveExprs() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegateNonBooleanPrimitiveExprs(Boolean b) {
			        return !(b != null /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegateNonBooleanPrimitiveExprs(Boolean b) {
			        return (b == null /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndLessOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndLessOperator(int i1, int i2) {
			        return !(i1 < i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndLessOperator(int i1, int i2) {
			        return (i1 >= i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndLessEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndLessEqualOperator(int i1, int i2) {
			        return !(i1 <= i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndLessEqualOperator(int i1, int i2) {
			        return (i1 > i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndGreaterOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndGreaterOperator(int i1, int i2) {
			        return !(i1 > i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndGreaterOperator(int i1, int i2) {
			        return (i1 <= i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndGreaterEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndGreaterEqualOperator(int i1, int i2) {
			        return !(i1 >= i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndGreaterEqualOperator(int i1, int i2) {
			        return (i1 < i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndEqualOperator(int i1, int i2) {
			        return !(i1 == i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndEqualOperator(int i1, int i2) {
			        return (i1 != i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testPushDownNegationReplaceNegationAndNotEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndNotEqualOperator(int i1, int i2) {
			        return !(i1 != i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= """
			package test1;

			public class E1 {
			    public boolean replaceNegationAndNotEqualOperator(int i1, int i2) {
			        return (i1 == i2 /* another refactoring removes the parentheses */);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testOperandFactorization() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.List;

			public class E {
			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = repeatedBoolean && thenExpression || repeatedBoolean && elseExpression;
			        boolean newBoolean2 = repeatedBoolean && !thenExpression || repeatedBoolean && elseExpression;
			        boolean newBoolean3 = repeatedBoolean && thenExpression || repeatedBoolean && !elseExpression;
			        boolean newBoolean4 = repeatedBoolean && !thenExpression || repeatedBoolean && !elseExpression;
			        boolean newBoolean5 = !repeatedBoolean && thenExpression || !repeatedBoolean && elseExpression;
			        boolean newBoolean6 = !repeatedBoolean && !thenExpression || !repeatedBoolean && elseExpression;
			        boolean newBoolean7 = !repeatedBoolean && thenExpression || !repeatedBoolean && !elseExpression;
			        boolean newBoolean8 = !repeatedBoolean && !thenExpression || !repeatedBoolean && !elseExpression;
			    }

			    public void replaceDuplicateConditionsWithPermutedExpressions(boolean repeatedExpression, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = repeatedExpression && thenExpression || elseExpression && repeatedExpression;
			        boolean newBoolean2 = repeatedExpression && !thenExpression || elseExpression && repeatedExpression;
			        boolean newBoolean3 = repeatedExpression && thenExpression || !elseExpression && repeatedExpression;
			        boolean newBoolean4 = repeatedExpression && !thenExpression || !elseExpression && repeatedExpression;
			        boolean newBoolean5 = !repeatedExpression && thenExpression || elseExpression && !repeatedExpression;
			        boolean newBoolean6 = !repeatedExpression && !thenExpression || elseExpression && !repeatedExpression;
			        boolean newBoolean7 = !repeatedExpression && thenExpression || !elseExpression && !repeatedExpression;
			        boolean newBoolean8 = !repeatedExpression && !thenExpression || !elseExpression && !repeatedExpression;

			        newBoolean1 = thenExpression && repeatedExpression || repeatedExpression && elseExpression;
			        newBoolean2 = !thenExpression && repeatedExpression || repeatedExpression && elseExpression;
			        newBoolean3 = thenExpression && repeatedExpression || repeatedExpression && !elseExpression;
			        newBoolean4 = !thenExpression && repeatedExpression || repeatedExpression && !elseExpression;
			        newBoolean5 = !repeatedExpression && thenExpression || !repeatedExpression && elseExpression;
			        newBoolean6 = !repeatedExpression && !thenExpression || !repeatedExpression && elseExpression;
			        newBoolean7 = !repeatedExpression && thenExpression || !repeatedExpression && !elseExpression;
			        newBoolean8 = !repeatedExpression && !thenExpression || !repeatedExpression && !elseExpression;
			    }

			    public void replaceDuplicateConditionsOnConditionalAndExpression(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean || thenExpression) && (repeatedBoolean || elseExpression);
			        boolean newBoolean2 = (repeatedBoolean || !thenExpression) && (repeatedBoolean || elseExpression);
			        boolean newBoolean3 = (repeatedBoolean || thenExpression) && (repeatedBoolean || !elseExpression);
			        boolean newBoolean4 = (repeatedBoolean || !thenExpression) && (repeatedBoolean || !elseExpression);
			        boolean newBoolean5 = (!repeatedBoolean || thenExpression) && (!repeatedBoolean || elseExpression);
			        boolean newBoolean6 = (!repeatedBoolean || !thenExpression) && (!repeatedBoolean || elseExpression);
			        boolean newBoolean7 = (!repeatedBoolean || thenExpression) && (!repeatedBoolean || !elseExpression);
			        boolean newBoolean8 = (!repeatedBoolean || !thenExpression) && (!repeatedBoolean || !elseExpression);
			    }

			    public void replaceDuplicateConditionsWithEagerOperator(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = repeatedBoolean & thenExpression | repeatedBoolean & elseExpression;
			        boolean newBoolean2 = repeatedBoolean & !thenExpression | repeatedBoolean & elseExpression;
			        boolean newBoolean3 = repeatedBoolean & thenExpression | repeatedBoolean & !elseExpression;
			        boolean newBoolean4 = repeatedBoolean & !thenExpression | repeatedBoolean & !elseExpression;
			        boolean newBoolean5 = !repeatedBoolean & thenExpression | !repeatedBoolean & elseExpression;
			        boolean newBoolean6 = !repeatedBoolean & !thenExpression | !repeatedBoolean & elseExpression;
			        boolean newBoolean7 = !repeatedBoolean & thenExpression | !repeatedBoolean & !elseExpression;
			        boolean newBoolean8 = !repeatedBoolean & !thenExpression | !repeatedBoolean & !elseExpression;
			    }

			    public void replaceDuplicateConditionsOnEagerAndExpression(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean | thenExpression) & (repeatedBoolean | elseExpression);
			        boolean newBoolean2 = (repeatedBoolean | !thenExpression) & (repeatedBoolean | elseExpression);
			        boolean newBoolean3 = (repeatedBoolean | thenExpression) & (repeatedBoolean | !elseExpression);
			        boolean newBoolean4 = (repeatedBoolean | !thenExpression) & (repeatedBoolean | !elseExpression);
			        boolean newBoolean5 = (!repeatedBoolean | thenExpression) & (!repeatedBoolean | elseExpression);
			        boolean newBoolean6 = (!repeatedBoolean | !thenExpression) & (!repeatedBoolean | elseExpression);
			        boolean newBoolean7 = (!repeatedBoolean | thenExpression) & (!repeatedBoolean | !elseExpression);
			        boolean newBoolean8 = (!repeatedBoolean | !thenExpression) & (!repeatedBoolean | !elseExpression);
			    }

			    public boolean replaceDuplicateConditionsWithWrapperAtTheStart(boolean factor, Boolean thenExpression, boolean elseExpression) {
			        return thenExpression && factor || factor && elseExpression;
			    }

			    public boolean replaceDuplicateConditionsWithWrapperAtTheEnd(boolean factor, boolean thenExpression, Boolean elseExpression) {
			        return thenExpression & factor | factor & elseExpression;
			    }

			    public void replaceDuplicateConditionsWithActiveExpressionAtFirstPosition(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = (!(i3 == i4++) && (i1 == i2)) || ((i1 == i2) && (i5 == i6));
			        boolean newBoolean2 = (!(i3 == ++i4) && (i1 == i2)) || ((i1 == i2) && (i5 == i6));
			        boolean newBoolean3 = (!(i3 == i4--) && (i1 == i2)) || ((i1 == i2) && (i5 == i6));
			        boolean newBoolean4 = (!(i3 == --i4) && (i1 == i2)) || ((i1 == i2) && (i5 == i6));

			        boolean newBoolean5 = ((i3 == i4++) && (i1 == i2)) || ((i1 == i2) && !(i5 == i6));
			        boolean newBoolean6 = ((i3 == ++i4) && (i1 == i2)) || ((i1 == i2) && !(i5 == i6));
			        boolean newBoolean7 = ((i3 == i4--) && (i1 == i2)) || ((i1 == i2) && !(i5 == i6));
			        boolean newBoolean8 = ((i3 == --i4) && (i1 == i2)) || ((i1 == i2) && !(i5 == i6));
			    }

			    public void replaceDuplicateConditionsOnEagerActiveExpression(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((i1 == i2) & !(i3 == i4)) | ((i1 == i2) & (i5 == i6++));
			        boolean newBoolean2 = ((i1 == i2) & !(i3 == i4)) | ((i1 == i2) & (i5 == ++i6));
			        boolean newBoolean3 = ((i1 == i2) & !(i3 == i4)) | ((i1 == i2) & (i5 == i6--));
			        boolean newBoolean4 = ((i1 == i2) & !(i3 == i4)) | ((i1 == i2) & (i5 == --i6));

			        boolean newBoolean5 = ((i1 == i2) & (i3 == i4)) | ((i1 == i2) & !(i5 == i6++));
			        boolean newBoolean6 = ((i1 == i2) & (i3 == i4)) | ((i1 == i2) & !(i5 == ++i6));
			        boolean newBoolean7 = ((i1 == i2) & (i3 == i4)) | ((i1 == i2) & !(i5 == i6--));
			        boolean newBoolean8 = ((i1 == i2) & (i3 == i4)) | ((i1 == i2) & !(i5 == --i6));
			    }

			    public void moveDuplicateExpressionOnTheLeftWithFinalEagerActiveExpression(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = (!(i3 == i4) & (i1 == i2)) | ((i1 == i2) & (i5 == i6++));
			        boolean newBoolean2 = (!(i3 == i4) & (i1 == i2)) | ((i1 == i2) & (i5 == ++i6));
			        boolean newBoolean3 = (!(i3 == i4) & (i1 == i2)) | ((i1 == i2) & (i5 == i6--));
			        boolean newBoolean4 = (!(i3 == i4) & (i1 == i2)) | ((i1 == i2) & (i5 == --i6));

			        boolean newBoolean5 = ((i3 == i4) & (i1 == i2)) | ((i1 == i2) & !(i5 == i6++));
			        boolean newBoolean6 = ((i3 == i4) & (i1 == i2)) | ((i1 == i2) & !(i5 == ++i6));
			        boolean newBoolean7 = ((i3 == i4) & (i1 == i2)) | ((i1 == i2) & !(i5 == i6--));
			        boolean newBoolean8 = ((i3 == i4) & (i1 == i2)) | ((i1 == i2) & !(i5 == --i6));
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2 * 2) && !(i3 == i4) || (i1 == 2 * i2 * 1) && (i5 == i6);
			        boolean newBoolean2 = (i1 + 1 + 0 == i2) && (i3 == i4) || (1 + i1 == i2) && !(i5 == i6);
			        boolean newBoolean3 = (i1 < i2) && (i3 == i4) || !(i1 >= i2) && !(i5 == i6);
			    }

			    public int replaceBitwiseOperation(int i1, int i2, int i3) {
			        return i1 & i2 | i1 & i3;
			    }

			    public char replaceCharBitwiseOperation(char c1, char c2, char c3) {
			        return c1 & c2 | c1 & c3;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.OPERAND_FACTORIZATION);

		String expected= """
			package test1;

			import java.util.List;

			public class E {
			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean && (thenExpression || elseExpression));
			        boolean newBoolean2 = (repeatedBoolean && (!thenExpression || elseExpression));
			        boolean newBoolean3 = (repeatedBoolean && (thenExpression || !elseExpression));
			        boolean newBoolean4 = (repeatedBoolean && (!thenExpression || !elseExpression));
			        boolean newBoolean5 = (!repeatedBoolean && (thenExpression || elseExpression));
			        boolean newBoolean6 = (!repeatedBoolean && (!thenExpression || elseExpression));
			        boolean newBoolean7 = (!repeatedBoolean && (thenExpression || !elseExpression));
			        boolean newBoolean8 = (!repeatedBoolean && (!thenExpression || !elseExpression));
			    }

			    public void replaceDuplicateConditionsWithPermutedExpressions(boolean repeatedExpression, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = ((thenExpression || elseExpression) && repeatedExpression);
			        boolean newBoolean2 = ((!thenExpression || elseExpression) && repeatedExpression);
			        boolean newBoolean3 = ((thenExpression || !elseExpression) && repeatedExpression);
			        boolean newBoolean4 = ((!thenExpression || !elseExpression) && repeatedExpression);
			        boolean newBoolean5 = ((thenExpression || elseExpression) && !repeatedExpression);
			        boolean newBoolean6 = ((!thenExpression || elseExpression) && !repeatedExpression);
			        boolean newBoolean7 = ((thenExpression || !elseExpression) && !repeatedExpression);
			        boolean newBoolean8 = ((!thenExpression || !elseExpression) && !repeatedExpression);

			        newBoolean1 = (repeatedExpression && (thenExpression || elseExpression));
			        newBoolean2 = (repeatedExpression && (!thenExpression || elseExpression));
			        newBoolean3 = (repeatedExpression && (thenExpression || !elseExpression));
			        newBoolean4 = (repeatedExpression && (!thenExpression || !elseExpression));
			        newBoolean5 = (!repeatedExpression && (thenExpression || elseExpression));
			        newBoolean6 = (!repeatedExpression && (!thenExpression || elseExpression));
			        newBoolean7 = (!repeatedExpression && (thenExpression || !elseExpression));
			        newBoolean8 = (!repeatedExpression && (!thenExpression || !elseExpression));
			    }

			    public void replaceDuplicateConditionsOnConditionalAndExpression(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean || (thenExpression && elseExpression));
			        boolean newBoolean2 = (repeatedBoolean || (!thenExpression && elseExpression));
			        boolean newBoolean3 = (repeatedBoolean || (thenExpression && !elseExpression));
			        boolean newBoolean4 = (repeatedBoolean || (!thenExpression && !elseExpression));
			        boolean newBoolean5 = (!repeatedBoolean || (thenExpression && elseExpression));
			        boolean newBoolean6 = (!repeatedBoolean || (!thenExpression && elseExpression));
			        boolean newBoolean7 = (!repeatedBoolean || (thenExpression && !elseExpression));
			        boolean newBoolean8 = (!repeatedBoolean || (!thenExpression && !elseExpression));
			    }

			    public void replaceDuplicateConditionsWithEagerOperator(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean & (thenExpression | elseExpression));
			        boolean newBoolean2 = (repeatedBoolean & (!thenExpression | elseExpression));
			        boolean newBoolean3 = (repeatedBoolean & (thenExpression | !elseExpression));
			        boolean newBoolean4 = (repeatedBoolean & (!thenExpression | !elseExpression));
			        boolean newBoolean5 = (!repeatedBoolean & (thenExpression | elseExpression));
			        boolean newBoolean6 = (!repeatedBoolean & (!thenExpression | elseExpression));
			        boolean newBoolean7 = (!repeatedBoolean & (thenExpression | !elseExpression));
			        boolean newBoolean8 = (!repeatedBoolean & (!thenExpression | !elseExpression));
			    }

			    public void replaceDuplicateConditionsOnEagerAndExpression(boolean repeatedBoolean, boolean thenExpression, boolean elseExpression) {
			        // Keep this comment
			        boolean newBoolean1 = (repeatedBoolean | (thenExpression & elseExpression));
			        boolean newBoolean2 = (repeatedBoolean | (!thenExpression & elseExpression));
			        boolean newBoolean3 = (repeatedBoolean | (thenExpression & !elseExpression));
			        boolean newBoolean4 = (repeatedBoolean | (!thenExpression & !elseExpression));
			        boolean newBoolean5 = (!repeatedBoolean | (thenExpression & elseExpression));
			        boolean newBoolean6 = (!repeatedBoolean | (!thenExpression & elseExpression));
			        boolean newBoolean7 = (!repeatedBoolean | (thenExpression & !elseExpression));
			        boolean newBoolean8 = (!repeatedBoolean | (!thenExpression & !elseExpression));
			    }

			    public boolean replaceDuplicateConditionsWithWrapperAtTheStart(boolean factor, Boolean thenExpression, boolean elseExpression) {
			        return ((thenExpression || elseExpression) && factor);
			    }

			    public boolean replaceDuplicateConditionsWithWrapperAtTheEnd(boolean factor, boolean thenExpression, Boolean elseExpression) {
			        return (factor & (thenExpression | elseExpression));
			    }

			    public void replaceDuplicateConditionsWithActiveExpressionAtFirstPosition(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((!(i3 == i4++) || (i5 == i6)) && (i1 == i2));
			        boolean newBoolean2 = ((!(i3 == ++i4) || (i5 == i6)) && (i1 == i2));
			        boolean newBoolean3 = ((!(i3 == i4--) || (i5 == i6)) && (i1 == i2));
			        boolean newBoolean4 = ((!(i3 == --i4) || (i5 == i6)) && (i1 == i2));

			        boolean newBoolean5 = (((i3 == i4++) || !(i5 == i6)) && (i1 == i2));
			        boolean newBoolean6 = (((i3 == ++i4) || !(i5 == i6)) && (i1 == i2));
			        boolean newBoolean7 = (((i3 == i4--) || !(i5 == i6)) && (i1 == i2));
			        boolean newBoolean8 = (((i3 == --i4) || !(i5 == i6)) && (i1 == i2));
			    }

			    public void replaceDuplicateConditionsOnEagerActiveExpression(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((i1 == i2) & (!(i3 == i4) | (i5 == i6++)));
			        boolean newBoolean2 = ((i1 == i2) & (!(i3 == i4) | (i5 == ++i6)));
			        boolean newBoolean3 = ((i1 == i2) & (!(i3 == i4) | (i5 == i6--)));
			        boolean newBoolean4 = ((i1 == i2) & (!(i3 == i4) | (i5 == --i6)));

			        boolean newBoolean5 = ((i1 == i2) & ((i3 == i4) | !(i5 == i6++)));
			        boolean newBoolean6 = ((i1 == i2) & ((i3 == i4) | !(i5 == ++i6)));
			        boolean newBoolean7 = ((i1 == i2) & ((i3 == i4) | !(i5 == i6--)));
			        boolean newBoolean8 = ((i1 == i2) & ((i3 == i4) | !(i5 == --i6)));
			    }

			    public void moveDuplicateExpressionOnTheLeftWithFinalEagerActiveExpression(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((i1 == i2) & (!(i3 == i4) | (i5 == i6++)));
			        boolean newBoolean2 = ((i1 == i2) & (!(i3 == i4) | (i5 == ++i6)));
			        boolean newBoolean3 = ((i1 == i2) & (!(i3 == i4) | (i5 == i6--)));
			        boolean newBoolean4 = ((i1 == i2) & (!(i3 == i4) | (i5 == --i6)));

			        boolean newBoolean5 = ((i1 == i2) & ((i3 == i4) | !(i5 == i6++)));
			        boolean newBoolean6 = ((i1 == i2) & ((i3 == i4) | !(i5 == ++i6)));
			        boolean newBoolean7 = ((i1 == i2) & ((i3 == i4) | !(i5 == i6--)));
			        boolean newBoolean8 = ((i1 == i2) & ((i3 == i4) | !(i5 == --i6)));
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((i1 == i2 * 2) && (!(i3 == i4) || (i5 == i6)));
			        boolean newBoolean2 = ((i1 + 1 + 0 == i2) && ((i3 == i4) || !(i5 == i6)));
			        boolean newBoolean3 = ((i1 < i2) && ((i3 == i4) || !(i5 == i6)));
			    }

			    public int replaceBitwiseOperation(int i1, int i2, int i3) {
			        return (i1 & (i2 | i3));
			    }

			    public char replaceCharBitwiseOperation(char c1, char c2, char c3) {
			        return (c1 & (c2 | c3));
			    }
			}
			""";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.OperandFactorizationCleanUp_description)));
	}

	@Test
	public void testDoNotUseOperandFactorization() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    private static int staticField = 0;

			    public boolean doNoRefactorFailingCode(boolean factor, boolean thenExpression, boolean[] elseCrashingExpression) {
			        return thenExpression && factor || elseCrashingExpression[-1] && factor;
			    }

			    public boolean doNotRefactorWithOtherCondition(boolean factor, boolean thenExpression, boolean elseExpression, boolean extendedOperand) {
			        return factor && thenExpression || factor && elseExpression && extendedOperand;
			    }

			    public void doNotRefactorWithOtherOperandBefore(boolean b1, boolean b2, boolean b3, boolean unrelevantCondition) {
			        boolean newBoolean1 = unrelevantCondition || (b1 && b2) || (!b1 && b3);
			        boolean newBoolean2 = unrelevantCondition || (b1 && !b2) || (b3 && !b1);
			        boolean newBoolean3 = unrelevantCondition || (b1 && b2) || (!b3 && !b1);
			        boolean newBoolean4 = unrelevantCondition || (b1 && !b2) || (!b3 && !b1);
			        boolean newBoolean5 = unrelevantCondition || (!b1 && b2) || (b3 && b1);
			        boolean newBoolean6 = unrelevantCondition || (!b1 && !b2) || (b3 && b1);
			        boolean newBoolean7 = unrelevantCondition || (!b1 && b2) || (!b3 && b1);
			        boolean newBoolean8 = unrelevantCondition || (!b1 && !b2) || (!b3 && b1);
			    }

			    public void doNotRefactorWithOtherOperandAfter(boolean b1, boolean b2, boolean b3, boolean unrelevantCondition) {
			        boolean newBoolean1 = (b1 && b2) || (!b1 && b3) || unrelevantCondition;
			        boolean newBoolean2 = (b1 && !b2) || (b3 && !b1) || unrelevantCondition;
			        boolean newBoolean3 = (b1 && b2) || (!b3 && !b1) || unrelevantCondition;
			        boolean newBoolean4 = (b1 && !b2) || (!b3 && !b1) || unrelevantCondition;
			        boolean newBoolean5 = (!b1 && b2) || (b3 && b1) || unrelevantCondition;
			        boolean newBoolean6 = (!b1 && !b2) || (b3 && b1) || unrelevantCondition;
			        boolean newBoolean7 = (!b1 && b2) || (!b3 && b1) || unrelevantCondition;
			        boolean newBoolean8 = (!b1 && !b2) || (!b3 && b1) || unrelevantCondition;
			    }

			    public boolean doNotRefactorWithWrapperFactor(Boolean factor, boolean thenExpression, boolean elseExpression) {
			        return factor & thenExpression | factor & elseExpression;
			    }

			    public boolean doNotRefactorWithWrapperInTheMiddle(boolean factor, Boolean thenExpression, boolean elseExpression) {
			        return factor & thenExpression | factor & elseExpression;
			    }

			    public boolean doNotRefactorWithLazyWrapperAtTheEnd(boolean factor, boolean thenExpression, Boolean elseExpression) {
			        return thenExpression && factor || factor && elseExpression;
			    }

			    public void doNotRefactorWithMethods(List<String> myList) {
			        boolean newBoolean1 = myList.remove("lorem") && !myList.remove("foo") || myList.remove("lorem")
			                && myList.remove("ipsum");
			        boolean newBoolean2 = myList.remove("lorem") && myList.remove("bar") || myList.remove("lorem")
			                && !myList.remove("ipsum");
			    }

			    public void doNotRefactorWithIncrements(int i1, int i2, int i3, int i4, int i5, int i6) {
			        boolean newBoolean1 = (i1 == i2) && !(i3 == i4++) || (i1 == i2) && (i5 == i6++);
			        boolean newBoolean2 = (i1 == i2) && !(i3 == ++i4) || (i1 == i2) && (i5 == ++i6);
			        boolean newBoolean3 = (i1 == i2) && !(i3 == i4--) || (i1 == i2) && (i5 == i6--);
			        boolean newBoolean4 = (i1 == i2) && !(i3 == --i4) || (i1 == i2) && (i5 == --i6);

			        boolean newBoolean5 = (i1 == i2) && (i3 == i4++) || (i1 == i2) && !(i5 == i6++);
			        boolean newBoolean6 = (i1 == i2) && (i3 == ++i4) || (i1 == i2) && !(i5 == ++i6);
			        boolean newBoolean7 = (i1 == i2) && (i3 == i4--) || (i1 == i2) && !(i5 == i6--);
			        boolean newBoolean8 = (i1 == i2) && (i3 == --i4) || (i1 == i2) && !(i5 == --i6);
			    }

			    public void doNotReplaceActiveDuplicateConditions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        boolean newBoolean1 = (i1 == i2++) & !(i3 == i4) | (i1 == i2++) & (i5 == i6);
			        boolean newBoolean2 = (i1 == ++i2) & !(i3 == i4) | (i1 == ++i2) & (i5 == i6);
			        boolean newBoolean3 = (i1 == i2--) & !(i3 == i4) | (i1 == i2--) & (i5 == i6);
			        boolean newBoolean4 = (i1 == --i2) & !(i3 == i4) | (i1 == --i2) & (i5 == i6);

			        boolean newBoolean5 = (i1 == i2++) & (i3 == i4) || (i1 == i2++) & !(i5 == i6);
			        boolean newBoolean6 = (i1 == ++i2) & (i3 == i4) || (i1 == ++i2) & !(i5 == i6);
			        boolean newBoolean7 = (i1 == i2--) & (i3 == i4) || (i1 == i2--) & !(i5 == i6);
			        boolean newBoolean8 = (i1 == --i2) & (i3 == i4) || (i1 == --i2) & !(i5 == i6);
			    }

			    public boolean doNotRefactorWithSideEffectDueToThenExpression(int i1, int i2, int i3, int i4, int i5) {
			        return ((i1 == i2) & (i3 == i2++)) | ((i1 == i2) & (i5 == i4));
			    }

			    public boolean doNotRefactorWithSideEffectDueToElseExpression(int i1, int i2, int i3, int i4, int i5) {
			        return ((i1 == i2) & (i3 == i4)) | ((i5 == i2++) & (i1 == i2));
			    }

			    public boolean doNotRefactorWithSideEffectDueToElseExpressionToo(int i1, int i2, int i3, int i4, int i5) {
			        return ((i3 == i4) & (i1 == i2)) | ((i5 == i2++) & (i1 == i2));
			    }

			    public void doNotRefactorWithAssignments(int i1, int i2, boolean b1, boolean b2, boolean b3) {
			        boolean newBoolean1 = (i1 == i2) && !(b1 = b2) || (i1 == i2) && (b1 = b3);
			        boolean newBoolean2 = (i1 == i2) && (b1 = b2) || (i1 == i2) && !(b1 = b3);
			    }

			    private class SideEffect {
			        private SideEffect() {
			            staticField++;
			        }
			    }

			    public void doNotRefactorWithInstantiation(boolean b1) {
			        boolean newBoolean1 = b1 && !(new SideEffect() instanceof SideEffect)
			                || b1 && new SideEffect() instanceof Object;
			        boolean newBoolean2 = b1 && new SideEffect() instanceof SideEffect
			                || b1 && !(new SideEffect() instanceof Object);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.OPERAND_FACTORIZATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testTernaryOperator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean isValid, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = isValid && b2 || !isValid && b3;
			        boolean newBoolean2 = isValid && !b2 || !isValid && b3;
			        boolean newBoolean3 = isValid && b2 || !isValid && !b3;
			        boolean newBoolean4 = isValid && !b2 || !isValid && !b3;
			        boolean newBoolean5 = !isValid && b2 || isValid && b3;
			        boolean newBoolean6 = !isValid && !b2 || isValid && b3;
			        boolean newBoolean7 = !isValid && b2 || isValid && !b3;
			        boolean newBoolean8 = !isValid && !b2 || isValid && !b3;
			    }

			    public void replaceDuplicateConditionsWithEagerOperator(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = b1 & b2 | !b1 & b3;
			        boolean newBoolean2 = b1 & !b2 | !b1 & b3;
			        boolean newBoolean3 = b1 & b2 | !b1 & !b3;
			        boolean newBoolean4 = b1 & !b2 | !b1 & !b3;
			        boolean newBoolean5 = !b1 & b2 | b1 & b3;
			        boolean newBoolean6 = !b1 & !b2 | b1 & b3;
			        boolean newBoolean7 = !b1 & b2 | b1 & !b3;
			        boolean newBoolean8 = !b1 & !b2 | b1 & !b3;
			    }

			    public void replaceDuplicateConditionsWithPermutedBooleans(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && b2 || b3 && !b1;
			        boolean newBoolean2 = b1 && !b2 || b3 && !b1;
			        boolean newBoolean3 = b1 && b2 || !b3 && !b1;
			        boolean newBoolean4 = b1 && !b2 || !b3 && !b1;
			        boolean newBoolean5 = !b1 && b2 || b3 && b1;
			        boolean newBoolean6 = !b1 && !b2 || b3 && b1;
			        boolean newBoolean7 = !b1 && b2 || !b3 && b1;
			        boolean newBoolean8 = !b1 && !b2 || !b3 && b1;

			        newBoolean1 = b2 && b1 || !b1 && b3;
			        newBoolean2 = !b2 && b1 || !b1 && b3;
			        newBoolean3 = b2 && b1 || !b1 && !b3;
			        newBoolean4 = !b2 && b1 || !b1 && !b3;
			        newBoolean5 = !b1 && b2 || b1 && b3;
			        newBoolean6 = !b1 && !b2 || b1 && b3;
			        newBoolean7 = !b1 && b2 || b1 && !b3;
			        newBoolean8 = !b1 && !b2 || b1 && !b3;
			    }

			    public void replaceDuplicateConditionsWithOtherCondition(boolean b1, boolean b2, boolean b3, boolean unrevelantCondition) {
			        boolean newBoolean1 = unrevelantCondition || (b1 && b2) || (!b1 && b3);
			        boolean newBoolean2 = unrevelantCondition || (b1 && !b2) || (b3 && !b1);
			        boolean newBoolean3 = unrevelantCondition || (b1 && b2) || (!b3 && !b1);
			        boolean newBoolean4 = unrevelantCondition || (b1 && !b2) || (!b3 && !b1);
			        boolean newBoolean5 = unrevelantCondition || (!b1 && b2) || (b3 && b1);
			        boolean newBoolean6 = unrevelantCondition || (!b1 && !b2) || (b3 && b1);
			        boolean newBoolean7 = unrevelantCondition || (!b1 && b2) || (!b3 && b1);
			        boolean newBoolean8 = unrevelantCondition || (!b1 && !b2) || (!b3 && b1);
			    }

			    public void replaceDuplicateConditionsWithOtherConditionAfter(boolean b1, boolean b2, boolean b3, boolean unrevelantCondition) {
			        boolean newBoolean1 = (b1 && b2) || (!b1 && b3) || unrevelantCondition;
			        boolean newBoolean2 = (b1 && !b2) || (b3 && !b1) || unrevelantCondition;
			        boolean newBoolean3 = (b1 && b2) || (!b3 && !b1) || unrevelantCondition;
			        boolean newBoolean4 = (b1 && !b2) || (!b3 && !b1) || unrevelantCondition;
			        boolean newBoolean5 = (!b1 && b2) || (b3 && b1) || unrevelantCondition;
			        boolean newBoolean6 = (!b1 && !b2) || (b3 && b1) || unrevelantCondition;
			        boolean newBoolean7 = (!b1 && b2) || (!b3 && b1) || unrevelantCondition;
			        boolean newBoolean8 = (!b1 && !b2) || (!b3 && b1) || unrevelantCondition;
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2 * 2) && !(i3 == i4) || !(i1 == 2 * i2 * 1) && (i5 == i6);
			        boolean newBoolean2 = (i1 + 1 + 0 == i2) && (i3 == i4) || !(1 + i1 == i2) && !(i5 == i6);
			        boolean newBoolean3 = (i1 < i2) && (i3 == i4) || (i1 >= i2) && !(i5 == i6);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.TERNARY_OPERATOR);

		String expected= """
			package test1;

			public class E {
			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean isValid, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = (isValid ? b2 : b3);
			        boolean newBoolean2 = (isValid ? !b2 : b3);
			        boolean newBoolean3 = (isValid ? b2 : !b3);
			        boolean newBoolean4 = (isValid ? !b2 : !b3);
			        boolean newBoolean5 = (isValid ? b3 : b2);
			        boolean newBoolean6 = (isValid ? b3 : !b2);
			        boolean newBoolean7 = (isValid ? !b3 : b2);
			        boolean newBoolean8 = (isValid ? !b3 : !b2);
			    }

			    public void replaceDuplicateConditionsWithEagerOperator(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = (b1 ? b2 : b3);
			        boolean newBoolean2 = (b1 ? !b2 : b3);
			        boolean newBoolean3 = (b1 ? b2 : !b3);
			        boolean newBoolean4 = (b1 ? !b2 : !b3);
			        boolean newBoolean5 = (b1 ? b3 : b2);
			        boolean newBoolean6 = (b1 ? b3 : !b2);
			        boolean newBoolean7 = (b1 ? !b3 : b2);
			        boolean newBoolean8 = (b1 ? !b3 : !b2);
			    }

			    public void replaceDuplicateConditionsWithPermutedBooleans(boolean b1, boolean b2, boolean b3) {
			        // Keep this comment
			        boolean newBoolean1 = (b1 ? b2 : b3);
			        boolean newBoolean2 = (b1 ? !b2 : b3);
			        boolean newBoolean3 = (b1 ? b2 : !b3);
			        boolean newBoolean4 = (b1 ? !b2 : !b3);
			        boolean newBoolean5 = (b1 ? b3 : b2);
			        boolean newBoolean6 = (b1 ? b3 : !b2);
			        boolean newBoolean7 = (b1 ? !b3 : b2);
			        boolean newBoolean8 = (b1 ? !b3 : !b2);

			        newBoolean1 = (b1 ? b2 : b3);
			        newBoolean2 = (b1 ? !b2 : b3);
			        newBoolean3 = (b1 ? b2 : !b3);
			        newBoolean4 = (b1 ? !b2 : !b3);
			        newBoolean5 = (b1 ? b3 : b2);
			        newBoolean6 = (b1 ? b3 : !b2);
			        newBoolean7 = (b1 ? !b3 : b2);
			        newBoolean8 = (b1 ? !b3 : !b2);
			    }

			    public void replaceDuplicateConditionsWithOtherCondition(boolean b1, boolean b2, boolean b3, boolean unrevelantCondition) {
			        boolean newBoolean1 = unrevelantCondition || (b1 ? b2 : b3);
			        boolean newBoolean2 = unrevelantCondition || (b1 ? !b2 : b3);
			        boolean newBoolean3 = unrevelantCondition || (b1 ? b2 : !b3);
			        boolean newBoolean4 = unrevelantCondition || (b1 ? !b2 : !b3);
			        boolean newBoolean5 = unrevelantCondition || (b1 ? b3 : b2);
			        boolean newBoolean6 = unrevelantCondition || (b1 ? b3 : !b2);
			        boolean newBoolean7 = unrevelantCondition || (b1 ? !b3 : b2);
			        boolean newBoolean8 = unrevelantCondition || (b1 ? !b3 : !b2);
			    }

			    public void replaceDuplicateConditionsWithOtherConditionAfter(boolean b1, boolean b2, boolean b3, boolean unrevelantCondition) {
			        boolean newBoolean1 = (b1 ? b2 : b3) || unrevelantCondition;
			        boolean newBoolean2 = (b1 ? !b2 : b3) || unrevelantCondition;
			        boolean newBoolean3 = (b1 ? b2 : !b3) || unrevelantCondition;
			        boolean newBoolean4 = (b1 ? !b2 : !b3) || unrevelantCondition;
			        boolean newBoolean5 = (b1 ? b3 : b2) || unrevelantCondition;
			        boolean newBoolean6 = (b1 ? b3 : !b2) || unrevelantCondition;
			        boolean newBoolean7 = (b1 ? !b3 : b2) || unrevelantCondition;
			        boolean newBoolean8 = (b1 ? !b3 : !b2) || unrevelantCondition;
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {
			        // Keep this comment
			        boolean newBoolean1 = ((i1 == i2 * 2) ? !(i3 == i4) : (i5 == i6));
			        boolean newBoolean2 = ((i1 + 1 + 0 == i2) ? (i3 == i4) : !(i5 == i6));
			        boolean newBoolean3 = ((i1 < i2) ? (i3 == i4) : !(i5 == i6));
			    }
			}
			""";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.TernaryOperatorCleanUp_description)));
	}

	@Test
	public void testDoNotUseTernaryOperator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    private static int staticField = 0;

			    public void doNoReplaceDuplicateConditionsWithOtherCondition(boolean b1, boolean b2, boolean b3, boolean b4) {
			        boolean newBoolean1 = b1 && b2 || !b1 && b3 && b4;
			    }

			    public void doNoUseTernaryOperatorWithSameExpressions(boolean b1, int number) {
			        boolean newBoolean1 = b1 && (number > 0) || !b1 && (0 < number);
			    }

			    public void doNoUseTernaryOperatorWithNegativeExpressions(boolean b1, int number) {
			        boolean newBoolean1 = b1 && (number > 0) || !b1 && (0 >= number);
			    }

			    public void doNoReplaceDuplicateConditionsWithWrappers(Boolean b1, Boolean b2, Boolean b3) {
			        boolean newBoolean1 = b1 && b2 || !b1 && b3;
			        boolean newBoolean2 = b1 && !b2 || !b1 && b3;
			        boolean newBoolean3 = b1 && b2 || !b1 && !b3;
			        boolean newBoolean4 = b1 && !b2 || !b1 && !b3;
			        boolean newBoolean5 = !b1 && b2 || b1 && b3;
			        boolean newBoolean6 = !b1 && !b2 || b1 && b3;
			        boolean newBoolean7 = !b1 && b2 || b1 && !b3;
			        boolean newBoolean8 = !b1 && !b2 || b1 && !b3;
			    }

			    public void doNotReplaceDuplicateConditionsWithMethods(List<String> myList) {
			        boolean newBoolean1 = myList.remove("foo") && !myList.remove("bar") || !myList.remove("lorem")
			                && myList.remove("ipsum");
			        boolean newBoolean2 = myList.remove("foo") && myList.remove("bar") || !myList.remove("lorem")
			                && !myList.remove("ipsum");
			    }

			    public void doNotReplaceDuplicateConditionsWithIncrements(int i1, int i2, int i3, int i4, int i5, int i6) {
			        boolean newBoolean1 = (i1 == i2) && !(i3 == i4++) || !(i1 == i2) && (i5 == i6++);
			        boolean newBoolean2 = (i1 == i2) && !(i3 == ++i4) || !(i1 == i2) && (i5 == ++i6);
			        boolean newBoolean3 = (i1 == i2) && !(i3 == i4--) || !(i1 == i2) && (i5 == i6--);
			        boolean newBoolean4 = (i1 == i2) && !(i3 == --i4) || !(i1 == i2) && (i5 == --i6);

			        boolean newBoolean5 = (i1 == i2) && (i3 == i4++) || !(i1 == i2) && !(i5 == i6++);
			        boolean newBoolean6 = (i1 == i2) && (i3 == ++i4) || !(i1 == i2) && !(i5 == ++i6);
			        boolean newBoolean7 = (i1 == i2) && (i3 == i4--) || !(i1 == i2) && !(i5 == i6--);
			        boolean newBoolean8 = (i1 == i2) && (i3 == --i4) || !(i1 == i2) && !(i5 == --i6);
			    }

			    public void doNotReplaceDuplicateConditionsWithAssignments(int i1, int i2, boolean b1, boolean b2, boolean b3) {
			        boolean newBoolean1 = (i1 == i2) && !(b1 = b2) || !(i1 == i2) && (b1 = b3);
			        boolean newBoolean2 = (i1 == i2) && (b1 = b2) || !(i1 == i2) && !(b1 = b3);
			    }

			    private class SideEffect {
			        private SideEffect() {
			            staticField++;
			        }
			    }

			    public void doNotReplaceDuplicateConditionsWithInstanciations(Boolean b1) {
			        boolean newBoolean1 = b1 && !(new SideEffect() instanceof SideEffect)
			                || !b1 && new SideEffect() instanceof Object;
			        boolean newBoolean2 = b1 && new SideEffect() instanceof SideEffect
			                || !b1 && !(new SideEffect() instanceof Object);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.TERNARY_OPERATOR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStrictlyEqualOrDifferent() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private static int staticField = 0;

			    public void replaceDuplicateConditionsWithEagerOperator(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 & !b2 | !b1 & b2;
			        boolean newBoolean2 = b1 & b2 | !b1 & !b2;
			    }

			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && !b2 || !b1 && b2;
			        boolean newBoolean2 = b1 && b2 || !b1 && !b2;
			    }

			    public void replaceDuplicateConditionsWithPermutedBooleans(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && !b2 || b2 && !b1;
			        boolean newBoolean2 = b1 && b2 || !b2 && !b1;
			    }

			    public void replaceDuplicateConditionsWithWrappers(Boolean b1, Boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 && !b2 || !b1 && b2;
			        boolean newBoolean2 = b1 && b2 || !b1 && !b2;
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) && !(i3 == i4) || !(i2 == i1) && (i3 == i4);
			        boolean newBoolean2 = (i1 == i2) && (i3 <= i4) || !(i1 == i2) && !(i4 >= i3);
			        boolean newBoolean3 = (i1 == i2) && (i3 != i4) || (i2 != i1) && (i3 == i4);
			        boolean newBoolean4 = (i1 == i2) && (i3 < i4) || (i1 != i2) && (i4 <= i3);
			        boolean newBoolean5 = (i1 == i2 && i3 != i4) || (i2 != i1 && i3 == i4);
			    }

			    public void replaceDuplicateConditionsWithFields() {
			        // Keep this comment
			        boolean newBoolean1 = (staticField > 0) && (staticField < 100) || (staticField <= 0) && (staticField >= 100);
			        boolean newBoolean2 = (staticField > 0) && (staticField < 100) || (staticField >= 100) && !(staticField > 0);
			    }

			    public void replaceTernaryWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ? !b2 : b2;
			        boolean newBoolean2 = b1 ? b2 : !b2;
			    }

			    public void replaceTernaryWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) ? !(i3 == i4) : (i3 == i4);
			        boolean newBoolean2 = (i1 == i2) ? (i3 <= i4) : !(i4 >= i3);
			        boolean newBoolean3 = (i1 == i2) ? (i3 != i4) : (i3 == i4);
			        boolean newBoolean4 = (i1 == i2) ? (i3 < i4) : (i4 <= i3);
			    }

			    public void replaceTernaryWithFields() {
			        // Keep this comment
			        boolean newBoolean1 = (staticField > 0) ? (staticField < 100) : (staticField >= 100);
			        boolean newBoolean2 = (staticField > 0) ? (staticField < 100) : !(staticField < 100);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT);

		String expected= """
			package test1;

			public class E {
			    private static int staticField = 0;

			    public void replaceDuplicateConditionsWithEagerOperator(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ^ b2;
			        boolean newBoolean2 = b1 == b2;
			    }

			    public void replaceDuplicateConditionsWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ^ b2;
			        boolean newBoolean2 = b1 == b2;
			    }

			    public void replaceDuplicateConditionsWithPermutedBooleans(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ^ b2;
			        boolean newBoolean2 = b1 == b2;
			    }

			    public void replaceDuplicateConditionsWithWrappers(Boolean b1, Boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ^ b2;
			        boolean newBoolean2 = b1 == b2;
			    }

			    public void replaceDuplicateConditionsWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) ^ (i3 == i4);
			        boolean newBoolean2 = (i1 == i2) == (i3 <= i4);
			        boolean newBoolean3 = (i1 == i2) == (i3 != i4);
			        boolean newBoolean4 = (i1 == i2) == (i3 < i4);
			        boolean newBoolean5 = (i1 == i2) == (i3 != i4);
			    }

			    public void replaceDuplicateConditionsWithFields() {
			        // Keep this comment
			        boolean newBoolean1 = (staticField > 0) == (staticField < 100);
			        boolean newBoolean2 = (staticField > 0) == (staticField < 100);
			    }

			    public void replaceTernaryWithPrimitiveTypes(boolean b1, boolean b2) {
			        // Keep this comment
			        boolean newBoolean1 = b1 ^ b2;
			        boolean newBoolean2 = b1 == b2;
			    }

			    public void replaceTernaryWithExpressions(int i1, int i2, int i3, int i4) {
			        // Keep this comment
			        boolean newBoolean1 = (i1 == i2) ^ (i3 == i4);
			        boolean newBoolean2 = (i1 == i2) == (i3 <= i4);
			        boolean newBoolean3 = (i1 == i2) == (i3 != i4);
			        boolean newBoolean4 = (i1 == i2) == (i3 < i4);
			    }

			    public void replaceTernaryWithFields() {
			        // Keep this comment
			        boolean newBoolean1 = (staticField > 0) == (staticField < 100);
			        boolean newBoolean2 = (staticField > 0) == (staticField < 100);
			    }
			}
			""";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StrictlyEqualOrDifferentCleanUp_description)));
	}

	@Test
	public void testDoNotUseStrictlyEqualOrDifferent() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    private static int staticField = 0;

			    public void doNoReplaceDuplicateConditionsWithOtherCondition(boolean b1, boolean b2, boolean b3) {
			        boolean newBoolean1 = b1 && !b2 || !b1 && b2 && b3;
			        boolean newBoolean2 = b1 && b2 || !b1 && !b2 && b3;
			    }

			    public void doNotReplaceDuplicateConditionsWithMethods(List<String> myList) {
			        boolean newBoolean1 = myList.remove("lorem") && !myList.remove("ipsum") || !myList.remove("lorem")
			                && myList.remove("ipsum");
			        boolean newBoolean2 = myList.remove("lorem") && myList.remove("ipsum") || !myList.remove("lorem")
			                && !myList.remove("ipsum");
			    }

			    public void doNotReplaceDuplicateConditionsWithIncrements(int i1, int i2, int i3, int i4) {
			        boolean newBoolean1 = (i1 == i2) && !(i3 == i4++) || !(i1 == i2) && (i3 == i4++);
			        boolean newBoolean2 = (i1 == i2) && !(i3 == ++i4) || !(i1 == i2) && (i3 == ++i4);
			        boolean newBoolean3 = (i1 == i2) && !(i3 == i4--) || !(i1 == i2) && (i3 == i4--);
			        boolean newBoolean4 = (i1 == i2) && !(i3 == --i4) || !(i1 == i2) && (i3 == --i4);

			        boolean newBoolean5 = (i1 == i2) && (i3 == i4++) || !(i1 == i2) && !(i3 == i4++);
			        boolean newBoolean6 = (i1 == i2) && (i3 == ++i4) || !(i1 == i2) && !(i3 == ++i4);
			        boolean newBoolean7 = (i1 == i2) && (i3 == i4--) || !(i1 == i2) && !(i3 == i4--);
			        boolean newBoolean8 = (i1 == i2) && (i3 == --i4) || !(i1 == i2) && !(i3 == --i4);
			    }

			    public void doNotReplaceDuplicateConditionsWithAssignments(int i1, int i2, boolean b1, boolean b2) {
			        boolean newBoolean1 = (i1 == i2) && !(b1 = b2) || !(i1 == i2) && (b1 = b2);
			        boolean newBoolean2 = (i1 == i2) && (b1 = b2) || !(i1 == i2) && !(b1 = b2);
			    }

			    private class SideEffect {
			        private SideEffect() {
			            staticField++;
			        }
			    }

			    public void doNotReplaceDuplicateConditionsWithInstanciations(Boolean b1) {
			        boolean newBoolean1 = b1 && !(new SideEffect() instanceof SideEffect)
			                || !b1 && new SideEffect() instanceof SideEffect;
			        boolean newBoolean2 = b1 && new SideEffect() instanceof SideEffect
			                || !b1 && !(new SideEffect() instanceof SideEffect);
			    }

			    public boolean doNotReplaceNullableObjects(Boolean booleanObject1, Boolean booleanObject2) {
			        return booleanObject1 ? booleanObject2 : !booleanObject2;
			    }

			    public void doNotReplaceTernaryWithIncrements(int i1, int i2, int i3, int i4) {
			        boolean newBoolean1 = (i1 == i2) ? !(i3 == i4++) : (i3 == i4++);
			        boolean newBoolean2 = (i1 == i2) ? !(i3 == ++i4) : (i3 == ++i4);
			        boolean newBoolean3 = (i1 == i2) ? !(i3 == i4--) : (i3 == i4--);
			        boolean newBoolean4 = (i1 == i2) ? !(i3 == --i4) : (i3 == --i4);

			        boolean newBoolean5 = (i1 == i2) ? (i3 == i4++) : !(i3 == i4++);
			        boolean newBoolean6 = (i1 == i2) ? (i3 == ++i4) : !(i3 == ++i4);
			        boolean newBoolean7 = (i1 == i2) ? (i3 == i4--) : !(i3 == i4--);
			        boolean newBoolean8 = (i1 == i2) ? (i3 == --i4) : !(i3 == --i4);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testBooleanValueRatherThanComparison() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void removeMethodCall(boolean isValid, int number) {
			        if (Boolean.TRUE.equals(isValid)) {
			            int i = 0;
			        }

			        if (Boolean.FALSE.equals(isValid)) {
			            int i = 0;
			        }

			        if (Boolean.TRUE.equals(number > 0) && isValid) {
			            int i = 0;
			        }

			        if (Boolean.FALSE.equals(number > 0) && isValid) {
			            int i = 0;
			        }
			    }

			    public boolean removeMethodCallOnObject(Boolean isValid) {
			        return isValid.equals(Boolean.TRUE);
			    }

			    public Boolean removeMethodCallOnNegativeExpression(Boolean isValid) {
			        return isValid.equals(Boolean.FALSE);
			    }

			    public void simplifyPrimitiveBooleanExpression(boolean isValid) {
			        if (isValid == true) {
			            int i = 0;
			        }

			        if (isValid != false) {
			            int i = 0;
			        }

			        if (isValid == false) {
			            int i = 0;
			        }

			        if (isValid != true) {
			            int i = 0;
			        }

			        if (isValid == Boolean.TRUE) {
			            int i = 0;
			        }

			        if (isValid != Boolean.FALSE) {
			            int i = 0;
			        }

			        if (isValid == Boolean.FALSE) {
			            int i = 0;
			        }

			        if (isValid != Boolean.TRUE) {
			            int i = 0;
			        }
			    }

			    public void removeParenthesis(boolean isValid, boolean isActive) {
			        if ((isValid == true) == isActive) {
			            int i = 0;
			        }

			        if (isActive == (isValid == true)) {
			            int i = 0;
			        }

			        if ((isValid == true) != isActive) {
			            int i = 0;
			        }

			        if (isActive != (isValid == true)) {
			            int i = 0;
			        }

			        if ((isValid == false) == isActive) {
			            int i = 0;
			        }

			        if (isActive == (isValid == false)) {
			            int i = 0;
			        }

			        if ((isValid == false) != isActive) {
			            int i = 0;
			        }

			        if (isActive != (isValid == false)) {
			            int i = 0;
			        }
			    }

			    public void simplifyBooleanWrapperExpression(Boolean isValid) {
			        if (isValid == true) {
			            int i = 0;
			        }

			        if (isValid != false) {
			            int i = 0;
			        }

			        if (isValid == false) {
			            int i = 0;
			        }

			        if (isValid != true) {
			            int i = 0;
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void removeMethodCall(boolean isValid, int number) {
			        if (isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }

			        if ((number > 0) && isValid) {
			            int i = 0;
			        }

			        if ((number <= 0) && isValid) {
			            int i = 0;
			        }
			    }

			    public boolean removeMethodCallOnObject(Boolean isValid) {
			        return isValid;
			    }

			    public Boolean removeMethodCallOnNegativeExpression(Boolean isValid) {
			        return !isValid;
			    }

			    public void simplifyPrimitiveBooleanExpression(boolean isValid) {
			        if (isValid) {
			            int i = 0;
			        }

			        if (isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }

			        if (isValid) {
			            int i = 0;
			        }

			        if (isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }
			    }

			    public void removeParenthesis(boolean isValid, boolean isActive) {
			        if (isValid == isActive) {
			            int i = 0;
			        }

			        if (isActive == isValid) {
			            int i = 0;
			        }

			        if (isValid != isActive) {
			            int i = 0;
			        }

			        if (isActive != isValid) {
			            int i = 0;
			        }

			        if (!isValid == isActive) {
			            int i = 0;
			        }

			        if (isActive == !isValid) {
			            int i = 0;
			        }

			        if (!isValid != isActive) {
			            int i = 0;
			        }

			        if (isActive != !isValid) {
			            int i = 0;
			        }
			    }

			    public void simplifyBooleanWrapperExpression(Boolean isValid) {
			        if (isValid) {
			            int i = 0;
			        }

			        if (isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }

			        if (!isValid) {
			            int i = 0;
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.BooleanValueRatherThanComparisonCleanUp_description)));
	}

	@Test
	public void testDoNotUseBooleanValueRatherThanComparison() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public void doNotRemoveMethodCallOnObjectParameter(Boolean isValid) {
			        if (Boolean.TRUE.equals(isValid)) {
			            int i = 0;
			        }

			        if (Boolean.FALSE.equals(isValid)) {
			            int i = 0;
			        }
			    }

			    public void doNotRemoveMethodCallOnObjectParameter(boolean isValid, Boolean isActive) {
			        if (isActive.equals(isValid)) {
			            int i = 0;
			        }
			    }

			    public Boolean doNotAvoidNPESkippingExpression(Boolean isValid) {
			        return isValid == true;
			    }

			    public Boolean doNotAvoidNPESkippingMethod(Boolean isValid) {
			        return isValid.equals(Boolean.TRUE);
			    }

			    public int doNotSimplifyBooleanWrapperExpression(Boolean isValid) {
			        if (isValid == Boolean.TRUE) {
			            return 1;
			        }

			        if (isValid != Boolean.FALSE) {
			            return 2;
			        }

			        if (isValid == Boolean.FALSE) {
			            return 3;
			        }

			        if (isValid != Boolean.TRUE) {
			            return 4;
			        }

			        return 0;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoubleNegation() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    public boolean reduceBooleanExpression(boolean b1, boolean b2) {
			        boolean b3 = !b1 == !b2;
			        boolean b4 = !b1 != !b2;
			        boolean b5 = !b1 ^ !b2;
			        boolean b6 = !b1 == b2;
			        boolean b7 = !b1 != b2;
			        boolean b8 = !b1 ^ b2;
			        boolean b9 = b1 == !b2;
			        boolean b10 = b1 != !b2;
			        boolean b11 = b1 ^ !b2;
			        return b3 && b4 && b5 && b6 && b7 && b8 && b9 && b10 && b11;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.DOUBLE_NEGATION);

		String output= """
			package test1;

			public class E {
			    public boolean reduceBooleanExpression(boolean b1, boolean b2) {
			        boolean b3 = b1 == b2;
			        boolean b4 = b1 ^ b2;
			        boolean b5 = b1 ^ b2;
			        boolean b6 = b1 ^ b2;
			        boolean b7 = b1 == b2;
			        boolean b8 = b1 == b2;
			        boolean b9 = b1 ^ b2;
			        boolean b10 = b1 == b2;
			        boolean b11 = b1 == b2;
			        return b3 && b4 && b5 && b6 && b7 && b8 && b9 && b10 && b11;
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.DoubleNegationCleanUp_description)));
	}

	@Test
	public void testDoNotRemoveDoubleNegation() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public boolean doNotRefactorPositiveExpression(boolean isValid, boolean isEnabled) {
			        boolean b1 = isValid == isEnabled;
			        boolean b2 = isValid != isEnabled;
			        return b1 && b2;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.DOUBLE_NEGATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRedundantComparisonStatement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.Date;
			import java.util.List;
			import java.util.Map;

			public class E {
			    private static final String DEFAULT = "";
			    private String input;

			    public String refactorLocalVariable1(String input) {
			        String output;
			        // Keep this comment
			        if (input == null) {
			            output = null;
			        } else {
			            output = /* Keep this comment too */ input;
			        }
			        return output;
			    }

			    public String refactorLocalVariable2(String input) {
			        String output;
			        // Keep this comment
			        if (null == input) {
			            output = null;
			        } else {
			            output = input;
			        }
			        return output;
			    }

			    public String refactorLocalVariable3(String input) {
			        String output;
			        // Keep this comment
			        if (input != null) {
			            output = input;
			        } else {
			            output = null;
			        }
			        return output;
			    }

			    public String refactorLocalVariable4(String input) {
			        String output;
			        // Keep this comment
			        if (null != input) {
			            output = input;
			        } else {
			            output = null;
			        }
			        return output;
			    }

			    public int removeHardCodedNumber(int input) {
			        int output;
			        // Keep this comment
			        if (123 != input) {
			            output = input;
			        } else {
			            output = 123;
			        }
			        return output;
			    }

			    public char removeHardCodedCharacter(char input) {
			        char output;
			        // Keep this comment
			        if (input == 'a') {
			            output = 'a';
			        } else {
			            output = input;
			        }
			        return output;
			    }

			    public int removeHardCodedExpression(int input) {
			        int output;
			        // Keep this comment
			        if (input != 1 + 2 + 3) {
			            output = input;
			        } else {
			            output = 3 + 2 + 1;
			        }
			        return output;
			    }

			    public String refactorLocalVariable5(String input, boolean isValid) {
			        String output = null;
			        if (isValid)
			            if (input != null) {
			                output = input;
			            } else {
			                output = null;
			            }
			        return output;
			    }

			    public void refactorFieldAssign1(String input) {
			        // Keep this comment
			        if (input == null) {
			            this.input = null;
			        } else {
			            this.input = input;
			        }
			    }

			    public void refactorFieldAssign2(String input) {
			        // Keep this comment
			        if (null == input) {
			            this.input = null;
			        } else {
			            this.input = input;
			        }
			    }

			    public void refactorFieldAssign3(String input) {
			        // Keep this comment
			        if (input != null) {
			            this.input = input;
			        } else {
			            this.input = null;
			        }
			    }

			    public void refactorFieldAssign4(String input) {
			        // Keep this comment
			        if (null != input) {
			            this.input = input;
			        } else {
			            this.input = null;
			        }
			    }

			    public String refactorReturn1(String input) {
			        // Keep this comment
			        if (input == null) {
			            return null;
			        } else {
			            return /* Keep this comment too */ input;
			        }
			    }

			    public String refactorReturn2(String input) {
			        // Keep this comment
			        if (null == input) {
			            return null;
			        } else {
			            return input;
			        }
			    }

			    public String refactorReturn3(String input) {
			        // Keep this comment
			        if (input != null) {
			            return input;
			        } else {
			            return null;
			        }
			    }

			    public String refactorReturn4(String input) {
			        // Keep this comment
			        if (null != input) {
			            return input;
			        } else {
			            return null;
			        }
			    }

			    public String refactorReturnNoElse1(String input) {
			        // Keep this comment
			        if (input == null) {
			            return null;
			        }
			        return input;
			    }

			    public String refactorReturnNoElse2(String input) {
			        // Keep this comment
			        if (null == input) {
			            return null;
			        }
			        return input;
			    }

			    public String refactorReturnNoElse3(String input) {
			        // Keep this comment
			        if (input != null) {
			            return input;
			        }
			        return null;
			    }

			    public Integer refactorReturnNoElse4(Integer number) {
			        // Keep this comment
			        if (null != number) {
			            return number;
			        }
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT);

		String expected= """
			package test1;

			import java.util.Date;
			import java.util.List;
			import java.util.Map;

			public class E {
			    private static final String DEFAULT = "";
			    private String input;

			    public String refactorLocalVariable1(String input) {
			        String output;
			        // Keep this comment
			        output = /* Keep this comment too */ input;
			        return output;
			    }

			    public String refactorLocalVariable2(String input) {
			        String output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public String refactorLocalVariable3(String input) {
			        String output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public String refactorLocalVariable4(String input) {
			        String output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public int removeHardCodedNumber(int input) {
			        int output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public char removeHardCodedCharacter(char input) {
			        char output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public int removeHardCodedExpression(int input) {
			        int output;
			        // Keep this comment
			        output = input;
			        return output;
			    }

			    public String refactorLocalVariable5(String input, boolean isValid) {
			        String output = null;
			        if (isValid)
			            output = input;
			        return output;
			    }

			    public void refactorFieldAssign1(String input) {
			        // Keep this comment
			        this.input = input;
			    }

			    public void refactorFieldAssign2(String input) {
			        // Keep this comment
			        this.input = input;
			    }

			    public void refactorFieldAssign3(String input) {
			        // Keep this comment
			        this.input = input;
			    }

			    public void refactorFieldAssign4(String input) {
			        // Keep this comment
			        this.input = input;
			    }

			    public String refactorReturn1(String input) {
			        // Keep this comment
			        return /* Keep this comment too */ input;
			    }

			    public String refactorReturn2(String input) {
			        // Keep this comment
			        return input;
			    }

			    public String refactorReturn3(String input) {
			        // Keep this comment
			        return input;
			    }

			    public String refactorReturn4(String input) {
			        // Keep this comment
			        return input;
			    }

			    public String refactorReturnNoElse1(String input) {
			        // Keep this comment
			        return input;
			    }

			    public String refactorReturnNoElse2(String input) {
			        // Keep this comment
			        return input;
			    }

			    public String refactorReturnNoElse3(String input) {
			        // Keep this comment
			        return input;
			    }

			    public Integer refactorReturnNoElse4(Integer number) {
			        // Keep this comment
			        return number;
			    }
			}
			""";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantComparisonStatementCleanup_description)));
	}

	@Test
	public void testKeepComparisonStatement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Collection;
			import java.util.Collections;
			import java.util.Date;
			import java.util.List;
			import java.util.Map;

			public class E {
			    private static final String DEFAULT = "";
			    private String input;

			    public String doNotRefactorLocalVariable(String input) {
			        String output;
			        if (input == null) {
			            output = DEFAULT;
			        } else {
			            output = input;
			        }
			        return output;
			    }

			    public String doNotRefactorConstant(String input) {
			        String output;
			        if (input != null) {
			            output = DEFAULT;
			        } else {
			            output = input;
			        }
			        return output;
			    }

			    public String doNotRefactorActiveExpression(List<String> input) {
			        String result;
			        if (input.remove(0) == null) {
			            result = null;
			        } else {
			            result = input.remove(0);
			        }
			        return result;
			    }

			    public String doNotUseConstantWithoutActiveExpression(List<String> input) {
			        String result;
			        if (input.remove(0) == null) {
			            result = null;
			        } else {
			            result = input.remove(0);
			        }
			        return result;
			    }

			    public void doNotRefactorFieldAssignXXX(String input, E other) {
			        if (input == null) {
			            this.input = null;
			        } else {
			            other.input = input;
			        }
			    }

			    public void doNotRefactorFieldAssign(String input) {
			        if (input == null) {
			            this.input = DEFAULT;
			        } else {
			            this.input = input;
			        }
			    }

			    public String doNotRefactorConstantReturn(String input) {
			        if (null != input) {
			            return input;
			        } else {
			            return DEFAULT;
			        }
			    }

			    public Collection<?> doNotRefactorDifferentReturn(Collection<?> c) {
			        if (c == null) {
			            return Collections.emptySet();
			        } else {
			            return c;
			        }
			    }

			    public Date doNotRefactorActiveAssignment(List<Date> input) {
			        Date date;
			        if (input.remove(0) != null) {
			            date = input.remove(0);
			        } else {
			            date = null;
			        }
			        return date;
			    }

			    public Date doNotRefactorActiveReturn(List<Date> input) {
			        if (input.remove(0) != null) {
			            return input.remove(0);
			        }
			        return null;
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUselessSuperCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    class A {
			        A(int a) {}

			        A() {
			            super();
			        }
			    }

			    class B extends A {
			        B(int b) {
			            super(b);
			        }

			        B() {
			            super();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		sample= """
			package test1;

			public class E1 {
			    class A {
			        A(int a) {}

			        A() {
			        }
			    }

			    class B extends A {
			        B(int b) {
			            super(b);
			        }

			        B() {
			        }
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantSuperCallCleanup_description)));
	}

	@Test
	public void testKeepSuperCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    class A {
			        A(int a) {}

			    }

			    class B extends A {
			        B(int b) {
			            super(b);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnreachableBlock() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.io.IOException;

			public class E {
			    public int removeDuplicateCondition(boolean isValid, boolean isFound) {
			        // Keep this comment
			        if (isValid && isFound) {
			            return 0;
			        } else if (isFound && isValid) {
			            return 1;
			        }

			        return 2;
			    }

			    public int removeDuplicateConditionWithElse(int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (i2 > i1) {
			            return 1;
			        } else {
			            return 2;
			        }
			    }

			    public int removeDuplicateConditionWithSeveralConditions(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            return 1;
			        } else if (i2 > i1) {
			            return 2;
			        } else {
			            return 3;
			        }
			    }

			    public int removeDuplicateConditionWithFollowingCode(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            return 1;
			        } else if (i2 > i1) {
			            return 2;
			        }

			        return 3;
			    }

			    public int removeDuplicateConditionWithoutFallingThrough(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            System.out.println("I do not fall through");
			        } else if (i2 > i1) {
			            System.out.println("I do not fall through too");
			        }

			        return 3;
			    }

			    public int removeDuplicateConditionAmongOthers(int i1, int i2) {
			        // Keep this comment
			        if (i1 == 0) {
			            return -1;
			        } else if (i1 < i2 + 1) {
			            return 0;
			        } else if (1 + i2 > i1) {
			            return 1;
			        }

			        return 2;
			    }

			    public void removeDuplicateConditionWithoutUnreachableCode(boolean isActive, boolean isFound) {
			        // Keep this comment
			        if (isActive && isFound) {
			            System.out.println("I fall through");
			            return;
			        } else if (isFound && isActive) {
			            System.out.println("I do not fall through");
			        }
			    }

			    public int removeUncaughtCode(boolean b1, boolean b2) throws IOException {
			        try {
			            // Keep this comment
			            if (b1 && b2) {
			                return 0;
			            } else if (b2 && b1) {
			                throw new IOException();
			            }
			        } catch (NullPointerException e) {
			            System.out.println("I should be reachable");
			        }

			        return 2;
			    }
			}
			""";

		String expected= """
			package test1;

			import java.io.IOException;

			public class E {
			    public int removeDuplicateCondition(boolean isValid, boolean isFound) {
			        // Keep this comment
			        if (isValid && isFound) {
			            return 0;
			        }

			        return 2;
			    }

			    public int removeDuplicateConditionWithElse(int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else {
			            return 2;
			        }
			    }

			    public int removeDuplicateConditionWithSeveralConditions(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            return 1;
			        } else {
			            return 3;
			        }
			    }

			    public int removeDuplicateConditionWithFollowingCode(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            return 1;
			        }

			        return 3;
			    }

			    public int removeDuplicateConditionWithoutFallingThrough(boolean isActive, int i1, int i2) {
			        // Keep this comment
			        if (i1 < i2) {
			            return 0;
			        } else if (isActive) {
			            System.out.println("I do not fall through");
			        }

			        return 3;
			    }

			    public int removeDuplicateConditionAmongOthers(int i1, int i2) {
			        // Keep this comment
			        if (i1 == 0) {
			            return -1;
			        } else if (i1 < i2 + 1) {
			            return 0;
			        }

			        return 2;
			    }

			    public void removeDuplicateConditionWithoutUnreachableCode(boolean isActive, boolean isFound) {
			        // Keep this comment
			        if (isActive && isFound) {
			            System.out.println("I fall through");
			            return;
			        }
			    }

			    public int removeUncaughtCode(boolean b1, boolean b2) throws IOException {
			        try {
			            // Keep this comment
			            if (b1 && b2) {
			                return 0;
			            }
			        } catch (NullPointerException e) {
			            System.out.println("I should be reachable");
			        }

			        return 2;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.UNREACHABLE_BLOCK);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.UnreachableBlockCleanUp_description)));
	}

	@Test
	public void testKeepUnreachableBlock() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.io.IOException;
			import java.util.List;

			public class E {
			    public String doNotCreateUnreachable(int i1, int i2) {
			        if (i1 < i2) {
			            return "Falls through";
			        } else if (i2 > i1) {
			            System.out.println("Does not fall through");
			        } else {
			            return "Falls through too";
			        }

			        return "I should be reachable";
			    }

			    public String doNotCreateUnreachableOverSeveralConditions(boolean isEnabled, int i1, int i2) {
			        if (i1 < i2) {
			            return "Falls through";
			        } else if (isEnabled) {
			            return "Falls through too";
			        } else if (i2 > i1) {
			            System.out.println("Does not fall through");
			        } else {
			            return "Falls through also";
			        }

			        return "I should be reachable";
			    }

			    public int doNotRemoveDifferentCondition(boolean b1, boolean b2) {
			        if (b1 && b2) {
			            return 0;
			        } else if (b2 || b1) {
			            return 1;
			        }

			        return 2;
			    }

			    public int doNotRemoveActiveCondition(List<String> myList) {
			        if (myList.remove("I will be removed")) {
			            return 0;
			        } else if (myList.remove("I will be removed")) {
			            return 1;
			        }

			        return 2;
			    }

			    public String doNotRemoveConditionPrecededByActiveCondition(int number) {
			        if (number > 0) {
			            return "Falls through";
			        } else if (number++ == 42) {
			            return "Active condition";
			        } else if (number > 0) {
			            return "Falls through too";
			        } else {
			            return "Falls through also";
			        }
			    }

			    public int doNotRemoveCaughtCode(boolean b1, boolean b2) {
			        try {
			            if (b1 && b2) {
			                return 0;
			            } else if (b2 && b1) {
			                throw new IOException();
			            }
			        } catch (IOException e) {
			            System.out.println("I should be reachable");
			        }

			        return 2;
			    }

			    public int doNotRemoveThrowingExceptionCode(boolean isValid, int number) {
			        if (isValid || true) {
			            return 0;
			        } else if (isValid || true || ((number / 0) == 42)) {
			            return 1;
			        }

			        return 2;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.UNREACHABLE_BLOCK);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testMergeConditionalBlocks() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {

			    /** Duplicate if and else if code, merge it */
			    public void duplicateIfAndElseIf(int i) {
			        // Keep this comment
			        if (i == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else if (i == 123) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void mergeTwoStructures(int a, int b) {
			        // Keep this comment
			        if (a == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (a + 1));
			        } else if (a == 1) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (1 + a));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }

			        // Keep this comment
			        if (b == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (a + 123 + 0));
			        } else if (b == 1) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (a + 123));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else code, merge it */
			    public void duplicateIfAndElse(int j) {
			        // Keep this comment
			        if (j == 0) {
			            // Keep this comment too
			            if (j > 0) {
			                System.out.println("Duplicate");
			            } else {
			                System.out.println("Duplicate too");
			            }
			        } else if (j == 1) {
			            // Keep this comment also
			            System.out.println("Different");
			        } else {
			            // Keep this comment too
			            if (j <= 0) {
			                System.out.println("Duplicate too");
			            } else {
			                System.out.println("Duplicate");
			            }
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void duplicateIfAndElseIfWithoutElse(int k) {
			        // Keep this comment
			        if (k == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate" + !(k == 0));
			        } else if (k == 1) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (k != 0));
			        }
			    }

			    /** Duplicate else if codes, merge it */
			    public void duplicateIfAndElseIfAmongOther(int m) {
			        // Keep this comment
			        if (m == 0) {
			            // Keep this comment too
			            System.out.println("A given code");
			        } if (m == 1) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (m > 0));
			        } else if (m == 2) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (0 < m));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void duplicateSingleStatement(int n) {
			        // Keep this comment
			        if (n == 0)
			            // Keep this comment too
			            if (n > 0) {
			                System.out.println("Duplicate");
			            } else {
			                System.out.println("Duplicate too");
			            }
			        else if (n == 1)
			            // Keep this comment too
			            if (n > 0)
			                System.out.println("Duplicate");
			            else
			                System.out.println("Duplicate too");
			        else
			            // Keep this comment also
			            System.out.println("Different");
			    }

			    /** Duplicate if and else if code, merge it */
			    public void numerousDuplicateIfAndElseIf(int o) {
			        // Keep this comment
			        if (o == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else if (o == 1) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else if (o == 2)
			            // Keep this comment too
			            System.out.println("Duplicate");
			        else if (o == 3) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void complexIfAndElseIf(int p) {
			        // Keep this comment
			        if (p == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate ");
			        } else if (p == 1 || p == 2) {
			            // Keep this comment too
			            System.out.println("Duplicate ");
			        } else if (p > 10) {
			            // Keep this comment too
			            System.out.println("Duplicate ");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void longIfAndElseIf(int q, boolean isValid) {
			        // Keep this comment
			        if (q == 0) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			            q = q + 1;
			        } else if (isValid ? (q == 1) : (q > 1)) {
			            // Keep this comment also
			            System.out.println("Different");
			        } else {
			            // Keep this comment too
			            System.out.println("Duplicate");
			            q++;
			        }
			    }

			    public void collapseIfStatements(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid) {
			                // Keep this comment also
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseInnerElse(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid) {
			                System.out.println("Duplicate code");
			            } else {
			                // Keep this comment also
			                int j = 0;
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseLoneIfStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid)
			                texts.clear();
			            else
			                System.out.println("Duplicate code");
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseCommentedLoneIfStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid)
			                texts.clear(); // Keep this comment too
			            else
			                System.out.println("Duplicate code");
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseWithCommentedElse(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid) {
			                texts.clear();
			            } else
			                System.out.println("Duplicate code"); // Keep this comment too
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseCommentedStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid)
			                texts.clear(); // Keep this comment too
			            else {
			                System.out.println("Duplicate code");
			            }
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseLoneStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid)
			                texts.clear();
			            else
			                System.out.println("Duplicate code");
			        else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseInnerLoneStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive) {
			            if (isValid)
			                texts.clear();
			            else
			                System.out.println("Duplicate code");
			        } else
			            System.out.println("Duplicate code");
			    }

			    public void collapseWithLoneElseStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive)
			            if (isValid) {
			                texts.clear();
			            } else
			                System.out.println("Duplicate code");
			        else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseWithFourOperands(int i1, int i2) {
			        // Keep this comment
			        if (0 < i1 && i1 < 10) {
			            // Keep this comment too
			            if (0 < i2 && i2 < 10) {
			                // Keep this comment also
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid,
			            boolean isEditMode) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid || isEditMode) {
			                // Keep this comment also
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid | isEditMode) {
			                // Keep this comment also
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void mergeLongDuplicateCode(boolean isActive, boolean isValid, int number) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid) {
			                // Keep this comment also
			                int i = 0;
			            } else {
			                int j = number + 123;
			                System.out.println((j == 0) ? "Duplicate" : "code");
			            }
			        } else {
			            int j = 123 + number;
			            System.out.println((0 != j) ? "code" : "Duplicate");
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {

			    /** Duplicate if and else if code, merge it */
			    public void duplicateIfAndElseIf(int i) {
			        // Keep this comment
			        if ((i == 0) || (i == 123)) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void mergeTwoStructures(int a, int b) {
			        // Keep this comment
			        if ((a == 0) || (a == 1)) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (a + 1));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }

			        // Keep this comment
			        if ((b == 0) || (b == 1)) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (a + 123 + 0));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else code, merge it */
			    public void duplicateIfAndElse(int j) {
			        // Keep this comment
			        if ((j == 0) || (j != 1)) {
			            // Keep this comment too
			            if (j > 0) {
			                System.out.println("Duplicate");
			            } else {
			                System.out.println("Duplicate too");
			            }
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void duplicateIfAndElseIfWithoutElse(int k) {
			        // Keep this comment
			        if ((k == 0) || (k == 1)) {
			            // Keep this comment too
			            System.out.println("Duplicate" + !(k == 0));
			        }
			    }

			    /** Duplicate else if codes, merge it */
			    public void duplicateIfAndElseIfAmongOther(int m) {
			        // Keep this comment
			        if (m == 0) {
			            // Keep this comment too
			            System.out.println("A given code");
			        } if ((m == 1) || (m == 2)) {
			            // Keep this comment too
			            System.out.println("Duplicate" + (m > 0));
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void duplicateSingleStatement(int n) {
			        // Keep this comment
			        if ((n == 0) || (n == 1))
			            // Keep this comment too
			            if (n > 0) {
			                System.out.println("Duplicate");
			            } else {
			                System.out.println("Duplicate too");
			            }
			        else
			            // Keep this comment also
			            System.out.println("Different");
			    }

			    /** Duplicate if and else if code, merge it */
			    public void numerousDuplicateIfAndElseIf(int o) {
			        // Keep this comment
			        if ((o == 0) || (o == 1) || (o == 2)
			                || (o == 3)) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void complexIfAndElseIf(int p) {
			        // Keep this comment
			        if ((p == 0) || (p == 1 || p == 2) || (p > 10)) {
			            // Keep this comment too
			            System.out.println("Duplicate ");
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    /** Duplicate if and else if code, merge it */
			    public void longIfAndElseIf(int q, boolean isValid) {
			        // Keep this comment
			        if ((q == 0) || (isValid ? (q != 1) : (q <= 1))) {
			            // Keep this comment too
			            System.out.println("Duplicate");
			            q = q + 1;
			        } else {
			            // Keep this comment also
			            System.out.println("Different");
			        }
			    }

			    public void collapseIfStatements(boolean isActive, boolean isValid) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && isValid) {
			            // Keep this comment also
			            int i = 0;
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseInnerElse(boolean isActive, boolean isValid) {
			        // Keep this comment
			        // Keep this comment too
			        if (!isActive || isValid) {
			            System.out.println("Duplicate code");
			        } else {
			            // Keep this comment also
			            int j = 0;
			        }
			    }

			    public void collapseLoneIfStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid)
			            texts.clear();
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseCommentedLoneIfStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid)
			            texts.clear(); // Keep this comment too
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseWithCommentedElse(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid) {
			            texts.clear();
			        } else
			            System.out.println("Duplicate code"); // Keep this comment too
			    }

			    public void collapseCommentedStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid)
			            texts.clear(); // Keep this comment too
			        else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseLoneStatements(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid)
			            texts.clear();
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseInnerLoneStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid)
			            texts.clear();
			        else
			            System.out.println("Duplicate code");
			    }

			    public void collapseWithLoneElseStatement(boolean isActive, boolean isValid, List<String> texts) {
			        // Keep this comment
			        if (isActive && isValid) {
			            texts.clear();
			        } else
			            System.out.println("Duplicate code");
			    }

			    public void collapseWithFourOperands(int i1, int i2) {
			        // Keep this comment
			        // Keep this comment too
			        if ((0 < i1 && i1 < 10) && (0 < i2 && i2 < 10)) {
			            // Keep this comment also
			            int i = 0;
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid,
			            boolean isEditMode) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && (isValid || isEditMode)) {
			            // Keep this comment also
			            int i = 0;
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && (isValid | isEditMode)) {
			            // Keep this comment also
			            int i = 0;
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void mergeLongDuplicateCode(boolean isActive, boolean isValid, int number) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && isValid) {
			            // Keep this comment also
			            int i = 0;
			        } else {
			            int j = number + 123;
			            System.out.println((j == 0) ? "Duplicate" : "code");
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.MergeConditionalBlocksCleanup_description_if_suite, MultiFixMessages.MergeConditionalBlocksCleanup_description_inner_if)));
	}

	@Test
	public void testDoNotMergeConditionalBlocks() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    /** 5 operands, not easily readable */
			    public void doNotMergeMoreThanFourOperands(int i) {
			        if ((i == 0) || (i == 1 || i == 2)) {
			            System.out.println("Duplicate ");
			        } else if (i > 10 && i < 100) {
			            System.out.println("Duplicate ");
			        } else {
			            System.out.println("Different");
			        }
			    }

			    /** Different if and else if code, leave it */
			    public void doNotMergeAdditionalCode(int i) {
			        if (i == 0) {
			            System.out.println("Duplicate");
			        } else if (i == 1) {
			            System.out.println("Duplicate");
			            System.out.println("but not only");
			        } else {
			            System.out.println("Different");
			        }
			    }

			    /** Different code in the middle, leave it */
			    public void doNotMergeIntruderCode(int i) {
			        if (i == 0) {
			            System.out.println("Duplicate");
			        } else if (i == 1) {
			            System.out.println("Intruder");
			        } else if (i == 2) {
			            System.out.println("Duplicate");
			        } else {
			            System.out.println("Different");
			        }
			    }

			    public void doNotCollapseIfStatementsWithAdditionalStatement(boolean isActive, boolean isValid) {
			        if (isActive) {
			            if (isValid) {
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			            System.out.println("Hi!");
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void doNotCollapseWithFiveOperands(int number1, int number2) {
			        if (0 < number1 && number1 < 10) {
			            if (100 < number2 && number2 < 200 || number2 < 0) {
			                int i = 0;
			            } else {
			                System.out.println("Duplicate code");
			            }
			        } else {
			            System.out.println("Duplicate code");
			        }
			    }

			    public void doNotMergeDifferentCode(boolean isActive, boolean isValid) {
			        if (isActive) {
			            if (isValid) {
			                int i = 0;
			            } else {
			                System.out.println("One code");
			            }
			        } else {
			            System.out.println("Another code");
			        }
			    }

			    public void doNotMergeEmptyCode(boolean isActive, boolean isValid) {
			        if (isActive) {
			            if (isValid) {
			                int i = 0;
			            } else {
			            }
			        } else {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRedundantFallingThroughBlockEnd() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.util.List;

			public class E {
			    private boolean b= true;

			    public void mergeIfBlocksIntoFollowingCode(int i) {
			        // Keep this comment
			        if (i <= 0) {
			            System.out.println("Doing something");
			            return;
			        } else if (i == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (i == 20) {
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public int mergeUselessElse(int i) {
			        // Keep this comment
			        if (i <= 0) {
			            System.out.println("Doing another thing");
			        } else return i + 123;
			        return i + 123;
			    }

			    public char mergeIfStatementIntoFollowingCode(int j) {
			        // Keep this comment
			        if (j <= 0) return 'a';
			        else if (j == 10) return 'b';
			        else if (j == 20) return 'a';
			        return 'a';
			    }

			    public void mergeEndOfIfIntoFollowingCode(int k) {
			        // Keep this comment
			        if (k <= 0) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        } else if (k == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (k == 20) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeWithoutContinue(int i, int j) {
			        while (j-- > 0) {
			            // Keep this comment
			            if (i <= 0) {
			                System.out.println("Doing another thing");
			                System.out.println("Doing something");
			                continue;
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                continue;
			            } else if (i == 20) {
			                System.out.println("Doing another thing");
			                System.out.println("Doing something");
			                continue;
			            }
			            System.out.println("Doing something");
			        }
			    }

			    public void mergeWithoutReturn(int n) {
			        // Keep this comment
			        if (n <= 0) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        } else if (n == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (n == 20) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			    }

			    public void mergeIfThrowingException(int i) throws Exception {
			        // Keep this comment
			        if (i <= 0) {
			            i += 42;
			            System.out.println("Doing something");
			            throw new Exception();
			        } else if (i == 10) {
			            i += 42;
			            System.out.println("Doing another thing");
			            throw new Exception();
			        } else if (i == 20) {
			            i += 42;
			            System.out.println("Doing something");
			            throw new Exception();
			        }
			        i = i + 42;
			        System.out.println("Doing something");
			        throw new Exception();
			    }

			    public void mergeDeepStatements(String number, int i) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            if (i <= 0) {
			                i += 42;
			                System.out.println("Doing something");
			                return;
			            } else if (i == 10) {
			                i += 42;
			                System.out.println("Doing another thing");
			                return;
			            } else if (i == 20) {
			                i += 42;
			                System.out.println("Doing something");
			                return;
			            }
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeIfWithContinue(int[] numbers) {
			        for (int i : numbers) {
			            // Keep this comment
			            if (i <= 0) {
			                System.out.println("Doing something");
			                continue;
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                continue;
			            } else if (i == 20) {
			                System.out.println("Doing something");
			                continue;
			            }
			            System.out.println("Doing something");
			            continue;
			        }
			    }

			    public void mergeIfWithBreak(int[] numbers) {
			        for (int i : numbers) {
			            // Keep this comment
			            if (i <= 0) {
			                System.out.println("Doing something");
			                break;
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                break;
			            } else if (i == 20) {
			                System.out.println("Doing something");
			                break;
			            }
			            System.out.println("Doing something");
			            break;
			        }
			    }

			    public void mergeIfThatAlwaysFallThrough(int i, boolean interruptCode) throws Exception {
			        // Keep this comment
			        if (i <= 0) {
			            i++;
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } else if (i == 10) {
			            i += 1;
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } else if (i == 20) {
			            i = 1 + i;
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        }
			        i = i + 1;
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        } else {
			            return;
			        }
			    }

			    public void mergeCatchIntoFollowingCode(String number) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			            return;
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeEndOfCatchIntoFollowingCode(String number) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			            System.out.println("Doing another thing");
			            System.out.println("Doing something");
			            return;
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeCatchThrowingException(String number) throws Exception {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			            throw new Exception();
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            throw new Exception();
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            throw new Exception();
			        }
			        System.out.println("Doing something");
			        throw new Exception();
			    }

			    public void mergeCatchWithContinue(List<String> numbers) {
			        for (String number : numbers) {
			            // Keep this comment
			            try {
			                Integer.valueOf(number);
			            } catch (NumberFormatException nfe) {
			                System.out.println("Doing something");
			                continue;
			            } catch (IllegalArgumentException iae) {
			                System.out.println("Doing another thing");
			                continue;
			            } catch (NullPointerException npe) {
			                System.out.println("Doing something");
			                continue;
			            }
			            System.out.println("Doing something");
			            continue;
			        }
			    }

			    public void mergeCatchWithBreak(List<String> numbers) {
			        for (String number : numbers) {
			            // Keep this comment
			            try {
			                Integer.valueOf(number);
			            } catch (NumberFormatException nfe) {
			                System.out.println("Doing something");
			                break;
			            } catch (IllegalArgumentException iae) {
			                System.out.println("Doing another thing");
			                break;
			            } catch (NullPointerException npe) {
			                System.out.println("Doing something");
			                break;
			            }
			            System.out.println("Doing something");
			            break;
			        }
			    }

			    public void mergeCatchThatAlwaysFallThrough(String number, boolean interruptCode) throws Exception {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            if (!interruptCode) {
			                return;
			            } else {
			                throw new Exception("Stop!");
			            }
			        }
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        } else {
			            return;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END);

		String output= """
			package test1;

			import java.util.List;

			public class E {
			    private boolean b= true;

			    public void mergeIfBlocksIntoFollowingCode(int i) {
			        // Keep this comment
			        if (i <= 0) {
			        } else if (i == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (i == 20) {
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public int mergeUselessElse(int i) {
			        // Keep this comment
			        if (i <= 0) {
			            System.out.println("Doing another thing");
			        }
			        return i + 123;
			    }

			    public char mergeIfStatementIntoFollowingCode(int j) {
			        // Keep this comment
			        if (j <= 0) {
			        } else if (j == 10) return 'b';
			        else if (j == 20) {
			        }
			        return 'a';
			    }

			    public void mergeEndOfIfIntoFollowingCode(int k) {
			        // Keep this comment
			        if (k <= 0) {
			            System.out.println("Doing another thing");
			        } else if (k == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (k == 20) {
			            System.out.println("Doing another thing");
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeWithoutContinue(int i, int j) {
			        while (j-- > 0) {
			            // Keep this comment
			            if (i <= 0) {
			                System.out.println("Doing another thing");
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                continue;
			            } else if (i == 20) {
			                System.out.println("Doing another thing");
			            }
			            System.out.println("Doing something");
			        }
			    }

			    public void mergeWithoutReturn(int n) {
			        // Keep this comment
			        if (n <= 0) {
			            System.out.println("Doing another thing");
			        } else if (n == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (n == 20) {
			            System.out.println("Doing another thing");
			        }
			        System.out.println("Doing something");
			    }

			    public void mergeIfThrowingException(int i) throws Exception {
			        // Keep this comment
			        if (i <= 0) {
			        } else if (i == 10) {
			            i += 42;
			            System.out.println("Doing another thing");
			            throw new Exception();
			        } else if (i == 20) {
			        }
			        i = i + 42;
			        System.out.println("Doing something");
			        throw new Exception();
			    }

			    public void mergeDeepStatements(String number, int i) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            if (i <= 0) {
			                i += 42;
			            } else if (i == 10) {
			                i += 42;
			                System.out.println("Doing another thing");
			                return;
			            } else if (i == 20) {
			                i += 42;
			            }
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeIfWithContinue(int[] numbers) {
			        for (int i : numbers) {
			            // Keep this comment
			            if (i <= 0) {
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                continue;
			            } else if (i == 20) {
			            }
			            System.out.println("Doing something");
			            continue;
			        }
			    }

			    public void mergeIfWithBreak(int[] numbers) {
			        for (int i : numbers) {
			            // Keep this comment
			            if (i <= 0) {
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                break;
			            } else if (i == 20) {
			            }
			            System.out.println("Doing something");
			            break;
			        }
			    }

			    public void mergeIfThatAlwaysFallThrough(int i, boolean interruptCode) throws Exception {
			        // Keep this comment
			        if (i <= 0) {
			        } else if (i == 10) {
			            i += 1;
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } else if (i == 20) {
			        }
			        i = i + 1;
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        } else {
			            return;
			        }
			    }

			    public void mergeCatchIntoFollowingCode(String number) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeEndOfCatchIntoFollowingCode(String number) {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing another thing");
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            return;
			        } catch (NullPointerException npe) {
			            System.out.println("Doing another thing");
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void mergeCatchThrowingException(String number) throws Exception {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            throw new Exception();
			        } catch (NullPointerException npe) {
			        }
			        System.out.println("Doing something");
			        throw new Exception();
			    }

			    public void mergeCatchWithContinue(List<String> numbers) {
			        for (String number : numbers) {
			            // Keep this comment
			            try {
			                Integer.valueOf(number);
			            } catch (NumberFormatException nfe) {
			            } catch (IllegalArgumentException iae) {
			                System.out.println("Doing another thing");
			                continue;
			            } catch (NullPointerException npe) {
			            }
			            System.out.println("Doing something");
			            continue;
			        }
			    }

			    public void mergeCatchWithBreak(List<String> numbers) {
			        for (String number : numbers) {
			            // Keep this comment
			            try {
			                Integer.valueOf(number);
			            } catch (NumberFormatException nfe) {
			            } catch (IllegalArgumentException iae) {
			                System.out.println("Doing another thing");
			                break;
			            } catch (NullPointerException npe) {
			            }
			            System.out.println("Doing something");
			            break;
			        }
			    }

			    public void mergeCatchThatAlwaysFallThrough(String number, boolean interruptCode) throws Exception {
			        // Keep this comment
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            } else {
			                return;
			            }
			        } catch (NullPointerException npe) {
			        }
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        } else {
			            return;
			        }
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantFallingThroughBlockEndCleanup_description)));
	}

	@Test
	public void testDoNotMergeRedundantFallingThroughBlockEnd() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    private boolean b= true;

			    public void doNotMergeDifferentVariable(int i) {
			        if (i <= 0) {
			            boolean b= false;
			            System.out.println("Display a varaible: " + b);
			            return;
			        } else if (i == 10) {
			            System.out.println("Doing another thing");
			            return;
			        } else if (i == 20) {
			            int b= 123;
			            System.out.println("Display a varaible: " + b);
			            return;
			        }
			        System.out.println("Display a varaible: " + b);
			        return;
			    }

			    public void doNotMergeWithoutLabeledContinue(int i, int j, int k) {
			        loop: while (k-- > 0) {
			            while (j-- > 0) {
			                if (i <= 0) {
			                    System.out.println("Doing another thing");
			                    System.out.println("Doing something");
			                    continue loop;
			                } else if (i == 10) {
			                    System.out.println("Doing another thing");
			                    continue loop;
			                } else if (i == 20) {
			                    System.out.println("Doing another thing");
			                    System.out.println("Doing something");
			                    continue loop;
			                }
			                System.out.println("Doing something");
			            }
			        }
			    }

			    public void doNotMergeWithoutBreak(int i, int j) {
			        while (j-- > 0) {
			            if (i <= 0) {
			                System.out.println("Doing another thing");
			                System.out.println("Doing something");
			                break;
			            } else if (i == 10) {
			                System.out.println("Doing another thing");
			                break;
			            } else if (i == 20) {
			                System.out.println("Doing another thing");
			                System.out.println("Doing something");
			                break;
			            }
			            System.out.println("Doing something");
			        }
			    }

			    public void doNotRefactorCodeThatDoesntFallThrough(int m) {
			        if (m <= 0) {
			            System.out.println("Doing something");
			        } else if (m == 20) {
			            System.out.println("Doing something");
			        }
			        System.out.println("Doing something");
			    }

			    public void doNotRefactorNotLastStatements(String number, int n) {
			        if (n > 0) {
			            try {
			                Integer.valueOf(number);
			            } catch (NumberFormatException nfe) {
			                if (n == 5) {
			                    n += 42;
			                    System.out.println("Doing something");
			                    return;
			                } else if (n == 10) {
			                    n += 42;
			                    System.out.println("Doing another thing");
			                    return;
			                } else if (n == 20) {
			                    n += 42;
			                    System.out.println("Doing something");
			                    return;
			                }
			            } catch (IllegalArgumentException iae) {
			                System.out.println("Doing another thing");
			                return;
			            } catch (NullPointerException npe) {
			                System.out.println("Doing something");
			                return;
			            }
			            System.out.println("Insidious code...");
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void doNotMergeIfThatNotAlwaysFallThrough(int i, boolean interruptCode) throws Exception {
			        if (i <= 0) {
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        } else if (i == 10) {
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        } else if (i == 20) {
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        }
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        }
			    }

			    public void doNotRefactorWithFinally(String number) {
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			            return;
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            return;
			        } finally {
			            System.out.println("Beware of finally!");
			        }
			        System.out.println("Doing something");
			        return;
			    }

			    public void doNotRefactorCodeThatDoesntFallThrough(String number) {
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			        }
			        System.out.println("Doing something");
			    }

			    public void doNotMergeCatchThatNotAlwaysFallThrough(String number, boolean interruptCode) throws Exception {
			        try {
			            Integer.valueOf(number);
			        } catch (NumberFormatException nfe) {
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        } catch (IllegalArgumentException iae) {
			            System.out.println("Doing another thing");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        } catch (NullPointerException npe) {
			            System.out.println("Doing something");
			            if (interruptCode) {
			                throw new Exception("Stop!");
			            }
			        }
			        System.out.println("Doing something");
			        if (interruptCode) {
			            throw new Exception("Stop!");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRedundantIfCondition() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.io.IOException;
			import java.util.List;

			public class E {
			    public int removeOppositeCondition(boolean b1, boolean b2) {
			        int i = -1;
			        // Keep this comment
			        if (b1 && b2) {
			            i = 0;
			        } else if (!b2 || !b1) {
			            i = 1;
			        }

			        return i;
			    }

			    public int removeOppositeConditionWithElse(int i1, int i2) {
			        int i = -1;
			        // Keep this comment
			        if (i1 < i2) {
			            i = 0;
			        } else if (i2 <= i1) {
			            i = 1;
			        } else {
			            i = 2;
			        }

			        return i;
			    }

			    public int removeOppositeConditionAmongOthers(int i1, int i2) {
			        int i = -1;
			        // Keep this comment
			        if (i1 == 0) {
			            i = -1;
			        } else if (i1 < i2 + 1) {
			            i = 0;
			        } else if (1 + i2 <= i1) {
			            i = 1;
			        }

			        return i;
			    }

			    public int refactorCaughtCode(boolean b1, boolean b2) {
			        int i = -1;
			        try {
			            // Keep this comment
			            if (b1 && b2) {
			                i = 0;
			            } else if (!b2 || !b1) {
			                throw new IOException();
			            }
			        } catch (IOException e) {
			            System.out.println("I should be reachable");
			        }

			        return i;
			    }

			    public int removeUncaughtCode(boolean b1, boolean b2) {
			        int i = -1;
			        try {
			            // Keep this comment
			            if (!b1 == b2) {
			                i = 0;
			            } else if (!b2 != b1) {
			                i = 1;
			            } else {
			                throw new NullPointerException();
			            }
			        } finally {
			            System.out.println("I should be reachable");
			        }

			        return i;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.REDUNDANT_IF_CONDITION);

		String output= """
			package test1;

			import java.io.IOException;
			import java.util.List;

			public class E {
			    public int removeOppositeCondition(boolean b1, boolean b2) {
			        int i = -1;
			        // Keep this comment
			        if (b1 && b2) {
			            i = 0;
			        } else {
			            i = 1;
			        }

			        return i;
			    }

			    public int removeOppositeConditionWithElse(int i1, int i2) {
			        int i = -1;
			        // Keep this comment
			        if (i1 < i2) {
			            i = 0;
			        } else {
			            i = 1;
			        }

			        return i;
			    }

			    public int removeOppositeConditionAmongOthers(int i1, int i2) {
			        int i = -1;
			        // Keep this comment
			        if (i1 == 0) {
			            i = -1;
			        } else if (i1 < i2 + 1) {
			            i = 0;
			        } else {
			            i = 1;
			        }

			        return i;
			    }

			    public int refactorCaughtCode(boolean b1, boolean b2) {
			        int i = -1;
			        try {
			            // Keep this comment
			            if (b1 && b2) {
			                i = 0;
			            } else {
			                throw new IOException();
			            }
			        } catch (IOException e) {
			            System.out.println("I should be reachable");
			        }

			        return i;
			    }

			    public int removeUncaughtCode(boolean b1, boolean b2) {
			        int i = -1;
			        try {
			            // Keep this comment
			            if (!b1 == b2) {
			                i = 0;
			            } else {
			                i = 1;
			            }
			        } finally {
			            System.out.println("I should be reachable");
			        }

			        return i;
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantIfConditionCleanup_description)));
	}

	@Test
	public void testKeepIfCondition() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.io.IOException;
			import java.util.List;

			public class E {
			    public int doNotRemoveDifferentCondition(boolean b1, boolean b2) {
			        int i = -1;
			        if (b1 && b2) {
			            i = 0;
			        } else if (b2 || b1) {
			            i = 1;
			        }

			        return i;
			    }

			    public int doNotRemoveMovedOperands(int number1, int number2) {
			        int i = -1;
			        if (number1 < number2) {
			            i = 0;
			        } else if (number2 < number1) {
			            i = 1;
			        }

			        return i;
			    }

			    public int doNotHandleObjectsThatCanBeNull(Boolean isValid) {
			        int i = -1;
			        if (isValid == Boolean.TRUE) {
			            i = 0;
			        } else if (isValid == Boolean.FALSE) {
			            i = 1;
			        }

			        return i;
			    }

			    public int doNotRemoveActiveCondition(List<String> myList) {
			        int i = -1;
			        if (myList.remove("I will be removed")) {
			            i = 0;
			        } else if (myList.remove("I will be removed")) {
			            i = 1;
			        }

			        return i;
			    }

			    public int doNotRemoveCaughtCode(boolean b1, boolean b2) {
			        int i = -1;
			        try {
			            if (b1 && b2) {
			                i = 0;
			            } else if (!b2 || !b1) {
			                i = 1;
			            } else {
			                throw new IOException();
			            }
			        } catch (IOException e) {
			            System.out.println("I should be reachable");
			        }

			        return i;
			    }

			    public int doNotRefactorFallThroughBlocks(boolean b1, boolean b2) {
			        if (b1 && b2) {
			            return 0;
			        } else if (!b2 || !b1) {
			            return 1;
			        }

			        return 2;
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_IF_CONDITION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testPullOutIfFromIfElse() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void refactorCommonInnerIf(boolean b1, boolean b2) throws Exception {
			        // Keep this comment
			        if (b1) {
			            if (b2) {
			                // Keep this comment too
			                System.out.println(b1);
			            }
			        } else {
			            if (b2) {
			                // Keep this comment too
			                System.out.println(!b1);
			            }
			        }
			    }

			    public void refactorWithoutBrackets(boolean isValid, boolean isCreation) {
			        if (isValid) {
			            if (isCreation) {
			                // Keep this comment
			                System.out.println(isValid);
			            }
			        } else if (isCreation) {
			            // Keep this comment
			            System.out.println(!isValid);
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void refactorCommonInnerIf(boolean b1, boolean b2) throws Exception {
			        // Keep this comment
			        if (b2) {
			            if (b1) {
			                // Keep this comment too
			                System.out.println(b1);
			            } else {
			                // Keep this comment too
			                System.out.println(!b1);
			            }
			        }
			    }

			    public void refactorWithoutBrackets(boolean isValid, boolean isCreation) {
			        if (isCreation) {
			            if (isValid) {
			                // Keep this comment
			                System.out.println(isValid);
			            } else {
			                // Keep this comment
			                System.out.println(!isValid);
			            }
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.PullOutIfFromIfElseCleanUp_description)));
	}

	@Test
	public void testDoNotPullOutIfFromIfElse() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E {
			    public void doNotRefactorBecauseOfInnerElse1(boolean isActive, boolean isFound) throws Exception {
			        if (isActive) {
			            if (isFound) {
			                System.out.println(isFound);
			            } else {
			                System.out.println(isActive);
			            }
			        } else {
			            if (isFound) {
			                System.out.println(!isActive);
			            }
			        }
			    }

			    public void doNotRefactorBecauseOfInnerElse2(boolean isActive, boolean isFound) throws Exception {
			        if (isActive) {
			            if (isFound) {
			                System.out.println(isActive);
			            }
			        } else {
			            if (isFound) {
			                System.out.println(isFound);
			            } else {
			                System.out.println(!isActive);
			            }
			        }
			    }

			    public void doNotRefactorActiveCondition(List<String> myList) throws Exception {
			        if (myList.remove("lorem")) {
			            if (myList.isEmpty()) {
			                System.out.println("Now empty");
			            }
			        } else {
			            if (myList.isEmpty()) {
			                System.out.println("Still empty");
			            }
			        }
			    }

			    public void doNotRefactorAssignment(boolean isActive, boolean isFound) throws Exception {
			        if (isFound = isActive) {
			            if (isFound) {
			                System.out.println(isActive);
			            }
			        } else {
			            if (isFound) {
			                System.out.println(!isActive);
			            }
			        }
			    }

			    public void doNotRefactorPostincrement(int i1, int i2) throws Exception {
			        if (i1 == i2++) {
			            if (i2 == 0) {
			                System.out.println(i1);
			            }
			        } else {
			            if (i2 == 0) {
			                System.out.println(-i1);
			            }
			        }
			    }

			    public void doNotRefactorPreincrement(int i1, int i2) throws Exception {
			        if (i1 == ++i2) {
			            if (i2 == 0) {
			                System.out.println(i1);
			            }
			        } else {
			            if (i2 == 0) {
			                System.out.println(-i1);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRedundantComparator() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.math.BigDecimal;
			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;

			public class RedundantComparatorSample {
			    public List<Date> removeComparatorClass(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.compareTo(o2);
			            }

			        });

			        return listToSort;
			    }

			    public Date removeComparatorOnMax(List<Date> listToSort) {
			        // Keep this comment
			        return Collections.max(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.compareTo(o2);
			            }

			        });
			    }

			    public Date removeComparatorOnMin(List<Date> listToSort) {
			        // Keep this comment
			        return Collections.min(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.compareTo(o2);
			            }

			        });
			    }

			    public List<Long> removeLambdaExpression(List<Long> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, (Long o1, Long o2) -> o1.compareTo(o2));

			        return listToSort;
			    }

			    public List<String> removeLambdaBody(List<String> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, (String o1, String o2) -> {
			            return o1.compareTo(o2);
			        });

			        return listToSort;
			    }

			    public List<Double> removeUntypedLambda(List<Double> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, (o1, o2) -> {
			            return o1.compareTo(o2);
			        });

			        return listToSort;
			    }

			    public List<Integer> removeComparatorOnPrimitive(List<Integer> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, (o1, o2) -> {
			            return Integer.compare(o1, o2);
			        });

			        return listToSort;
			    }

			    public List<Date> removeIdentityFunction(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.comparing(Function.identity()));

			        return listToSort;
			    }

			    public List<Date> removeComparingLambda(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.comparing((Date d) -> d));

			        return listToSort;
			    }

			    public List<Date> removeComparingBody(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.comparing((Date d) -> {
			            return d;
			        }));

			        return listToSort;
			    }

			    public List<Date> removeUntypedParameter(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.comparing(d -> {
			            return d;
			        }));

			        return listToSort;
			    }

			    public List<Date> removeNaturalOrder(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.naturalOrder());

			        return listToSort;
			    }

			    public List<Date> removeNullComparator(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, null);

			        return listToSort;
			    }

			    public List<BigDecimal> removeOpposedComparatorClass(List<BigDecimal> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, new Comparator<BigDecimal>() {
			            @Override
			            public int compare(BigDecimal o1, BigDecimal o2) {
			                return -o2.compareTo(o1);
			            }

			        });

			        return listToSort;
			    }

			    public List<Date> removeTwiceReversedComparatorClass(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o2.compareTo(o1);
			            }

			        }.reversed());

			        return listToSort;
			    }

			    public List<Date> refactoreSortedList(List<Date> listToSort) {
			        // Keep this comment
			        listToSort.sort(new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o2.compareTo(o1);
			            }

			        }.reversed());

			        return listToSort;
			    }
			}
			""";

		String expected= """
			package test1;

			import java.math.BigDecimal;
			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;

			public class RedundantComparatorSample {
			    public List<Date> removeComparatorClass(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public Date removeComparatorOnMax(List<Date> listToSort) {
			        // Keep this comment
			        return Collections.max(listToSort);
			    }

			    public Date removeComparatorOnMin(List<Date> listToSort) {
			        // Keep this comment
			        return Collections.min(listToSort);
			    }

			    public List<Long> removeLambdaExpression(List<Long> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<String> removeLambdaBody(List<String> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Double> removeUntypedLambda(List<Double> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Integer> removeComparatorOnPrimitive(List<Integer> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeIdentityFunction(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeComparingLambda(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeComparingBody(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeUntypedParameter(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeNaturalOrder(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeNullComparator(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<BigDecimal> removeOpposedComparatorClass(List<BigDecimal> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> removeTwiceReversedComparatorClass(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }

			    public List<Date> refactoreSortedList(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort);

			        return listToSort;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.REDUNDANT_COMPARATOR);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.RedundantComparatorCleanUp_description)));
	}

	@Test
	public void testKeepRedundantComparator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;

			public class E {
			    public List<String> doNotRemoveComparatorWithoutCompareToMethod(List<String> listToSort) {
			        Collections.sort(listToSort, new Comparator<String>() {
			            @Override
			            public int compare(String o1, String o2) {
			                return o1.compareToIgnoreCase(o2);
			            }

			        });

			        return listToSort;
			    }

			    public List<String> doNotRemoveComparatorWithOtherStatement(List<String> listToSort) {
			        Collections.sort(listToSort, new Comparator<String>() {
			            @Override
			            public int compare(String o1, String o2) {
			                System.out.println("Don't lose me!");
			                return o1.compareTo(o2);
			            }

			        });

			        return listToSort;
			    }

			    public List<String> doNotRemoveLambdaWithOtherStatement(List<String> listToSort) {
			        Collections.sort(listToSort, (String o1, String o2) -> {
			            System.out.println("Don't lose me!");
			            return o1.compareTo(o2);
			        });

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedComparatorClass(List<Date> listToSort) {
			        Collections.sort(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o2.compareTo(o1);
			            }

			        });

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedComparatorOnMethod(List<Date> listToSort) {
			        Collections.sort(listToSort, new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.toString().compareTo(o2.toString());
			            }

			        });

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedLambdaExpression(List<Date> listToSort) {
			        Collections.sort(listToSort, (Date o1, Date o2) -> o2.compareTo(o1));

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedLambdaBody(List<Date> listToSort) {
			        Collections.sort(listToSort, (Date o1, Date o2) -> {
			            return o2.compareTo(o1);
			        });

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedUntypedLambda(List<Date> listToSort) {
			        Collections.sort(listToSort, (o1, o2) -> {
			            return o2.compareTo(o1);
			        });

			        return listToSort;
			    }

			    public List<Date> doNotRemoveComparatorOnSpecialMethod(List<Date> listToSort) {
			        Collections.sort(listToSort, Comparator.comparing(Date::toString));

			        return listToSort;
			    }

			    public List<Date> doNotRemoveReservedIdentityFunction(List<Date> listToSort) {
			        Collections.sort(listToSort, Comparator.<Date, Date>comparing(Function.identity()).reversed());

			        return listToSort;
			    }

			    private class NonComparable {
			        public int compareTo(Object anotherObject) {
			            return 42;
			        }
			    }

			    public List<NonComparable> doNotRemoveComparatorOnNonComparable(List<NonComparable> listToSort) {
			        Collections.sort(listToSort, new Comparator<NonComparable>() {
			            @Override
			            public int compare(NonComparable o1, NonComparable o2) {
			                return o1.compareTo(o2);
			            }

			        });

			        return listToSort;
			    }

			    public List<NonComparable> doNotRemoveLambdaOnNonComparable(List<NonComparable> listToSort) {
			        Collections.sort(listToSort, (NonComparable o1, NonComparable o2) -> {
			            return o1.compareTo(o2);
			        });

			        return listToSort;
			    }

			    public List<NonComparable> doNotRemoveMethodRefOnNonComparable(List<NonComparable> listToSort) {
			        Collections.sort(listToSort, NonComparable::compareTo);

			        return listToSort;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_COMPARATOR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testArrayWithCurly() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.Observable;

			public class E {
			    /**
			     * Keep this comment.
			     */
			    private double[] refactorThisDoubleArray = new double[] { 42.42 };

			    /**
			     * Keep this comment.
			     */
			    private int[][] refactorThis2DimensionArray = new int[][] { { 42 } };

			    /**
			     * Keep this comment.
			     */
			    private Observable[] refactorThisObserverArray = new Observable[0];

			    /**
			     * Keep this comment.
			     */
			    private short[] refactorThisShortArray, andThisArrayToo = new short[0];

			    public void refactorArrayInstantiations() {
			        // Keep this comment
			        double[] refactorLocalDoubleArray = new double[] { 42.42 };
			        char[][] refactorLocal2DimensionArray = new char[][] { { 'a' } };
			        Observable[] refactorLocalObserverArray = new Observable[0];
			        short[] refactorThisShortArray, andThisArrayToo = new short[0];
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.Observable;

			public class E {
			    /**
			     * Keep this comment.
			     */
			    private double[] refactorThisDoubleArray = { 42.42 };

			    /**
			     * Keep this comment.
			     */
			    private int[][] refactorThis2DimensionArray = { { 42 } };

			    /**
			     * Keep this comment.
			     */
			    private Observable[] refactorThisObserverArray = {};

			    /**
			     * Keep this comment.
			     */
			    private short[] refactorThisShortArray, andThisArrayToo = {};

			    public void refactorArrayInstantiations() {
			        // Keep this comment
			        double[] refactorLocalDoubleArray = { 42.42 };
			        char[][] refactorLocal2DimensionArray = { { 'a' } };
			        Observable[] refactorLocalObserverArray = {};
			        short[] refactorThisShortArray, andThisArrayToo = {};
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.ARRAY_WITH_CURLY);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ArrayWithCurlyCleanup_description)));
	}

	@Test
	public void testDoNotUseArrayWithCurly() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Observable;

			public class E {
			    private Byte[] doNotRefactorNotInitializedArray = new Byte[10];

			    private Object doNotRefactorThisObserverArray = new Observable[0];

			    public void doNotRefactorArrayAssignment() {
			        char[] refactorLocalDoubleArray;
			        refactorLocalDoubleArray = new char[] { 'a', 'b' };
			    }

			    public void doNotRefactorArrayInstantiationsInBrackets() {
			        boolean[] refactorLocalDoubleArray = (new boolean[] { true });
			    }

			    public void doNotRefactorCastedArrayInstantiations() {
			        Object refactorLocalDoubleArray = (double[]) new double[] { 42.42 };
			    }

			    public double[] doNotRefactorReturnedArrayInstantiation() {
			        return new double[] { 42.42 };
			    }

			    public void doNotRefactorArrayInstantiationParameter() {
			        System.out.println(new double[] { 42.42 });
			    }

			    public String doNotRefactorArrayInstantiationExpression() {
			        return new float[] { 42.42f }.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ARRAY_WITH_CURLY);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testReturnExpression() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private double[] arrayField = new double[] { 42.42 };

			    public int inlineLocalVariableDeclaration() {
			        int i = 0;
			        return i;
			    }

			    public int inlineLocalVariableAssignment(int i) {
			        i = 0;
			        return i;
			    }

			    /**
			     * No need to check for array variable assignments.
			     * <p>
			     * Trying to use it reports compile error "Array constants can only be used in initializers"
			     * <p>
			     */
			    public String[] inlineStringArrayConstants() {
			        String[] array = { "test" };
			        return array;
			    }

			    public String[][] inlineStringArray2Constants() {
			        String[][] array = { { "test" } };
			        return array;
			    }
			   \s
			    public String[] inlineCStyleStringArrayConstants() {
			        String array[] = { "test" };
			        return array;
			    }

			    public String[][] inlineCStyleStringArray2Constants() {
			        String array[][] = { { "test" } };
			        return array;
			    }
			   \s
			    public String[][] inlineMixedStyleStringArrayConstantsNotSupportedYet() {
			        String[] array[] = { { "mixtest" } };
			        return array;
			    }
			   \s
			    public boolean[] inlineBooleanArrayConstants() {
			        boolean[] array = { true };
			        return array;
			    }

			    public char[] inlineCharArrayConstants() {
			        char[] array = { 'a' };
			        return array;
			    }

			    public byte[] inlineByteArrayConstants() {
			        byte[] array = { 42 };
			        return array;
			    }

			    public short[] inlineShortArrayConstants() {
			        short[] array = { 42 };
			        return array;
			    }

			    public int[] inlineIntArrayConstants() {
			        int[] array = { 42 };
			        return array;
			    }

			    public long[] inlineLongArrayConstants() {
			        long[] array = { 42 };
			        return array;
			    }

			    public float[] inlineFloatArrayConstants() {
			        float[] array = { 42.42f };
			        return array;
			    }

			    public double[] inlineDoubleArrayConstants() {
			        double[] array = { 42.42 };
			        return array;
			    }

			    public double[] inlineDoubleArrayCreation() {
			        double[] array = new double[]{ 42.42 };
			        return array;
			    }

			    public double[] inlineDoubleArrayVariableDeclaration() {
			        double[] array = arrayField;
			        return array;
			    }

			    public double[] inlineDoubleArrayAssignment() {
			        double[] array = null;
			        array = arrayField;
			        return array;
			    }

			    public Throwable[] inlineStatementWithEmptyArray() {
			        Throwable[] t = {};
			        return t;
			    }

			    public Throwable[] inlineExpressionWithEmptyArray(Throwable[] t) {
			        t = new Throwable[]{};
			        return t;
			    }

			    public char[] refactorMethodCall(String s) {
			        char[] res = s.toCharArray();
			        return res;
			    }

			    public int inlineSeveralReturns(int i1, int i2) {
			        if (i1 == 0) {
			            i1 = 10;
			            return i1;
			        } else {
			            i2 = 11;
			            return i2;
			        }
			    }

			    public int inlineUnusedVariableInFinally() {
			        int i = 0;
			        try {
			            i = 1;
			            return i;
			        } finally {
			            System.out.println("Finished");
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    private double[] arrayField = new double[] { 42.42 };

			    public int inlineLocalVariableDeclaration() {
			        return 0;
			    }

			    public int inlineLocalVariableAssignment(int i) {
			        return 0;
			    }

			    /**
			     * No need to check for array variable assignments.
			     * <p>
			     * Trying to use it reports compile error "Array constants can only be used in initializers"
			     * <p>
			     */
			    public String[] inlineStringArrayConstants() {
			        return new String[]{ "test" };
			    }

			    public String[][] inlineStringArray2Constants() {
			        return new String[][]{ { "test" } };
			    }
			   \s
			    public String[] inlineCStyleStringArrayConstants() {
			        return new String[]{ "test" };
			    }

			    public String[][] inlineCStyleStringArray2Constants() {
			        return new String[][]{ { "test" } };
			    }
			   \s
			    public String[][] inlineMixedStyleStringArrayConstantsNotSupportedYet() {
			        String[] array[] = { { "mixtest" } };
			        return array;
			    }
			   \s
			    public boolean[] inlineBooleanArrayConstants() {
			        return new boolean[]{ true };
			    }

			    public char[] inlineCharArrayConstants() {
			        return new char[]{ 'a' };
			    }

			    public byte[] inlineByteArrayConstants() {
			        return new byte[]{ 42 };
			    }

			    public short[] inlineShortArrayConstants() {
			        return new short[]{ 42 };
			    }

			    public int[] inlineIntArrayConstants() {
			        return new int[]{ 42 };
			    }

			    public long[] inlineLongArrayConstants() {
			        return new long[]{ 42 };
			    }

			    public float[] inlineFloatArrayConstants() {
			        return new float[]{ 42.42f };
			    }

			    public double[] inlineDoubleArrayConstants() {
			        return new double[]{ 42.42 };
			    }

			    public double[] inlineDoubleArrayCreation() {
			        return new double[]{ 42.42 };
			    }

			    public double[] inlineDoubleArrayVariableDeclaration() {
			        return arrayField;
			    }

			    public double[] inlineDoubleArrayAssignment() {
			        double[] array = null;
			        return arrayField;
			    }

			    public Throwable[] inlineStatementWithEmptyArray() {
			        return new Throwable[]{};
			    }

			    public Throwable[] inlineExpressionWithEmptyArray(Throwable[] t) {
			        return new Throwable[]{};
			    }

			    public char[] refactorMethodCall(String s) {
			        return s.toCharArray();
			    }

			    public int inlineSeveralReturns(int i1, int i2) {
			        if (i1 == 0) {
			            return 10;
			        } else {
			            return 11;
			        }
			    }

			    public int inlineUnusedVariableInFinally() {
			        int i = 0;
			        try {
			            return 1;
			        } finally {
			            System.out.println("Finished");
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.RETURN_EXPRESSION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ReturnExpressionCleanUp_description)));
	}

	@Test
	public void testDoNotReturnExpression() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
				private int i;
				public int doNotInlineFieldAssignment1() {
					i = 0;
					return i;
				}

				public int doNotInlineFieldAssignment2() {
					this.i = 0;
					return i;
				}

				public int doNotInlineVariableInFinally() {
					int i = 0;
					try {
						i = 1;
						return i;
					} finally {
						System.out.println(i);
					}
				}
				public int doNotInlineCatchVariableInFinally() {
					int i = 0;
					try {
						return 1;
					} catch (Exception e) {
						i = 1;
						return 2;
					} finally {
						System.out.println(i);
					}
				}

				public int doNotInlineVariableInFarAwayFinally() {
					int i = 0;
					try {
						try {
							i = 1;
							return i;
						} finally {
							System.out.println("Finished");
						}
					} finally {
						System.out.println(i);
					}
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.RETURN_EXPRESSION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testSimplifyBooleanIfElseExpression01() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public boolean simplifyNormal(int x) {
			        if (x > 0 && x < 7) {
			            return true;
			        } else {
			            return false;
			        }
			    }

			    public boolean simplifyReverse(int x) {
			        if (x > 0 && x < 7) {
			            return false;
			        } else {
			            return true;
			        }
			    }

				public boolean simplifyCompoundIf(int x) {
				    if (x < 0) {
				        return false;
				    } else if (x < 7) {
				        return true;
				    } else {
				        return false;
				    }
				}

				public boolean simplifyNLS(String x) {
				    if (x.equals("abc")) { //$NON-NLS-1$
				        return true;
				    } else {
				        return false;
				    }
				}
			}
		    """;

		String expected= """
			package test1;

			public class E {
			    public boolean simplifyNormal(int x) {
			        return x > 0 && x < 7;
			    }

			    public boolean simplifyReverse(int x) {
			        return !(x > 0 && x < 7);
			    }

				public boolean simplifyCompoundIf(int x) {
				    if (x < 0) {
				        return false;
				    } else
			            return x < 7;
				}

				public boolean simplifyNLS(String x) {
				    return x.equals("abc"); //$NON-NLS-1$
				}
			}
		    """;

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_SimplifyBooleanIfElse_description)));
	}

	@Test
	public void testSimplifyBooleanIfElseExpression02() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public boolean simplifyNormal(int x) {
			        if (x > 0 && x < 7) {
			            return true;
			        } else {
			            return false;
			        }
			    }

			    public boolean simplifyReverse(int x) {
			        if (x > 0 && x < 7) {
			            return false;
			        } else {
			            return true;
			        }
			    }

				public boolean simplifyCompoundIf(int x) {
				    if (x < 0) {
				        return false;
				    } else if (x < 7) {
				        return true;
				    } else {
				        return false;
				    }
				}

				public boolean simplifyNLS(String x) {
				    if (x.equals("abc")) { //$NON-NLS-1$
				        return true;
				    } else {
				        return false;
				    }
				}
			}
		    """;

		String expected= """
			package test1;

			public class E {
			    public boolean simplifyNormal(int x) {
			        return x > 0 && x < 7;
			    }

			    public boolean simplifyReverse(int x) {
			        return !(x > 0 && x < 7);
			    }

				public boolean simplifyCompoundIf(int x) {
				    if (x < 0) {
				        return false;
				    } else
			            return x < 7;
				}

				public boolean simplifyNLS(String x) {
				    return x.equals("abc"); //$NON-NLS-1$
				}
			}
		    """;

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE);
		enable(CleanUpConstants.REDUCE_INDENTATION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_SimplifyBooleanIfElse_description)));
	}

	@Test
	public void testDoNotSimplifyBooleanIfElseExpression01() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public boolean doNotSimplifyIfBooleanSame(int x) {
			        if (x > 0 && x < 7) {
			            return true;
			        } else {
			            return true;
			        }
			    }

			    public boolean doNotSimplifyIfBooleanTheSame2(int x) {
			        if (x > 0 && x < 7) {
			            return false;
			        } else {
			            return false;
			        }
			    }

				public boolean doNotSimplifyIfNoElse(int x) {
				    if (x < 0) {
				        return false;
				    } else if (x < 7) {
				        return true;
				    }
				    return false;
				}

				public int doNotSimplifyNoneBoolean(String x) {
				    if (x.equals("abc")) { //$NON-NLS-1$
				        return 1;
				    } else {
				        return 2;
				    }
				}
			}
		    """;


		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRemoveUselessReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public void removeUselessReturn() {
			        return;
			    }

			    public void removeUselessReturnWithPreviousCode() {
			        System.out.println("Keep this line");
			        return;
			    }

			    public void removeUselessReturnWithIf(boolean isValid) {
			        if (isValid) {
			            System.out.println("Keep this line");
			            return;
			        }
			    }

			    public void replaceByBlock(boolean isEnabled) {
			        System.out.println("Keep this line");
			        if (isEnabled)
			            return;
			    }

			    public void removeElseStatement(boolean isValid) {
			        System.out.println("Keep this line");
			        if (isValid)
			            System.out.println("isValid is true");
			        else
			            return;
			    }

			    public void removeElseBlock(boolean isValid) {
			        System.out.println("Keep this line");
			        if (isValid) {
			            System.out.println("isValid is true");
			        } else {
			            return;
			        }
			    }

			    public void removeUselessReturnWithSwitch(int myNumber) {
			        switch (myNumber) {
			        case 0:
			            System.out.println("Keep this line");
			            return;
			        }
			    }

			    public void removeUselessReturnWithIfElse(boolean isValid) {
			        if (isValid) {
			            System.out.println("Keep this line");
			            return;
			        } else {
			            System.out.println("Remove anyway");
			        }
			    }

			    public void removeUselessReturnInLambda() {
			        Runnable r = () -> {return;};
			        r.run();
			        System.out.println("Remove anyway");
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_RETURN);

		sample= """
			package test1;

			public class E1 {
			    public void removeUselessReturn() {
			    }

			    public void removeUselessReturnWithPreviousCode() {
			        System.out.println("Keep this line");
			    }

			    public void removeUselessReturnWithIf(boolean isValid) {
			        if (isValid) {
			            System.out.println("Keep this line");
			        }
			    }

			    public void replaceByBlock(boolean isEnabled) {
			        System.out.println("Keep this line");
			        if (isEnabled) {
			        }
			    }

			    public void removeElseStatement(boolean isValid) {
			        System.out.println("Keep this line");
			        if (isValid)
			            System.out.println("isValid is true");
			    }

			    public void removeElseBlock(boolean isValid) {
			        System.out.println("Keep this line");
			        if (isValid) {
			            System.out.println("isValid is true");
			        }
			    }

			    public void removeUselessReturnWithSwitch(int myNumber) {
			        switch (myNumber) {
			        case 0:
			            System.out.println("Keep this line");
			        }
			    }

			    public void removeUselessReturnWithIfElse(boolean isValid) {
			        if (isValid) {
			            System.out.println("Keep this line");
			        } else {
			            System.out.println("Remove anyway");
			        }
			    }

			    public void removeUselessReturnInLambda() {
			        Runnable r = () -> {};
			        r.run();
			        System.out.println("Remove anyway");
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.UselessReturnCleanUp_description)));
	}

	@Test
	public void testDoNotRemoveReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public int doNotRemoveReturnWithValue() {
			        return 0;
			    }

			    public void doNotRemoveUselessReturnInMiddleOfSwitch(int myNumber) {
			        switch (myNumber) {
			        case 0:
			            System.out.println("I'm not the last statement");
			            return;
			        case 1:
			            System.out.println("Do some stuff");
			            break;
			        }
			    }

			    public void doNotRemoveReturnWithFollowingCode(boolean isValid) {
			        if (isValid) {
			            System.out.println("Keep this line");
			            return;
			        }
			        System.out.println("Do not forget me");
			    }

			    public void doNotRemoveReturnInWhile(int myNumber) {
			        while (myNumber-- > 0) {
			            System.out.println("Keep this line");
			            return;
			        }
			    }

			    public void doNotRemoveReturnInDoWhile(int myNumber) {
			        do {
			            System.out.println("Keep this line");
			            return;
			        } while (myNumber-- > 0);
			    }

			    public void doNotRemoveReturnInFor() {
			        for (int myNumber = 0; myNumber < 10; myNumber++) {
			            System.out.println("Keep this line");
			            return;
			        }
			    }

			    public void doNotRemoveReturnInForEach(int[] integers) {
			        for (int myNumber : integers) {
			            System.out.println("Only the first value: " + myNumber);
			            return;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_RETURN);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveUselessContinue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.util.List;

			public class E1 {
			    public void removeUselessContinue(List<String> texts) {
			        for (String text : texts) {
			            continue;
			        }
			    }

			    public void removeUselessContinueWithPreviousCode(List<String> texts) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            continue;
			        }
			    }

			    public void removeUselessContinueWithIf(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            if (isValid) {
			                System.out.println("Keep this line");
			                continue;
			            }
			        }
			    }

			    public void replaceByBlock(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid)
			                continue;
			        }
			    }

			    public void removeElseStatement(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid)
			                System.out.println("isValid is true");
			            else
			                continue;
			        }
			    }

			    public void removeElseBlock(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid) {
			                System.out.println("isValid is true");
			            } else {
			                continue;
			            }
			        }
			    }

			    public void removeUselessContinueWithSwitch(List<String> texts, int myNumber) {
			        for (String text : texts) {
			            switch (myNumber) {
			            case 0:
			                System.out.println("Keep this line");
			                continue;
			            }
			        }
			    }

			    public void removeUselessContinueWithIfElse(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            if (isValid) {
			                System.out.println("Keep this line");
			                continue;
			            } else {
			                System.out.println("Remove anyway");
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", input, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_CONTINUE);

		String output= """
			package test1;

			import java.util.List;

			public class E1 {
			    public void removeUselessContinue(List<String> texts) {
			        for (String text : texts) {
			        }
			    }

			    public void removeUselessContinueWithPreviousCode(List<String> texts) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			        }
			    }

			    public void removeUselessContinueWithIf(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            if (isValid) {
			                System.out.println("Keep this line");
			            }
			        }
			    }

			    public void replaceByBlock(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid) {
			            }
			        }
			    }

			    public void removeElseStatement(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid)
			                System.out.println("isValid is true");
			        }
			    }

			    public void removeElseBlock(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            System.out.println("Keep this line");
			            if (isValid) {
			                System.out.println("isValid is true");
			            }
			        }
			    }

			    public void removeUselessContinueWithSwitch(List<String> texts, int myNumber) {
			        for (String text : texts) {
			            switch (myNumber) {
			            case 0:
			                System.out.println("Keep this line");
			            }
			        }
			    }

			    public void removeUselessContinueWithIfElse(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            if (isValid) {
			                System.out.println("Keep this line");
			            } else {
			                System.out.println("Remove anyway");
			            }
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.UselessContinueCleanUp_description)));
	}

	@Test
	public void testDoNotRemoveContinue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.List;

			public class E1 {
			    public void doNotRemoveBreak(List<String> texts) {
			        for (String text : texts) {
			            break;
			        }
			    }

			    public void doNotRemoveReturn(List<String> texts) {
			        for (String text : texts) {
			            return;
			        }
			    }

			    public void doNotRemoveThrow(List<String> texts) {
			        for (String text : texts) {
			            throw new NullPointerException();
			        }
			    }

			    public void doNotRemoveContinueWithLabel(List<String> texts, List<String> otherTexts) {
			        begin: for (String text : texts) {
			            for (String otherText : otherTexts) {
			                System.out.println("Keep this line");
			                continue begin;
			            }
			        }
			    }

			    public void doNotRemoveUselessContinueInMiddleOfSwitch(List<String> texts, int myNumber) {
			        for (String text : texts) {
			            switch (myNumber) {
			            case 0:
			                System.out.println("I'm not the last statement");
			                continue;
			            case 1:
			                System.out.println("Do some stuff");
			                break;
			            }
			        }
			    }

			    public void doNotRemoveContinueWithFollowingCode(List<String> texts, boolean isValid) {
			        for (String text : texts) {
			            if (isValid) {
			                System.out.println("Keep this line");
			                continue;
			            }
			            System.out.println("Keep this line");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_CONTINUE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testIssue1638() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E1 {
			    public void doNotRemoveUselessReturn(boolean arg) {
				    if (arg) {
					    return;
					} else {
						System.out.println("here");
					}
			    }

			    public void doNotRemoveUselessContinue(boolean arg) {
					while (arg) {
						arg = bar();
						if (arg) {
							continue;
						} else {
							System.out.println("Hello World");
						}
					}
			    }

				public boolean bar() {
					return true;
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", input, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_CONTINUE);
		enable(CleanUpConstants.REMOVE_USELESS_RETURN);
		enable(CleanUpConstants.REDUCE_INDENTATION);

		String output= """
			package test1;

			public class E1 {
			    public void doNotRemoveUselessReturn(boolean arg) {
				    if (arg) {
					    return;
					}
			        System.out.println("here");
			    }

			    public void doNotRemoveUselessContinue(boolean arg) {
					while (arg) {
						arg = bar();
						if (arg) {
							continue;
						}
			            System.out.println("Hello World");
					}
			    }

				public boolean bar() {
					return true;
				}
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testUnloopedWhile() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void replaceWhileByIf(boolean isValid) {
			        // Keep this comment
			        while (isValid) {
			            System.out.println("foo");
			            return;
			        }
			    }

			    public void replaceWhileThrowingExceptions(boolean isEnabled) {
			        // Keep this comment
			        while (isEnabled) {
			            System.out.println("foo");
			            throw new NullPointerException();
			        }
			    }

			    public void replaceWhileByIfAndRemoveBreak(boolean isVisible) {
			        // Keep this comment
			        while (isVisible) {
			            System.out.println("foo");
			            break;
			        }
			    }

			    public void replaceWhileByIfAndReplaceBreaksByBlocks(boolean isVisible, int i) {
			        // Keep this comment
			        while (isVisible) {
			            if (i > 0)
			                break;
			            else
			                break;
			        }
			    }

			    public void replaceWhileWithComplexCode(boolean b1, boolean b2) {
			        // Keep this comment
			        while (b1) {
			            System.out.println("foo");
			            if (b2) {
			                System.out.println("bar");
			                return;
			            } else {
			                throw new NullPointerException();
			            }
			        }
			    }

			    public void replaceWhileButOnlyRemoveBreakForTheWhileLoop(boolean b, int magicValue) {
			        // Keep this comment
			        while (b) {
			            for (int i = 0; i < 10; i++) {
			                if (i == magicValue) {
			                    System.out.println("Magic value! Goodbye!");
			                    break;
			                } else {
			                    System.out.println("Current value: " + i);
			                }
			            }
			            break;
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void replaceWhileByIf(boolean isValid) {
			        // Keep this comment
			        if (isValid) {
			            System.out.println("foo");
			            return;
			        }
			    }

			    public void replaceWhileThrowingExceptions(boolean isEnabled) {
			        // Keep this comment
			        if (isEnabled) {
			            System.out.println("foo");
			            throw new NullPointerException();
			        }
			    }

			    public void replaceWhileByIfAndRemoveBreak(boolean isVisible) {
			        // Keep this comment
			        if (isVisible) {
			            System.out.println("foo");
			        }
			    }

			    public void replaceWhileByIfAndReplaceBreaksByBlocks(boolean isVisible, int i) {
			        // Keep this comment
			        if (isVisible) {
			            if (i > 0) {
			            }
			        }
			    }

			    public void replaceWhileWithComplexCode(boolean b1, boolean b2) {
			        // Keep this comment
			        if (b1) {
			            System.out.println("foo");
			            if (b2) {
			                System.out.println("bar");
			                return;
			            } else {
			                throw new NullPointerException();
			            }
			        }
			    }

			    public void replaceWhileButOnlyRemoveBreakForTheWhileLoop(boolean b, int magicValue) {
			        // Keep this comment
			        if (b) {
			            for (int i = 0; i < 10; i++) {
			                if (i == magicValue) {
			                    System.out.println("Magic value! Goodbye!");
			                    break;
			                } else {
			                    System.out.println("Current value: " + i);
			                }
			            }
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.UNLOOPED_WHILE);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.UnloopedWhileCleanUp_description)));
	}

	@Test
	public void testKeepUnloopedWhile() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public void doNotReplaceWhileEndedByContinue(boolean b) {
			        while (b) {
			            System.out.println("foo");
			            continue;
			        }
			    }

			    public int doNotReplaceInfiniteWhile() {
			        while (true) {
			            System.out.println("foo");
			            return 0;
			        }
			    }

			    public int doNotReplaceComplexInfiniteWhile() {
			        while (42 == 42) {
			            System.out.println("foo");
			            return 0;
			        }
			    }

			    public void doNotReplaceWhileUsingContinue(boolean b1, boolean b2) {
			        while (b1) {
			            if (b2) {
			                System.out.println("bar");
			                continue;
			            }
			            System.out.println("foo");
			            return;
			        }
			    }

			    public void doNotReplaceWhileThatMayHaveSeveralIterations(int i) {
			        while (i-- > 0) {
			            System.out.println("foo");
			            if (i == 1) {
			                System.out.println("bar");
			                return;
			            } else if (i == 2) {
			                throw new NullPointerException();
			            }
			        }
			    }

			    public void doNotReplaceWhileThatHasLabeledBreak(boolean b) {
			        doNotTrashThisSpecialBreak:while (b) {
			            System.out.println("foo");
			            break doNotTrashThisSpecialBreak;
			        }
			    }

			    public void doNotRemoveBreakThatShortcutsCode(boolean isValid, boolean isEnabled) {
			        while (isValid) {
			            if (isEnabled) {
			                System.out.println("foo");
			                break;
			            }
			            System.out.println("bar");
			            break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.UNLOOPED_WHILE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testMapMethodRatherThanKeySetMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;

			public class E1 {
			    public int replaceUnnecesaryCallsToMapKeySet(Map<String, String> map) {
			        // Keep this comment
			        int x = map.keySet().size();

			        if (map.keySet().contains("hello")) {
			            map.keySet().remove("hello");
			        }

			        if (map.keySet().remove("world")) {
			            // Cannot replace, because `map.removeKey("world") != null` is not strictly equivalent
			            System.out.println(map);
			        }

			        // Keep this comment also
			        map.keySet().clear();

			        // Keep this comment too
			        if (map.keySet().isEmpty()) {
			            x++;
			        }

			        return x;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		sample= """
			package test1;

			import java.util.Map;

			public class E1 {
			    public int replaceUnnecesaryCallsToMapKeySet(Map<String, String> map) {
			        // Keep this comment
			        int x = map.size();

			        if (map.containsKey("hello")) {
			            map.remove("hello");
			        }

			        if (map.keySet().remove("world")) {
			            // Cannot replace, because `map.removeKey("world") != null` is not strictly equivalent
			            System.out.println(map);
			        }

			        // Keep this comment also
			        map.clear();

			        // Keep this comment too
			        if (map.isEmpty()) {
			            x++;
			        }

			        return x;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testMapMethodRatherThanValuesMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Map;

			public class E1 {
			    public int replaceUnnecesaryCallsToMapValues(Map<String, String> map) {
			        // Keep this comment
			        int x = map.values().size();

			        if (map.values().contains("hello")) {
			            map.values().remove("hello");
			        }

			        if (map.values().remove("world")) {
			            System.out.println(map);
			        }

			        // Keep this comment also
			        map.values().clear();

			        // Keep this comment too
			        if (map.values().contains("foo")) {
			            x++;
			        }

			        return x;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		sample= """
			package test1;

			import java.util.Map;

			public class E1 {
			    public int replaceUnnecesaryCallsToMapValues(Map<String, String> map) {
			        // Keep this comment
			        int x = map.size();

			        if (map.containsValue("hello")) {
			            map.values().remove("hello");
			        }

			        if (map.values().remove("world")) {
			            System.out.println(map);
			        }

			        // Keep this comment also
			        map.clear();

			        // Keep this comment too
			        if (map.containsValue("foo")) {
			            x++;
			        }

			        return x;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.UseDirectlyMapMethodCleanup_description)));
	}

	@Test
	public void testDoNotUseMapMethodInsideMapImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.HashMap;
			import java.util.Map;

			public class E1<K,V> extends HashMap<K,V> {
			    @Override
			    public boolean containsKey(Object key) {
			        return keySet().contains(key);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseMapMethodInsideThisMapImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.HashMap;
			import java.util.Map;

			public class E1<K,V> extends HashMap<K,V> {
			    @Override
			    public boolean containsKey(Object key) {
			        return this.keySet().contains(key);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCloneCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.Date;
			import java.util.List;
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Stack;

			public class E1 {
			    public void replaceNewNoArgsAssignmentThenAddAll(List<String> col, List<String> output) {
			        // Keep this comment
			        output = new ArrayList<>();
			        output.addAll(col);
			    }

			    public List<String> replaceNewNoArgsThenAddAll(List<String> col) {
			        // Keep this comment
			        final List<String> output = new ArrayList<>();
			        output.addAll(col);
			        return output;
			    }

			    public List<Date> replaceNewOneArgThenAddAll(List<Date> col) {
			        // Keep this comment
			        final List<Date> output = new ArrayList<>(0);
			        output.addAll(col);
			        return output;
			    }

			    public List<Integer> replaceNewCollectionSizeThenAddAll(List<Integer> col, List<List<Integer>> listOfCol) {
			        // Keep this comment
			        final List<Integer> output = new ArrayList<>(col.size());
			        output.addAll(col);
			        return output;
			    }

			    public Object replaceNewThenAddAllParameterizedType(Map<String, String> map) {
			        // Keep this comment
			        List<Entry<String, String>> output = new ArrayList<Entry<String, String>>();
			        output.addAll(map.entrySet());
			        return output;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.COLLECTION_CLONING);

		sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.Date;
			import java.util.List;
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Stack;

			public class E1 {
			    public void replaceNewNoArgsAssignmentThenAddAll(List<String> col, List<String> output) {
			        // Keep this comment
			        output = new ArrayList<>(col);
			    }

			    public List<String> replaceNewNoArgsThenAddAll(List<String> col) {
			        // Keep this comment
			        final List<String> output = new ArrayList<>(col);
			        return output;
			    }

			    public List<Date> replaceNewOneArgThenAddAll(List<Date> col) {
			        // Keep this comment
			        final List<Date> output = new ArrayList<>(col);
			        return output;
			    }

			    public List<Integer> replaceNewCollectionSizeThenAddAll(List<Integer> col, List<List<Integer>> listOfCol) {
			        // Keep this comment
			        final List<Integer> output = new ArrayList<>(col);
			        return output;
			    }

			    public Object replaceNewThenAddAllParameterizedType(Map<String, String> map) {
			        // Keep this comment
			        List<Entry<String, String>> output = new ArrayList<Entry<String, String>>(map.entrySet());
			        return output;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.CollectionCloningCleanUp_description)));
	}

	@Test
	public void testDoNotCloneCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.List;
			import java.util.Stack;

			public class E1 {
			    public void doNotReplaceStackCtor(List<String> col, List<String> output) {
			        output = new Stack<>();
			        output.addAll(col);
			    }

			    public List<String> doNotReplaceAlreadyInitedCol(List<String> col1, List<String> col2) {
			        final List<String> output = new ArrayList<>(col1);
			        output.addAll(col2);
			        return output;
			    }

			    public List<String> doNotReplaceWithSpecificSize(List<String> col) {
			        final List<String> output = new ArrayList<>(10);
			        output.addAll(col);
			        return output;
			    }

			    public List<Object> doNotReplaceNewThenAddAllIncompatibleTypes(List<String> col) {
			        final List<Object> output = new ArrayList<>();
			        output.addAll(col);
			        return output;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.COLLECTION_CLONING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCloneMap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.HashMap;
			import java.util.List;
			import java.util.Map;

			public class E1 {
			    public void replaceNewNoArgsAssignmentThenPutAll(Map<String, String> map, Map<String, String> output) {
			        // Keep this comment
			        output = new HashMap<>();
			        output.putAll(map);
			    }

			    public Map<String, String> replaceNewNoArgsThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>();
			        output.putAll(map);
			        return output;
			    }

			    public Map<String, String> replaceNew0ArgThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(0);
			        output.putAll(map);
			        return output;
			    }

			    public Map<String, String> replaceNew1ArgThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(0);
			        output.putAll(map);
			        return output;
			    }

			    public Map<String, String> replaceNewMapSizeThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(map.size());
			        output.putAll(map);
			        return output;
			    }

			    public Map<String, String> replaceWithSizeOfSubMap(List<Map<String, String>> listOfMap) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(listOfMap.get(0).size());
			        output.putAll(listOfMap.get(0));
			        return output;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MAP_CLONING);

		sample= """
			package test1;

			import java.util.HashMap;
			import java.util.List;
			import java.util.Map;

			public class E1 {
			    public void replaceNewNoArgsAssignmentThenPutAll(Map<String, String> map, Map<String, String> output) {
			        // Keep this comment
			        output = new HashMap<>(map);
			    }

			    public Map<String, String> replaceNewNoArgsThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(map);
			        return output;
			    }

			    public Map<String, String> replaceNew0ArgThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(map);
			        return output;
			    }

			    public Map<String, String> replaceNew1ArgThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(map);
			        return output;
			    }

			    public Map<String, String> replaceNewMapSizeThenPutAll(Map<String, String> map) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(map);
			        return output;
			    }

			    public Map<String, String> replaceWithSizeOfSubMap(List<Map<String, String>> listOfMap) {
			        // Keep this comment
			        final Map<String, String> output = new HashMap<>(listOfMap.get(0));
			        return output;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, new HashSet<>(Arrays.asList(MultiFixMessages.MapCloningCleanUp_description)));
	}

	@Test
	public void testDoNotCloneMap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.HashMap;
			import java.util.Map;

			public class E1 {
			    public Map<String, String> doNotReplaceAlreadyInitedMap(Map<String, String> map1, Map<String, String> map2) {
			        final Map<String, String> output = new HashMap<>(map1);
			        output.putAll(map2);
			        return output;
			    }

			    public Map<String, String> doNotReplaceWithSpecificSize(Map<String, String> map) {
			        Map<String, String> output = new HashMap<>(10);
			        output.putAll(map);
			        return output;
			    }

			    public Map<Object, Object> doNotReplaceNewThenAddAllIncompatibleTypes(Map<String, String> map) {
			        final Map<Object, Object> output = new HashMap<>();
			        output.putAll(map);
			        return output;
			    }

			    public Map<String, String> doNotReplaceAnonymousMap(Map<String, String> map) {
			        final Map<String, String> output = new HashMap<>() {
			            private static final long serialVersionUID= 1L;

			            @Override
			            public void putAll(Map<? extends String, ? extends String> map) {
			                // Drop the map
			            }
			        };
			        output.putAll(map);
			        return output;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MAP_CLONING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testOverriddenAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.io.File;

			public class E {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = true;
			        reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar = 0;
			        System.out.println();
			        reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = "";
			        System.out.println();
			        reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = i > 0;
			        reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfEnabled() {
			        // comment 1
			        long time = 0;
			        // comment 2
			        String separator = "";
			        separator = System.lineSeparator();
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = null;
			        // s is a String
			        String s = null;
			        // No move for multiple declarations
			        String v = "ppp", w = "qqq"; //$NON-NLS-1$ //$NON-NLS-2$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 7;
			        // this comment will get lost
			        s = /* abc */ "def" +	//$NON-NLS-1$
			            "xyz" +
			            "ghi"; //$NON-NLS-1$
			        j = 4 +
			        	       // some comment
			        	       5 +
			        	       6;
			        t = /* abc */ "pqr"; //$NON-NLS-1$
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		disable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		disable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		String output= """
			package test1;

			import java.io.File;

			public class E {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar;
			        System.out.println();
			        reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = File.pathSeparator;
			        System.out.println();
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfEnabled() {
			        // comment 1
			        long time;
			        // comment 2
			        String separator = System.lineSeparator();
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = /* abc */ "pqr"; //$NON-NLS-1$
			        // s is a String
			        String s = /* abc */ "def" +	//$NON-NLS-1$
			                    "xyz" +
			                    "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w = "qqq"; //$NON-NLS-1$ //$NON-NLS-2$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 4 +
			                	       // some comment
			                	       5 +
			                	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));

		input= """
			package test1;

			import java.io.File;

			public class F {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = true;
			        reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // This comment will be lost
			        long reassignedVar = 0;
			        System.out.println();
			        // Keep this comment
			        reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = "";
			        System.out.println();
			        reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = i > 0;
			        reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfEnabled() {
			        // comment 1
			        long time = 0;
			        // comment 2
			        String separator = "";
			        separator = System.lineSeparator();
			        System.out.println(separator);
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = null;
			        // s is a String
			        String s = null;
			        // No move for multiple declarations
			        String v = "ppp", w = "qqq"; //$NON-NLS-1$ //$NON-NLS-2$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 7;
			        // this comment will get lost
			        s = /* abc */ "def" +	//$NON-NLS-1$
			            "xyz" +
			            "ghi"; //$NON-NLS-1$
			        j = 4 +
			        	       // some comment
			        	       5 +
			        	       6;
			        t = /* abc */ "pqr"; //$NON-NLS-1$
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";
		cu= pack.createCompilationUnit("F.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		output= """
			package test1;

			import java.io.File;

			public class F {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        System.out.println();
			        // Keep this comment
			        long reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = File.pathSeparator;
			        System.out.println();
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfEnabled() {
			       \s
			        // comment 2
			        String separator = System.lineSeparator();
			        System.out.println(separator);
			        // comment 3
			        long time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = /* abc */ "pqr"; //$NON-NLS-1$
			        // s is a String
			        String s = /* abc */ "def" +	//$NON-NLS-1$
			                    "xyz" +
			                    "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w = "qqq"; //$NON-NLS-1$ //$NON-NLS-2$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 4 +
			                	       // some comment
			                	       5 +
			                	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));
	}

	@Test
	public void testOverriddenAssignment2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.io.File;

			public class E {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar;
			        reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar;
			        reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar;
			        reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar;
			        reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfInitialized() {
			        // comment 1
			        long time;
			        // comment 2
			        String separator = "";
			        separator = System.lineSeparator();
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t;
			        t = /* abc */ "pqr"; //$NON-NLS-1$
			        // s is a String
			        String s;
			        // this comment will get lost
			        s = /* abc */ "def" +	//$NON-NLS-1$
			            "xyz" +
			            "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w; //$NON-NLS-1$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 7;
			        j = 4 +
			        	       // some comment
			        	       5 +
			        	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		disable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		disable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		String output= """
			package test1;

			import java.io.File;

			public class E {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfInitialized() {
			        // comment 1
			        long time;
			        // comment 2
			        String separator = System.lineSeparator();
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = /* abc */ "pqr"; //$NON-NLS-1$
			       \s
			        // s is a String
			        String s = /* abc */ "def" +	//$NON-NLS-1$
			                    "xyz" +
			                    "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w; //$NON-NLS-1$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 4 +
			                	       // some comment
			                	       5 +
			                	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));

		input= """
			package test1;

			import java.io.File;

			public class F {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar;
			        reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar;
			        // This comment will be lost
			        reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar;
			        reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar;
			        reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfInitialized() {
			        // comment 1
			        long time;
			        // comment 2
			        String separator;
			        separator = System.lineSeparator();
			        System.out.println(separator);
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t;
			        t = /* abc */ "pqr"; //$NON-NLS-1$
			        // s is a String
			        String s;
			        // this comment will get lost
			        s = /* abc */ "def" +	//$NON-NLS-1$
			            "xyz" +
			            "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w; //$NON-NLS-1$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 7;
			        j = 4 +
			        	       // some comment
			        	       5 +
			        	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";
		cu= pack.createCompilationUnit("F.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		output= """
			package test1;

			import java.io.File;

			public class F {
			    public boolean removeUselessInitialization() {
			        // Keep this comment
			        boolean reassignedVar = "\\n".equals(File.pathSeparator);//$NON-NLS-1
			        return reassignedVar;
			    }

			    public long removeInitForLong() {
			        // Keep this comment
			        long reassignedVar = System.currentTimeMillis();
			        return reassignedVar;
			    }

			    public String removeInitForString() {
			        // Keep this comment
			        String reassignedVar = File.pathSeparator;
			        return reassignedVar;
			    }

			    public boolean removePassiveInitialization(int i) {
			        // Keep this comment
			        boolean reassignedPassiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedPassiveVar;
			    }

			    public long moveDeclOnlyIfInitialized() {
			        // comment 1
			        long time;
			        // comment 2
			        String separator = System.lineSeparator();
			        System.out.println(separator);
			        // comment 3
			        time = System.currentTimeMillis();
			        return time;
			    }

			    public void complexMoves(String str) {
			        // t is a String
			        String t = /* abc */ "pqr"; //$NON-NLS-1$
			       \s
			        // s is a String
			        String s = /* abc */ "def" +	//$NON-NLS-1$
			                    "xyz" +
			                    "ghi"; //$NON-NLS-1$
			        // No move for multiple declarations
			        String v = "ppp", w; //$NON-NLS-1$
			        // No move for multiple statements on line
			        String k = "rrr"; String l = "sss"; //$NON-NLS-1$ //$NON-NLS-2$
			        int j = 4 +
			                	       // some comment
			                	       5 +
			                	       6;
			        w = "aaa"; //$NON-NLS-1$
			        k = "ttt"; //$NON-NLS-1$
			        l = "uuu"; //$NON-NLS-1$
			        // No move when parent statement other than block is on same line
			        if ("TRUE".equals(str)) { var x= "bar"; //$NON-NLS-1$ //$NON-NLS-2$
			           x = v;
			           System.out.println(x);
			        }
			        System.out.println(j);
			        System.out.println(k);
			        System.out.println(l);
			        System.out.println(s);
			        System.out.println(t);
			        System.out.println(w);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));
	}

	@Test
	public void testDoNotMoveAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.io.File;

			public class E1 {
			    public boolean dontMoveForNestedReassignement() {
			        boolean reassignedVar= true;
			        if (System.lineSeparator()) {
			            reassignedVar = false;
			        }
			        reassignedVar = "\\n".equals(File.pathSeparator);
			        return reassignedVar;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E1.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		disable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		String output= """
			package test1;

			import java.io.File;

			public class E1 {
			    public boolean dontMoveForNestedReassignement() {
			        boolean reassignedVar;
			        if (System.lineSeparator()) {
			            reassignedVar = false;
			        }
			        reassignedVar = "\\n".equals(File.pathSeparator);
			        return reassignedVar;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));

		input= """
			package test1;

			import java.io.File;

			public class E2 {
			    public boolean dontMoveForUndefinedVarst() {
			        boolean reassignedVar= true;
			        String pathSep= File.pathSeparator;
			        reassignedVar = "\\n".equals(pathSep);
			        return reassignedVar;
			    }
			}
			""";
		cu= pack.createCompilationUnit("E2.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		disable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);
		disable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		output= """
			package test1;

			import java.io.File;

			public class E2 {
			    public boolean dontMoveForUndefinedVarst() {
			        boolean reassignedVar;
			        String pathSep= File.pathSeparator;
			        reassignedVar = "\\n".equals(pathSep);
			        return reassignedVar;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));
	}

	@Test
	public void testDontMoveUpOverriddenAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public boolean dontMoveUpIfUsingUndefined() {\n" //
				+ "        int totalHeight = 0;\n" //;
				+ "        int innerHeight = 0;\n" //
				+ "        int topMargin= 0;\n" //
				+ "        int bottomMargin = 0;\n" //
				+ "        totalHeight = topMargin + innerHeight + bottomMargin;\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		disable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public boolean dontMoveUpIfUsingUndefined() {\n" //
				+ "        int totalHeight;\n" //;
				+ "        int innerHeight = 0;\n" //
				+ "        int topMargin= 0;\n" //
				+ "        int bottomMargin = 0;\n" //
				+ "        totalHeight = topMargin + innerHeight + bottomMargin;\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.OverriddenAssignmentCleanUp_description)));
	}

	@Test
	public void testDoNotRemoveAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;
			import java.io.File;
			import java.util.List;

			public class E {
			    public boolean doNotRemoveWithOrAssignment() {
			        boolean isValid = true;
			        isValid |= false;
			        isValid = false;
			        return isValid;
			    }

			    public long doNotRemoveWithMinusAssignment() {
			        long decrementedVar = 123;
			        decrementedVar -= 456;
			        decrementedVar = 789;
			        return decrementedVar;
			    }

			    public long doNotRemoveWithEmbeddedPlusAssignment() {
			        long incrementedVar = 123;
			        long dummy = incrementedVar += 456;
			        incrementedVar = 789;
			        return incrementedVar;
			    }

			    public List<String> doNotRemoveActiveInit() {
			        List<String> aList = Arrays.asList("lorem", "ipsum");

			        boolean reassignedVar = aList.remove("lorem");
			        reassignedVar = "\\n".equals(File.pathSeparator);
			        return aList;
			    }

			    public String doNotRemoveInitWithoutOverriding() {
			        String usedVar = "";
			        return usedVar;
			    }

			    public String doNotRemoveInitWithUse() {
			        String usedVar = "";
			        System.out.println(usedVar);
			        usedVar = File.pathSeparator;
			        return usedVar;
			    }

			    public String doNotRemoveInitWithUseInIf() {
			        String usedVar = "";
			        if ("\\n".equals(File.pathSeparator)) {
			            System.out.println(usedVar);
			        }
			        usedVar = File.pathSeparator;
			        return usedVar;
			    }

			    public String doNotRemoveInitWithCall() {
			        String usedVar = "";
			        usedVar.length();
			        usedVar = File.pathSeparator;
			        return usedVar;
			    }

			    public char[] doNotRemoveInitWithIndex() {
			        char[] usedVar = new char[] {'a', 'b', 'c'};
			        char oneChar = usedVar[1];
			        usedVar = new char[] {'d', 'e', 'f'};
			        return usedVar;
			    }

			    public byte doNotRemoveInitWhenUsed() {
			        byte usedVar = 0;
			        usedVar = usedVar++;
			        return usedVar;
			    }

			    public String doNotRemoveInitWhenOverriddenInIf() {
			        String usedVar = "";
			        if ("\\n".equals(File.pathSeparator)) {
			            usedVar = File.pathSeparator;
			        }
			        return usedVar;
			    }

			    public boolean doNotRemoveActiveInitialization(List<String> aList) {
			        boolean reassignedActiveVar = aList.remove("foo");
			        reassignedActiveVar = "\\n".equals(File.pathSeparator);
			        return reassignedActiveVar;
			    }

			    public int doNotRemoveInitializationWithIncrement(int i) {
			        int reassignedActiveVar = i++;
			        reassignedActiveVar = 123;
			        return reassignedActiveVar + i;
			    }

			    public long doNotRemoveInitializationWithAssignment(long i, long j) {
			        long reassignedActiveVar = i = j;
			        reassignedActiveVar = 123;
			        return reassignedActiveVar + i + j;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testAddBlockBug149110_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            throw new IllegalAccessError();
			        if (true) {
			            throw new IllegalAccessError();
			        }
			        if (false)
			            System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            throw new IllegalAccessError();
			        if (true)
			            throw new IllegalAccessError();
			        if (false) {
			            System.out.println();
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testAddBlockBug149110_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            return;
			        if (true) {
			            return;
			        }
			        if (false)
			            System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (true)
			            return;
			        if (true)
			            return;
			        if (false) {
			            System.out.println();
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testRemoveBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void if_() {
			        if (true) {
			            ;
			        } else if (false) {
			            ;
			        } else {
			            ;
			        }
			       \s
			        if (true) {
			            ;;
			        } else if (false) {
			            ;;
			        } else {
			            ;;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void if_() {
			        if (true)
			            ;
			        else if (false)
			            ;
			        else
			            ;
			       \s
			        if (true) {
			            ;;
			        } else if (false) {
			            ;;
			        } else {
			            ;;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlock02() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void foo() {
			        for (;;) {
			            ;\s
			        }
			    }
			    public void bar() {
			        for (;;) {
			            ;;\s
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void foo() {
			        for (;;);
			    }
			    public void bar() {
			        for (;;) {
			            ;;\s
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlock03() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void foo() {
			        while (true) {
			            ;
			        }
			    }
			    public void bar() {
			        while (true) {
			            ;;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void foo() {
			        while (true);
			    }
			    public void bar() {
			        while (true) {
			            ;;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlock04() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void foo() {
			        do {
			            ;
			        } while (true);
			    }
			    public void bar() {
			        do {
			            ;;
			        } while (true);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void foo() {
			        do; while (true);
			    }
			    public void bar() {
			        do {
			            ;;
			        } while (true);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlock05() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void foo() {
			        int[] is= null;
			        for (int i= 0;i < is.length;i++) {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void foo() {
			        int[] is= null;
			        for (int element : is);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlockBug138628() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            if (true)
			                ;
			        } else if (true) {
			            if (false) {
			                ;
			            } else
			                ;
			        } else if (false) {
			            if (true) {
			                ;
			            }
			        } else {
			            if (true)
			                ;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            if (true)
			                ;
			        } else if (true) {
			            if (false)
			                ;
			            else
			                ;
			        } else if (false) {
			            if (true)
			                ;
			        } else if (true)
			            ;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlockBug149990() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo() {
			        if (false) {
			            while (true) {
			                if (false) {
			                    ;
			                }
			            }
			        } else
			            ;
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo() {
			        if (false) {
			            while (true)
			                if (false)
			                    ;
			        } else
			            ;
			    }
			}""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlockBug156513_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(boolean b, int[] ints) {
			        if (b) {
			            for (int i = 0; i < ints.length; i++) {
			                System.out.println(ints[i]);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo(boolean b, int[] ints) {
			        if (b)
			            for (int j : ints)
			                System.out.println(j);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveBlockBug156513_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(boolean b, int[] ints) {
			        for (int i = 0; i < ints.length; i++) {
			            for (int j = 0; j < ints.length; j++) {
			                System.out.println(ints[j]);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo(boolean b, int[] ints) {
			        for (int k : ints)
			            for (int l : ints)
			                System.out.println(l);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testElseIf() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    public void refactor(boolean isValid, boolean isEnabled) throws Exception {
			        if (isValid) {
			            // Keep this comment
			            System.out.println(isValid);
			        } else {
			            if (isEnabled) {
			                // Keep this comment
			                System.out.println(isEnabled);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.ELSE_IF);

		String output= """
			package test1;

			public class E {
			    public void refactor(boolean isValid, boolean isEnabled) throws Exception {
			        if (isValid) {
			            // Keep this comment
			            System.out.println(isValid);
			        } else if (isEnabled) {
			            // Keep this comment
			            System.out.println(isEnabled);
			        }
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_ElseIf_description)));
	}

	@Test
	public void testDoNotUseElseIf() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public void doNotRefactor(boolean isValid, boolean isEnabled) throws Exception {
			        if (isValid) {
			            System.out.println(isValid);
			        } else if (isEnabled) {
			            System.out.println(isEnabled);
			        }
			    }

			    public void doNotLoseRemainingStatements(boolean isValid, boolean isEnabled) throws Exception {
			        if (isValid) {
			            System.out.println(isValid);
			        } else {
			            if (isEnabled) {
			                System.out.println(isEnabled);
			            }

			            System.out.println("Don't forget me!");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ELSE_IF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testReduceIndentation() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.Calendar;
			import java.util.Date;
			import java.util.List;

			public class E {
			    private Date conflictingName = new Date();

			    public int refactorThen(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            i = i + 1;
			        } else {
			            // Keep this comment also
			            return 0;
			        }

			        return i;
			    }

			    public int refactorElse(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        } else {
			            // Keep this comment also
			            i = i + 1;
			        }

			        return i;
			    }

			    public int refactorWithTryCatch(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        } else {
			            // Keep this comment also
			            try {
			                throw new Exception();
			            } catch(Exception e) {
			            }
			        }

			        return i;
			    }

			    public int refactorIndentation(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        } else {
			            // Keep this comment also
			            return 1;
			        }
			    }

			    public int refactorInTry(int i) {
			        // Keep this comment
			        try {
			            if (i > 0) {
			                // Keep this comment too
			                return 1;
			            } else {
			                // Keep this comment also
			                return 2;
			            }
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			        return 0;
			    }

			    public int reduceIndentationFromElse(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        } else {
			            // Keep this comment also
			            for (Integer integer : integers) {
			                System.out.println("Reading " + integer);
			            }
			            return 51;
			        }
			    }

			    public int reduceIndentationFromIf(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            for (Integer integer : integers) {
			                System.out.println("Reading " + integer);
			            }

			            return 0;
			        } else {
			            // Keep this comment also
			            return 51;
			        }
			    }

			    public int negateCommentedCondition(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i > 0 /* comment */) {
			            // Keep this comment too
			            for (Integer integer : integers) {
			                System.out.println("Reading " + integer);
			            }

			            return 0;
			        } else {
			            // Keep this comment also
			            return 51;
			        }
			    }

			    public int reduceBigIndentationFromIf(int i, List<String> integers) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            try {
			                for (String integer : integers) {
			                    System.out.println("Reading " + (Integer.parseInt(integer) + 100));
			                }
			            } catch (Exception e) {
			                e.printStackTrace();
			            }
			            return 0;
			        } else {
			            // Keep this comment also
			            return 51;
			        }
			    }

			    public int refactorThenInUnbrackettedForLoop(int[] integers) {
			        for (int integer : integers)
			            if (integer > 0) {
			                // Keep this comment too
			                integer = integer + 1;
			            } else {
			                // Keep this comment
			                return 0;
			            }

			        return -1;
			    }

			    public int refactorElseInUnbrackettedForLoop(double[] reals) {
			        for (double real : reals)
			            if (real > 0) {
			                // Keep this comment
			                return 0;
			            } else {
			                // Keep this comment too
			                real = real + 1;
			                System.out.println("New value: " + real);
			            }

			        return -1;
			    }

			    public int reduceWithUnbrackettedThenAndParent(boolean isValid, boolean isActive) {
			        if (isValid)
			            if (isActive)
			                return 0; // This kind of comment is correctly handled
			            else {
			                System.out.println("Valid and active");
			            }

			        return -1;
			    }

			    public int refactorElseInSwitch(int discriminant, boolean isVisible) {
			        switch (discriminant) {
			        case 0:
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            } else {
			                // Keep this comment too
			                discriminant = discriminant + 1;
			                System.out.println("New value: " + discriminant);
			            }
			        }

			        return -1;
			    }

			    public int refactorElseInTry(int discriminant, boolean isVisible) {
			        try {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            } else {
			                // Keep this comment too
			                discriminant = discriminant + 1;
			                System.out.println("New value: " + discriminant);
			            }
			        } finally {
			            System.out.println("Finally");
			        }

			        return -1;
			    }

			    public int refactorElseInCatch(int discriminant, boolean isVisible) {
			        try {
			            System.out.println("Very dangerous code");
			        } catch (Exception e) {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            } else {
			                // Keep this comment too
			                discriminant = discriminant + 1;
			                System.out.println("New value: " + discriminant);
			            }
			        }

			        return -1;
			    }

			    public int refactorElseInFinally(int discriminant, boolean isVisible) {
			        try {
			            System.out.println("Very dangerous code");
			        } finally {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            } else {
			                // Keep this comment too
			                discriminant = discriminant + 1;
			                System.out.println("New value: " + discriminant);
			            }
			        }

			        return -1;
			    }

			    public int refactorWithoutNameConflict(int i) {
			        System.out.println("Today: " + conflictingName);

			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        } else {
			            // Keep this comment also
			            int conflictingName = 123;

			            i = i + conflictingName;
			        }

			        return i;
			    }

			    public int refactorWithThrow(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            throw new IllegalArgumentException("Positive argument");
			        } else {
			            // Keep this comment also
			            i = i + 1;
			        }

			        return i;
			    }

			    public void refactorWithContinue(List<Integer> integers) {
			        for (Integer integer : integers) {
			            // Keep this comment
			            if (integer > 0) {
			                // Keep this comment too
			                continue;
			            } else {
			                // Keep this comment also
			                System.out.println(integer);
			            }
			        }
			    }

			    public void refactorWithBreak(List<Integer> integers) {
			        for (Integer integer : integers) {
			            // Keep this comment
			            if (integer > 0) {
			                // Keep this comment too
			                break;
			            } else {
			                // Keep this comment also
			                System.out.println(integer);
			            }
			        }
			    }

			    public int refactorElse(List<Date> dates) {
			        // Keep this comment
			        if (dates.isEmpty()) {
			            return 0;
			        } else
			            return 1;
			    }

			    public void refactorUnparameterizedReturn(List<Date> dates) {
			        // Keep this comment
			        if (dates.isEmpty()) {
			        } else {
			            return;
			        }
			    }

			    public void refactorEmptyElse(List<Date> dates) {
			        // Keep this comment
			        if (dates.isEmpty()) {
			            return;
			        } else {
			        }
			    }

			    public void refactorNegativeCondition(Date date) {
			        // Keep this comment
			        if (!(date != null)) {
			            System.out.println("null args: we should not be here");
			        } else {
			            return;
			        }
			        return;
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.Calendar;
			import java.util.Date;
			import java.util.List;

			public class E {
			    private Date conflictingName = new Date();

			    public int refactorThen(int i) {
			        // Keep this comment
			        if (i <= 0) {
			            // Keep this comment also
			            return 0;
			        }
			        // Keep this comment too
			        i = i + 1;

			        return i;
			    }

			    public int refactorElse(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        }
			        // Keep this comment also
			        i = i + 1;

			        return i;
			    }

			    public int refactorWithTryCatch(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        }
			        // Keep this comment also
			        try {
			            throw new Exception();
			        } catch(Exception e) {
			        }

			        return i;
			    }

			    public int refactorIndentation(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        }
			        // Keep this comment also
			        return 1;
			    }

			    public int refactorInTry(int i) {
			        // Keep this comment
			        try {
			            if (i > 0) {
			                // Keep this comment too
			                return 1;
			            }
			            // Keep this comment also
			            return 2;
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			        return 0;
			    }

			    public int reduceIndentationFromElse(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        }
			        // Keep this comment also
			        for (Integer integer : integers) {
			            System.out.println("Reading " + integer);
			        }
			        return 51;
			    }

			    public int reduceIndentationFromIf(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i <= 0) {
			            // Keep this comment also
			            return 51;
			        }
			        // Keep this comment too
			        for (Integer integer : integers) {
			            System.out.println("Reading " + integer);
			        }

			        return 0;
			    }

			    public int negateCommentedCondition(int i, List<Integer> integers) {
			        // Keep this comment
			        if (i <= 0 /* comment */) {
			            // Keep this comment also
			            return 51;
			        }
			        // Keep this comment too
			        for (Integer integer : integers) {
			            System.out.println("Reading " + integer);
			        }

			        return 0;
			    }

			    public int reduceBigIndentationFromIf(int i, List<String> integers) {
			        // Keep this comment
			        if (i <= 0) {
			            // Keep this comment also
			            return 51;
			        }
			        // Keep this comment too
			        try {
			            for (String integer : integers) {
			                System.out.println("Reading " + (Integer.parseInt(integer) + 100));
			            }
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			        return 0;
			    }

			    public int refactorThenInUnbrackettedForLoop(int[] integers) {
			        for (int integer : integers) {
			            if (integer <= 0) {
			                // Keep this comment
			                return 0;
			            }
			            // Keep this comment too
			            integer = integer + 1;
			        }

			        return -1;
			    }

			    public int refactorElseInUnbrackettedForLoop(double[] reals) {
			        for (double real : reals) {
			            if (real > 0) {
			                // Keep this comment
			                return 0;
			            }
			            // Keep this comment too
			            real = real + 1;
			            System.out.println("New value: " + real);
			        }

			        return -1;
			    }

			    public int reduceWithUnbrackettedThenAndParent(boolean isValid, boolean isActive) {
			        if (isValid) {
			            if (isActive)
			                return 0; // This kind of comment is correctly handled
			            System.out.println("Valid and active");
			        }

			        return -1;
			    }

			    public int refactorElseInSwitch(int discriminant, boolean isVisible) {
			        switch (discriminant) {
			        case 0:
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            }
			                // Keep this comment too
			                discriminant = discriminant + 1;
			                System.out.println("New value: " + discriminant);
			        }

			        return -1;
			    }

			    public int refactorElseInTry(int discriminant, boolean isVisible) {
			        try {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            }
			            // Keep this comment too
			            discriminant = discriminant + 1;
			            System.out.println("New value: " + discriminant);
			        } finally {
			            System.out.println("Finally");
			        }

			        return -1;
			    }

			    public int refactorElseInCatch(int discriminant, boolean isVisible) {
			        try {
			            System.out.println("Very dangerous code");
			        } catch (Exception e) {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            }
			            // Keep this comment too
			            discriminant = discriminant + 1;
			            System.out.println("New value: " + discriminant);
			        }

			        return -1;
			    }

			    public int refactorElseInFinally(int discriminant, boolean isVisible) {
			        try {
			            System.out.println("Very dangerous code");
			        } finally {
			            if (isVisible) {
			                // Keep this comment
			                return 0;
			            }
			            // Keep this comment too
			            discriminant = discriminant + 1;
			            System.out.println("New value: " + discriminant);
			        }

			        return -1;
			    }

			    public int refactorWithoutNameConflict(int i) {
			        System.out.println("Today: " + conflictingName);

			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            return 0;
			        }
			        // Keep this comment also
			        int conflictingName = 123;

			        i = i + conflictingName;

			        return i;
			    }

			    public int refactorWithThrow(int i) {
			        // Keep this comment
			        if (i > 0) {
			            // Keep this comment too
			            throw new IllegalArgumentException("Positive argument");
			        }
			        // Keep this comment also
			        i = i + 1;

			        return i;
			    }

			    public void refactorWithContinue(List<Integer> integers) {
			        for (Integer integer : integers) {
			            // Keep this comment
			            if (integer > 0) {
			                // Keep this comment too
			                continue;
			            }
			            // Keep this comment also
			            System.out.println(integer);
			        }
			    }

			    public void refactorWithBreak(List<Integer> integers) {
			        for (Integer integer : integers) {
			            // Keep this comment
			            if (integer > 0) {
			                // Keep this comment too
			                break;
			            }
			            // Keep this comment also
			            System.out.println(integer);
			        }
			    }

			    public int refactorElse(List<Date> dates) {
			        // Keep this comment
			        if (dates.isEmpty()) {
			            return 0;
			        }
			        return 1;
			    }

			    public void refactorUnparameterizedReturn(List<Date> dates) {
			        // Keep this comment
			        if (!dates.isEmpty()) {
			            return;
			        }
			    }

			    public void refactorEmptyElse(List<Date> dates) {
			        // Keep this comment
			        if (dates.isEmpty()) {
			            return;
			        }
			    }

			    public void refactorNegativeCondition(Date date) {
			        // Keep this comment
			        if (date != null) {
			            return;
			        }
			        System.out.println("null args: we should not be here");
			        return;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.REDUCE_INDENTATION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_ReduceIndentation_description)));
	}

	@Test
	public void testDoNotReduceIndentation() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Date;

			public class E {
			    private Date conflictingName = new Date();

			    public int doNotRefactorWithNameConflict(int i) {
			        if (i > 0) {
			            return 0;
			        } else {
			            int conflictingName = 123;
			            i = i + conflictingName;
			        }

			        int conflictingName = 321;

			        return i + conflictingName;
			    }

			    public int doNotRefactorWithNameConfusion(int i) {
			        if (i > 0) {
			            return 0;
			        } else {
			            int conflictingName = 123;
			            i = i + conflictingName;
			        }

			        System.out.println("Today: " + conflictingName);

			        return i;
			    }

			    public int doNotRefactorWithNameConfusion(int i, int discriminant) {
			        switch (discriminant) {
			        case 0:
			            if (i > 0) {
			                return 0;
			            } else {
			                int conflictingName = 123;
			                i = i + conflictingName;
			            }

			            System.out.println("Today: " + conflictingName);
			        }

			        return i;
			    }

			    public int doNotRefactorWithUnbrackettedNodeAndParent(boolean isValid, boolean isActive) {
			        if (isValid)
			            if (isActive) {
			                System.out.println("Valid and active");
			            } else
			                return 0; // This kind of comment is badly handled

			        return -1;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUCE_INDENTATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testUnnecessaryCodeBug127704_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private boolean foo() {
			        return (boolean) (Boolean) Boolean.TRUE;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			public class E {
			    private boolean foo() {
			        return Boolean.TRUE;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testUnnecessaryCodeBug127704_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private Integer foo() {
			        return (Integer) (Number) getNumber();
			    }
			    private Number getNumber() {return null;}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= """
			package test1;
			public class E {
			    private Integer foo() {
			        return (Integer) getNumber();
			    }
			    private Number getNumber() {return null;}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddParentheses01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if (i == 0 || i == 1)
			            System.out.println(i);
			       \s
			        while (i > 0 && i < 10)
			            System.out.println(1);
			       \s
			        boolean b= i != -1 && i > 10 && i < 100 || i > 20;
			       \s
			        do ; while (i > 5 && b || i < 100 && i > 90);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if ((i == 0) || (i == 1)) {
			            System.out.println(i);
			        }
			       \s
			        while ((i > 0) && (i < 10)) {
			            System.out.println(1);
			        }
			       \s
			        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);
			       \s
			        do {
			            ;
			        } while (((i > 5) && b) || ((i < 100) && (i > 90)));
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddParentheses02() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=331845
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    void foo(int i, int j) {
			        if (i + 10 != j - 5)
			            System.out.println(i - j + 10 - i * j);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= """
			package test1;
			public class E {
			    void foo(int i, int j) {
			        if ((i + 10) != (j - 5)) {
			            System.out.println(((i - j) + 10) - (i * j));
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddParenthesesBug578081() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if (i == 0 || i == 1 /* i is 0 or 1 */) // if comment
			            /* additional if comment */
			            System.out.println(i);
			       \s
			        while (i > 0 && i < 10 /* i gt 0 and lt 10 */) // while comment
			            /* additional while comment */
			            System.out.println(1);
			       \s
			        boolean b= i != -1 && i > 10 && i < 100 || i > 20;
			       \s
			        do ; while (i > 5 && b || i < 100 && i > 90);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if (i == 0 || i == 1 /* i is 0 or 1 */) { // if comment
			        	/* additional if comment */
			        	System.out.println(i);
			        }
			       \s
			        while (i > 0 && i < 10 /* i gt 0 and lt 10 */) { // while comment
			        	/* additional while comment */
			        	System.out.println(1);
			        }
			       \s
			        boolean b= i != -1 && i > 10 && i < 100 || i > 20;
			       \s
			        do {
			            ;
			        } while (i > 5 && b || i < 100 && i > 90);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveParentheses01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if ((i == 0) || (i == 1)) {
			            System.out.println(i);
			        }
			       \s
			        while ((i > 0) && (i < 10)) {
			            System.out.println(1);
			        }
			       \s
			        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);
			       \s
			        do {
			            ;
			        } while (((i > 5) && b) || ((i < 100) && (i > 90)));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test1;
			public class E {
			    void foo(int i) {
			        if (i == 0 || i == 1)
			            System.out.println(i);
			       \s
			        while (i > 0 && i < 10)
			            System.out.println(1);
			       \s
			        boolean b= i != -1 && i > 10 && i < 100 || i > 20;
			       \s
			        do; while (i > 5 && b || i < 100 && i > 90);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveParenthesesBug134739() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(boolean a) {
			        if (((a)))
			            return;
			    }
			    public void bar(boolean a, boolean b) {
			        if (((a)) || ((b)))
			            return;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(boolean a) {
			        if (a)
			            return;
			    }
			    public void bar(boolean a, boolean b) {
			        if (a || b)
			            return;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveParenthesesBug134741_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public boolean foo(Object o) {
			        if ((((String)o)).equals(""))
			            return true;
			        return false;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public boolean foo(Object o) {
			        if (((String)o).equals(""))
			            return true;
			        return false;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveParenthesesBug134741_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(boolean a) {
			        if (("" + "b").equals("a"))
			            return;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testRemoveParenthesesBug134741_3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public String foo2(String s) {
			        return (s != null ? s : "").toString();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testRemoveParenthesesBug134985_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public boolean foo(String s1, String s2, boolean a, boolean b) {
			        return (a == b) == (s1 == s2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public boolean foo(String s1, String s2, boolean a, boolean b) {
			        return a == b == (s1 == s2);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testRemoveParenthesesBug134985_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public String foo() {
			        return ("" + 3) + (3 + 3);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public String foo() {
			        return "" + 3 + (3 + 3);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testRemoveParenthesesBug188207() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public int foo() {
			        boolean b= (true ? true : (true ? false : true));
			        return ((b ? true : true) ? 0 : 1);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public int foo() {
			        boolean b= true ? true : true ? false : true;
			        return (b ? true : true) ? 0 : 1;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testRemoveParenthesesBug208752() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        double d = 2.0 * (0.5 / 4.0);
			        int spaceCount = (3);
			        spaceCount = 2 * (spaceCount / 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        double d = 2.0 * (0.5 / 4.0);
			        int spaceCount = 3;
			        spaceCount = 2 * (spaceCount / 2);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testRemoveParenthesesBug190188() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        (new Object()).toString();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        new Object().toString();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testRemoveParenthesesBug212856() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int foo() {
			        int n= 1 + (2 - 3);
			        n= 1 - (2 + 3);
			        n= 1 - (2 - 3);
			        n= 1 * (2 * 3);
			        return 2 * (n % 10);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test1;
			public class E1 {
			    public int foo() {
			        int n= 1 + 2 - 3;
			        n= 1 - (2 + 3);
			        n= 1 - (2 - 3);
			        n= 1 * 2 * 3;
			        return 2 * (n % 10);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_1() throws Exception {
		//while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(boolean a) {
			        while (((a))) {
			        }
			    }
			    public void bar(int x) {
			        while ((x > 2)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(boolean a) {
			        while (a) {
			        }
			    }
			    public void bar(int x) {
			        while (x > 2) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_2() throws Exception {
		//do while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        do {
			        } while ((x > 2));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        do {
			        } while (x > 2);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_3() throws Exception {
		//for loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        for (int x = 0; (x > 2); x++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        for (int x = 0; x > 2; x++) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_4() throws Exception {
		//switch statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        switch ((x - 2)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        switch (x - 2) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_5() throws Exception {
		//switch case expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        switch (x) {
			        case (1 + 2):
			            break;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        switch (x) {
			        case 1 + 2:
			            break;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_6() throws Exception {
		//throw statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int type) throws Exception {
			        throw (type == 1 ? new IllegalArgumentException() : new Exception());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int type) throws Exception {
			        throw type == 1 ? new IllegalArgumentException() : new Exception();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_7() throws Exception {
		//synchronized statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    private static final Object OBJECT = new Object();
			    private static final String STRING = new String();
			   \s
			    public void foo(int x) {
			        synchronized ((x == 1 ? STRING : OBJECT)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    private static final Object OBJECT = new Object();
			    private static final String STRING = new String();
			   \s
			    public void foo(int x) {
			        synchronized (x == 1 ? STRING : OBJECT) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_8() throws Exception {
		//assert statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        assert (x > 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        assert x > 2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_9() throws Exception {
		//assert statement message expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        assert x > 2 : (x - 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        assert x > 2 : x - 2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_10() throws Exception {
		//array access index expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int a[], int x) {
			        int i = a[(x + 2)];
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int a[], int x) {
			        int i = a[x + 2];
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_11() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? (x > 5 ? x - 1 : x - 2): x;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? x > 5 ? x - 1 : x - 2: x;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_12() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? x: (x > 5 ? x - 1 : x - 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? x: x > 5 ? x - 1 : x - 2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_13() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? (x = x - 2): x;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? (x = x - 2): x;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_14() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? x: (x = x - 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int i = x > 10 ? x: (x = x - 2);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_15() throws Exception {
		//shift operators
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int m= (x >> 2) >> 1;
			        m= x >> (2 >> 1);
			        int n= (x << 2) << 1;
			        n= x << (2 << 1);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x) {
			        int m= x >> 2 >> 1;
			        m= x >> (2 >> 1);
			        int n= x << 2 << 1;
			        n= x << (2 << 1);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_16() throws Exception {
		//integer multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x, long y) {
			        int m= (4 * x) * 2;
			        int n= 4 * (x * 2);
			        int p= 4 * (x % 3);
			        int q= 4 * (x / 3);
			        int r= 4 * (x * y);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x, long y) {
			        int m= 4 * x * 2;
			        int n= 4 * x * 2;
			        int p= 4 * (x % 3);
			        int q= 4 * (x / 3);
			        int r= 4 * (x * y);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_17() throws Exception {
		//floating point multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(double x) {
			        int m= (4.0 * x) * 0.5;
			        int n= 4.0 * (x * 0.5);
			        int p= 4.0 * (x / 100);
			        int q= 4.0 * (x % 3);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(double x) {
			        int m= 4.0 * x * 0.5;
			        int n= 4.0 * (x * 0.5);
			        int p= 4.0 * (x / 100);
			        int q= 4.0 * (x % 3);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_18() throws Exception {
		//integer addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(int x, long y) {
			        int m= (4 + x) + 2;
			        int n= 4 + (x + 2);
			        int p= 4 + (x + y);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(int x, long y) {
			        int m= 4 + x + 2;
			        int n= 4 + x + 2;
			        int p= 4 + (x + y);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_19() throws Exception {
		//floating point addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(double x) {
			        int m= (4.0 + x) + 100.0;
			        int n= 4.0 + (x + 100.0);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(double x) {
			        int m= 4.0 + x + 100.0;
			        int n= 4.0 + (x + 100.0);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug335173_20() throws Exception {
		//string concatenation
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo(String s, String t, String u) {
			        String a= (s + t) + u;
			        String b= s + (t + u);
			        String c= (1 + 2) + s;
			        String d= 1 + (2 + s);
			        String e= s + (1 + 2);
			        String f= (s + 1) + 2;
			        String g= (1 + s) + 2;
			        String h= 1 + (s + 2);
			        String i= s + (1 + t);
			        String j= s + (t + 1);
			        String k= s + (1 - 2);
			        String l= s + (1 * 2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo(String s, String t, String u) {
			        String a= s + t + u;
			        String b= s + t + u;
			        String c= 1 + 2 + s;
			        String d= 1 + (2 + s);
			        String e= s + (1 + 2);
			        String f= s + 1 + 2;
			        String g= 1 + s + 2;
			        String h= 1 + s + 2;
			        String i= s + 1 + t;
			        String j= s + t + 1;
			        String k= s + (1 - 2);
			        String l= s + 1 * 2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug405096_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    final Short cache[] = new Short[-(-128) + 1];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    int a= 10;
			    final Short cache[] = new Short[-(-a) + 1];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    int a= 10;
			    final Short cache[] = new Short[-(--a) + 1];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;

			public class E {
			    int a= 10;
			    final Short cache[] = new Short[+(+a) + 1];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    int a= 10
			    final Short cache[] = new Short[+(++a) + +(-127)];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    int a= 10
			    final Short cache[] = new Short[+(++a) + +-127];
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug405096_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    final Short cache[] = new Short[+(+128) + ~(-127)];
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    final Short cache[] = new Short[+(+128) + ~-127];
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesBug405096_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    String a= "";
			    int n= 0;
			   \s
			    int i1 = 1+(1+(+128));
			    int j1 = 1+(1+(+n));
			    int i2 = 1-(-128);
			    int j2 = 1-(-n);
			    int i3 = 1+(++n);
			    int j3 = 1-(--n);
			    String s1 = a+(++n);
			    String s2 = a+(+128);
			    int i5 = 1+(--n);
			    int j5 = 1-(++n);
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    String a= "";
			    int n= 0;
			   \s
			    int i1 = 1+1+(+128);
			    int j1 = 1+1+(+n);
			    int i2 = 1-(-128);
			    int j2 = 1-(-n);
			    int i3 = 1+(++n);
			    int j3 = 1-(--n);
			    String s1 = a+(++n);
			    String s2 = a+(+128);
			    int i5 = 1+--n;
			    int j5 = 1-++n;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveParenthesesIssue1319() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo() {
			        Integer i = 0;
			        (i++).toString();
			        (++i).toString();
			        (i--).toString();
			        (--i).toString();
			        ((i++)).toString();
			        ((++i)).toString();
			        ((i--)).toString();
			        ((--i)).toString();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= """
			package test;
			public class E {
			    public void foo() {
			        Integer i = 0;
			        (i++).toString();
			        (++i).toString();
			        (i--).toString();
			        (--i).toString();
			        (i++).toString();
			        (++i).toString();
			        (i--).toString();
			        (--i).toString();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveQualifier01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int foo;
			    public void setFoo(int foo) {
			        this.foo= foo;
			    }
			    public int getFoo() {
			        return this.foo;
			    }  \s
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 {
			    public int foo;
			    public void setFoo(int foo) {
			        this.foo= foo;
			    }
			    public int getFoo() {
			        return foo;
			    }  \s
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testNumberSuffix() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    private long usual = 101l;
			    private long octal = 0121l;
			    private long hex = 0xdafdafdafl;

			    private float usualFloat = 101f;
			    private float octalFloat = 0121f;

			    private double usualDouble = 101d;

			    public long refactorIt() {
			        long localVar = 11l;
			        return localVar + 333l;
			    }

			    public double doNotRefactor() {
			        long l = 11L;
			        float f = 11F;
			        double d = 11D;
			        float localFloat = 11f;
			        double localDouble = 11d;
			        return l + 101L + f + 11F + d + 11D + localFloat + 11f + localDouble + 11d;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NUMBER_SUFFIX);

		sample= """
			package test1;

			public class E1 {
			    private long usual = 101L;
			    private long octal = 0121L;
			    private long hex = 0xdafdafdafL;

			    private float usualFloat = 101f;
			    private float octalFloat = 0121f;

			    private double usualDouble = 101d;

			    public long refactorIt() {
			        long localVar = 11L;
			        return localVar + 333L;
			    }

			    public double doNotRefactor() {
			        long l = 11L;
			        float f = 11F;
			        double d = 11D;
			        float localFloat = 11f;
			        double localDouble = 11d;
			        return l + 101L + f + 11F + d + 11D + localFloat + 11f + localDouble + 11d;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRegExPrecompilation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {
			    private String dateValidation= ".*";
			    private static boolean valid;

			    static {
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			        String dateValidation2= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			        String date1= "1962-12-18";
			        String date2= "2000-03-15";

			        // Keep this comment too
			        valid= date1.matches(dateValidation) && date2.matches(dateValidation)
			                && date1.matches(dateValidation2) && date2.matches(dateValidation2);
			    }

			    public boolean usePattern(String date1, String date2) {
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			        String dateValidation2= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			        // Keep this comment too
			        return date1.matches(dateValidation) && date2.matches(dateValidation)
			                && date1.matches(dateValidation2) && date2.matches(dateValidation2);
			    }

			    public boolean usePatternAmongStatements(String date1, String date2) {
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			        System.out.println("Do other things");

			        // Keep this comment too
			        return date1.matches(dateValidation) && date2.matches(dateValidation);
			    }

			    public String usePatternForReplace(String date1, String date2) {
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			        // Keep this comment too
			        String dateText1= date1.replaceFirst(dateValidation, "0000-00-00");
			        // Keep this comment also
			        String dateText2= date2.replaceAll(dateValidation, "0000-00-00");

			        return dateText1 + dateText2;
			    }

			    public String usePatternForSplit1(String speech1, String speech2) {
			        // Keep this comment
			        String line= "\\\\r?\\\\n";

			        // Keep this comment too
			        String[] phrases1= speech1.split(line);
			        // Keep this comment also
			        String[] phrases2= speech2.split(line, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    public String usePatternForSplit2(String speech1, String speech2) {
			        // Keep this comment
			        String line= ".";

			        // Keep this comment too
			        String[] phrases1= speech1.split(line);
			        // Keep this comment also
			        String[] phrases2= speech2.split(line, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    public String usePatternForSplit3(String speech1, String speech2) {
			        // Keep this comment
			        String line= "\\\\a";

			        // Keep this comment too
			        String[] phrases1= speech1.split(line);
			        // Keep this comment also
			        String[] phrases2= speech2.split(line, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    public String usePatternForLocalVariableOnly(String date1, String date2, String date3) {
			        String dateText1= date1.replaceFirst(dateValidation, "0000-00-00");
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			        // Keep this comment too
			        String dateText2= date2.replaceFirst(dateValidation, "0000-00-00");
			        // Keep this comment also
			        String dateText3= date3.replaceAll(dateValidation, "0000-00-00");

			        return dateText1 + dateText2 + dateText3;
			    }

			   public boolean usePatternFromVariable(String regex, String date1, String date2) {
			        // Keep this comment
			        String dateValidation= regex;

			        // Keep this comment too
			        return date1.matches(dateValidation) && "".equals(date2.replaceFirst(dateValidation, ""));
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public class E1 {
			    private String dateValidation= ".*";
			    private static boolean valid;

			    static {
			        // Keep this comment
			        Pattern dateValidation= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			        Pattern dateValidation2= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			        String date1= "1962-12-18";
			        String date2= "2000-03-15";

			        // Keep this comment too
			        valid= dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()
			                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();
			    }

			    private static final Pattern dateValidation_pattern = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			    private static final Pattern dateValidation2_pattern = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public boolean usePattern(String date1, String date2) {
			        // Keep this comment
			        Pattern dateValidation= dateValidation_pattern;
			        Pattern dateValidation2= dateValidation2_pattern;

			        // Keep this comment too
			        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()
			                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();
			    }

			    private static final Pattern dateValidation_pattern2 = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public boolean usePatternAmongStatements(String date1, String date2) {
			        // Keep this comment
			        Pattern dateValidation= dateValidation_pattern2;
			        System.out.println("Do other things");

			        // Keep this comment too
			        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();
			    }

			    private static final Pattern dateValidation_pattern3 = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public String usePatternForReplace(String date1, String date2) {
			        // Keep this comment
			        Pattern dateValidation= dateValidation_pattern3;

			        // Keep this comment too
			        String dateText1= dateValidation.matcher(date1).replaceFirst("0000-00-00");
			        // Keep this comment also
			        String dateText2= dateValidation.matcher(date2).replaceAll("0000-00-00");

			        return dateText1 + dateText2;
			    }

			    private static final Pattern line_pattern = Pattern.compile("\\\\r?\\\\n");

			    public String usePatternForSplit1(String speech1, String speech2) {
			        // Keep this comment
			        Pattern line= line_pattern;

			        // Keep this comment too
			        String[] phrases1= line.split(speech1);
			        // Keep this comment also
			        String[] phrases2= line.split(speech2, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    private static final Pattern line_pattern2 = Pattern.compile(".");

			    public String usePatternForSplit2(String speech1, String speech2) {
			        // Keep this comment
			        Pattern line= line_pattern2;

			        // Keep this comment too
			        String[] phrases1= line.split(speech1);
			        // Keep this comment also
			        String[] phrases2= line.split(speech2, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    private static final Pattern line_pattern3 = Pattern.compile("\\\\a");

			    public String usePatternForSplit3(String speech1, String speech2) {
			        // Keep this comment
			        Pattern line= line_pattern3;

			        // Keep this comment too
			        String[] phrases1= line.split(speech1);
			        // Keep this comment also
			        String[] phrases2= line.split(speech2, 123);

			        return Arrays.toString(phrases1) + Arrays.toString(phrases2);
			    }

			    private static final Pattern dateValidation_pattern4 = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public String usePatternForLocalVariableOnly(String date1, String date2, String date3) {
			        String dateText1= date1.replaceFirst(dateValidation, "0000-00-00");
			        // Keep this comment
			        Pattern dateValidation= dateValidation_pattern4;

			        // Keep this comment too
			        String dateText2= dateValidation.matcher(date2).replaceFirst("0000-00-00");
			        // Keep this comment also
			        String dateText3= dateValidation.matcher(date3).replaceAll("0000-00-00");

			        return dateText1 + dateText2 + dateText3;
			    }

			    public boolean usePatternFromVariable(String regex, String date1, String date2) {
			        // Keep this comment
			        Pattern dateValidation= Pattern.compile(regex);

			        // Keep this comment too
			        return dateValidation.matcher(date1).matches() && "".equals(dateValidation.matcher(date2).replaceFirst(""));
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRegExPrecompilationInDefaultMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public interface I1 {
			    private String dateValidation= ".*";

			    public default boolean usePattern(String date1, String date2) {
			        // Keep this comment
			        String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			        String dateValidation2= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			        // Keep this comment too
			        return date1.matches(dateValidation) && date2.matches(dateValidation)
			                && date1.matches(dateValidation2) && date2.matches(dateValidation2);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public interface I1 {
			    private String dateValidation= ".*";

			    public default boolean usePattern(String date1, String date2) {
			        // Keep this comment
			        Pattern dateValidation= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			        Pattern dateValidation2= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			        // Keep this comment too
			        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()
			                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRegExPrecompilationInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {

			    private String dateValidation= ".*";

			    private class Inner1 {
			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			            // Keep this comment too
			            return date1.matches(dateValidation) && date2.matches(dateValidation);
			        }
			    }

			    private static class Inner2 {
			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			            // Keep this comment too
			            return date1.matches(dateValidation) && date2.matches(dateValidation);
			        }
			    }

			    public void foo() {
			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			            // Keep this comment too
			            return date1.matches(dateValidation) && date2.matches(dateValidation);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public class E1 {

			    private String dateValidation= ".*";

			    private class Inner1 {
			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            Pattern dateValidation= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			            // Keep this comment too
			            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();
			        }
			    }

			    private static class Inner2 {
			        private static final Pattern dateValidation_pattern = Pattern
			                .compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            Pattern dateValidation= dateValidation_pattern;

			            // Keep this comment too
			            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();
			        }
			    }

			    private static final Pattern dateValidation_pattern = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public void foo() {
			        public default boolean usePattern(String date1, String date2) {
			            // Keep this comment
			            Pattern dateValidation= dateValidation_pattern;

			            // Keep this comment too
			            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRegExPrecompilationInLocalClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {
			    private String dateValidation= ".*";

			    public void foo() {
			        class Inner {
			            public default boolean usePattern(String date1, String date2) {
			                // Keep this comment
			                String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			                String dateValidation2= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			                // Keep this comment too
			                return date1.matches(dateValidation) && date2.matches(dateValidation)
			                    && date1.matches(dateValidation2) && date2.matches(dateValidation2);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public class E1 {
			    private String dateValidation= ".*";

			    public void foo() {
			        class Inner {
			            public default boolean usePattern(String date1, String date2) {
			                // Keep this comment
			                Pattern dateValidation= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			                Pattern dateValidation2= Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			                // Keep this comment too
			                return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()
			                        && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRegExPrecompilationInAnonymousClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {

			    abstract class I1 {
			        public abstract boolean validate(String date1, String date2);
			    }

			    public void foo() {
			        I1 i1= new I1() {
			            @Override
			            public boolean validate(String date1, String date2) {
			                // Keep this comment
			                String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			                String dateValidation2= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			                // Keep this comment too
			                return date1.matches(dateValidation) && date2.matches(dateValidation)
			                        && date1.matches(dateValidation2) && date2.matches(dateValidation2);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public class E1 {

			    abstract class I1 {
			        public abstract boolean validate(String date1, String date2);
			    }

			    private static final Pattern dateValidation_pattern = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");
			    private static final Pattern dateValidation2_pattern = Pattern.compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public void foo() {
			        I1 i1= new I1() {
			            @Override
			            public boolean validate(String date1, String date2) {
			                // Keep this comment
			                Pattern dateValidation= dateValidation_pattern;
			                Pattern dateValidation2= dateValidation2_pattern;

			                // Keep this comment too
			                return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()
			                        && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();
			            }
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSingleUsedFieldInInnerClass() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public class SubClass {
			        private int refactorField;

			        public void refactorFieldInSubClass() {
			            this.refactorField = 123;
			            System.out.println(refactorField);
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public class SubClass {
			        public void refactorFieldInSubClass() {
			            int refactorField = 123;
			            System.out.println(refactorField);
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldWithComplexUse() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.List;

			public class E {
			    private short refactorFieldWithComplexUse= 42;

			    public void refactorFieldWithComplexUse(boolean b, List<String> texts) {
			        // Keep this comment
			        refactorFieldWithComplexUse = 123;
			        if (b) {
			            System.out.println(refactorFieldWithComplexUse);
			        } else {
			            refactorFieldWithComplexUse = 321;

			            for (String text : texts) {
			                System.out.println(text);
			                System.out.println(this.refactorFieldWithComplexUse);
			            }
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.List;

			public class E {
			    public void refactorFieldWithComplexUse(boolean b, List<String> texts) {
			        // Keep this comment
			        short refactorFieldWithComplexUse = 123;
			        if (b) {
			            System.out.println(refactorFieldWithComplexUse);
			        } else {
			            refactorFieldWithComplexUse = 321;

			            for (String text : texts) {
			                System.out.println(text);
			                System.out.println(refactorFieldWithComplexUse);
			            }
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldArray() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private int refactorArray[];

			    public void refactorArray() {
			        // Keep this comment
			        this.refactorArray = new int[]{123};
			        System.out.println(refactorArray);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void refactorArray() {
			        // Keep this comment
			        int refactorArray[] = new int[]{123};
			        System.out.println(refactorArray);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldInMultiFragment() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private int refactorOneFragment, severalUses;

			    public void refactorOneFragment() {
			        // Keep this comment
			        refactorOneFragment = 123;
			        System.out.println(refactorOneFragment);
			    }

			    public void severalUses() {
			        severalUses = 123;
			        System.out.println(severalUses);
			    }

			    public void severalUses(int i) {
			        severalUses = i;
			        System.out.println(severalUses);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    private int severalUses;

			    public void refactorOneFragment() {
			        // Keep this comment
			        int refactorOneFragment = 123;
			        System.out.println(refactorOneFragment);
			    }

			    public void severalUses() {
			        severalUses = 123;
			        System.out.println(severalUses);
			    }

			    public void severalUses(int i) {
			        severalUses = i;
			        System.out.println(severalUses);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldStatic() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private static long refactorStaticField;

			    public void refactorStaticField() {
			        // Keep this comment
			        refactorStaticField = 123;
			        System.out.println(refactorStaticField);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void refactorStaticField() {
			        // Keep this comment
			        long refactorStaticField = 123;
			        System.out.println(refactorStaticField);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldWithSameNameAsLocalVariable() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private int refactorFieldWithSameNameAsLocalVariable;

			    public void refactorFieldWithSameNameAsLocalVariable() {
			        refactorFieldWithSameNameAsLocalVariable = 123;
			        System.out.println(test1.E.this.refactorFieldWithSameNameAsLocalVariable);
			    }

			    public void methodWithLocalVariable() {
			        long refactorFieldWithSameNameAsLocalVariable = 123;
			        System.out.println(refactorFieldWithSameNameAsLocalVariable);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void refactorFieldWithSameNameAsLocalVariable() {
			        int refactorFieldWithSameNameAsLocalVariable = 123;
			        System.out.println(refactorFieldWithSameNameAsLocalVariable);
			    }

			    public void methodWithLocalVariable() {
			        long refactorFieldWithSameNameAsLocalVariable = 123;
			        System.out.println(refactorFieldWithSameNameAsLocalVariable);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testSingleUsedFieldWithSameNameAsAttribute() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    private int out;

			    public void refactorFieldWithSameNameAsAttribute() {
			        out = 123;
			        System.out.println(out);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void refactorFieldWithSameNameAsAttribute() {
			        int out = 123;
			        System.out.println(out);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SINGLE_USED_FIELD);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration,
						MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var)));
	}

	@Test
	public void testKeepSingleUsedField() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.Arrays;
			import java.util.List;

			public class E {
			    public int doNotRefactorPublicField;
			    protected int doNotRefactorProtectedField;
			    int doNotRefactorPackageField;
			    private int doNotRefactorFieldsInSeveralMethods;
			    private int doNotRefactorFieldInOtherField;
			    private int oneField = doNotRefactorFieldInOtherField;
			    private int doNotRefactorReadFieldBeforeAssignment;
			    private int doNotRefactorUnusedField;
			    private List<String> dynamicList= new ArrayList<>(Arrays.asList("foo", "bar"));
			    private boolean doNotRefactorFieldWithActiveInitializer = dynamicList.remove("foo");
			    private Runnable doNotRefactorObject;
			    @Deprecated
			    private int doNotRefactorFieldWithAnnotation;
			    private int doNotRefactorFieldsInLambda;
			    private int doNotRefactorFieldsInAnnonymousClass;

			    public void doNotRefactorPublicField() {
			        doNotRefactorPublicField = 123;
			        System.out.println(doNotRefactorPublicField);
			    }

			    public void doNotRefactorProtectedField() {
			        doNotRefactorProtectedField = 123;
			        System.out.println(doNotRefactorProtectedField);
			    }

			    public void doNotRefactorPackageField() {
			        doNotRefactorPackageField = 123;
			        System.out.println(doNotRefactorPackageField);
			    }

			    public void doNotRefactorFieldsInSeveralMethods() {
			        doNotRefactorFieldsInSeveralMethods = 123;
			        System.out.println(doNotRefactorFieldsInSeveralMethods);
			    }

			    public void doNotRefactorFieldsInSeveralMethods(int i) {
			        doNotRefactorFieldsInSeveralMethods = i;
			        System.out.println(doNotRefactorFieldsInSeveralMethods);
			    }

			    public void doNotRefactorReadFieldBeforeAssignment() {
			        System.out.println(doNotRefactorReadFieldBeforeAssignment);
			        doNotRefactorReadFieldBeforeAssignment = 123;
			        System.out.println(doNotRefactorReadFieldBeforeAssignment);
			    }

			    public void doNotRefactorFieldInOtherField() {
			        doNotRefactorFieldInOtherField = 123;
			        System.out.println(doNotRefactorFieldInOtherField);
			    }

			    public void doNotRefactorFieldWithActiveInitializer() {
			        doNotRefactorFieldWithActiveInitializer = true;
			        System.out.println(doNotRefactorFieldWithActiveInitializer);
			    }

			    public void doNotRefactorObject() {
			        doNotRefactorObject = new Runnable() {
			            @Override
			            public void run() {
			                while (true) {
			                    System.out.println("Don't stop me!");
			                }
			            }
			        };
			        doNotRefactorObject.run();
			    }

			    public void doNotRefactorFieldWithAnnotation() {
			        doNotRefactorFieldWithAnnotation = 123456;
			        System.out.println(doNotRefactorFieldWithAnnotation);
			    }

			    public class SubClass {
			        private int subClassField = 42;

			        public void doNotRefactorFieldInSubClass() {
			            this.subClassField = 123;
			            System.out.println(subClassField);
			        }
			    }

			    public void oneMethod() {
			        SubClass aSubClass = new SubClass();
			        System.out.println(aSubClass.subClassField);
			    }

			    public Runnable doNotRefactorFieldsInLambda() {
			        doNotRefactorFieldsInLambda = 123;
			        return () -> doNotRefactorFieldsInLambda++;
			    }

			    public Runnable doNotRefactorFieldsInAnnonymousClass() {
			        doNotRefactorFieldsInAnnonymousClass = 123;
			        return new Runnable() {
			            @Override
			            public void run() {
			                doNotRefactorFieldsInAnnonymousClass++;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SINGLE_USED_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testBreakLoop() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			public class E {
			    private int[] innerArray = new int[10];

			    public String addBreak(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakInForeachLoop(int[] array) {
			        boolean isFound = false;

			        for (int i : array) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakWithField() {
			        boolean isFound = false;

			        for (int i = 0; i < this.innerArray.length; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakWithoutBlock(int[] array) {
			        boolean isFound = false;

			        for (int i : array) {
			            // Keep this comment
			            if (i == 42)
			                isFound = true;
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakAfterSeveralAssignments(String[] array, boolean isFound, int count) {
			        for (String text : array) {
			            if (text == null) {
			                // Keep this comment
			                isFound = true;
			                count = 1;
			            }
			        }

			        if (isFound) {
			            return "We have found " + count + " result(s)";
			        } else {
			            return "The result has not been found";
			        }
			    }

			    public String addBreakAfterComplexAssignment(int[] array) {
			        int hourNumber = 0;

			        for (int dayNumber : array) {
			            if (dayNumber == 7) {
			                // Keep this comment
			                hourNumber = 7 * 24;
			            }
			        }

			        return "Hour number: " + hourNumber;
			    }

			    public String addBreakWithTemporaryVariable(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            int temporaryInteger = i * 3;

			            if (temporaryInteger == 42) {
			                // Keep this comment
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public boolean[] addBreakWithFixedAssignment(int number, int index) {
			        boolean[] isFound = new boolean[number];

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound[index] = true;
			            }
			        }

			        return isFound;
			    }

			    public String addBreakWithUpdatedIterator(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            if (i++ == 42) {
			                // Keep this comment
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.BREAK_LOOP);

		String output= """
			package test1;

			public class E {
			    private int[] innerArray = new int[10];

			    public String addBreak(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakInForeachLoop(int[] array) {
			        boolean isFound = false;

			        for (int i : array) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakWithField() {
			        boolean isFound = false;

			        for (int i = 0; i < this.innerArray.length; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakWithoutBlock(int[] array) {
			        boolean isFound = false;

			        for (int i : array) {
			            // Keep this comment
			            if (i == 42) {
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public String addBreakAfterSeveralAssignments(String[] array, boolean isFound, int count) {
			        for (String text : array) {
			            if (text == null) {
			                // Keep this comment
			                isFound = true;
			                count = 1;
			                break;
			            }
			        }

			        if (isFound) {
			            return "We have found " + count + " result(s)";
			        } else {
			            return "The result has not been found";
			        }
			    }

			    public String addBreakAfterComplexAssignment(int[] array) {
			        int hourNumber = 0;

			        for (int dayNumber : array) {
			            if (dayNumber == 7) {
			                // Keep this comment
			                hourNumber = 7 * 24;
			                break;
			            }
			        }

			        return "Hour number: " + hourNumber;
			    }

			    public String addBreakWithTemporaryVariable(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            int temporaryInteger = i * 3;

			            if (temporaryInteger == 42) {
			                // Keep this comment
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }

			    public boolean[] addBreakWithFixedAssignment(int number, int index) {
			        boolean[] isFound = new boolean[number];

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                // Keep this comment
			                isFound[index] = true;
			                break;
			            }
			        }

			        return isFound;
			    }

			    public String addBreakWithUpdatedIterator(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++) {
			            if (i++ == 42) {
			                // Keep this comment
			                isFound = true;
			                break;
			            }
			        }

			        return isFound ? "The result has been found" : "The result has not been found";
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.BreakLoopCleanUp_description)));
	}

	@Test
	public void testDoNotBreakLoop() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    private int crazyInteger = 0;

			    public String doNotBreakWithoutAssignment(int number) {
			        boolean isFound = false;
			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			            }
			        }
			        return isFound ? "The result has been found" : ("The result has not been found");
			    }

			    public boolean doNotShortcutReturn(int number) {
			        boolean isFound = false;
			        for (int i = 0; i < number; i++) {
			            if (i == 43) {
			                return false;
			            }
			            if (i == 42) {
			                isFound = true;
			            }
			        }
			        return isFound;
			    }

			    public boolean doNotShortcutThrow(int number) {
			        boolean isFound = false;
			        for (int i = 0; i < number; i++) {
			            if (i == 43) {
			                throw null;
			            }
			            if (i == 42) {
			                isFound = true;
			            }
			        }
			        return isFound;
			    }

			    public boolean doNotShortcutLabelledBreak(int number) {
			        boolean isFound = false;
			        doNotForgetMe: for (int j = 0; j < 10; j++) {
			            for (int i = 0; i < number; i++) {
			                if (i == 43) {
			                    break doNotForgetMe;
			                }
			                if (i == 42) {
			                    isFound = true;
			                }
			            }
			            isFound = false;
			        }
			        return isFound;
			    }

			    public String doNotBreakWithExternalIterator(int number) {
			        boolean isFound = false;
			        int i;
			        for (i = 0; i < number; i++) {
			            if (i == 42) {
			                isFound = true;
			            }
			        }
			        return isFound ? "The result has been found" : ("The result has not been found on " + i + " iteration(s)");
			    }

			    public String doNotBreakWithActiveConditions(int number) {
			        boolean isFound = false;
			        for (int i = 0; i < number--; i++) {
			            if (i == 42) {
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : ("The result has not been found on " + number + " iteration(s)");
			    }

			    public boolean[] doNotBreakWithChangingAssignment(int number) {
			        boolean[] hasNumber42 = new boolean[number];

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                hasNumber42[i] = true;
			            }
			        }

			        return hasNumber42;
			    }

			    public int[] doNotBreakForeachLoopWithChangingAssignment(int[] input, int[] output) {
			        for (int i : input) {
			            if (i == 42) {
			                output[i] = 123456;
			            }
			        }

			        return output;
			    }

			    public boolean[] doNotBreakWithActiveAssignment(int number, int index) {
			        boolean[] isFound = new boolean[number];

			        for (int i = 0; i < number; i++) {
			            if (i == 42) {
			                isFound[index++] = true;
			            }
			        }

			        return isFound;
			    }

			    public String doNotBreakWithActiveUpdater(int number) {
			        boolean isFound = false;

			        for (int i = 0; i < number; i++, number--) {
			            if (i == 42) {
			                isFound = true;
			            }
			        }

			        return isFound ? "The result has been found" : ("The result has not been found on " + number + " iteration(s)");
			    }

			    public String doNotBreakWithSeveralConditions(int[] array) {
			        int tenFactor = 0;

			        for (int i : array) {
			            if (i == 10) {
			                tenFactor = 1;
			            }
			            if (i == 100) {
			                tenFactor = 2;
			            }
			        }

			        return "The result: " + tenFactor;
			    }

			    public int doNotBreakWithActiveCondition(int[] array, int modifiedInteger) {
			        boolean isFound = false;

			        for (int i : array) {
			            if (i == modifiedInteger++) {
			                isFound = true;
			            }
			        }

			        return isFound ? 0 : modifiedInteger;
			    }

			    public int doNotBreakWithActiveAssignment(int[] array, int modifiedInteger) {
			        int result = 0;

			        for (int i : array) {
			            if (i == 42) {
			                result = modifiedInteger++;
			            }
			        }

			        return result;
			    }

			    public int doNotBreakWithVariableAssignment(int[] array) {
			        int result = 0;

			        new Thread() {
			            @Override
			            public void run() {
			                while (crazyInteger++ < 10000) {}
			            }
			        }.start();

			        for (int i : array) {
			            if (i == 42) {
			                result = crazyInteger;
			            }
			        }

			        return result;
			    }

			    public String doNotRefactorWithSpecialAssignment(int[] array) {
			        int tenFactor = 0;

			        for (int i : array) {
			            if (i == 10) {
			                tenFactor += 1;
			            }
			        }

			        return "The result: " + tenFactor;
			    }

			    public void doNotBreakInfiniteLoop(int[] array) {
			        int tenFactor = 0;

			        for (;;) {
			            if (crazyInteger == 10) {
			                tenFactor = 1;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.BREAK_LOOP);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStaticInnerClass() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import static java.lang.Integer.bitCount;
			import static java.lang.Integer.valueOf;

			import java.io.File;
			import java.util.Arrays;
			import java.util.Date;
			import java.util.jar.Attributes.Name;
			import java.util.List;

			public final class E {
			    public RefactorThisInnerClass keepInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteInnerInstanciation() {
			        return this.new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteQualifiedInnerInstanciation() {
			        return test1.E.this.new RefactorThisInnerClass();
			    }

			    public static RefactorThisInnerClass rewriteInnerInstanciationOnTopLevelObject() {
			        E object = new E();
			        return object.new RefactorThisInnerClass();
			    }

			    public class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class StaticInnerClass {
			        public boolean motherMethod() {
			            return true;
			        }
			    }

			    public class RefactorInnerClassInheritingStaticClass extends StaticInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class RefactorInnerClassInheritingTopLevelClass extends Date {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class RefactorThisInnerClassThatAccessesField {
			        File picture;

			        public char anotherMethod() {
			            return picture.separatorChar;
			        }
			    }

			    public class RefactorThisInnerClassThatUsesStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return CONSTANT != null;
			        }
			    }

			    public class RefactorThisInnerClassThatUsesQualifiedStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return E.CONSTANT != null;
			        }
			    }

			    public class RefactorThisInnerClassThatUsesFullyQualifiedStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return test1.E.CONSTANT != null;
			        }
			    }

			    public class RefactorInnerClassThatOnlyUsesItsFields {
			        int i;

			        public boolean anotherMethod() {
			            return i == 0;
			        }
			    }

			    public class RefactorInnerClassThatUsesStaticMethod {
			        int i;

			        public boolean anotherMethod() {
			            return aStaticMethod();
			        }
			    }

			    public final class RefactorThisFinalInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    class RefactorThisInnerClassWithoutModifier {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    @Deprecated
			    class RefactorThisInnerClassWithAnnotation {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class RefactorInnerClassThatUsesStaticImport {
			        int i;

			        public int anotherMethod() {
			            return bitCount(0);
			        }
			    }

			    public class RefactorInnerClassThatUsesStaticField {
			        int i;

			        public char anotherMethod() {
			            return File.separatorChar;
			        }
			    }

			    public class RefactorInheritedInnerClass extends File {
			        private static final long serialVersionUID = -1124849036813595100L;
			        private int i;

			        public RefactorInheritedInnerClass(File arg0, String arg1) {
			            super(arg0, arg1);
			        }

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class RefactorGenericInnerClass<T> {
			        T i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    private static final String CONSTANT= "foo";

			    private String aString= "bar";

			    public static boolean aStaticMethod() {
			        return false;
			    }

			    public boolean aMethod() {
			        return true;
			    }

			    public class RefactorInnerClassWithThisReference {
			        public RefactorInnerClassWithThisReference aMethod() {
			            return this;
			        }
			    }

			    public class RefactorInnerClassWithQualifiedThisReference {
			        public RefactorInnerClassWithQualifiedThisReference anotherMethod() {
			            return RefactorInnerClassWithQualifiedThisReference.this;
			        }
			    }

			    public class RefactorInnerClassWithMethodCall {
			        public int methodWithStaticMethodCall(List<String> texts) {
			            return texts.size();
			        }
			    }

			    public class RefactorInnerClassWithStaticMethodCallFromInteger {
			        public int methodWithStaticMethodCall(List<String> texts) {
			            return Integer.valueOf("1");
			        }
			    }

			    public class RefactorInnerClassWithTopLevelInstanciation {
			        public Date methodWithTopLevelInstanciation() {
			            return new Date();
			        }
			    }

			    public class RefactorInnerClassWithStaticInnerInstanciation {
			        public Name methodWithStaticInnerInstanciation() {
			            return new Name("foo");
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			import static java.lang.Integer.bitCount;
			import static java.lang.Integer.valueOf;

			import java.io.File;
			import java.util.Arrays;
			import java.util.Date;
			import java.util.jar.Attributes.Name;
			import java.util.List;

			public final class E {
			    public RefactorThisInnerClass keepInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteQualifiedInnerInstanciation() {
			        return new test1.E.RefactorThisInnerClass();
			    }

			    public static RefactorThisInnerClass rewriteInnerInstanciationOnTopLevelObject() {
			        E object = new E();
			        return new test1.E.RefactorThisInnerClass();
			    }

			    public static class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class StaticInnerClass {
			        public boolean motherMethod() {
			            return true;
			        }
			    }

			    public static class RefactorInnerClassInheritingStaticClass extends StaticInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class RefactorInnerClassInheritingTopLevelClass extends Date {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class RefactorThisInnerClassThatAccessesField {
			        File picture;

			        public char anotherMethod() {
			            return picture.separatorChar;
			        }
			    }

			    public static class RefactorThisInnerClassThatUsesStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return CONSTANT != null;
			        }
			    }

			    public static class RefactorThisInnerClassThatUsesQualifiedStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return E.CONSTANT != null;
			        }
			    }

			    public static class RefactorThisInnerClassThatUsesFullyQualifiedStaticField {
			        int i;

			        public boolean anotherMethod() {
			            return test1.E.CONSTANT != null;
			        }
			    }

			    public static class RefactorInnerClassThatOnlyUsesItsFields {
			        int i;

			        public boolean anotherMethod() {
			            return i == 0;
			        }
			    }

			    public static class RefactorInnerClassThatUsesStaticMethod {
			        int i;

			        public boolean anotherMethod() {
			            return aStaticMethod();
			        }
			    }

			    public static final class RefactorThisFinalInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    static class RefactorThisInnerClassWithoutModifier {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    @Deprecated
			    static
			    class RefactorThisInnerClassWithAnnotation {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class RefactorInnerClassThatUsesStaticImport {
			        int i;

			        public int anotherMethod() {
			            return bitCount(0);
			        }
			    }

			    public static class RefactorInnerClassThatUsesStaticField {
			        int i;

			        public char anotherMethod() {
			            return File.separatorChar;
			        }
			    }

			    public static class RefactorInheritedInnerClass extends File {
			        private static final long serialVersionUID = -1124849036813595100L;
			        private int i;

			        public RefactorInheritedInnerClass(File arg0, String arg1) {
			            super(arg0, arg1);
			        }

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public static class RefactorGenericInnerClass<T> {
			        T i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    private static final String CONSTANT= "foo";

			    private String aString= "bar";

			    public static boolean aStaticMethod() {
			        return false;
			    }

			    public boolean aMethod() {
			        return true;
			    }

			    public static class RefactorInnerClassWithThisReference {
			        public RefactorInnerClassWithThisReference aMethod() {
			            return this;
			        }
			    }

			    public static class RefactorInnerClassWithQualifiedThisReference {
			        public RefactorInnerClassWithQualifiedThisReference anotherMethod() {
			            return RefactorInnerClassWithQualifiedThisReference.this;
			        }
			    }

			    public static class RefactorInnerClassWithMethodCall {
			        public int methodWithStaticMethodCall(List<String> texts) {
			            return texts.size();
			        }
			    }

			    public static class RefactorInnerClassWithStaticMethodCallFromInteger {
			        public int methodWithStaticMethodCall(List<String> texts) {
			            return Integer.valueOf("1");
			        }
			    }

			    public static class RefactorInnerClassWithTopLevelInstanciation {
			        public Date methodWithTopLevelInstanciation() {
			            return new Date();
			        }
			    }

			    public static class RefactorInnerClassWithStaticInnerInstanciation {
			        public Name methodWithStaticInnerInstanciation() {
			            return new Name("foo");
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.STATIC_INNER_CLASS);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StaticInnerClassCleanUp_description)));
	}

	@Test
	public void testStaticInnerClassOnGenricTopLevelClass() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E<T> {
			    public RefactorThisInnerClass rewriteInnerInstanciationOnTopLevelObject(E<String> parameterizedObject) {
			        return parameterizedObject.new RefactorThisInnerClass();
			    }

			    private class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E<T> {
			    public RefactorThisInnerClass rewriteInnerInstanciationOnTopLevelObject(E<String> parameterizedObject) {
			        return new test1.E.RefactorThisInnerClass();
			    }

			    private static class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.STATIC_INNER_CLASS);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StaticInnerClassCleanUp_description)));
	}

	@Test
	public void testStaticInnerClassOnPrivateInnerClass() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public RefactorThisInnerClass keepInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteInnerInstanciation() {
			        return this.new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteQualifiedInnerInstanciation() {
			        return test1.E.this.new RefactorThisInnerClass();
			    }

			    private class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public RefactorThisInnerClass keepInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteInnerInstanciation() {
			        return new RefactorThisInnerClass();
			    }

			    public RefactorThisInnerClass rewriteQualifiedInnerInstanciation() {
			        return new test1.E.RefactorThisInnerClass();
			    }

			    private static class RefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.STATIC_INNER_CLASS);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StaticInnerClassCleanUp_description)));
	}

	@Test
	public void testDoNotUseStaticInnerClass() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.sql.DriverPropertyInfo;
			import org.junit.jupiter.api.Nested;

			public final class E<T> {
			    public interface DoNotRefactorInnerInterface {
			        boolean anotherMethod();
			    }

			    public class DoNotRefactorThisInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return aString != null;
			        }
			    }

			    public class DoNotRefactorClassUsingInheritedMemberAsItSNotHandledYet extends DriverPropertyInfo {
			        private static final long serialVersionUID = 1L;

			        public DoNotRefactorClassUsingInheritedMemberAsItSNotHandledYet() {
			            super("", "");
			        }

			        public boolean itSNotHandledYet() {
			            return choices != null;
			        }
			    }

			    public class DoNotRefactorInnerClassThatUsesMethod {
			        int i;

			        public boolean anotherMethod() {
			            return aMethod();
			        }
			    }

			    public boolean aMethodWithAMethodLocalInnerClass() {
			        class DoNotRefactorMethodLocalInnerClass {
			            int k;

			            boolean anotherMethod() {
			                return true;
			            }
			        }

			        return new DoNotRefactorMethodLocalInnerClass().anotherMethod();
			    }

			    public static class DoNotRefactorAlreadyStaticInnerClass {
			        int i;

			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class DoNotRefactorInnerClassWithQualifiedThis {
			        public E anotherMethod() {
			            return E.this;
			        }
			    }

			    public class DoNotRefactorInnerClassWithFullyQualifiedThis {
			        public E anotherMethod() {
			            return test1.E.this;
			        }
			    }

			    public class NotStaticClass {
			        public class DoNotRefactorInnerClassInNotStaticClass {
			            int i;

			            public boolean anotherMethod() {
			                return true;
			            }
			        }

			        public boolean anotherMethod() {
			            return aMethod();
			        }
			    }

			    public class DoNotRefactorInnerClassInheritingADynamicClass extends NotStaticClass {
			        public boolean anotherMethod() {
			            return true;
			        }
			    }

			    public class DoNotRefactorInnerClassThatInstanciateAnInnerDynamicClass {
			        public NotStaticClass anotherMethod() {
			            return new NotStaticClass();
			        }
			    }

			    private static final String CONSTANT= "foo";

			    private String aString= "bar";

			    public static boolean aStaticMethod() {
			        return false;
			    }

			    public boolean aMethod() {
			        return true;
			    }

			    public class DoNotRefactorInnerClassThatUsesTheTopLevelGenericity {
			        public T aGenericField= null;
			    }

			    @Nested
			    public class DoNotRefactorInnerClassWithJunitNestedAnnotation {
			        public int a;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STATIC_INNER_CLASS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotUseStaticInnerClassOnNotFinalTopLevelClass() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E<T> {
			    public class DoNotRefactorInnerInheritableClass {
			        boolean anotherMethod() {
			            return true;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STATIC_INNER_CLASS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotUseStaticInnerClassOnInterface() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public interface E {
			    public class DoNotRefactorInnerClassInInterface {
			        public int i;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STATIC_INNER_CLASS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStringBuilder() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.List;

			public class E {
			    public static String useStringBuilder() {
			        // Keep this comment
			        String text = "";

			        // Keep this comment also
			        text += "foo";
			        text += "bar";
			        text += "foobar";
			        // Keep this comment too
			        return text;
			    }

			    public static String useStringBuilderInFinalConcatenation() {
			        // Keep this comment
			        String text = "";

			        // Keep this comment also
			        text += "foo";
			        text += "bar";
			        text += "foobar";

			        // Keep this comment too
			        return text + "append me!";
			    }

			    public static String useStringBuilderInPreviousConcatenation() {
			        // Keep this comment
			        String text = "";

			        // Keep this comment also
			        text += "foo";
			        text += "bar";
			        text += "foobar";

			        // Keep this comment too
			        return "previous text" + text + "append me!";
			    }

			    public static String useStringBuilderWithInitializer() {
			        // Keep this comment
			        String concatenation = "foo";
			        // Keep this comment also
			        concatenation += "bar";
			        concatenation += "foobar";
			        // Keep this comment too
			        return concatenation;
			    }

			    public static String useStringBuilderWithConcatenationInitializer() {
			        // Keep this comment
			        String concatenation = "foo" + "bar";
			        // Keep this comment also
			        concatenation += "bar";
			        concatenation += "foobar";
			        // Keep this comment too
			        return concatenation;
			    }

			    public static String useStringBuilderWithNonStringInitializer() {
			        // Keep this comment
			        String concatenation = 123 + "bar";
			        // Keep this comment also
			        concatenation += "bar";
			        concatenation += "foobar";
			        // Keep this comment too
			        return concatenation;
			    }

			    public static String useStringBuilderAndRemoveValueOfMethod() {
			        // Keep this comment
			        String text = "";

			        // Keep this comment also
			        text += "foo";
			        text += "bar";
			        text += String.valueOf(123);
			        // Keep this comment too
			        return text;
			    }

			    public static String useStringBuilderAndRemoveValueOfMethodInFinalConcatenation() {
			        // Keep this comment
			        String text = "";

			        // Keep this comment also
			        text += "foo";
			        text += "bar";
			        text += "foobar";

			        // Keep this comment too
			        return text + String.valueOf(123) + new String(456);
			    }

			    public static String useStringBuilderOnBasicAssignment(int number) {
			        // Keep this comment
			        String serialization = "";
			        // Keep this comment also
			        serialization = serialization + "foo";
			        serialization = serialization + number;
			        serialization = serialization + "bar";
			        // Keep this comment too
			        return serialization;
			    }

			    public static String useStringBuilderWithExtendedOperation(String text) {
			        // Keep this comment
			        String variable = "";
			        // Keep this comment also
			        variable += text + "foo";
			        variable = variable + text + "bar";
			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderWithDifferentAssignment() {
			        // Keep this comment
			        String variousConcatenations = "";
			        // Keep this comment also
			        variousConcatenations += "foo";
			        variousConcatenations = variousConcatenations + "bar" + "foobar";
			        // Keep this comment too
			        return variousConcatenations;
			    }

			    public static String useStringBuilderWithBlock(boolean isEnabled) {
			        // Keep this comment
			        String variable = "";

			        if (isEnabled) {
			            // Keep this comment also
			            variable += "foo";
			            variable = variable + "bar";
			            variable = variable + "foobar";
			        }

			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderWithLoop(List<String> texts) {
			        // Keep this comment
			        String variable = "";

			        for (String text : texts) {
			            // Keep this comment also
			            variable = variable + "[";
			            variable += text;
			            variable = variable + "]";
			        }

			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderOnOneLoopedAssignment(List<String> texts) {
			        // Keep this comment
			        String variable = "";

			        for (String text : texts) {
			            // Keep this comment also
			            variable += text;
			        }

			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderOnOneLoopedReassignment(List<String> words) {
			        // Keep this comment
			        String variable = "";

			        for (String word : words) {
			            // Keep this comment also
			            variable = variable + word;
			        }

			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderWithWhile(String text, int i) {
			        // Keep this comment
			        String variable = "";

			        while (i-- > 0) {
			            // Keep this comment also
			            variable = variable + "{";
			            variable += text;
			            variable = variable + "}";
			        }

			        // Keep this comment too
			        return variable;
			    }

			    public static String useStringBuilderWithTry(String number, int i) {
			        // Keep this comment
			        String iterableConcatenation = "";

			        try {
			            while (i-- > 0) {
			                // Keep this comment also
			                iterableConcatenation = iterableConcatenation + "(";
			                iterableConcatenation += (Integer.parseInt(number) + 1);
			                iterableConcatenation = iterableConcatenation + ")";
			            }
			        } catch (NumberFormatException e) {
			            return "0";
			        }

			        // Keep this comment too
			        return iterableConcatenation;
			    }

			    public static String useStringBuilderWithFinally(String number) {
			        // Keep this comment
			        String variable = "";
			        int i = 123;

			        try {
			            i+= Integer.parseInt(number);
			        } catch (NumberFormatException e) {
			            System.out.println("error");
			        } finally {
			            // Keep this comment also
			            variable += "foo";
			            variable = variable + "bar";
			            variable = variable + "foobar";
			        }

			        // Keep this comment too
			        return variable + i;
			    }

			    public static String useStringBuilderWithConditionalRead(boolean isEnabled) {
			        // Keep this comment
			        String variable = "";

			        if (isEnabled) {
			            // Keep this comment also
			            variable += "foo";
			            variable = variable + "bar";
			            variable = variable + "foobar";
			            // Keep this comment too
			            return variable;
			        }

			        return "";
			    }

			    public static String useStringBuilderInElse(boolean isEnabled) {
			        // Keep this comment
			        String conditionalConcatenation = "";

			        if (isEnabled) {
			            return "OK";
			        } else {
			            // Keep this comment also
			            conditionalConcatenation += "foo";
			            conditionalConcatenation = conditionalConcatenation + "bar";
			            conditionalConcatenation = conditionalConcatenation + "foobar";
			            // Keep this comment too
			            return "Another " + "text " + conditionalConcatenation;
			        }
			    }

			    public static String useStringBuilderWithAdditions() {
			        // Keep this comment
			        String text = "1 + 2 = " + (1 + 2);

			        // Keep this comment also
			        text += " foo";
			        text += "bar ";
			        text += "3 + 4 = ";

			        // Keep this comment too
			        return text + (3 + 4);
			    }

			    public static String useStringBuilderWithNullableStart(String nullableString) {
			        // Keep this comment
			        String text = nullableString + "literal";

			        // Keep this comment also
			        text += " foo";
			        text += "bar ";

			        // Keep this comment too
			        return text + (3 + 4);
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.List;

			public class E {
			    public static String useStringBuilder() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder();

			        // Keep this comment also
			        text.append("foo");
			        text.append("bar");
			        text.append("foobar");
			        // Keep this comment too
			        return text.toString();
			    }

			    public static String useStringBuilderInFinalConcatenation() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder();

			        // Keep this comment also
			        text.append("foo");
			        text.append("bar");
			        text.append("foobar");

			        // Keep this comment too
			        return text.append("append me!").toString();
			    }

			    public static String useStringBuilderInPreviousConcatenation() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder();

			        // Keep this comment also
			        text.append("foo");
			        text.append("bar");
			        text.append("foobar");

			        // Keep this comment too
			        return "previous text" + text.append("append me!").toString();
			    }

			    public static String useStringBuilderWithInitializer() {
			        // Keep this comment
			        StringBuilder concatenation = new StringBuilder("foo");
			        // Keep this comment also
			        concatenation.append("bar");
			        concatenation.append("foobar");
			        // Keep this comment too
			        return concatenation.toString();
			    }

			    public static String useStringBuilderWithConcatenationInitializer() {
			        // Keep this comment
			        StringBuilder concatenation = new StringBuilder("foo").append("bar");
			        // Keep this comment also
			        concatenation.append("bar");
			        concatenation.append("foobar");
			        // Keep this comment too
			        return concatenation.toString();
			    }

			    public static String useStringBuilderWithNonStringInitializer() {
			        // Keep this comment
			        StringBuilder concatenation = new StringBuilder().append(123).append("bar");
			        // Keep this comment also
			        concatenation.append("bar");
			        concatenation.append("foobar");
			        // Keep this comment too
			        return concatenation.toString();
			    }

			    public static String useStringBuilderAndRemoveValueOfMethod() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder();

			        // Keep this comment also
			        text.append("foo");
			        text.append("bar");
			        text.append(123);
			        // Keep this comment too
			        return text.toString();
			    }

			    public static String useStringBuilderAndRemoveValueOfMethodInFinalConcatenation() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder();

			        // Keep this comment also
			        text.append("foo");
			        text.append("bar");
			        text.append("foobar");

			        // Keep this comment too
			        return text.append(123).append(456).toString();
			    }

			    public static String useStringBuilderOnBasicAssignment(int number) {
			        // Keep this comment
			        StringBuilder serialization = new StringBuilder();
			        // Keep this comment also
			        serialization.append("foo");
			        serialization.append(number);
			        serialization.append("bar");
			        // Keep this comment too
			        return serialization.toString();
			    }

			    public static String useStringBuilderWithExtendedOperation(String text) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();
			        // Keep this comment also
			        variable.append(text).append("foo");
			        variable.append(text).append("bar");
			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderWithDifferentAssignment() {
			        // Keep this comment
			        StringBuilder variousConcatenations = new StringBuilder();
			        // Keep this comment also
			        variousConcatenations.append("foo");
			        variousConcatenations.append("bar").append("foobar");
			        // Keep this comment too
			        return variousConcatenations.toString();
			    }

			    public static String useStringBuilderWithBlock(boolean isEnabled) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        if (isEnabled) {
			            // Keep this comment also
			            variable.append("foo");
			            variable.append("bar");
			            variable.append("foobar");
			        }

			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderWithLoop(List<String> texts) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        for (String text : texts) {
			            // Keep this comment also
			            variable.append("[");
			            variable.append(text);
			            variable.append("]");
			        }

			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderOnOneLoopedAssignment(List<String> texts) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        for (String text : texts) {
			            // Keep this comment also
			            variable.append(text);
			        }

			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderOnOneLoopedReassignment(List<String> words) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        for (String word : words) {
			            // Keep this comment also
			            variable.append(word);
			        }

			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderWithWhile(String text, int i) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        while (i-- > 0) {
			            // Keep this comment also
			            variable.append("{");
			            variable.append(text);
			            variable.append("}");
			        }

			        // Keep this comment too
			        return variable.toString();
			    }

			    public static String useStringBuilderWithTry(String number, int i) {
			        // Keep this comment
			        StringBuilder iterableConcatenation = new StringBuilder();

			        try {
			            while (i-- > 0) {
			                // Keep this comment also
			                iterableConcatenation.append("(");
			                iterableConcatenation.append(Integer.parseInt(number)).append(1);
			                iterableConcatenation.append(")");
			            }
			        } catch (NumberFormatException e) {
			            return "0";
			        }

			        // Keep this comment too
			        return iterableConcatenation.toString();
			    }

			    public static String useStringBuilderWithFinally(String number) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();
			        int i = 123;

			        try {
			            i+= Integer.parseInt(number);
			        } catch (NumberFormatException e) {
			            System.out.println("error");
			        } finally {
			            // Keep this comment also
			            variable.append("foo");
			            variable.append("bar");
			            variable.append("foobar");
			        }

			        // Keep this comment too
			        return variable.append(i).toString();
			    }

			    public static String useStringBuilderWithConditionalRead(boolean isEnabled) {
			        // Keep this comment
			        StringBuilder variable = new StringBuilder();

			        if (isEnabled) {
			            // Keep this comment also
			            variable.append("foo");
			            variable.append("bar");
			            variable.append("foobar");
			            // Keep this comment too
			            return variable.toString();
			        }

			        return "";
			    }

			    public static String useStringBuilderInElse(boolean isEnabled) {
			        // Keep this comment
			        StringBuilder conditionalConcatenation = new StringBuilder();

			        if (isEnabled) {
			            return "OK";
			        } else {
			            // Keep this comment also
			            conditionalConcatenation.append("foo");
			            conditionalConcatenation.append("bar");
			            conditionalConcatenation.append("foobar");
			            // Keep this comment too
			            return "Another " + "text " + conditionalConcatenation.toString();
			        }
			    }

			    public static String useStringBuilderWithAdditions() {
			        // Keep this comment
			        StringBuilder text = new StringBuilder("1 + 2 = ").append(1 + 2);

			        // Keep this comment also
			        text.append(" foo");
			        text.append("bar ");
			        text.append("3 + 4 = ");

			        // Keep this comment too
			        return text.append(3 + 4).toString();
			    }

			    public static String useStringBuilderWithNullableStart(String nullableString) {
			        // Keep this comment
			        StringBuilder text = new StringBuilder().append(nullableString).append("literal");

			        // Keep this comment also
			        text.append(" foo");
			        text.append("bar ");

			        // Keep this comment too
			        return text.append(3 + 4).toString();
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.STRINGBUILDER);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StringBuilderCleanUp_description)));
	}

	@Test
	public void testDoNotUseStringBuilder() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.List;

			public class E {
			    private String field = "";

			    public static String doNotRefactorWithoutAssignment() {
			        String concatenation = 123 + "bar";
			        return concatenation + String.valueOf(456);
			    }

			    public static String doNotRefactorNullString() {
			        String text = null;
			        text += "foo";
			        text += "bar";
			        text += "foobar";
			        return text;
			    }

			    public static String doNotRefactorConcatenationOfOnlyTwoStrings() {
			        String text= "foo";
			        text += "bar";
			        return text;
			    }

			    public static String doNotRefactorMultideclaration() {
			        String serialization= "", anotherSerialization= "";
			        serialization += "foo";
			        serialization += "bar";
			        serialization += "foobar";
			        return serialization;
			    }

			    public static String doNotRefactorStringUsedAsExpression() {
			        String variable= "foo";
			        variable += "bar";
			        variable += "foobar";
			        if ((variable+= "bar").contains("i")) {
			            return "foobar";
			        }
			        return variable;
			    }

			    public static String doNotUseStringBuilderWithoutAppending() {
			        String variable= "";
			        variable = "foo" + variable;
			        variable += "bar";
			        variable += "foobar";
			        return variable;
			    }

			    public static String doNotRefactorWrongAssignmentOperator() {
			        String variable= "";
			        variable = "foo";
			        variable += "bar";
			        variable += "foobar";
			        return variable;
			    }

			    public static String doNotRefactorBadAssignmentOperator() {
			        String variable= "";
			        variable += variable + "foo";
			        variable += "bar";
			        variable += "foobar";
			        return variable;
			    }

			    public static String doNotUseStringBuilderWithoutConcatenation() {
			        String variable = "";
			        return variable;
			    }

			    public static void doNotRefactorStringChangedAfterUse(String text) {
			        String variable= "";
			        variable += text + "foo";
			        variable += "bar";
			        variable += "foobar";
			        System.out.println(variable);
			        variable= variable + text + "bar";
			    }

			    public static String doNotBuildStringSeveralTimes() {
			        String variable= "";
			        variable += "foo";
			        variable += "bar";
			        variable += "foobar";
			        variable = variable + "bar";
			        return variable + variable;
			    }

			    public static List<String> doNotStringifySeveralTimes(List<String> texts) {
			        String variable= "";
			        List<String> output= new ArrayList<String>();

			        for (String text : texts) {
			            variable += text;
			            variable += "bar";
			            variable += "foobar";
			            variable = variable + ",";
			            output.add(variable);
			        }

			        return output;
			    }

			    public static void doNotStringifySeveralTimesToo(List<String> words) {
			        String variable= "";
			        variable += "foo";
			        variable = variable + "bar";
			        variable += "foobar";

			        for (String word : words) {
			            System.out.println(variable);
			        }
			    }

			    public static String doNotRefactorStringsWithoutConcatenation(boolean isEnabled) {
			        String variable1 = "First variable";
			        String variable2 = "Second variable";

			        if (isEnabled) {
			            variable1 += "foo";
			            variable1 = variable2 + "bar";
			        } else {
			            variable2 += "foo";
			            variable2 = variable1 + "bar";
			        }

			        return variable1 + variable2;
			    }

			    public static String doNotUseStringBuilderOnParameter(String variable) {
			        variable += "foo";
			        variable += "bar";
			        variable += "foobar";
			        return variable;
			    }

			    public String doNotUseStringBuilderOnField() {
			        field = "Lorem";
			        field += " ipsum";
			        field += " dolor sit amet";
			        return field;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUILDER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPlainReplacement() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.io.File;
			import java.util.regex.Matcher;
			import java.util.regex.Pattern;

			public class E {
			    private static final String CONSTANT = "&";
			    private static final String CONSTANT2 = "+";

			    public void refactorUsingString(String text, String placeholder, String value) {
			        String result1 = text.replaceAll("&", "&amp;");
			        String result2 = text.replaceAll(",:#", "/");
			        String result3 = text.replaceAll("\\\\^a", "b" + "c");
			        String result4 = text.replaceAll(CONSTANT, "&amp;");
			        String result5 = text.replaceAll("\\\\.", "\\r\\n");
			        String result6 = text.replaceAll("-\\\\.-", "\\\\$\\\\.\\\\x");
			        String result7 = text.replaceAll("foo", "\\\\\\\\-\\\\$1\\\\s");
			        String result8 = text.replaceAll("foo", "bar");
			        String result9 = text.replaceAll("\\\\$0\\\\.02", "\\\\$0.50");
			        String result10 = text.replaceAll(/*Keep this comment*/"n"/*o*/, /*Keep this comment too*/"\\\\$\\\\a\\\\\\\\"/*y*/);
			        String result11 = text.replaceAll("a" + "b", "c\\\\$");
			        String result12 = text.replaceAll("\\\\+", CONSTANT);
			        String result13 = text.replaceAll(CONSTANT, "\\\\$");
			        String result14 = text.replaceAll(CONSTANT, CONSTANT2);
			    }

			    public void removeQuote(String text) {
			        String result = text.replaceAll(Pattern.quote(File.separator), "/");
			        String result2 = text.replaceAll(Pattern.quote(File.separator + "a"), "\\\\.");
			        String result3 = text.replaceAll(Pattern.quote(placeholder), Matcher.quoteReplacement(value));
			        String result4 = text.replaceAll("\\\\.", Matcher.quoteReplacement(File.separator));
			        String result5 = text.replaceAll("/", Matcher.quoteReplacement(File.separator + "\\n"));
			        String result6 = text.replaceAll("n", Matcher.quoteReplacement(System.getProperty("java.version")));
			        String result7 = text.replaceAll(CONSTANT, Matcher.quoteReplacement(System.getProperty("java.version")));
			    }

			    public void refactorUsingChar(String text) {
			        String result = text.replaceAll("\\\\.", "/");
			        String result2 = text.replaceAll("\\\\.", "/");
			        String result3 = text.replaceAll("/", ".");
			    }

			    public String refactorUselessEscapingInReplacement() {
			        return "foo".replaceAll("foo", "\\\\.");
			    }

			    public void refactorChained() {
			        System.out.println("${p1}...???".replaceAll("\\\\$", "\\\\\\\\\\\\$")
			            .replaceAll("\\\\.", "\\\\\\\\.").replaceAll("\\\\?", "^"));
			    }
			}
			""";

		String expected= """
			package test1;

			import java.io.File;
			import java.util.regex.Matcher;
			import java.util.regex.Pattern;

			public class E {
			    private static final String CONSTANT = "&";
			    private static final String CONSTANT2 = "+";

			    public void refactorUsingString(String text, String placeholder, String value) {
			        String result1 = text.replace("&", "&amp;");
			        String result2 = text.replace(",:#", "/");
			        String result3 = text.replace("^a", "b" + "c");
			        String result4 = text.replace(CONSTANT, "&amp;");
			        String result5 = text.replace(".", "\\r\\n");
			        String result6 = text.replace("-.-", "$.x");
			        String result7 = text.replace("foo", "\\\\-$1s");
			        String result8 = text.replace("foo", "bar");
			        String result9 = text.replace("$0.02", "$0.50");
			        String result10 = text.replace(/*Keep this comment*/"n"/*o*/, /*Keep this comment too*/"$a\\\\"/*y*/);
			        String result11 = text.replace("a" + "b", "c$");
			        String result12 = text.replace("+", CONSTANT);
			        String result13 = text.replace(CONSTANT, "$");
			        String result14 = text.replace(CONSTANT, CONSTANT2);
			    }

			    public void removeQuote(String text) {
			        String result = text.replace(File.separator, "/");
			        String result2 = text.replace(File.separator + "a", ".");
			        String result3 = text.replace(placeholder, value);
			        String result4 = text.replace(".", File.separator);
			        String result5 = text.replace("/", File.separator + "\\n");
			        String result6 = text.replace("n", System.getProperty("java.version"));
			        String result7 = text.replace(CONSTANT, System.getProperty("java.version"));
			    }

			    public void refactorUsingChar(String text) {
			        String result = text.replace('.', '/');
			        String result2 = text.replace('.', '/');
			        String result3 = text.replace('/', '.');
			    }

			    public String refactorUselessEscapingInReplacement() {
			        return "foo".replace("foo", ".");
			    }

			    public void refactorChained() {
			        System.out.println("${p1}...???".replace("$", "\\\\$")
			            .replace(".", "\\\\.").replace('?', '^'));
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PLAIN_REPLACEMENT);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PlainReplacementCleanUp_description)));
	}

	@Test
	public void testDoNotUsePlainReplacement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.regex.Matcher;
			import java.util.regex.Pattern;

			public class E {
			    private static final String CONSTANT = "|";

			    public void doNotRefactorEscapableCharacters(String text) {
			        String result1 = text.replaceAll("[ab]", "c");
			        String result2 = text.replaceAll("d.e", "foo");
			        String result3 = text.replaceAll("f?", "bar");
			        String result4 = text.replaceAll("g+", "foo");
			        String result5 = text.replaceAll("h*", "foo");
			        String result6 = text.replaceAll("i{42}", "foo");
			        String result7 = text.replaceAll("j{1,42}", "foo");
			        String result8 = text.replaceAll("(k)", "foo");
			        String result9 = text.replaceAll("^m", "foo");
			        String result10 = text.replaceAll("n$", "foo");
			        String result11 = text.replaceAll("\\\\s", "");
			        String result12 = text.replaceAll("a|b", "foo");
			        String result13 = text.replaceAll("\\r\\n|\\n", " ");
			        String result14 = text.replaceAll("\\\\\\\\$", System.getProperty("java.version"));
			        String result15 = text.replaceAll(System.getProperty("java.version"), "");
			        String result16 = text.replaceAll(CONSTANT, "not &amp;");
			    }

			    public String doNotRefactorReplacementWithCapturedGroup(String text) {
			        return text.replaceAll("foo", "$0");
			    }

			    public String doNotRefactorUnknownPattern(String text, String pattern) {
			        return text.replaceAll(pattern, "c");
			    }

			    public String doNotRefactorOtherMethod(Matcher matcher, String text) {
			        return matcher.replaceAll(text);
			    }

			    public void doNotRefactorSurrogates(String text, String unquoted) {
			        String result1 = text.replaceAll("\\ud83c", "");
			        String result2 = text.replaceAll("\\\\ud83c", "");
			        String result3 = text.replaceAll("\\udf09", "\\udf10");
			        String result4 = text.replaceAll(Pattern.quote(unquoted), "\\udf10");
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PLAIN_REPLACEMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPlainReplacementPreview() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String previewHeader= """
			package test1;

			import java.io.File;
			import java.util.regex.Matcher;
			import java.util.regex.Pattern;

			public class E {
			    public void preview(String text, String placeholder, String value) {
			""";
		String previewFooter= """
			    }
			}
			""";
		AbstractCleanUp cleanUp= new PlainReplacementCleanUpCore() {
			@Override
			public boolean isEnabled(String key) {
				return false;
			}
		};
		String given= previewHeader + cleanUp.getPreview() + previewFooter;
		cleanUp= new PlainReplacementCleanUpCore() {
			@Override
			public boolean isEnabled(String key) {
				return true;
			}
		};
		String expected= previewHeader + cleanUp.getPreview() + previewFooter;

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.PLAIN_REPLACEMENT);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PlainReplacementCleanUp_description)));
	}

	@Test
	public void testControlFlowMerge() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.Arrays;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;
			import java.util.function.Predicate;

			public class E {
			    private Date j = new Date();

			    /** Common code: i++, Remove if statement */
			    public void ifElseRemoveIfNoBrackets(boolean isValid, int i) {
			        // Keep this!
			        if (isValid)
			            // Keep this comment
			            i = 1;
			        else
			            i = (2 - 1) * 1;
			    }

			    /** Common code: i++, Remove if statement */
			    public void ifElseRemoveIf(boolean b, int number) {
			        if (b) {
			            // Keep this comment
			            number = 1;
			        } else {
			            number = 001;
			        }
			    }

			    /** Common code: i++, Remove then case */
			    public void ifElseRemoveThen(boolean condition, int i, int j) {
			        if (condition) {
			            // Keep this comment
			            ++i;
			        } else {
			            j++;
			            // Keep this comment
			            i = i + 1;
			        }
			    }

			    /** Common code: i++, Remove else case */
			    public void ifElseRemoveElse(boolean b, int i, int j) {
			        if (b) {
			            j++;
			            // Keep this comment
			            i++;
			        } else {
			            // Keep this comment
			            i++;
			        }
			    }

			    /** Common code: i++, Remove second case */
			    public void reverseMiddle(boolean isActive, boolean isEnabled, int i, int j) {
			        if (isActive) {
			            j++;
			            // Keep this comment
			            i++;
			        } else if (isEnabled) {
			            // Keep this comment
			            i++;
			        } else {
			            j++;
			            // Keep this comment
			            i++;
			        }
			    }

			    /** Common code: i++, Remove second case */
			    public void reverseEmptySecond(boolean isActive, boolean isEnabled, int i, int j) {
			        if (isActive) {
			            j++;
			            // Keep this comment
			            i++;
			        } else if (isEnabled) {
			            // Keep this comment
			            i++;
			        } else if (i > 0) {
			            j--;
			            // Keep this comment
			            i++;
			        } else {
			            j++;
			            // Keep this comment
			            i++;
			        }
			    }

			    /** Only common code, Remove if statement */
			    public void ifElseRemoveIfSeveralStatements(boolean b1, boolean b2, int i, int j) {
			        if (b1) {
			            // Keep this comment
			            i++;
			            if (b2 && true) {
			                i++;
			            } else {
			                j++;
			            }
			        } else {
			            // Keep this comment
			            i++;
			            if (false || !b2) {
			                j++;
			            } else {
			                i++;
			            }
			        }
			    }

			    /** Not all cases covered, Do not remove anything */
			    public void ifElseIfNoElseDoNotTouch(boolean isValid, int k, int l) {
			        if (isValid) {
			            k++;
			            l++;
			        } else if (!isValid) {
			            k++;
			            l++;
			        }
			    }

			    /** Only common code: remove if statement */
			    public void ifElseIfElseRemoveIf(boolean b, int i, int j) {
			        if (b) {
			            // Keep this comment
			            i++;
			            j++;
			        } else if (!b) {
			            // Keep this comment
			            i++;
			            j++;
			        } else {
			            // Keep this comment
			            i++;
			            j++;
			        }
			    }

			    /** Specific code: keep some if statement */
			    public void ifElseIfElseRemoveSomeIf(boolean b1, boolean b2, List<String> modifiableList, int i, int j) {
			        if (b1) {
			            // Keep this comment
			            i++;

			            j++;
			        } else if (b2) {
			            i++;
			            // Keep this comment
			            i++;

			            j++;
			        } else if (modifiableList.remove("foo")) {
			            // Keep this comment
			            i++;

			            j++;
			        } else {
			            // Keep this comment
			            i++;

			            j++;
			        }
			    }

			    public void refactorMethodInvocation(boolean b, Object o) {
			        if (b) {
			            System.out.println(b);
			            o.toString();
			        } else {
			            o.toString();
			        }
			    }

			    public void pullDownSeveralNotFallingThroughLines(int number) {
			        if (number == 1) {
			            System.out.println("First case");
			            System.out.println("Identical");
			            System.out.println("code");
			        } else if (number == 2) {
			            System.out.println("Second case");
			            System.out.println("Identical");
			            System.out.println("code");
			        } else if (number == 3) {
			            throw new NullPointerException("I do completely other things and fall through");
			        } else {
			            System.out.println("Fourth case");
			            System.out.println("Identical");
			            System.out.println("code");
			        }
			    }

			    public void createBlockToPullDown(int number) {
			        if (number == 1) {
			            System.out.println("Completely different code");
			        } else if (number == 2) {
			            System.out.println("First case");
			            System.out.println("Identical");
			            System.out.println("code");
			        } else {
			            System.out.println("Second case");
			            System.out.println("Identical");
			            System.out.println("code");
			        }
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.Arrays;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;
			import java.util.function.Predicate;

			public class E {
			    private Date j = new Date();

			    /** Common code: i++, Remove if statement */
			    public void ifElseRemoveIfNoBrackets(boolean isValid, int i) {
			        // Keep this!
			        // Keep this comment
			        i = 1;
			    }

			    /** Common code: i++, Remove if statement */
			    public void ifElseRemoveIf(boolean b, int number) {
			        // Keep this comment
			        number = 1;
			    }

			    /** Common code: i++, Remove then case */
			    public void ifElseRemoveThen(boolean condition, int i, int j) {
			        if (!condition) {
			            j++;
			        }
			        // Keep this comment
			        ++i;
			    }

			    /** Common code: i++, Remove else case */
			    public void ifElseRemoveElse(boolean b, int i, int j) {
			        if (b) {
			            j++;
			        }
			        // Keep this comment
			        i++;
			    }

			    /** Common code: i++, Remove second case */
			    public void reverseMiddle(boolean isActive, boolean isEnabled, int i, int j) {
			        if (isActive) {
			            j++;
			        } else if (!isEnabled) {
			            j++;
			        }
			        // Keep this comment
			        i++;
			    }

			    /** Common code: i++, Remove second case */
			    public void reverseEmptySecond(boolean isActive, boolean isEnabled, int i, int j) {
			        if (isActive) {
			            j++;
			        } else if (isEnabled) {
			        } else if (i > 0) {
			            j--;
			        } else {
			            j++;
			        }
			        // Keep this comment
			        i++;
			    }

			    /** Only common code, Remove if statement */
			    public void ifElseRemoveIfSeveralStatements(boolean b1, boolean b2, int i, int j) {
			        // Keep this comment
			        i++;
			        if (b2 && true) {
			            i++;
			        } else {
			            j++;
			        }
			    }

			    /** Not all cases covered, Do not remove anything */
			    public void ifElseIfNoElseDoNotTouch(boolean isValid, int k, int l) {
			        if (isValid) {
			            k++;
			            l++;
			        } else if (!isValid) {
			            k++;
			            l++;
			        }
			    }

			    /** Only common code: remove if statement */
			    public void ifElseIfElseRemoveIf(boolean b, int i, int j) {
			        // Keep this comment
			        i++;
			        j++;
			    }

			    /** Specific code: keep some if statement */
			    public void ifElseIfElseRemoveSomeIf(boolean b1, boolean b2, List<String> modifiableList, int i, int j) {
			        if (b1) {
			        } else if (b2) {
			            i++;
			        } else if (modifiableList.remove("foo")) {
			        }
			        // Keep this comment
			        i++;

			        j++;
			    }

			    public void refactorMethodInvocation(boolean b, Object o) {
			        if (b) {
			            System.out.println(b);
			        }
			        o.toString();
			    }

			    public void pullDownSeveralNotFallingThroughLines(int number) {
			        if (number == 1) {
			            System.out.println("First case");
			        } else if (number == 2) {
			            System.out.println("Second case");
			        } else if (number == 3) {
			            throw new NullPointerException("I do completely other things and fall through");
			        } else {
			            System.out.println("Fourth case");
			        }
			        System.out.println("Identical");
			        System.out.println("code");
			    }

			    public void createBlockToPullDown(int number) {
			        if (number == 1) {
			            System.out.println("Completely different code");
			        } else {
			            if (number == 2) {
			                System.out.println("First case");
			            } else {
			                System.out.println("Second case");
			            }
			            System.out.println("Identical");
			            System.out.println("code");
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.CONTROLFLOW_MERGE);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ControlFlowMergeCleanUp_description)));
	}

	@Test
	public void testDoNotControlFlowMerge() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;
			import java.util.Date;
			import java.util.List;
			import java.util.function.Function;
			import java.util.function.Predicate;

			public class E {
			    private Date j = new Date();

			    /** No common code, Do not remove anything */
			    public void doNotRemoveNotCommonCode(boolean condition, int number1, int number2) {
			        if (condition) {
			            number1++;
			        } else {
			            number2++;
			        }
			    }

			    public int doNotRefactorDifferentVariablesInReturn(boolean condition) {
			        if (condition) {
			            int i = 1;
			            return i;
			        } else {
			            int i = 2;
			            return i;
			        }
			    }

			    public int doNotRefactorNoElse(boolean b) {
			        if (b) {
			            return 1;
			        }
			        return 1;
			    }

			    public int doNotRefactorWithNameConflict(boolean isActive) {
			        int k;

			        if (isActive) {
			            int j = 1;
			            k = j + 10;
			        } else {
			            int j = 1;
			            k = j + 10;
			        }

			        int j = 123;
			        System.out.println("Other number: " + j);
			        return k;
			    }

			    public int doNotRefactorWithNameConflictInBlock(boolean isActive) {
			        int m;

			        if (isActive) {
			            int j = 1;
			            m = j + 10;
			        } else {
			            int j = 1;
			            m = j + 10;
			        }

			        if (isActive) {
			            int j = 123;
			            System.out.println("Other number: " + j);
			        }
			        return m;
			    }

			    public int doNotRefactorWithNameConfusion(boolean hasError) {
			        int i;

			        if (hasError) {
			            int j = 1;
			            i = j + 10;
			        } else {
			            int j = 1;
			            i = j + 10;
			        }

			        System.out.println("Today: " + j);
			        return i;
			    }

			    public int doNotMoveVarOutsideItsScope(boolean isValid) {
			        if (isValid) {
			            int dontMoveMeIMLocal = 1;
			            return dontMoveMeIMLocal + 10;
			        } else {
			            int dontMoveMeIMLocal = 2;
			            return dontMoveMeIMLocal + 10;
			        }
			    }

			    public static Predicate<String> doNotMergeDifferentLambdaExpression(final boolean caseSensitive, final String... allowedSet) {
			        if (caseSensitive) {
			            return x -> Arrays.stream(allowedSet).anyMatch(y -> (x == null && y == null) || (x != null && x.equals(y)));
			        } else {
			            Function<String,String> toLower = x -> x == null ? null : x.toLowerCase();
			            return x -> Arrays.stream(allowedSet).map(toLower).anyMatch(y -> (x == null && y == null) || (x != null && toLower.apply(x).equals(y)));
			        }
			    }

			    public String doNotRefactorWithNotFallingThroughCase(boolean isValid, boolean isEnabled, int i, int j) {
			        if (isValid) {
			            i++;
			            if (isEnabled && true) {
			                i++;
			            } else {
			                j++;
			            }
			        } else if (i > 0) {
			            "Do completely other things".chars();
			        } else {
			            i++;
			            if (false || !isEnabled) {
			                j++;
			            } else {
			                i++;
			            }
			        }

			        return "Common code";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROLFLOW_MERGE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testOneIfRatherThanDuplicateBlocksThatFallThrough() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void mergeConditionsWithReturn(int i1) {
			        // Keep this comment
			        if (i1 == 0) {
			            System.out.println("The same code");
			            return;
			        }
			        // Keep this comment too
			        if (i1 == 1) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void mergeConditionsWithThrow(int i1) throws Exception {
			        // Keep this comment
			        if (i1 == 0) {
			            System.out.println("The same code");
			            i1--;
			            throw new Exception();
			        }
			        // Keep this comment too
			        if (i1 == 1) {
			            System.out.println("The same code");
			            --i1;
			            throw new Exception();
			        }
			        System.out.println("Next code");
			    }

			    public void mergeConditionsWithContinue() {
			        for (int i1 = 0; i1 < 10; i1++) {
			            // Keep this comment
			            if (i1 == 0) {
			                System.out.println("The same code");
			                i1++;
			                continue;
			            }
			            // Keep this comment too
			            if (i1 == 1) {
			                System.out.println("The same code");
			                ++i1;
			                continue;
			            }
			            System.out.println("Next code");
			        }
			        System.out.println("Another code");
			    }

			    public void mergeConditionsWithBreak() {
			        for (int i1 = 0; i1 < 10; i1++) {
			            // Keep this comment
			            if (i1 == 0) {
			                System.out.println("The same code");
			                i1++;
			                break;
			            }
			            // Keep this comment too
			            if (i1 == 1) {
			                System.out.println("The same code");
			                i1 = i1 + 1;
			                break;
			            }
			            System.out.println("Next code");
			        }
			        System.out.println("Another code");
			    }

			    public void mergeConditionsWithReturnAndThrow(int i1, int i2) throws Exception {
			        // Keep this comment
			        if (i1 == 0) {
			            System.out.println("The same code");
			            if (i2 == 0) {
			                return;
			            } else {
			                throw new Exception("Error #" + i1++);
			            }
			        }
			        if (i1 == 1) {
			            System.out.println("The same code");
			            if (i2 == 0) {
			                return;
			            } else {
			                throw new Exception("Error #" + i1++);
			            }
			        }
			        System.out.println("Next code");
			    }

			    public void mergeSeveralConditions(int i1) {
			        // Keep this comment
			        if (i1 == 0) {
			            System.out.println("The same code");
			            return;
			        }
			        if (i1 == 1) {
			            System.out.println("The same code");
			            return;
			        }
			        if (i1 == 2) {
			            System.out.println("The same code");
			            return;
			        }
			        if (i1 == 3) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void mergeORConditions(boolean isValid, boolean isActive, boolean isEnabled, boolean isFound) {
			        // Keep this comment
			        if (isValid || isActive) {
			            System.out.println("The same code");
			            return;
			        }
			        if (isEnabled || isFound) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public void mergeConditionsWithReturn(int i1) {
			        // Keep this comment
			        // Keep this comment too
			        if ((i1 == 0) || (i1 == 1)) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void mergeConditionsWithThrow(int i1) throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        if ((i1 == 0) || (i1 == 1)) {
			            System.out.println("The same code");
			            --i1;
			            throw new Exception();
			        }
			        System.out.println("Next code");
			    }

			    public void mergeConditionsWithContinue() {
			        for (int i1 = 0; i1 < 10; i1++) {
			            // Keep this comment
			            // Keep this comment too
			            if ((i1 == 0) || (i1 == 1)) {
			                System.out.println("The same code");
			                ++i1;
			                continue;
			            }
			            System.out.println("Next code");
			        }
			        System.out.println("Another code");
			    }

			    public void mergeConditionsWithBreak() {
			        for (int i1 = 0; i1 < 10; i1++) {
			            // Keep this comment
			            // Keep this comment too
			            if ((i1 == 0) || (i1 == 1)) {
			                System.out.println("The same code");
			                i1 = i1 + 1;
			                break;
			            }
			            System.out.println("Next code");
			        }
			        System.out.println("Another code");
			    }

			    public void mergeConditionsWithReturnAndThrow(int i1, int i2) throws Exception {
			        // Keep this comment
			        if ((i1 == 0) || (i1 == 1)) {
			            System.out.println("The same code");
			            if (i2 == 0) {
			                return;
			            } else {
			                throw new Exception("Error #" + i1++);
			            }
			        }
			        System.out.println("Next code");
			    }

			    public void mergeSeveralConditions(int i1) {
			        // Keep this comment
			        if ((i1 == 0) || (i1 == 1) || (i1 == 2)
			                || (i1 == 3)) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void mergeORConditions(boolean isValid, boolean isActive, boolean isEnabled, boolean isFound) {
			        // Keep this comment
			        if (isValid || isActive || isEnabled || isFound) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(MultiFixMessages.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp_description)));
	}

	@Test
	public void testDoNotUseOneIfRatherThanDuplicateBlocksThatFallThrough() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public void doNotMergeConditionsWithConditionalReturn(int i1, int i2) {
			        if (i1 == 0) {
			            System.out.println("The same code");
			            if (i2 == 0) {
			                return;
			            }
			        }
			        if (i1 == 1) {
			            System.out.println("The same code");
			            if (i2 == 0) {
			                return;
			            }
			        }
			        System.out.println("Next code");
			    }

			    public void doNotMergeMoreThanFourOperands(int i1) {
			        if ((i1 == 0) || (i1 == 1) || (i1 == 2) || (i1 == 3)) {
			            System.out.println("The same code");
			            return;
			        }
			        if (i1 == 4) {
			            System.out.println("The same code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void doNotMergeConditionsWithoutJump(int i) {
			        if (i == 0) {
			            System.out.println("The same code");
			        }
			        if (i == 1) {
			            System.out.println("The same code");
			        }
			        System.out.println("Next code");
			    }

			    public void doNotMergeDifferentBlocks(int i) {
			        if (i == 0) {
			            System.out.println("A code");
			            return;
			        }
			        if (i == 1) {
			            System.out.println("Another code");
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void doNotMergeConditionsWithElse(int i1, int counter) {
			        // Keep this comment
			        if (i1 == 0) {
			            System.out.println("The count is: " + counter++);
			            return;
			        } else {
			            System.out.println("The count is: " + ++counter);
			        }
			        if (i1 == 1) {
			            System.out.println("The count is: " + counter++);
			            return;
			        }
			        System.out.println("Next code");
			    }

			    public void doNotMergeConditionsWithAnotherElse(int i) {
			        // Keep this comment
			        if (i == 0) {
			            System.out.println("The same code");
			            return;
			        }
			        if (i == 1) {
			            System.out.println("The same code");
			            return;
			        } else {
			            System.out.println("Another code");
			        }
			        System.out.println("Next code");
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRegExPrecompilationInLambda() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {

			    I1 i1 = () -> {
			        String p = "abcd";
			        String x = "abcdef";
			        String y = "bcdefg";
			        String[] a = x.split(p);
			        String[] b = y.split(p);
			    };

			    interface I1 {
			        public void m();
			    }

			    public void foo() {
			        I1 i1= () -> {
			            String p = "abcd";
			            String x = "abcdef";
			            String y = "bcdefg";
			            String[] a = x.split(p);
			            String[] b = y.split(p);
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import java.util.Arrays;
			import java.util.regex.Pattern;

			public class E1 {

			    private static final Pattern p_pattern = Pattern.compile("abcd");
			    I1 i1 = () -> {
			        Pattern p = p_pattern;
			        String x = "abcdef";
			        String y = "bcdefg";
			        String[] a = p.split(x);
			        String[] b = p.split(y);
			    };

			    interface I1 {
			        public void m();
			    }

			    private static final Pattern p_pattern2 = Pattern.compile("abcd");

			    public void foo() {
			        I1 i1= () -> {
			            Pattern p = p_pattern2;
			            String x = "abcdef";
			            String y = "bcdefg";
			            String[] a = p.split(x);
			            String[] b = p.split(y);
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testDoNotRefactorRegExWithPrecompilation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Arrays;

			public class E1 {
			    public boolean doNotUsePatternForOneUse(String date) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			       return date.matches(dateValidation);
			    }

			    public boolean doNotUsePatternWithOtherUse(String date1, String date2) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";
			       System.out.println("The pattern is: " + dateValidation);

			       return date1.matches(dateValidation) && date2.matches(dateValidation);
			    }

			    public boolean doNotUsePatternWithOtherMethod(String date1, String date2) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			       return date1.matches(dateValidation) && "".equals(date2.replace(dateValidation, ""));
			    }

			    public boolean doNotUsePatternInMultiDeclaration(String date1, String date2) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}", foo= "bar";

			       return date1.matches(dateValidation) && date2.matches(dateValidation);
			    }

			    public boolean doNotUsePatternOnMisplacedUse(String date1, String date2) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			       return dateValidation.matches(date1) && dateValidation.matches(date2);
			    }

			    public String doNotUsePatternOnMisplacedParameter(String date1, String date2) {
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			       String dateText1= date1.replaceFirst("0000-00-00", dateValidation);
			       String dateText2= date2.replaceAll("0000-00-00", dateValidation);

			       return dateText1 + dateText2;
			    }
			    public String doNotUsePatternOnSimpleSplit1(String speech1, String speech2) {
			       String line= "a";

			       String[] phrases1= speech1.split(line);
			       String[] phrases2= speech2.split(line, 1);
			       return phrases1[0] + phrases2[0];
			    }
			    public String doNotUsePatternOnSimpleSplit2(String speech1, String speech2) {
			       String line= "\\\\;";

			       String[] phrases1= speech1.split(line);
			       String[] phrases2= speech2.split(line, 1);
			       return phrases1[0] + phrases2[0];
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRegExPrecompilationWithExistingImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import javax.validation.constraints.Pattern;

			public class E1 {
			    private String code;
			    private String dateValidation= ".*";

			   public boolean usePattern(String date1, String date2) {
			       // Keep this comment
			       String dateValidation= "\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}";

			       // Keep this comment too
			       return date1.matches(dateValidation) && date2.matches(dateValidation);
			    }

			    @Pattern(regexp="\\\\d{4}",
			        message="The code should contain exactly four numbers.")
			    public String getCode() {
			        return code;
			    }

			    public void setCode(String code) {
			        this.code= code;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;

			import javax.validation.constraints.Pattern;

			public class E1 {
			    private String code;
			    private String dateValidation= ".*";
			    private static final java.util.regex.Pattern dateValidation_pattern = java.util.regex.Pattern
			            .compile("\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}");

			    public boolean usePattern(String date1, String date2) {
			        // Keep this comment
			        java.util.regex.Pattern dateValidation= dateValidation_pattern;

			        // Keep this comment too
			        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();
			    }

			    @Pattern(regexp="\\\\d{4}",
			            message="The code should contain exactly four numbers.")
			    public String getCode() {
			        return code;
			    }

			    public void setCode(String code) {
			        this.code= code;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testEmbeddedIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public int collapseIfStatements(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid) {
			                // Keep this comment also
			                return 1;
			            }
			        }

			        return 0;
			    }

			    public int collapseInnerLoneIf(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive) {
			            if (isValid)
			                return 1;
			        }

			        return 0;
			    }

			    public int collapseOutterLoneIf(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive)
			            if (isValid) {
			                return 1;
			            }

			        return 0;
			    }

			    public int collapseWithFourOperands(int i1, int i2) {
			        // Keep this comment
			        if (0 < i1 && i1 < 10) {
			            // Keep this comment too
			            if (0 < i2 && i2 < 10) {
			                // Keep this comment also
			                return 1;
			            }
			        }

			        return 0;
			    }

			    public int collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid || isEditMode) {
			                // Keep this comment also
			                return 1;
			            }
			        }

			        return 0;
			    }

			    public int collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        if (isActive) {
			            // Keep this comment too
			            if (isValid | isEditMode) {
			                // Keep this comment also
			                return 1;
			            }
			        }

			        return 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.RAISE_EMBEDDED_IF);

		sample= """
			package test1;

			public class E {
			    public int collapseIfStatements(boolean isActive, boolean isValid) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && isValid) {
			            // Keep this comment also
			            return 1;
			        }

			        return 0;
			    }

			    public int collapseInnerLoneIf(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive && isValid)
			            return 1;

			        return 0;
			    }

			    public int collapseOutterLoneIf(boolean isActive, boolean isValid) {
			        // Keep this comment
			        if (isActive && isValid) {
			            return 1;
			        }

			        return 0;
			    }

			    public int collapseWithFourOperands(int i1, int i2) {
			        // Keep this comment
			        // Keep this comment too
			        if ((0 < i1 && i1 < 10) && (0 < i2 && i2 < 10)) {
			            // Keep this comment also
			            return 1;
			        }

			        return 0;
			    }

			    public int collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && (isValid || isEditMode)) {
			            // Keep this comment also
			            return 1;
			        }

			        return 0;
			    }

			    public int collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {
			        // Keep this comment
			        // Keep this comment too
			        if (isActive && (isValid | isEditMode)) {
			            // Keep this comment also
			            return 1;
			        }

			        return 0;
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { sample },
				new HashSet<>(Arrays.asList(MultiFixMessages.EmbeddedIfCleanup_description)));
	}

	@Test
	public void testDoNotRaiseEmbeddedIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public int doNotCollapseWithFiveOperands(int i1, int i2) {
			        if (0 < i1 && i1 < 10) {
			            if (100 < i2 && i2 < 200 || i2 < 0) {
			                return 1;
			            }
			        }

			        return 0;
			    }

			    public int doNotCollapseTwoLoneIfsWithEndOfLineComment(boolean isActive, boolean isValid) {
			        if (isActive)
			            if (isValid)
			                return 1; // This comment makes crash the parser

			        return 0;
			    }

			    public void doNotCollapseOuterIfWithElseStatement(boolean isActive, boolean isValid) {
			        if (isActive) {
			            if (isValid) {
			                int i = 0;
			            }
			        } else {
			            int i = 0;
			        }
			    }

			    public void doNotCollapseIfWithElseStatement2(boolean isActive, boolean isValid) {
			        if (isActive) {
			            if (isValid) {
			                int i = 0;
			            } else {
			                int i = 0;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.RAISE_EMBEDDED_IF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testExtractIncrement() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.ArrayList;
			import java.util.Date;
			import java.util.List;

			public class E extends ArrayList<String> {
			    private static final long serialVersionUID = -5909621993540999616L;

			    private int field= 0;

			    public E(int i) {
			        super(i++);
			    }

			    public E(int doNotRefactor, boolean isEnabled) {
			        super(++doNotRefactor);
			    }

			    public E(int i, int j) {
			        this(i++);
			    }

			    public String moveIncrementBeforeIf(int i) {
			        // Keep this comment
			        if (++i > 0) {
			            return "Positive";
			        } else {
			            return "Negative";
			        }
			    }

			    public String moveDecrementBeforeIf(int i) {
			        // Keep this comment
			        if (--i > 0) {
			            return "Positive";
			        } else {
			            return "Negative";
			        }
			    }

			    public int moveDecrementBeforeThrow(int i) {
			        // Keep this comment
			        throw new NullPointerException("++i " + ++i);
			    }

			    public void moveIncrementOutsideStatement(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        // Keep this comment
			        String[] texts= new String[++i];
			    }

			    public void moveIncrementOutsideStatement2(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        texts.wait(++i);
			    }

			    public void moveIncrementOutsideStatement3(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int j= i++, k= ++z;
			    }

			    public void moveIncrementOutsideStatement4(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        j= i-- + 123;
			    }

			    public void moveIncrementOutsideStatement5(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        boolean isString= obj[++i] instanceof String;
			    }

			    public void moveIncrementOutsideStatement6(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        List<Date> dates= new ArrayList<>(--i);
			    }

			    public void moveIncrementOutsideStatement7(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        long l= (long)i++;
			    }

			    public void moveIncrementOutsideStatement8(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int m= (i++);
			    }

			    public void moveIncrementOutsideStatement9(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        boolean isEqual= !(i++ == 10);
			    }

			    public void moveIncrementOutsideStatement10(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        theClass[i++].field--;
			    }

			    public void moveIncrementOutsideStatement11(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int[] integers= {i++, 1, 2, 3};
			    }

			    public int moveIncrementOutsideStatement12(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        return ++i;
			    }

			    public boolean moveIncrementOutsideInfix(int i, boolean isEnabled) {
			        // Keep this comment
			        boolean isEqual= (i++ == 10) && isEnabled;
			        return isEqual;
			    }

			    public String moveIncrementOutsideSuperMethod(int i) {
			        // Keep this comment
			        return super.remove(++i);
			    }

			    public boolean moveIncrementOutsideEagerInfix(int i, boolean isEnabled) {
			        // Keep this comment
			        boolean isEqual= isEnabled & (i++ == 10);
			        return isEqual;
			    }

			    public int moveIncrementOutsideTernaryExpression(int i) {
			        // Keep this comment
			        int j= (i++ == 10) ? 10 : 20;
			        return j * 2;
			    }

			    public int moveIncrementInIf(int i, boolean isEnabled) {
			        if (isEnabled)
			            return ++i;

			        return 0;
			    }

			    public int moveIncrementInSwitch(int i, int discriminant) {
			        switch (discriminant) {
			        case 0:
			                return ++i;
			        case 1:
			                return --i;
			        }

			        return 0;
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.ArrayList;
			import java.util.Date;
			import java.util.List;

			public class E extends ArrayList<String> {
			    private static final long serialVersionUID = -5909621993540999616L;

			    private int field= 0;

			    public E(int i) {
			        super(i);
			        i++;
			    }

			    public E(int doNotRefactor, boolean isEnabled) {
			        super(++doNotRefactor);
			    }

			    public E(int i, int j) {
			        this(i);
			        i++;
			    }

			    public String moveIncrementBeforeIf(int i) {
			        i++;
			        // Keep this comment
			        if (i > 0) {
			            return "Positive";
			        } else {
			            return "Negative";
			        }
			    }

			    public String moveDecrementBeforeIf(int i) {
			        i--;
			        // Keep this comment
			        if (i > 0) {
			            return "Positive";
			        } else {
			            return "Negative";
			        }
			    }

			    public int moveDecrementBeforeThrow(int i) {
			        i++;
			        // Keep this comment
			        throw new NullPointerException("++i " + i);
			    }

			    public void moveIncrementOutsideStatement(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        i++;
			        // Keep this comment
			        String[] texts= new String[i];
			    }

			    public void moveIncrementOutsideStatement2(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        i++;
			        texts.wait(i);
			    }

			    public void moveIncrementOutsideStatement3(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int j= i, k= ++z;
			        i++;
			    }

			    public void moveIncrementOutsideStatement4(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        j= i + 123;
			        i--;
			    }

			    public void moveIncrementOutsideStatement5(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        i++;
			        boolean isString= obj[i] instanceof String;
			    }

			    public void moveIncrementOutsideStatement6(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        i--;
			        List<Date> dates= new ArrayList<>(i);
			    }

			    public void moveIncrementOutsideStatement7(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        long l= (long)i;
			        i++;
			    }

			    public void moveIncrementOutsideStatement8(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int m= i;
			        i++;
			    }

			    public void moveIncrementOutsideStatement9(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        boolean isEqual= !(i == 10);
			        i++;
			    }

			    public void moveIncrementOutsideStatement10(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        theClass[i].field--;
			        i++;
			    }

			    public void moveIncrementOutsideStatement11(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        int[] integers= {i, 1, 2, 3};
			        i++;
			    }

			    public int moveIncrementOutsideStatement12(int i, int z, Object[] obj, E[] theClass) throws InterruptedException {
			        i++;
			        return i;
			    }

			    public boolean moveIncrementOutsideInfix(int i, boolean isEnabled) {
			        // Keep this comment
			        boolean isEqual= (i == 10) && isEnabled;
			        i++;
			        return isEqual;
			    }

			    public String moveIncrementOutsideSuperMethod(int i) {
			        i++;
			        // Keep this comment
			        return super.remove(i);
			    }

			    public boolean moveIncrementOutsideEagerInfix(int i, boolean isEnabled) {
			        // Keep this comment
			        boolean isEqual= isEnabled & (i == 10);
			        i++;
			        return isEqual;
			    }

			    public int moveIncrementOutsideTernaryExpression(int i) {
			        // Keep this comment
			        int j= (i == 10) ? 10 : 20;
			        i++;
			        return j * 2;
			    }

			    public int moveIncrementInIf(int i, boolean isEnabled) {
			        if (isEnabled) {
			            i++;
			            return i;
			        }

			        return 0;
			    }

			    public int moveIncrementInSwitch(int i, int discriminant) {
			        switch (discriminant) {
			        case 0:
			                i++;
			                return i;
			        case 1:
			                return --i;
			        }

			        return 0;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.EXTRACT_INCREMENT);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_ExtractIncrement_description)));
	}

	@Test
	public void testDoNotExtractIncrement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public String doNotMoveIncrementAfterIf(int i) {
			        String result= null;

			        if (i++ > 0) {
			            result= "Positive";
			        } else {
			            result= "Negative";
			        }

			        return result;
			    }

			    public int doNotMoveDecrementAfterReturn(int i) {
			        return i--;
			    }

			    public int doNotMoveDecrementAfterThrow(int i) {
			        throw new NullPointerException("i++ " + i++);
			    }

			    public int doNotMoveIncrementAfterFallThrough(boolean isEnabled, int i) {
			        if (i-- > 0) {
			            return i++;
			        } else {
			            throw new NullPointerException("i++ " + i++);
			        }
			    }

			    public boolean doNotMoveIncrementOutsideConditionalInfix(int i, boolean isEnabled) {
			        boolean isEqual= isEnabled && (i++ == 10);
			        return isEqual;
			    }

			    public int doNotMoveIncrementOutsideTernaryExpression(int i) {
			        int j= (i == 10) ? i++ : 20;
			        return j * 2;
			    }

			    public int doNotMoveIncrementOnReadVariable(int i) {
			        int j= i++ + i++;
			        int k= i++ + i;
			        int l= i + i++;
			        int m= (i = 0) + i++;
			        return j + k + l + m;
			    }

			    public void doNotRefactorIncrementStatement(int i) {
			        i++;
			    }

			    public void doNotMoveIncrementOutsideWhile(int i) {
			        while (i-- > 0) {
			            System.out.println("Must decrement on each loop");
			        }
			    }

			    public void doNotMoveIncrementOutsideDoWhile(int i) {
			        do {
			            System.out.println("Must decrement on each loop");
			        } while (i-- > 0);
			    }

			    public void doNotMoveIncrementOutsideFor() {
			        for (int i = 0; i < 10; i++) {
			            System.out.println("Must increment on each loop");
			        }
			    }

			    public void doNotMoveIncrementOutsideElseIf(int i) {
			        if (i == 0) {
			            System.out.println("I equals zero");
			        } else if (i++ == 10) {
			            System.out.println("I has equaled ten");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXTRACT_INCREMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testPullUpAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;

			import java.util.Queue;

			public class E {
			    public void moveLeftHandSideAssignmentBeforeIf(Queue<Integer> queue) {
			        Integer i;
			        System.out.println("Before polling");
			        // Keep this comment
			        if ((i = queue.poll()) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveRightHandSideAssignmentBeforeIf(Queue<Integer> q) {
			        Integer number;
			        System.out.println("Before polling");
			        // Keep this comment
			        if (null != (number = q.poll())) {
			            System.out.println("Value=" + number);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfMultipleParenthesesToRemove(Queue<Integer> q) {
			        Integer i;
			        System.out.println("Before polling");
			        // Keep this comment
			        if ((((i = q.poll()))) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfAndMergeWithDeclaration(Queue<Integer> q) {
			        Integer i;
			        // Keep this comment
			        if ((i = q.poll(/* Keep this comment too */)) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBelowDeclaration(Queue<Integer> q) {
			        Integer i = q.poll();
			        // Keep this comment
			        if ((i = q.poll()) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void erasePassiveValue(Queue<Integer> q) {
			        Integer i = 0;
			        // Keep this comment
			        if ((i = q.poll()) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentWithoutParenthesis(Queue<Boolean> q) {
			        Boolean b;
			        // Keep this comment
			        if (b = q.poll()) {
			            System.out.println("Value=" + b);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfAtConditionOfTernaryExpression(String s, int i) {
			        final char c;
			        // Keep this comment
			        if ((c = s.charAt(i)) == 'A' ? c == 'B' : c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveAssignmentBeforeIfAtStartOfInfixExpression(String s, int i) {
			        final char c;
			        // Keep this comment
			        if ((c = s.charAt(i)) == 'A' || c == 'B' || c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveNotConditionalAssignment(String s, int i, boolean isValid) {
			        final char c;
			        // Keep this comment
			        if (isValid | (c = s.charAt(i)) == 'A') {
			            System.out.println("valid or A");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveAssignmentInComplexExpression(String s, int i, boolean isValid) {
			        final char c;
			        // Keep this comment
			        if (!(isValid | (i == 10 & (c = s.charAt(i)) == 'A'))) {
			            System.out.println("valid or A");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public boolean refactorSingleStatementBlock(int i, int j) {
			        if (i > 0)
			            if ((i = j) < 10)
			                return true;
			        return false;
			    }

			    public void moveLeftHandSideAssignmentInSwitch(Queue<Integer> q, int discriminant) {
			        Integer i;
			        System.out.println("Before polling");
			        switch (discriminant) {
			        case 0:
			            // Keep this comment
			            if ((i = q.poll()) != null) {
			                System.out.println("Value=" + i);
			            } else {
			                System.out.println("Empty");
			            }
			        case 1:
			            System.out.println("Another case");
			        }
			    }

			    public void moveAssignmentAndKeepUsedInitialization(Queue<Boolean> inputQueue) {
			        Boolean overusedVariable = Boolean.TRUE, doNotForgetMe = overusedVariable;

			        // Keep this comment
			        if (overusedVariable = inputQueue.poll()) {
			            System.out.println("Value=" + doNotForgetMe);
			        } else {
			            System.out.println("Empty");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.PULL_UP_ASSIGNMENT);

		String output= """
			package test1;

			import java.util.Queue;

			public class E {
			    public void moveLeftHandSideAssignmentBeforeIf(Queue<Integer> queue) {
			        Integer i;
			        System.out.println("Before polling");
			        i = queue.poll();
			        // Keep this comment
			        if (i != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveRightHandSideAssignmentBeforeIf(Queue<Integer> q) {
			        Integer number;
			        System.out.println("Before polling");
			        number = q.poll();
			        // Keep this comment
			        if (null != number) {
			            System.out.println("Value=" + number);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfMultipleParenthesesToRemove(Queue<Integer> q) {
			        Integer i;
			        System.out.println("Before polling");
			        i = q.poll();
			        // Keep this comment
			        if (i != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfAndMergeWithDeclaration(Queue<Integer> q) {
			        Integer i = q.poll(/* Keep this comment too */);
			        // Keep this comment
			        if (i != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBelowDeclaration(Queue<Integer> q) {
			        Integer i = q.poll();
			        i = q.poll();
			        // Keep this comment
			        if (i != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void erasePassiveValue(Queue<Integer> q) {
			        Integer i = q.poll();
			        // Keep this comment
			        if (i != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentWithoutParenthesis(Queue<Boolean> q) {
			        Boolean b = q.poll();
			        // Keep this comment
			        if (b) {
			            System.out.println("Value=" + b);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void moveAssignmentBeforeIfAtConditionOfTernaryExpression(String s, int i) {
			        final char c = s.charAt(i);
			        // Keep this comment
			        if (c == 'A' ? c == 'B' : c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveAssignmentBeforeIfAtStartOfInfixExpression(String s, int i) {
			        final char c = s.charAt(i);
			        // Keep this comment
			        if (c == 'A' || c == 'B' || c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveNotConditionalAssignment(String s, int i, boolean isValid) {
			        final char c = s.charAt(i);
			        // Keep this comment
			        if (isValid | c == 'A') {
			            System.out.println("valid or A");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void moveAssignmentInComplexExpression(String s, int i, boolean isValid) {
			        final char c = s.charAt(i);
			        // Keep this comment
			        if (!(isValid | (i == 10 & c == 'A'))) {
			            System.out.println("valid or A");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public boolean refactorSingleStatementBlock(int i, int j) {
			        if (i > 0) {
			            i = j;
			            if (i < 10)
			                return true;
			        }
			        return false;
			    }

			    public void moveLeftHandSideAssignmentInSwitch(Queue<Integer> q, int discriminant) {
			        Integer i;
			        System.out.println("Before polling");
			        switch (discriminant) {
			        case 0:
			                i = q.poll();
			                // Keep this comment
			            if (i != null) {
			                System.out.println("Value=" + i);
			            } else {
			                System.out.println("Empty");
			            }
			        case 1:
			            System.out.println("Another case");
			        }
			    }

			    public void moveAssignmentAndKeepUsedInitialization(Queue<Boolean> inputQueue) {
			        Boolean overusedVariable = Boolean.TRUE, doNotForgetMe = overusedVariable;

			        overusedVariable = inputQueue.poll();
			        // Keep this comment
			        if (overusedVariable) {
			            System.out.println("Value=" + doNotForgetMe);
			        } else {
			            System.out.println("Empty");
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_PullUpAssignment_description)));
	}

	@Test
	public void testDoNotPullUpAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Queue;

			public class E {
			    public void doNotRefactor(Queue<Integer> q) {
			        Integer i;
			        System.out.println("Before polling");

			        // Keep this comment
			        if (q == null) {
			            System.out.println("Null queue");
			        } else if ((i = q.poll()) != null) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }

			    public void doNotMoveAssignmentBeforeIfAtLeftOperandOfTernaryExpression(String s, int i, char c) {
			        if (c == 'A' ? (c = s.charAt(i)) == 'B' : c == 'C') {
			            System.out.println("Found");
			        } else {
			            System.out.println("Not found");
			        }
			    }

			    public void doNotMoveAssignmentBeforeIfAtRightOperandOfTernaryExpression(String s, int i, char c) {
			        if (c == 'A' ? c == 'B' : (c = s.charAt(i)) == 'C') {
			            System.out.println("Found");
			        } else {
			            System.out.println("Not found");
			        }
			    }

			    public void doNotMoveAssignmentBeforeIfInsideInfixExpression(String s, int i, char c) {
			        if (c == 'A' || (c = s.charAt(i)) == 'A' || c == 'B' || c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void doNotMoveAssignmentAfterActiveCondition(String s, int i, char c) {
			        if (i++ == 10 || (c = s.charAt(i)) == 'A' || c == 'B' || c == 'C') {
			            System.out.println("A, B or C");
			        } else {
			            System.out.println("Not A, B or C");
			        }
			    }

			    public void doNotRefactorConditionalAnd(Queue<Boolean> q, boolean isValid) {
			        Boolean i;

			        if (isValid && (i = q.poll())) {
			            System.out.println("Value=" + i);
			        } else {
			            System.out.println("Empty");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PULL_UP_ASSIGNMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testInstanceof() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public boolean useInstanceof(Object o) {
			        // Keep this comment
			        return String.class.isInstance(o);
			    }

			    public boolean useInstanceofOnComplexType(Object o) {
			        // Keep this comment
			        return String[].class.isInstance(o);
			    }

			    public boolean useInstanceofOnQualifiedType(Object o) {
			        // Keep this comment
			        return java.util.Date.class.isInstance(o);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public boolean useInstanceof(Object o) {
			        // Keep this comment
			        return (o instanceof String);
			    }

			    public boolean useInstanceofOnComplexType(Object o) {
			        // Keep this comment
			        return (o instanceof String[]);
			    }

			    public boolean useInstanceofOnQualifiedType(Object o) {
			        // Keep this comment
			        return (o instanceof java.util.Date);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.INSTANCEOF);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_Instanceof_description)));
	}

	@Test
	public void testDoNotUseInstanceof() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public boolean doNotUseInstanceofOnPrimitive(Object o) {
			        return int.class.isInstance(o);
			    }

			    public boolean doNotUseInstanceofOnDynamicClass(Object o, Class<?> clazz) {
			        return clazz.isInstance(o);
			    }

			    public boolean doNotUseInstanceofOnOtherMethod(Object o) {
			        return String.class.equals(o);
			    }

			    public boolean doNotUseInstanceofOnIncompatibleTypes(Integer o) {
			        return String.class.isInstance(o);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.INSTANCEOF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRemoveQualifier02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int foo() {return 0;}
			    public int getFoo() {
			        return this.foo();
			    }  \s
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 {
			    public int foo() {return 0;}
			    public int getFoo() {
			        return foo();
			    }  \s
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1},
				new HashSet<>(Arrays.asList(FixMessages.CodeStyleFix_removeThis_groupDescription)));
	}

	@Test
	public void testRemoveQualifier03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int foo;
			    public int bar;
			    public class E1Inner {
			        private int bar;
			        public int getFoo() {
			            E1.this.bar= this.bar;
			            return E1.this.foo;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 {
			    public int foo;
			    public int bar;
			    public class E1Inner {
			        private int bar;
			        public int getFoo() {
			            E1.this.bar= bar;
			            return foo;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveQualifier04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int foo() {return 0;}
			    public int bar() {return 0;}
			    public class E1Inner {
			        private int bar() {return 1;}
			        public int getFoo() {
			            E1.this.bar();\s
			            this.bar();
			            return E1.this.foo();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 {
			    public int foo() {return 0;}
			    public int bar() {return 0;}
			    public class E1Inner {
			        private int bar() {return 1;}
			        public int getFoo() {
			            E1.this.bar();\s
			            bar();
			            return foo();
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveQualifierBug134720() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo() {
			        this.setEnabled(true);
			    }
			    private void setEnabled(boolean b) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= """
			package test;
			public class E {
			    public void foo() {
			        setEnabled(true);
			    }
			    private void setEnabled(boolean b) {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveQualifierBug150481_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public class Inner extends E {
			        public void test() {
			            E.this.foo();
			            this.foo();
			            foo();
			        }
			    }
			    public void foo() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= """
			package test;
			public class E {
			    public class Inner extends E {
			        public void test() {
			            E.this.foo();
			            foo();
			            foo();
			        }
			    }
			    public void foo() {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveQualifierBug150481_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public class Inner {
			        public void test() {
			            E.this.foo();
			            foo();
			        }
			    }
			    public void foo() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= """
			package test;
			public class E {
			    public class Inner {
			        public void test() {
			            foo();
			            foo();
			        }
			    }
			    public void foo() {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveQualifierBug219478() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 extends E2 {
			    private class E1Inner extends E2 {
			        public E1Inner() {
			            i = 2;
			            System.out.println(i + E1.this.i);
			        }
			    }
			}
			class E2 {
			    protected int i = 1;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 extends E2 {
			    private class E1Inner extends E2 {
			        public E1Inner() {
			            i = 2;
			            System.out.println(i + E1.this.i);
			        }
			    }
			}
			class E2 {
			    protected int i = 1;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveQualifierBug219608() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 extends E2 {
			    private int i = 1;
			    private class E1Inner extends E2 {
			        public E1Inner() {
			            System.out.println(i + E1.this.i);
			        }
			    }
			}
			class E2 {
			    private int i = 1;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			public class E1 extends E2 {
			    private int i = 1;
			    private class E1Inner extends E2 {
			        public E1Inner() {
			            System.out.println(i + i);
			        }
			    }
			}
			class E2 {
			    private int i = 1;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveQualifierBug330754() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class Test {
			    String label = "works";
			    class Nested extends Test {
			        Nested() {
			            label = "broken";
			        }
			        @Override
			        public String toString() {
			            return Test.this.label;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddFinal01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    private int i= 0;
			    public void foo(int j, int k) {
			        int h, v;
			        v= 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test;
			public class E {
			    private final int i= 0;
			    public void foo(final int j, final int k) {
			        final int h;
			        int v;
			        v= 0;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1},
				new HashSet<>(Arrays.asList(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description)));
	}

	@Test
	public void testAddFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    private Object obj1= new Object();
			    protected Object obj2;
			    Object obj3;
			    public Object obj4;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test;
			public class E {
			    private final Object obj1= new Object();
			    protected Object obj2;
			    Object obj3;
			    public Object obj4;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddFinal03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    private int i = 0;
			    public void foo() throws Exception {
			    }
			    public void bar(int j) {
			        int k;
			        try {
			            foo();
			        } catch (Exception e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test;
			public class E {
			    private int i = 0;
			    public void foo() throws Exception {
			    }
			    public void bar(int j) {
			        final int k;
			        try {
			            foo();
			        } catch (final Exception e) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddFinal04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    private int i = 0;
			    public void foo() throws Exception {
			    }
			    public void bar(int j) {
			        int k;
			        try {
			            foo();
			        } catch (Exception e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= """
			package test;
			public class E {
			    private int i = 0;
			    public void foo() throws Exception {
			    }
			    public void bar(final int j) {
			        int k;
			        try {
			            foo();
			        } catch (Exception e) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddFinal05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    public void foo() {
			        int i= 0;
			        if (i > 1 || i == 1 && i > 1)
			            ;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= """
			package test;
			public class E {
			    public void foo() {
			        final int i= 0;
			        if ((i > 1) || ((i == 1) && (i > 1)))
			            ;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddFinalBug129807() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public abstract class E {
			    public interface I {
			        void foo(int i);
			    }
			    public class IImpl implements I {
			        public void foo(int i) {}
			    }
			    public abstract void bar(int i, String s);
			    public void foobar(int i, int j) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= """
			package test;
			public abstract class E {
			    public interface I {
			        void foo(int i);
			    }
			    public class IImpl implements I {
			        public void foo(final int i) {}
			    }
			    public abstract void bar(int i, String s);
			    public void foobar(final int i, final int j) {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testAddFinalBug134676_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E<T> {\s
			    private String s;
			    void setS(String s) {
			        this.s = s;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug134676_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E<T> {\s
			    private String s= "";
			    private T t;
			    private T t2;
			    public E(T t) {t2= t;}
			    void setT(T t) {
			        this.t = t;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test;
			public class E<T> {\s
			    private final String s= "";
			    private T t;
			    private final T t2;
			    public E(T t) {t2= t;}
			    void setT(T t) {
			        this.t = t;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	//Changed test due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=220124
	@Test
	public void testAddFinalBug145028() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private volatile int field= 0;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=294768
	@Test
	public void testAddFinalBug294768() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private transient int field= 0;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testAddFinalBug157276_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			        field= 10;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test1;
			public class E1 {
			    private final int field;
			    public E1() {
			        field= 10;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug157276_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			        field= 10;
			    }
			    public E1(int f) {
			        field= f;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test1;
			public class E1 {
			    private final int field;
			    public E1() {
			        field= 10;
			    }
			    public E1(int f) {
			        field= f;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug157276_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			        field= 10;
			    }
			    public E1(final int f) {
			        field= f;
			        field= 5;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			    }
			    public E1(final int f) {
			        field= f;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field= 0;
			    public E1() {
			        field= 5;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			        if (false) field= 5;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private static int field;
			    public E1() {
			        field= 5;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug156842() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int f0;
			    private int f1= 0;
			    private int f3;
			    public E1() {
			        f3= 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test1;
			public class E1 {
			    private int f0;
			    private final int f1= 0;
			    private final int f3;
			    public E1() {
			        f3= 0;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug158041_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(int[] ints) {
			        for (int j = 0; j < ints.length; j++) {
			            System.out.println(ints[j]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo(int[] ints) {
			        for (final int i : ints) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testAddFinalBug158041_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(int[] ints) {
			        for (int j = 0; j < ints.length; j++) {
			            int i = ints[j];
			            System.out.println(i);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo(int[] ints) {
			        for (final int i : ints) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testAddFinalBug158041_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<E1> es) {
			        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {
			            System.out.println(iterator.next());
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void foo(List<E1> es) {
			        for (final E1 e1 : es) {
			            System.out.println(e1);
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug158041_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<E1> es) {
			        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {
			            E1 e1 = iterator.next();
			            System.out.println(e1);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void foo(List<E1> es) {
			        for (final E1 e1 : es) {
			            System.out.println(e1);
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testAddFinalBug163789() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int i;
			    public E1() {
			        this(10);
			        i = 10;
			    }
			    public E1(int j) {
			        i = j;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= """
			package test1;
			public class E1 {
			    private int i;
			    public E1() {
			        this(10);
			        i = 10;
			    }
			    public E1(final int j) {
			        i = j;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug191862() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E01 {
			    @SuppressWarnings("unused")
			    @Deprecated
			    private int x = 5, y= 10;
			   \s
			    private void foo() {
			        @SuppressWarnings("unused")
			        @Deprecated
			        int i= 10, j;
			        j= 10;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test1;
			public class E01 {
			    @SuppressWarnings("unused")
			    @Deprecated
			    private final int x = 5, y= 10;
			   \s
			    private void foo() {
			        @SuppressWarnings("unused")
			        @Deprecated
			        final
			        int i= 10;
			        @SuppressWarnings("unused")
			        @Deprecated
			        int j;
			        j= 10;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample}, null);
	}

	@Test
	public void testAddFinalBug213995() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private Object foo = new Object() {
			        public boolean equals(Object obj) {
			            return super.equals(obj);
			        }
			    };\s
			    public void foo() {
			        Object foo = new Object() {
			            public boolean equals(Object obj) {
			                return super.equals(obj);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= """
			package test1;
			public class E1 {
			    private Object foo = new Object() {
			        public boolean equals(final Object obj) {
			            return super.equals(obj);
			        }
			    };\s
			    public void foo() {
			        Object foo = new Object() {
			            public boolean equals(final Object obj) {
			                return super.equals(obj);
			            }
			        };
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testAddFinalBug272532() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int field;
			    public E1() {
			        if (true)
			            return;
			        field= 5;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug297566() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private int x;
			    public E1() {
			        this();
			    }
			    public E1(int a) {
			        x = a;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug297566_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    private int x;

			    public E1() {
			        this(10);
			    }

			    public E1(int a) {
			        this();
			    }

			    public E1(int f, int y) {
			        x = a;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug475462_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			final class E {
			    enum E1 {
			        FOO("a");
			        private String message;
			        E1(final String message) {
			            this.message = message;
			        }

			        E1() {
			        }

			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug475462_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    enum E1 {
			        FOO("a");
			        private String message;
			        public E1(final String message) {
			            this.message = message;
			        }

			        public E1() {
			            this("abc");
			        }

			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= """
			package test1;

			public class E {
			    enum E1 {
			        FOO("a");
			        private final String message;
			        public E1(final String message) {
			            this.message = message;
			        }

			        public E1() {
			            this("abc");
			        }

			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testRemoveStringCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public String replaceNewString() {
			        // Keep this comment
			        return new String("");
			    }

			    public String replaceNewStringParenthesized() {
			        // Keep this comment
			        return ((new String("")));
			    }

			    public String replaceNewStringInMethodInvocation(String s, int i) {
			        // Keep this comment
			        return new String(s + i).toLowerCase();
			    }

			    public String replaceNewStringInMethodInvocation2(String s, String s2) {
			        // Keep this comment
			        return new String(s.concat(s2)).toLowerCase();
			    }

			    public String replaceNewStringInIf(String s, String s2) {
			        // Keep this comment
			        if ((new String(s)).equals("abc")) {
			            return s2;
			    }

			    public String replaceNewStringInNestedNewStrings(String s, String s2) {
			        // Keep this comment
			        if ((new String(new String(s))).equals("abc")) {
			            return s2;
			    }

			    public void replaceNewStringInFieldAccess(String s) {
			        // Keep this comment
			        Object x = (new String(s)).CASE_INSENSITIVE_ORDER;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NO_STRING_CREATION);

		sample= """
			package test1;

			public class E1 {
			    public String replaceNewString() {
			        // Keep this comment
			        return "";
			    }

			    public String replaceNewStringParenthesized() {
			        // Keep this comment
			        return "";
			    }

			    public String replaceNewStringInMethodInvocation(String s, int i) {
			        // Keep this comment
			        return (s + i).toLowerCase();
			    }

			    public String replaceNewStringInMethodInvocation2(String s, String s2) {
			        // Keep this comment
			        return s.concat(s2).toLowerCase();
			    }

			    public String replaceNewStringInIf(String s, String s2) {
			        // Keep this comment
			        if (s.equals("abc")) {
			            return s2;
			    }

			    public String replaceNewStringInNestedNewStrings(String s, String s2) {
			        // Keep this comment
			        if (s.equals("abc")) {
			            return s2;
			    }

			    public void replaceNewStringInFieldAccess(String s) {
			        // Keep this comment
			        Object x = s.CASE_INSENSITIVE_ORDER;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample }, null);
	}

	@Test
	public void testDoNotRemoveStringCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E1 {
			    public String doNotReplaceNullableObject(String s) {
			        return new String(s);
			    }

			    public String doNotReplaceCopyString(String s, String s2) {
			        String k = new String(s);
			        String l = null;
			        l = new String(s2);
			        return l;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NO_STRING_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCheckSignOfBitwiseOperation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;

			public class Foo {
			  private static final int CONSTANT = -1;

			  public int foo () {
			    int i = 0;
			    if (i & (CONSTANT | C2) > 0) {}
			    if (0 < (i & (CONSTANT | C2))) {}
			    return (1>>4 & CONSTANT) > 0;
			  };
			};
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Foo.java", sample, false, null);
		sample= """
			package test;

			public class Foo {
			  private static final int CONSTANT = -1;

			  public int foo () {
			    int i = 0;
			    if (i & (CONSTANT | C2) != 0) {}
			    if (0 != (i & (CONSTANT | C2))) {}
			    return (1>>4 & CONSTANT) != 0;
			  };
			};
			""";
		String expected = sample;

		enable(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CheckSignOfBitwiseOperation_description)));
	}

	@Test
	public void testKeepCheckSignOfBitwiseOperation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;

			public class Foo {
			  private static final int CONSTANT = -1;

			  public void bar() {
			    int i = 0;
			    if (i > 0) {}
			    if (i > 0 && (CONSTANT +1) > 0) {}
			  };
			};
			""";
		String original= sample;
		ICompilationUnit cu= pack1.createCompilationUnit("Foo.java", original, false, null);

		enable(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testInvertEquals() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public static interface Itf {
			        int primitiveConstant = 1;
			        String objConstant = "fkjfkjf";
			        String objNullConstant = null;
			        MyEnum enumConstant = MyEnum.NOT_NULL;
			        MyEnum enumNullConstant = null;
			    }

			    private static enum MyEnum {
			        NOT_NULL
			    }

			    public boolean invertEquals(Object obj, String text1, String text2) {
			        // Keep this comment
			        return obj.equals("")
			                && obj.equals(Itf.objConstant)
			                && obj.equals("" + Itf.objConstant)
			                && obj.equals(MyEnum.NOT_NULL)
			                && obj.equals(text1 + text2)
			                && obj.equals(this);
			    }

			    public boolean invertEqualsIgnoreCase(String s) {
			        // Keep this comment
			        return s.equalsIgnoreCase("")
			                && s.equalsIgnoreCase(Itf.objConstant)
			                && s.equalsIgnoreCase("" + Itf.objConstant);
			    }
			}
			""";

		String expected= """
			package test1;

			public class E {
			    public static interface Itf {
			        int primitiveConstant = 1;
			        String objConstant = "fkjfkjf";
			        String objNullConstant = null;
			        MyEnum enumConstant = MyEnum.NOT_NULL;
			        MyEnum enumNullConstant = null;
			    }

			    private static enum MyEnum {
			        NOT_NULL
			    }

			    public boolean invertEquals(Object obj, String text1, String text2) {
			        // Keep this comment
			        return "".equals(obj)
			                && Itf.objConstant.equals(obj)
			                && ("" + Itf.objConstant).equals(obj)
			                && MyEnum.NOT_NULL.equals(obj)
			                && (text1 + text2).equals(obj)
			                && this.equals(obj);
			    }

			    public boolean invertEqualsIgnoreCase(String s) {
			        // Keep this comment
			        return "".equalsIgnoreCase(s)
			                && Itf.objConstant.equalsIgnoreCase(s)
			                && ("" + Itf.objConstant).equalsIgnoreCase(s);
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.INVERT_EQUALS);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.InvertEqualsCleanUp_description)));
	}

	@Test
	public void testDoNotInvertEquals() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
			    public static interface Itf {
			        int primitiveConstant = 1;
			        String objConstant = "fkjfkjf";
			        String objNullConstant = null;
			        MyEnum enumConstant = MyEnum.NOT_NULL;
			        MyEnum enumNullConstant = null;
			    }

			    private static enum MyEnum {
			        NOT_NULL
			    }

			    private int primitiveField;

			    public boolean doNotInvertEqualsOnInstance() {
			        return equals("");
			    }

			    public boolean doNotInvertEqualsOnThis() {
			        return this.equals("");
			    }

			    public boolean doNotInvertEqualsWhenParameterIsNull(Object obj) {
			        return obj.equals(Itf.objNullConstant) && obj.equals(Itf.enumNullConstant);
			    }

			    public boolean doNotInvertEqualsWithPrimitiveParameter(Object obj) {
			        return obj.equals(1)
			            && obj.equals(Itf.primitiveConstant)
			            && obj.equals(primitiveField);
			    }

			    public boolean doNotInvertEqualsIgnoreCaseWhenParameterIsNull(String s) {
			        return s.equalsIgnoreCase(Itf.objNullConstant);
			    }

			    public boolean doNotInvertEqualsOnOperationThatIsNotConcatenation(Integer number, Integer i1, Integer i2) {
			        return number.equals(i1 + i2);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.INVERT_EQUALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStandardComparison() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			import java.util.Comparator;

			public class E {
			    public boolean refactorComparableComparingToZero() {
			        boolean b = true;
			        final String s = "";

			        b &= s.compareTo("smaller") == -1;
			        b &= s.compareTo("greater") != -1;
			        b &= s.compareTo("smaller") != 1;
			        b &= (s.compareTo("greater")) == 1;
			        b &= (s.compareToIgnoreCase("greater")) == 1;
			        b &= -1 == (s.compareTo("smaller"));
			        b &= -1 != s.compareTo("greater");
			        b &= 1 != s.compareTo("smaller");
			        b &= 1 == s.compareTo("greater");
			        b &= 1 == s.compareToIgnoreCase("greater");

			        return b;
			    }

			    public boolean refactorComparatorComparingToZero(Comparator<String> comparator) {
			        boolean b = true;
			        final String s = "";

			        b &= comparator.compare(s, "smaller") == -1;
			        b &= comparator.compare(s, "greater") != -1;
			        b &= comparator.compare(s, "smaller") != 1;
			        b &= (comparator.compare(s, "greater")) == 1;
			        b &= -1 == (comparator.compare(s, "smaller"));
			        b &= -1 != comparator.compare(s, "greater");
			        b &= 1 != comparator.compare(s, "smaller");
			        b &= 1 == comparator.compare(s, "greater");

			        return b;
			    }
			}
			""";

		String expected= """
			package test1;

			import java.util.Comparator;

			public class E {
			    public boolean refactorComparableComparingToZero() {
			        boolean b = true;
			        final String s = "";

			        b &= s.compareTo("smaller") < 0;
			        b &= s.compareTo("greater") >= 0;
			        b &= s.compareTo("smaller") <= 0;
			        b &= s.compareTo("greater") > 0;
			        b &= s.compareToIgnoreCase("greater") > 0;
			        b &= s.compareTo("smaller") < 0;
			        b &= s.compareTo("greater") >= 0;
			        b &= s.compareTo("smaller") <= 0;
			        b &= s.compareTo("greater") > 0;
			        b &= s.compareToIgnoreCase("greater") > 0;

			        return b;
			    }

			    public boolean refactorComparatorComparingToZero(Comparator<String> comparator) {
			        boolean b = true;
			        final String s = "";

			        b &= comparator.compare(s, "smaller") < 0;
			        b &= comparator.compare(s, "greater") >= 0;
			        b &= comparator.compare(s, "smaller") <= 0;
			        b &= comparator.compare(s, "greater") > 0;
			        b &= comparator.compare(s, "smaller") < 0;
			        b &= comparator.compare(s, "greater") >= 0;
			        b &= comparator.compare(s, "smaller") <= 0;
			        b &= comparator.compare(s, "greater") > 0;

			        return b;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.STANDARD_COMPARISON);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.StandardComparisonCleanUp_description)));
	}

	@Test
	public void testDoNotUseStandardComparison() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			import java.util.Comparator;

			public class E implements Comparator<Double> {
			    public boolean doNotRefactorValidCases() {
			        boolean b = true;
			        final String s = "";

			        b &= s.compareTo("smaller") < 0;
			        b &= s.compareTo("smaller") <= 0;
			        b &= s.compareTo("equal") == 0;
			        b &= s.compareTo("different") != 0;
			        b &= s.compareTo("greater") >= 0;
			        b &= s.compareTo("greater") > 0;
			        b &= s.compareToIgnoreCase("equal") == 0;

			        return b;
			    }

			    public boolean doNotRefactorValidCases(Comparator<String> comparator) {
			        boolean b = true;
			        final String s = "";

			        b &= comparator.compare(s, "smaller") < 0;
			        b &= comparator.compare(s, "smaller") <= 0;
			        b &= comparator.compare(s, "equal") == 0;
			        b &= comparator.compare(s, "different") != 0;
			        b &= comparator.compare(s, "greater") >= 0;
			        b &= comparator.compare(s, "greater") > 0;

			        return b;
			    }

			    public boolean doNotRefactorLocalComparingToZero() {
			        boolean b = true;
			        final Double s = 123d;

			        b &= compare(s, 100d) < 100;
			        b &= compare(s, 100d) <= 100;
			        b &= compare(s, 123d) == 100;
			        b &= compare(s, 321d) != 100;
			        b &= compare(s, 200d) >= 100;
			        b &= compare(s, 200d) > 100;

			        b &= compare(s, 100d) == 99;
			        b &= compare(s, 200d) != 99;
			        b &= compare(s, 100d) != 101;
			        b &= (compare(s, 200d)) == 101;
			        b &= 99 == (compare(s, 100d));
			        b &= 99 != compare(s, 200d);
			        b &= 101 != compare(s, 100d);
			        b &= 101 == compare(s, 200d);

			        return b;
			    }

			    @Override
			    public int compare(Double o1, Double o2) {
			        return Double.compare(o1, o2) + 100;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STANDARD_COMPARISON);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRemoveBlockReturnThrows01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public abstract class E {
			    public void foo(Object obj) {
			        if (obj == null) {
			            throw new IllegalArgumentException();
			        }
			       \s
			        if (obj.hashCode() > 0) {
			            return;
			        }
			       \s
			        if (obj.hashCode() < 0) {
			            System.out.println("");
			            return;
			        }
			       \s
			        if (obj.toString() != null) {
			            System.out.println(obj.toString());
			        } else {
			            System.out.println("");
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= """
			package test;
			public abstract class E {
			    public void foo(Object obj) {
			        if (obj == null)
			            throw new IllegalArgumentException();
			       \s
			        if (obj.hashCode() > 0)
			            return;
			       \s
			        if (obj.hashCode() < 0) {
			            System.out.println("");
			            return;
			        }
			       \s
			        if (obj.toString() != null) {
			            System.out.println(obj.toString());
			        } else {
			            System.out.println("");
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveTrailingWhitespace01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package    test1;    \s
			   public class E1 { \s
			                  \s
			}                 \s
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package    test1;
			public class E1 {

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveTrailingWhitespace02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package    test1;    \s
			   public class E1 { \s
			                  \s
			}                 \s
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);

		sample= """
			package    test1;
			   public class E1 {
			                  \s
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testRemoveTrailingWhitespaceBug173081() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			/**
			 *\s
			 *    \s
			 */
			public class E1 {\s
			    /**
			     *\s
				 *    \s
			     */
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;
			/**
			 *\s
			 *\s
			 */
			public class E1 {
			    /**
			     *\s
			     *\s
			     */
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testSortMembers01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			   public class SM01 {
			   int b;
			   int a;
			   void d() {};
			   void c() {};
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM01.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= """
			package test;
			   public class SM01 {
			   int a;
			   int b;
			   void c() {};
			   void d() {};
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testSortMembers02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			   public class SM02 {
			   int b;
			   int a;
			   void d() {};
			   void c() {};
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= """
			package test;
			   public class SM02 {
			   int b;
			   int a;
			   void c() {};
			   void d() {};
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testSortMembersBug218542() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		assertTrue(JavaPlugin.getDefault().getMemberOrderPreferenceCache().isSortByVisibility());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			String sample= """
				package test;
				   public class SM02 {
				   private int b;
				   public int a;
				   void d() {};
				   void c() {};
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

			enable(CleanUpConstants.SORT_MEMBERS);

			sample= """
				package test;
				   public class SM02 {
				   private int b;
				   public int a;
				   void c() {};
				   void d() {};
				}
				""";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);
		}
	}

	@Test
	public void testSortMembersBug223997() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class SM02 {
			    public String s2;
			    public static String s1;
			   void d() {};
			   void c() {};
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= """
			package test;
			public class SM02 {
			    public static String s1;
			    public String s2;
			   void c() {};
			   void d() {};
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSortMembersBug263173() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class SM263173 {
			    static int someInt;
			    static {
			        someInt = 1;
			    };
			    static int anotherInt = someInt;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM263173.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= """
			package test;
			public class SM263173 {
			    static int someInt;
			    static {
			        someInt = 1;
			    };
			    static int anotherInt = someInt;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSortMembersBug434941() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class A {
			    public static final int CONSTANT = 5;
			    public static void main(final String[] args) { }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testSortMembersMixedFields() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class A {
			    public static final int B = 1;
			    public final int A = 2;
			    public static final int C = 3;
			    public final int D = 4;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= """
			package test;
			public class A {
			    public static final int B = 1;
			    public static final int C = 3;
			    public final int A = 2;
			    public final int D = 4;
			}
			""";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSortMembersMixedFieldsInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public interface A {
			    public static final int B = 1;
			    public final int A = 2;
			    public static final int C = 3;
			    public final int D = 4;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= """
			package test;
			public interface A {
			    public final int A = 2;
			    public static final int B = 1;
			    public static final int C = 3;
			    public final int D = 4;
			}
			""";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSortMembersBug407759() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class A {
			    void foo2() {}
			    void foo1() {}
			    static int someInt;
			    static void fooStatic() {}
			    static {
			    	someInt = 1;
			    }
			    void foo3() {}
			    static int anotherInt = someInt;
			    void foo() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= """
			package test;
			public class A {
			    static int someInt;
			    static {
			    	someInt = 1;
			    }
			    static int anotherInt = someInt;
			    static void fooStatic() {}
			    void foo() {}
			    void foo1() {}
			    void foo2() {}
			    void foo3() {}
			}
			""";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= """
			package test;
			public class A {
			    static int anotherInt = someInt;
			    static int someInt;
			    static {
			    	someInt = 1;
			    }
			    static void fooStatic() {}
			    void foo() {}
			    void foo1() {}
			    void foo2() {}
			    void foo3() {}
			}
			""";

		expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testSortMembersVisibility() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			String sample= """
				package test;
				public class A {
				    public final int B = 1;
				    private static final int AA = 1;
				    public static final int BB = 2;
				    private final int A = 2;
				    final int C = 3;
				    protected static final int DD = 3;
				    final static int CC = 4;
				    protected final int D = 4;
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

			enable(CleanUpConstants.SORT_MEMBERS);
			disable(CleanUpConstants.SORT_MEMBERS_ALL);

			sample= """
				package test;
				public class A {
				    private static final int AA = 1;
				    public static final int BB = 2;
				    protected static final int DD = 3;
				    final static int CC = 4;
				    public final int B = 1;
				    private final int A = 2;
				    final int C = 3;
				    protected final int D = 4;
				}
				""";

			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

			enable(CleanUpConstants.SORT_MEMBERS);
			enable(CleanUpConstants.SORT_MEMBERS_ALL);

			sample= """
				package test;
				public class A {
				    public static final int BB = 2;
				    private static final int AA = 1;
				    protected static final int DD = 3;
				    final static int CC = 4;
				    public final int B = 1;
				    private final int A = 2;
				    protected final int D = 4;
				    final int C = 3;
				}
				""";

			expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER);
		}
	}

	@Test
	public void testOrganizeImports01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    A a;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		sample= """
			package test1;
			public class A {}
			""";
		pack2.createCompilationUnit("A.java", sample, false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class A {}
			""";
		pack3.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertEquals(1, entries.length);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.contains("ambiguous"));
	}

	@Test
	public void testOrganizeImports02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    Vect or v;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertEquals(1, entries.length);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.contains("parse"));
	}

	@Test
	public void testOrganizeImportsBug202266() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		String sample= """
			package test2;
			public class E2 {
			}
			""";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test3", false, null);
		sample= """
			package test3;
			public class E2 {
			}
			""";
		pack2.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test1", false, null);
		sample= """
			package test1;
			public class E1 {
			    ArrayList foo;
			    E2 foo2;
			}
			""";
		ICompilationUnit cu1= pack3.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		sample= """
			package test1;

			import java.util.ArrayList;

			public class E1 {
			    ArrayList foo;
			    E2 foo2;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testOrganizeImportsBug229570() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public interface E1 {
			  List<IEntity> getChildEntities();
			  ArrayList<String> test;
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		sample= """
			package test1;

			import java.util.ArrayList;
			import java.util.List;

			public interface E1 {
			  List<IEntity> getChildEntities();
			  ArrayList<String> test;
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCorrectIndetation01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			/**
			*\s
			*/
			package test1;
			/**
			 *\s
			*\s
			 */
			        public class E1 {
			    /**
			         *\s
			 *\s
			     */
			            public void foo() {
			            //a
			        //b
			            }
			    /*
			     *
			           *
			*\s
			     */
			        }
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			/**
			 *\s
			 */
			package test1;
			/**
			 *\s
			 *\s
			 */
			public class E1 {
			    /**
			     *\s
			     *\s
			     */
			    public void foo() {
			        //a
			        //b
			    }
			    /*
			     *
			     *
			     *\s
			     */
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1}, null);
	}

	@Test
	public void testCorrectIndetationBug202145_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			// \s
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        //
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testCorrectIndetationBug202145_2() throws Exception {
		IJavaProject project= getProject();
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.TRUE);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= """
				package test1;
				public class E1 {
				    public void foo() {
				// \s
				    }
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

			sample= """
				package test1;
				public class E1 {
				    public void foo() {
				//
				    }
				}
				""";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.FALSE);
		}
	}

	@Test
	public void testUnimplementedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public interface IFace {
			    void foo();
			    void bar();
			}
			""";
		pack1.createCompilationUnit("IFace.java", sample, false, null);

		sample= """
			package test;
			public class E01 implements IFace {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", sample, false, null);

		sample= """
			package test;
			public class E02 implements IFace {
			    public class Inner implements IFace {
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_METHODES);

		String expected1= """
			package test;
			public class E01 implements IFace {

			    /* comment */
			    @Override
			    public void foo() {
			        //TODO
			       \s
			    }

			    /* comment */
			    @Override
			    public void bar() {
			        //TODO
			       \s
			    }
			}
			""";

		String expected2= """
			package test;
			public class E02 implements IFace {
			    public class Inner implements IFace {

			        /* comment */
			        @Override
			        public void foo() {
			            //TODO
			           \s
			        }

			        /* comment */
			        @Override
			        public void bar() {
			            //TODO
			           \s
			        }
			    }

			    /* comment */
			    @Override
			    public void foo() {
			        //TODO
			       \s
			    }

			    /* comment */
			    @Override
			    public void bar() {
			        //TODO
			       \s
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 }, null);
	}

	@Test
	public void testUnimplementedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public interface IFace {
			    void foo();
			}
			""";
		pack1.createCompilationUnit("IFace.java", sample, false, null);

		sample= """
			package test;
			public class E01 implements IFace {
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", sample, false, null);

		sample= """
			package test;
			public class E02 implements IFace {
			   \s
			    public class Inner implements IFace {
			       \s
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", sample, false, null);

		enable(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT);

		sample= """
			package test;
			public abstract class E01 implements IFace {
			}
			""";
		String expected1= sample;

		sample= """
			package test;
			public abstract class E02 implements IFace {
			   \s
			    public abstract class Inner implements IFace {
			       \s
			    }
			}
			""";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 }, null);
	}

	@Test
	public void testConstantsForSystemProperty() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			public class E {
			    public void simpleCase() {
			        // Keep this comment
			        String fs = System.getProperty("file.separator"); //$NON-NLS-1$
			        System.out.println("out:"+fs); //$NON-NLS-1$
			        String ps = System.getProperty("path.separator"); //$NON-NLS-1$
			        System.out.println("out:"+ps); //$NON-NLS-1$
			        String cdn = System.getProperty("file.encoding"); //$NON-NLS-1$
			        System.out.println("out:"+cdn); //$NON-NLS-1$
			        String lsp = System.getProperty("line.separator"); //$NON-NLS-1$
			        System.out.println("out:"+lsp); //$NON-NLS-1$
			        Boolean value = Boolean.parseBoolean(System.getProperty("arbitrarykey")); //$NON-NLS-1$
			        System.out.println("out:"+value); //$NON-NLS-1$
			        Boolean value2 = Boolean.parseBoolean(System.getProperty("arbitrarykey","false")); //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+value2); //$NON-NLS-1$
			        Integer intvalue = Integer.parseInt(System.getProperty("arbitrarykey")); //$NON-NLS-1$
			        System.out.println("out:"+intvalue); //$NON-NLS-1$
			        Integer intvalue2 = Integer.parseInt(System.getProperty("arbitrarykey","0")); //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+intvalue2); //$NON-NLS-1$
			        Integer intvalue3 = Integer.parseInt(System.getProperty("arbitrarykey","15")); //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+intvalue3); //$NON-NLS-1$
			        Long longvalue = Long.parseLong(System.getProperty("arbitrarykey")); //$NON-NLS-1$
			        System.out.println("out:"+longvalue); //$NON-NLS-1$
			        Long longvalue2 = Long.parseLong(System.getProperty("arbitrarykey","0")); //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+longvalue2); //$NON-NLS-1$
			        Long longvalue3 = Long.parseLong(System.getProperty("arbitrarykey","15")); //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+longvalue3); //$NON-NLS-1$
			        String jrv = System.getProperty("java.runtime.version"); //$NON-NLS-1$
			        System.out.println("out:"+jrv); //$NON-NLS-1$
			        String jsv = System.getProperty("java.specification.version"); //$NON-NLS-1$
			        System.out.println("out:"+jsv); //$NON-NLS-1$
			    }
			}
			""";

		String expected= """
			package test1;

			import java.io.File;
			import java.nio.charset.Charset;
			import java.nio.file.FileSystems;

			public class E {
			    public void simpleCase() {
			        // Keep this comment
			        String fs = FileSystems.getDefault().getSeparator();
			        System.out.println("out:"+fs); //$NON-NLS-1$
			        String ps = File.pathSeparator;
			        System.out.println("out:"+ps); //$NON-NLS-1$
			        String cdn = Charset.defaultCharset().displayName();
			        System.out.println("out:"+cdn); //$NON-NLS-1$
			        String lsp = System.lineSeparator();
			        System.out.println("out:"+lsp); //$NON-NLS-1$
			        Boolean value = Boolean.getBoolean("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+value); //$NON-NLS-1$
			        Boolean value2 = Boolean.getBoolean("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+value2); //$NON-NLS-1$
			        Integer intvalue = Integer.getInteger("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+intvalue); //$NON-NLS-1$
			        Integer intvalue2 = Integer.getInteger("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+intvalue2); //$NON-NLS-1$
			        Integer intvalue3 = Integer.getInteger("arbitrarykey", 15); //$NON-NLS-1$
			        System.out.println("out:"+intvalue3); //$NON-NLS-1$
			        Long longvalue = Long.getLong("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+longvalue); //$NON-NLS-1$
			        Long longvalue2 = Long.getLong("arbitrarykey"); //$NON-NLS-1$
			        System.out.println("out:"+longvalue2); //$NON-NLS-1$
			        Long longvalue3 = Long.getLong("arbitrarykey", 15); //$NON-NLS-1$
			        System.out.println("out:"+longvalue3); //$NON-NLS-1$
			        String jrv = Runtime.version().toString();
			        System.out.println("out:"+jrv); //$NON-NLS-1$
			        String jsv = Runtime.version().feature();
			        System.out.println("out:"+jsv); //$NON-NLS-1$
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_RUNTIME_VERSION);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_SPECIFICATION_VERSION);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(Messages.format(ConstantsCleanUp_description,UpdateProperty.FILE_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.PATH_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.LINE_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.FILE_ENCODING.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.BOOLEAN_PROPERTY.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.INTEGER_PROPERTY.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.LONG_PROPERTY.toString()))));
	}

	@Test
	public void testConstantsForSystemProperty_NLS() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			public class E {
			    public void simpleCase() {
			        // Keep this comment
			        // Keep this comment
			        String fs = System.getProperty("file.separator"); //$NON-NLS-1$
			        System.out.println("out:"+fs); //$NON-NLS-1$
						// Keep this comment
						// Keep this comment
			        String ps = System.getProperty("path.separator"); //$NON-NLS-1$
			        System.out.println("out:"+ps); //$NON-NLS-1$
			        String lsp = System.getProperty("line.separator"); String bla="ohoh"; //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println("out:"+lsp); //$NON-NLS-1$
			    }
			}
			""";

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "import java.nio.file.FileSystems;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void simpleCase() {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment\n" //
				+ "        String fs = FileSystems.getDefault().getSeparator();\n" //
				+ "        System.out.println(\"out:\"+fs); //$NON-NLS-1$\n" //
				+ "			// Keep this comment\n" //
				+ "        // Keep this comment\n" // Here is a problem - cause might be deeper(?)
				+ "        String ps = File.pathSeparator;\n" //
				+ "        System.out.println(\"out:\"+ps); //$NON-NLS-1$\n" //
				+ "        String lsp = System.lineSeparator(); String bla=\"ohoh\"; //$NON-NLS-1$ //$NON-NLS-2$\n" // Another problem, comment not removed
				+ "        System.out.println(\"out:\"+lsp); //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED);

		// Then
		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(Messages.format(ConstantsCleanUp_description,UpdateProperty.FILE_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.PATH_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.LINE_SEPARATOR.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.FILE_ENCODING.toString()),
						Messages.format(ConstantsCleanUp_description,UpdateProperty.BOOLEAN_PROPERTY.toString()))));
	}

	@Test
	public void testConstantsForSystemProperty_donttouch() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			public class E {
			    public void simpleCase() {
			        // Keep this comment
			        String fb = System.getProperty("foo.bah");//$NON-NLS-1$
			        System.out.println("out:"+fb);//$NON-NLS-1$
			        Boolean value = Boolean.parseBoolean(System.getProperty("jdt.codeCompleteSubstringMatch","true"));//$NON-NLS-1$
			        System.out.println("out:"+value);//$NON-NLS-1$
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED);

		// Then
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRemoveRedundantModifiers () throws Exception {
		String str= """
			package test;
			public abstract interface IFoo {
			  public static final int MAGIC_NUMBER = 646;
			  public abstract int foo ();
			  abstract void func ();
			  public int bar (int bazz);
			}
			""";
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		ICompilationUnit cu1= pack1.createCompilationUnit("IFoo.java", str, false, null);

		String expected1 = """
			package test;
			public interface IFoo {
			  int MAGIC_NUMBER = 646;
			  int foo ();
			  void func ();
			  int bar (int bazz);
			}
			""";

		String str1= """
			package test;
			public final class Sealed {
			  public final void foo () {};
			 \s
			  abstract static interface INested {
			  }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("Sealed.java", str1, false, null);

		String expected2 = """
			package test;
			public final class Sealed {
			  public void foo () {};
			 \s
			  interface INested {
			  }
			}
			""";

		// Anonymous class within an interface:
		// public keyword must not be removed (see bug#536612)
		String str2= """
			package test;
			public interface X {
			  void B();
			  void A();
			  default X y() {
			    return new X() {
			      @Override public void A() {}
			      @Override public void B() {}
			    };
			  }
			}
			""";
		String expected3 = str2;
		ICompilationUnit cu3= pack1.createCompilationUnit("AnonymousNestedInInterface.java", str2, false, null);

		String input4= """
			package test;

			public enum SampleEnum {
			  VALUE1("1"), VALUE2("2");

			  private SampleEnum(String string) {}
			}
			""";
		ICompilationUnit cu4= pack1.createCompilationUnit("SampleEnum.java", input4, false, null);

		String expected4= """
			package test;

			public enum SampleEnum {
			  VALUE1("1"), VALUE2("2");

			  SampleEnum(String string) {}
			}
			""";

		// public modifier must not be removed from enum methods
		String str3= """
			package test;
			public interface A {
			  public static enum B {
			    public static void method () { }
			  }
			}
			""";
		ICompilationUnit cu5= pack1.createCompilationUnit("NestedEnum.java", str3, false, null);
		// https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.9
		// nested enum type is implicitly static
		// Bug#538459 'public' modified must not be removed from static method in nested enum
		String expected5 = str3.replace("static enum", "enum");

		// Bug#551038: final keyword must not be removed from method with varargs
		String str4= """
			package test;
			public final class SafeVarargsExample {
			  @SafeVarargs
			  public final void errorRemoveRedundantModifiers(final String... input) {
			  }
			}
			""";
		String expected6 = str4;
		ICompilationUnit cu6= pack1.createCompilationUnit("SafeVarargsExample.java", str4, false, null);

		// Bug#553608: modifiers public static final must not be removed from inner enum within interface
		String str5= """
			package test;
			public interface Foo {
			  enum Bar {
			    A;
			    public static final int B = 0;
			  }
			}
			""";
		String expected7 = str5;
		ICompilationUnit cu7= pack1.createCompilationUnit("NestedEnumExample.java", str5, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2, cu3, cu4, cu5, cu6, cu7 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7 }, null);

	}

	@Test
	public void testDoNotRemoveModifiers() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public enum SampleEnum {\n" //$NON-NLS-1$
				+ "  VALUE1, VALUE2;\n" //$NON-NLS-1$
				+ "\n" //
				+ "  private void notAConstructor(String string) {}\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
		ICompilationUnit cu= pack.createCompilationUnit("SampleEnum.java", sample, false, null);

		// When
		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);

		// Then
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotTouchCleanedModifiers() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public interface ICleanInterface {
			  int MAGIC_NUMBER = 646;
			  int foo();
			  void func();
			  int bar(int bazz);
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("ICleanInterface.java", sample, false, null);

		// When
		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);

		// Then
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		// When
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(cu1);
		parser.setResolveBindings(true);
		CompilationUnit unit= (CompilationUnit) parser.createAST(null);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS, CleanUpOptions.TRUE);
		NoChangeRedundantModifiersCleanUp cleanup= new NoChangeRedundantModifiersCleanUp(options);
		ICleanUpFix fix= cleanup.createFix(unit);

		// Then
		assertNull("ICleanInterface should not be cleaned up", fix);
	}

	@Test
	public void testRemoveRedundantSemicolons () throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //

		// Ensure various extra semi-colons are removed and required ones are left intact.
		// This includes a lambda expression.
				+ "package test; ;\n" //
				+ "enum cars { sedan, coupe };\n" //
				+ "public class Foo {\n" //
				+ "  int add(int a, int b) {return a+b;};\n" //
				+ "  int a= 3;; ;\n" //
				+ "  int b= 7; // leave this ; alone\n" //
				+ "  int c= 10; /* and this ; too */\n" //
				+ "  public int foo () {\n" //
				+ "    ;\n" //
				+ "    Runnable r = () -> {\n" //
				+ "      System.out.println(\"running\");\n" //
				+ "    };;\n" //
				+ "    for (;;)\n" //
				+ "      ;;\n" //
				+ "      ;\n" //
				+ "    while (a++ < 1000) ;\n" //
				+ "  };\n" //
				+ "};\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("Foo.java", sample, false, null);

		// Ensure semi-colon after lambda expression remains intact.
		sample= """
			package test;
			enum cars { sedan, coupe }
			public class Foo {
			  int add(int a, int b) {return a+b;}
			  int a= 3;
			  int b= 7; // leave this ; alone
			  int c= 10; /* and this ; too */
			  public int foo () {
			   \s
			    Runnable r = () -> {
			      System.out.println("running");
			    };
			    for (;;)
			      ;
			     \s
			    while (a++ < 1000) ;
			  }
			}
			""";
		String expected1 = sample;

		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

	}

	@Test
	public void testBug491087() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			interface A {
			    class B {
			        String field;
			       B() { field = "foo"; }
			    }
			}
			class C {
			    class D {
			       String field;
			       D() { field = "bar"; }
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("C.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= """
			package test1;
			interface A {
			    class B {
			        String field;
			       B() { this.field = "foo"; }
			    }
			}
			class C {
			    class D {
			       String field;
			       D() { this.field = "bar"; }
			    }
			}
			""";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1},
				new HashSet<>(Arrays.asList(new String[] {
						Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {"field", "this"})
				})));
	}

	@Test
	public void testRemoveParenthesesBug438266_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public static void main(String[] args) {
			        Integer b = (Integer) (-1);
			        System.out.println(b);
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		String expected= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testRemoveParenthesesBug438266_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public static void main(String[] args) {
			        Integer b = (int) (-1);
			        System.out.println(b);
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		String expected= """
			package test1;
			public class E {
			    public static void main(String[] args) {
			        Integer b = (int) -1;
			        System.out.println(b);
			    }
			}""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}
}
