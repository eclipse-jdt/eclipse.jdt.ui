/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *             (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class CallHierarchyTest extends TestCase {
    private static final String[] EMPTY= new String[0];
    private static final Class THIS= CallHierarchyTest.class;

    private IJavaProject fJavaProject1;
    private IJavaProject fJavaProject2;
    private IType fType1;
    private IType fType2;
    private IPackageFragment fPack2;
    private IPackageFragment fPack1;

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
        fType1= null;
        fType2= null;
        fPack1= null;
        fPack2= null;
    }

    protected void tearDown() throws Exception {
        JavaProjectHelper.delete(fJavaProject1);
        JavaProjectHelper.delete(fJavaProject2);
    }

    public void testCallers() throws Exception {
        createSimpleClasses();

        IMethod method= fType1.getMethod("method1", EMPTY);
        IMethod secondLevelMethod= fType2.getMethod("method3", EMPTY);

        Collection expectedMethods= new ArrayList();
        expectedMethods.add(fType1.getMethod("method2", EMPTY));
        expectedMethods.add(secondLevelMethod);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, cachedCalls);

        MethodWrapper wrapper2= null;
        for (int i= 0; i < cachedCalls.length; i++) {
            if (secondLevelMethod.equals(cachedCalls[i].getMember())) {
                wrapper2= cachedCalls[i];
                break;
            }
        }

        Collection expectedSecondLevelMethods= new ArrayList();
        expectedSecondLevelMethods.add(fType2.getMethod("method4", EMPTY));
        assertCalls(expectedSecondLevelMethods, wrapper2.getCalls());
    }

    public void testCallersNoResults() throws Exception {
        createSimpleClasses();

        IMethod method= fType2.getMethod("method4", EMPTY);

        Collection expectedMethods= new ArrayList();

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, cachedCalls);
    }

    public void testCallees() throws Exception {
        createSimpleClasses();

        IMethod method= fType2.getMethod("method4", EMPTY);
        IMethod secondLevelMethod= fType2.getMethod("method3", EMPTY);

        Collection expectedMethods= new ArrayList();
        expectedMethods.add(secondLevelMethod);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, cachedCalls);

        MethodWrapper wrapper2= null;
        for (int i= 0; i < cachedCalls.length; i++) {
            if (secondLevelMethod.equals(cachedCalls[i].getMember())) {
                wrapper2= cachedCalls[i];
                break;
            }
        }

        Collection expectedMethodsTo3= new ArrayList();
        expectedMethodsTo3.add(fType1.getMethod("method1", EMPTY));
        expectedMethodsTo3.add(fType1.getMethod("method2", EMPTY));

        assertCalls(expectedMethodsTo3, wrapper2.getCalls());
    }

    public void testCalleesNoResults() throws Exception {
        createSimpleClasses();

        IMethod method= fType1.getMethod("method1", EMPTY);

        Collection expectedMethods= new ArrayList();

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls();
        assertCalls(expectedMethods, cachedCalls);
    }

    public void testRecursiveCallers() throws Exception {
        createSimpleClasses();

        IMethod method1= fType1.getMethod("recursiveMethod1", EMPTY);
        IMethod method2= fType1.getMethod("recursiveMethod2", EMPTY);

        Collection expectedMethodsFrom1= new ArrayList();
        expectedMethodsFrom1.add(method2);

        Collection expectedMethodsFrom2= new ArrayList();
        expectedMethodsFrom2.add(method1);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(method1);
        MethodWrapper[] callsFrom1= wrapper.getCalls();
        assertRecursive(callsFrom1, false);

        MethodWrapper wrapper2= callsFrom1[0];
        assertFalse("Should be marked as recursive", wrapper2.isRecursive());

        MethodWrapper[] callsFrom2= wrapper2.getCalls();
        assertCalls(expectedMethodsFrom2, callsFrom2);

        assertRecursive(callsFrom2, true);
    }

    public void testRecursiveCallees() throws Exception {
        createSimpleClasses();

        IMethod method1= fType1.getMethod("recursiveMethod1", EMPTY);
        IMethod method2= fType1.getMethod("recursiveMethod2", EMPTY);

        Collection expectedMethodsFrom1= new ArrayList();
        expectedMethodsFrom1.add(method2);

        Collection expectedMethodsFrom2= new ArrayList();
        expectedMethodsFrom2.add(method1);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(method1);
        MethodWrapper[] callsFrom1= wrapper.getCalls();
        assertCalls(expectedMethodsFrom1, callsFrom1);

        MethodWrapper wrapper2= callsFrom1[0];
        assertRecursive(callsFrom1, false);

        MethodWrapper[] callsFrom2= wrapper2.getCalls();
        assertCalls(expectedMethodsFrom2, callsFrom2);

        assertRecursive(callsFrom2, true);
    }

    /**
     * Tests calls that origin from an inner class  
     */
    public void testInnerClassCallers() throws Exception {
        createInnerClass();
        
        IMethod someMethod= fType1.getMethod("outerMethod1", EMPTY);

        IMethod innerMethod1= fType1.getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= fType1.getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(innerMethod1);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
        
        Collection expectedCallersSecondLevel= new ArrayList();
        expectedCallersSecondLevel.add(innerMethod2);
        assertCalls(expectedCallersSecondLevel, callers[0].getCalls());
    }

    /**
     * Tests callees that enter an inner class  
     */
    public void testInnerClassCalleesEntering() throws Exception {
        createInnerClass();

        IMethod someMethod= fType1.getMethod("outerMethod2", EMPTY);

        IMethod innerMethod1= fType1.getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= fType1.getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(innerMethod2);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
        
        Collection expectedCallersSecondLevel= new ArrayList();
        expectedCallersSecondLevel.add(innerMethod1);
        assertCalls(expectedCallersSecondLevel, callers[0].getCalls());
    }

    /**
     * Tests callees that exits an inner class  
     */
    public void testInnerClassCalleesExiting() throws Exception {
        createInnerClass();

        IMethod someMethod= fType1.getMethod("outerMethod1", EMPTY);

        IMethod innerMethod1= fType1.getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= fType1.getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(innerMethod1);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(innerMethod2);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
        
        Collection expectedCallersSecondLevel= new ArrayList();
        expectedCallersSecondLevel.add(someMethod);
        assertCalls(expectedCallersSecondLevel, callers[0].getCalls());
    }

    /**
     * Tests calls that origin from an inner class  
     */
    public void testAnonymousInnerClassCallers() throws Exception {
        createAnonymousInnerClass();
        
        IMethod someMethod= fType1.getMethod("someMethod", EMPTY);

        IField anonField= fType1.getField("anonClass");

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(anonField);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
    }

    /**
     * Tests calls that origin from a static initializer block.
     */
    public void testInitializerCallers() throws Exception {
        createStaticInitializerClass();

        IMethod someMethod= fType1.getMethod("someMethod", EMPTY);

        IInitializer initializer= fType1.getInitializer(1);

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(initializer);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
    }

    public void testImplicitConstructorCallers() throws Exception {
        createSimpleClasses();
        
        IMethod constructorA= fType1.getMethod("A", EMPTY);

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(fType2);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCallerRoot(constructorA);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
    }
    
    public void testImplicitConstructorCallees() throws Exception {
        createImplicitConstructorClasses();
        
        IMethod constructorB= fType2.getMethods()[0];

        Collection expectedCallers= new ArrayList();
        expectedCallers.add(fType1);

        MethodWrapper wrapper= CallHierarchy.getDefault().getCalleeRoot(constructorB);
        MethodWrapper[] callers= wrapper.getCalls();
        assertRecursive(callers, false);
        assertCalls(expectedCallers, callers);
    }
    
    /**
     * Creates two simple classes, A and B. Sets the instance fields fType1 and fType2.
     */
    private void createSimpleClasses() throws CoreException, JavaModelException {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("A.java");
        fType1=
            cu1.createType(
                "public class A {public A() { }\n public void method1() { }\n public void method2() { method1(); }\n public void recursiveMethod1() { recursiveMethod2(); }\n public void recursiveMethod2() { recursiveMethod1(); }\n}\n",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("B.java");
        fType2=
            cu2.createType(
                "public class B extends pack1.A {public void method3() { method1(); method2(); }\n public void method4() { method3(); }\n}\n",
                null,
                true,
                null);
    }

    /**
     * Creates two simple classes, A and its subclass B, where B calls A's implicit constructor explicitly. Sets the instance fields fType1 and fType2.
     */
    private void createImplicitConstructorClasses() throws CoreException, JavaModelException {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("A.java");
        fType1=
            cu1.createType(
                "public class A {\n public void method1() { }\n public void method2() { method1(); }\n public void recursiveMethod1() { recursiveMethod2(); }\n public void recursiveMethod2() { recursiveMethod1(); }\n}\n",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("B.java");
        fType2=
            cu2.createType(
                "public class B extends pack1.A {\n public B(String name) { super(); }\n}\n",
                null,
                true,
                null);
    }

    /**
     * Creates an inner class and sets the class attribute fType1.
     */
    private void createInnerClass() throws Exception {
        createPackages();
        
        ICompilationUnit cu1= fPack1.getCompilationUnit("Outer.java");
        fType1=
            cu1.createType(
                "public class Outer {\n" +                "private Inner inner= new Inner();\n" +                "class Inner { public void innerMethod1() { outerMethod1(); }\n public void innerMethod2() { innerMethod1(); }\n }\n" +                "public void outerMethod1() { }\n public void outerMethod2() { inner.innerMethod2(); }\n" +                "}",
                null,
                true,
                null);
    }
    
    /**
     * Creates an anonymous inner class and sets the class attribute fType1.
     */
    private void createAnonymousInnerClass() throws Exception {
        createPackages();
        
        ICompilationUnit cu1= fPack1.getCompilationUnit("AnonymousInner.java");
        fType1=
            cu1.createType(
                "public class AnonymousInner {  Object anonClass = new Object() {\n void anotherMethod() \n{ someMethod(); }\n };\n void someMethod() { }\n }\n",
                null,
                true,
                null);
    }

    /**
     * Creates a class with a static initializer and sets the class attribute fType1.
     */
    private void createStaticInitializerClass() throws Exception {
        createPackages();
        
        ICompilationUnit cu1= fPack1.getCompilationUnit("Initializer.java");
        fType1=
            cu1.createType(
                "public class Initializer { static { someMethod(); }\n public static void someMethod() { }\n }\n",
                null,
                true,
                null);
    }
    
    /**
     * Creates two packages (pack1 and pack2) in different projects. Sets the
     * instance fields fPack1 and fPack2.
     */
    private void createPackages() throws CoreException, JavaModelException {
        IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJavaProject1);
        assertTrue("jdk not found", jdk != null);

        IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
        fPack1= root1.createPackageFragment("pack1", true, null);

        JavaProjectHelper.addRTJar(fJavaProject2);
        JavaProjectHelper.addRequiredProject(fJavaProject2, fJavaProject1);

        IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
        fPack2= root2.createPackageFragment("pack2", true, null);
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    private void assertCalls(Collection expectedMembers, MethodWrapper[] callResults) {
        Collection calls= Arrays.asList(callResults);
        Collection foundMembers= new ArrayList();

        for (int i= 0; i < callResults.length; i++) {
            foundMembers.add(callResults[i].getMember());
        }

        assertEquals("Wrong number of calls", expectedMembers.size(), calls.size());
        assertTrue("One or more members not found", foundMembers.containsAll(expectedMembers));
    }

    private void assertRecursive(MethodWrapper[] callResults, boolean shouldBeRecursive) {
        for (int i= 0; i < callResults.length; i++) {
            assertEquals(
                "Wrong recursive value: " + callResults[i].getName(),
                shouldBeRecursive,
                callResults[i].isRecursive());
        }
    }
}
