/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class CallHierarchyTest extends TestCase {
    
    private static final Class THIS= CallHierarchyTest.class;
    
    private IJavaProject fJavaProject1;
    private IJavaProject fJavaProject2;

    public CallHierarchyTest(String name) {
        super(name);
    }
            
    public static Test suite() {
        if (true) {
            return new TestSuite(THIS);
        } else {
            TestSuite suite= new TestSuite();
            suite.addTest(new CallHierarchyTest("test1"));
            return suite;
        }   
    }
    
    protected void setUp() throws Exception {
        fJavaProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
        fJavaProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
    }

    protected void tearDown () throws Exception {
        JavaProjectHelper.delete(fJavaProject1);
        JavaProjectHelper.delete(fJavaProject2);        
    }
                    
    public void test1() throws Exception {
        
        IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJavaProject1);
        assertTrue("jdk not found", jdk != null);
        IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
        IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
        
        ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
        IType type1= cu1.createType("public class A {public void method1() { }\n public void method2() { method1(); }\n}\n", null, true, null);
        
        JavaProjectHelper.addRTJar(fJavaProject2);
        JavaProjectHelper.addRequiredProject(fJavaProject2, fJavaProject1);
        IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
        IPackageFragment pack2= root2.createPackageFragment("pack2", true, null);
        
        ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
        IType type2= cu2.createType("public class B extends pack1.A {public void method3() { method1(); }\n}\n", null, true, null);

        IMethod method1= type1.getMethod("method1", new String[0]);
        
        Collection expectedMethods= new ArrayList();
        expectedMethods.add(type1.getMethod("method2", new String[0]));
        expectedMethods.add(type2.getMethod("method3", new String[0]));
        
        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(method1);

        MethodWrapper[] uncachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, uncachedCalls);
        
        MethodWrapper[] cachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, cachedCalls);
    }

    private void assertCalls(
        Collection expectedMethods,
        MethodWrapper[] callResults) {
        Collection calls= Arrays.asList(callResults);
        Collection foundMethods= new ArrayList();
        for (int i= 0; i < callResults.length; i++) {
            foundMethods.add(callResults[i].getMember());
        } 
        assertEquals("Wrong number of callers", expectedMethods.size(), calls.size());
        assertTrue("One or more methods not found", foundMethods.containsAll(expectedMethods));
    }   
}
