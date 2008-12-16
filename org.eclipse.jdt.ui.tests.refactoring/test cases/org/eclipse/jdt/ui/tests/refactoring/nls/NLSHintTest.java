/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class NLSHintTest extends TestCase {

    private IJavaProject javaProject;

    private IPackageFragmentRoot fSourceFolder;

    private final static String TEST_KLAZZ =
        "public class Test {" +
        "	private String str=TestMessages.getString(\"whateverKey\");//$NON-NLS-1$\n" +
        "}\n";

    private final static String ACCESSOR_KLAZZ =
		"public class TestMessages {\n" +
		"	private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" +
		"	public static String getString(String s) {" +
		"		return \"\";\n" +
		"	}\n" +
		"}\n";

	private static final boolean ALL_TESTS= true;


    public NLSHintTest(String arg) {
        super(arg);
    }

    public static Test allTests() {
		if (ALL_TESTS) {
    		return new ProjectTestSetup(new TestSuite(NLSHintTest.class));
    	} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new NLSHintTest("testResourceBundleHint"));
			return new ProjectTestSetup(suite);
    	}
	}

	public static Test suite() {
		return allTests();
	}

    protected void setUp() throws Exception {
        javaProject = ProjectTestSetup.getProject();
        fSourceFolder = JavaProjectHelper.addSourceContainer(javaProject, "src");
    }

    protected void tearDown() throws Exception {
        JavaProjectHelper.clear(javaProject, ProjectTestSetup.getDefaultClasspath());
    }

    /*
     * documents bug 57622.
     */
    public void testNlsedButNotTranslated() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"package test;\n" +
    		"public class Test {" +
			"	private String str=\"whateverKey\";//$NON-NLS-1$\n" +
			"}\n";
    	ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * documents bug 59074
     */
    public void testLooksLikeAccessor() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
            "public class Test {\n" +
            "	String[] foo = {\"ab\", String.valueOf(Boolean.valueOf(\"cd\")), \"de\"}; //$NON-NLS-1$ //$NON-NLS-2$\n" +
			"}\n";
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
    public void testNoAccessorClassHint1() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"package test;\n" +
    		"public class Test {" +
			"	private String str=\"whateverKey\".toString();//$NON-NLS-1$\n" +
			"}\n";
    	ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * method has no necessary static modifier.
     */
    public void testNoAccessorClassHint2() throws Exception {
    	IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
    	String klazz =
    		"package test;\n" +
    		"public class Test {" +
			"	private String str=new Wrong().meth(\"whatever\");//$NON-NLS-1$\n" +
			"}\n";

    	String klazz2 =
    		"package test;\n" +
			"public class Wrong {\n" +
			"	public void meth(String str) {};\n" +
			"}\n";
    	ICompilationUnit cu= pack.createCompilationUnit("Wrong.java", klazz2, false, null);
    	cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    /*
     * accessor class does not exist.
     */
    public void testNoAccessorClassHint3() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("Messages", hint.getAccessorClassName());
    }

    public void testAccessorClassAndPackageHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        klazz = "package test;\n" + ACCESSOR_KLAZZ;
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("TestMessages", hint.getAccessorClassName());
        assertEquals(pack, hint.getAccessorClassPackage());
    }

	public void testPackageHintWithNoPackage() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", TEST_KLAZZ, false, null);

        pack.createCompilationUnit("TestMessages.java", ACCESSOR_KLAZZ, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals(pack, hint.getAccessorClassPackage());
    }

    public void testPackageHintWithDifferentPackages() throws Exception {
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

    public void testResourceBundleHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        klazz = "package test;\n" +ACCESSOR_KLAZZ;
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);
        NLSHint hint = createNLSHint(cu);
        assertEquals("test.properties", hint.getResourceBundleName());
    }

    public void testResourceBundleHintWithDifferentPackagesAndClassGetName() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
        	"import test.foo.TestMessages;\n" +
        	TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        klazz =
            "package test.foo;\n" +
            "public class TestMessages {\n" +
            "	private static final String BUNDLE_NAME = TestMessages.class.getName();\n" +
            "	public static String getString(String s) {\n" +
            "		return \"\"\n;" +
            "	}\n" +
            "}\n";
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        NLSHint hint = createNLSHint(cu);
        assertEquals("TestMessages.properties", hint.getResourceBundleName());
    }

    public void testResourceBundlePackageHint() throws Exception {
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

    public void testResourceBundlePackageHintWithClassGetName() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz =
            "package test;\n" +
        	"import test.foo.TestMessages;\n" +
        	TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);

        IPackageFragment fooPackage = fSourceFolder.createPackageFragment("test.foo", false, null);
        klazz =
            "package test.foo;\n" +
            "public class TestMessages {\n" +
            "	private static final String BUNDLE_NAME = TestMessages.class.getName();\n" +
            "	public static String getString(String s) {\n" +
            "		return \"\"\n;" +
            "	}\n" +
            "}\n";
        fooPackage.createCompilationUnit("TestMessages.java", klazz, false, null);

        createResource(fooPackage, "TestMessages.properties", "a=0");
        NLSHint hint = createNLSHint(cu);
        assertEquals(fooPackage, hint.getResourceBundlePackage());
    }




    public void testPackageHintWithoutPreviousNLSing() throws Exception {
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
	    InputStream is = new ByteArrayInputStream(content.getBytes());
	    file.create(is, true, new NullProgressMonitor());
	    is.close();
        return file;
	}

	private NLSHint createNLSHint(ICompilationUnit cu) {
		CompilationUnit unit= ASTCreator.createAST(cu, null);
		return new NLSHint(cu, unit);
	}

}
