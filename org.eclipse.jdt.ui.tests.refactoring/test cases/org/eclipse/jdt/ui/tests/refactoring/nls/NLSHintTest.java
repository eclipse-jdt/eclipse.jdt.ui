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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class NLSHintTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

    private IJavaProject javaProject;

    private IPackageFragmentRoot fSourceFolder;

    private final static String TEST_KLAZZ =
        """
		public class Test {\
			private String str=TestMessages.getString("whateverKey");//$NON-NLS-1$
		}
		""";

    private final static String ACCESSOR_KLAZZ =
		"""
		public class TestMessages {
			private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
			public static String getString(String s) {\
				return "";
			}
		}
		""";

    @Before
	public void setUp() throws Exception {
        javaProject = pts.getProject();
        fSourceFolder = JavaProjectHelper.addSourceContainer(javaProject, "src");
    }

    @After
	public void tearDown() throws Exception {
        JavaProjectHelper.clear(javaProject, pts.getDefaultClasspath());
    }

    /*
     * documents bug 57622.
     */
	@Test
	public void nlsedButNotTranslated() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"""
			package test;
			public class Test {\
				private String str="whateverKey";//$NON-NLS-1$
			}
			""";
    	ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * documents bug 59074
     */
	@Test
	public void looksLikeAccessor() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            """
			package test;
			public class Test {
				String[] foo = {"ab", String.valueOf(Boolean.valueOf("cd")), "de"}; //$NON-NLS-1$ //$NON-NLS-2$
			}
			""";
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
        assertEquals(pack, hint.getAccessorClassPackage());
        assertEquals("messages.properties", hint.getResourceBundleName());
        assertEquals(pack, hint.getResourceBundlePackage());
   }

    /*
     * nlsed-String must be an argument of method.
     */
	@Test
	public void noAccessorClassHint1() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"""
			package test;
			public class Test {\
				private String str="whateverKey".toString();//$NON-NLS-1$
			}
			""";
    	ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * method has no necessary static modifier.
     */
	@Test
	public void noAccessorClassHint2() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"""
			package test;
			public class Test {\
				private String str=new Wrong().meth("whatever");//$NON-NLS-1$
			}
			""";

    	String klazz2 =
    		"""
			package test;
			public class Wrong {
				public void meth(String str) {};
			}
			""";
    	ICompilationUnit cu= pack.createCompilationUnit("Wrong.java", klazz2, false, null);
    	cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * accessor class does not exist.
     */
	@Test
	public void noAccessorClassHint3() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

	@Test
	public void accessorClassAndPackageHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        klazz = "package test;\n" + ACCESSOR_KLAZZ;
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("TestMessages", hint.getAccessorClassName());
        assertEquals(pack, hint.getAccessorClassPackage());
    }

	@Test
	public void packageHintWithNoPackage() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", TEST_KLAZZ, false, null);

        pack.createCompilationUnit("TestMessages.java", ACCESSOR_KLAZZ, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals(pack, hint.getAccessorClassPackage());
    }

	@Test
	public void packageHintWithDifferentPackages() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
            "import test.foo.TestMessages;\n" +
            TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        klazz = "package test.foo;\n" + ACCESSOR_KLAZZ;
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals(fooPackage, hint.getAccessorClassPackage());
    }

	@Test
	public void resourceBundleHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        klazz = "package test;\n" +ACCESSOR_KLAZZ;
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("test.properties", hint.getResourceBundleName());
    }

	@Test
	public void resourceBundleHintWithDifferentPackagesAndClassGetName() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
        	"import test.foo.TestMessages;\n" +
        	TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        klazz =
            """
				package test.foo;
				public class TestMessages {
					private static final String BUNDLE_NAME = TestMessages.class.getName();
					public static String getString(String s) {
						return ""
				;\
					}
				}
				""";
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("TestMessages.properties", hint.getResourceBundleName());
    }

	@Test
	public void resourceBundlePackageHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
        	"import test.foo.TestMessages;\n" +
        	TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        createResource(pack, "test.properties", "a=0");
        NLSHint hint = createNLSHint(cu);
        assertEquals(pack, hint.getResourceBundlePackage());
    }

	@Test
	public void resourceBundlePackageHintWithClassGetName() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
        	"import test.foo.TestMessages;\n" +
        	TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        klazz =
            """
				package test.foo;
				public class TestMessages {
					private static final String BUNDLE_NAME = TestMessages.class.getName();
					public static String getString(String s) {
						return ""
				;\
					}
				}
				""";
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        createResource(fooPackage, "TestMessages.properties", "a=0");
        NLSHint hint = createNLSHint(cu);
        assertEquals(fooPackage, hint.getResourceBundlePackage());
    }




	@Test
	public void packageHintWithoutPreviousNLSing() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals(pack, hint.getAccessorClassPackage());
        assertEquals(pack, hint.getResourceBundlePackage());
    }

    private IFile createResource(IPackageFragment pack, String resourceName, String content) throws Exception {
	    IPath path = pack.getPath().append(resourceName);
	    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		try (InputStream is = new ByteArrayInputStream(content.getBytes())) {
			file.create(is, true, new NullProgressMonitor());
		}
        return file;
	}

	private NLSHint createNLSHint(ICompilationUnit cu) {
		CompilationUnit unit= ASTCreator.createAST(cu, null);
		return new NLSHint(cu, unit);
	}

}
