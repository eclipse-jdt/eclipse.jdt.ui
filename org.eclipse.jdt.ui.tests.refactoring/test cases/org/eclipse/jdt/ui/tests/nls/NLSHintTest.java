/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.nls;

import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHolder;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
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

    public NLSHintTest(String arg) {
        super(arg);        
    }
    
    public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NLSHintTest.class));
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

    public void testNoMessageClassHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals(null, hint.getMessageClass());
    }

    public void testAccessorClassAndPackageHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
                
        klazz = "package test;\n" + ACCESSOR_KLAZZ;
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals("TestMessages", hint.getMessageClass());
        assertEquals(pack, hint.getMessageClassPackage());
    }
    
    public void testPackageHintWithNoPackage() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("", false, null);
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", TEST_KLAZZ, false, null);                	
        
        pack.createCompilationUnit("TestMessages.java", ACCESSOR_KLAZZ, false, null);        
        
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);        
        assertEquals(pack, hint.getMessageClassPackage());
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
                
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals(fooPackage, hint.getMessageClassPackage());
    }    

    public void testResourceBundleHint() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;            
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        klazz = "package test;\n" +ACCESSOR_KLAZZ;        
        pack.createCompilationUnit("TestMessages.java", klazz, false, null);
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals("test.properties", hint.getResourceBundle());
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
        
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals("TestMessages.properties", hint.getResourceBundle());        
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
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
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
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals(fooPackage, hint.getResourceBundlePackage());
    }
    
    public void testPackageHintWithoutPreviousNLSing() throws Exception {
        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);
        String klazz = "package test;\n" + TEST_KLAZZ;
        ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
        
        NLSHolder nlsHolder = NLSHolder.create(cu);
        NLSHint hint = new NLSHint(nlsHolder.getLines(), cu);
        assertEquals(pack, hint.getMessageClassPackage());
        assertEquals(pack, hint.getResourceBundlePackage());
    }
    
    private IFile createResource(IPackageFragment pack, String resourceName, String content) throws Exception {
	    IPath path = pack.getPath().append(resourceName);
	    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);	    
	    file.create(new ByteArrayInputStream(content.getBytes()), true, new NullProgressMonitor());
        return file;
	}
    
}
