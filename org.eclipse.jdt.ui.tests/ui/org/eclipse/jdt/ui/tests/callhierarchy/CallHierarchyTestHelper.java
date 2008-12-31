/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *             (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class CallHierarchyTestHelper {
    private static final String[] EMPTY= new String[0];

    private IJavaProject fJavaProject1;
    private IJavaProject fJavaProject2;
    private IType fType1;
    private IType fType2;
    private IPackageFragment fPack2;
    private IPackageFragment fPack1;

    private IMethod fMethod1;
    private IMethod fMethod2;
    private IMethod fMethod3;
    private IMethod fMethod4;
    private IMethod fRecursiveMethod1;
    private IMethod fRecursiveMethod2;

    public void setUp() throws Exception {
        fJavaProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
        fJavaProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
        fType1= null;
        fType2= null;
        fPack1= null;
        fPack2= null;
    }

    public void tearDown() throws Exception {
        JavaProjectHelper.delete(fJavaProject1);
        JavaProjectHelper.delete(fJavaProject2);
    }


    /**
     * Creates two simple classes, A and B. Sets the instance fields fType1 and fType2.
     */
    public void createSimpleClasses() throws CoreException, JavaModelException {
        createPackages();


        ICompilationUnit cu1= fPack1.getCompilationUnit("A.java");

        fType1=
            cu1.createType(
                "public class A {\n" +
                "public A() {\n" +
                "}\n " +
                "public void method1() {\n" +
                "}\n " +
                "public void method2() {\n" +
                "  method1();\n" +
                "}\n " +
                "public void recursiveMethod1() {\n" +
                "  recursiveMethod2();\n " +
                "}\n " +
                "public void recursiveMethod2() {\n" +
                "  recursiveMethod1();\n " +
                "}\n" +
                "}\n",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("B.java");
        fType2=
            cu2.createType(
                "public class B extends pack1.A {\npublic void method3() { method1(); method2(); }\n public void method4() { method3(); }\n}\n",
                null,
                true,
                null);
    }

    /**
     * Creates two simple classes, A and its subclass B, where B calls A's implicit constructor explicitly. Sets the instance fields fType1 and fType2.
     */
    public void createImplicitConstructorClasses() throws CoreException, JavaModelException {
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
    public void createInnerClass() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("Outer.java");
        fType1=
            cu1.createType(
                "public class Outer {\n" +
                "private Inner inner= new Inner();\n" +
                "class Inner { public void innerMethod1() { outerMethod1(); }\n public void innerMethod2() { innerMethod1(); }\n }\n" +
                "public void outerMethod1() { }\n public void outerMethod2() { inner.innerMethod2(); }\n" +
                "}",
                null,
                true,
                null);
    }

    /**
     * Creates an anonymous inner class and sets the class attribute fType1.
     */
    public void createAnonymousInnerClass() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("AnonymousInner.java");
        fType1=
            cu1.createType(
                "public class AnonymousInner {\n" +
                "  Object anonClass = new Object() {\n" +
                "    void anotherMethod() {\n" +
                "      someMethod();\n" +
                "    }\n" +
                "  };\n" +
                "  void someMethod() {\n" +
                "  }\n" +
                "}\n",
                null,
                true,
                null);

        ICompilationUnit cu2= fPack2.getCompilationUnit("Outer.java");
        fType2=
            cu2.createType(
                "public class Outer {\n" +
                "    interface Intf {\n" +
                "         public void foo();\n" +
                "    }\n" +
                "    class Clazz {\n" +
                "         public void foo() { };\n" +
                "    }\n" +
                "    public void anonymousOnInterface() {\n" +
                "        new Intf() {\n"+
                "            public void foo() {\n"+
                "                someMethod();\n"+
                "            }\n"+
                "        };\n"+
                "    }\n" +
                "    public void anonymousOnClass() {\n" +
                "        new Clazz() {\n"+
                "            public void foo() {\n"+
                "                someMethod();\n"+
                "            }\n"+
                "        };\n"+
                "    }\n" +
                "    public void someMethod() { }\n"+
                "}\n",
                null,
                true,
                null);

    }

    /**
     * Creates an anonymous inner class inside another method and sets the class attribute fType1.
     */
    public void createAnonymousInnerClassInsideMethod() throws Exception {
        createPackages();

        ICompilationUnit cu1= fPack1.getCompilationUnit("AnonymousInnerInsideMethod.java");
        fType1=
            cu1.createType(
                "public class AnonymousInnerInsideMethod {\n" +
                "  void m() {\n" +
                "    System.out.println(\"before\");\n"+
                "    Runnable runnable = new Runnable() {\n"+
                "      public void run() {\n"+
                "        System.out.println(\"run\");\n"+
                "      }\n"+
                "    };\n"+
                "    runnable.run();\n"+
                "  }\n"+
                "}\n",
                null,
                true,
                null);

    }

    /**
     * Creates a class with a static initializer and sets the class attribute fType1.
     */
    public void createStaticInitializerClass() throws Exception {
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
    public void createPackages() throws CoreException, JavaModelException {
        JavaProjectHelper.addRTJar(fJavaProject1);

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
    public void assertCalls(Collection expectedMembers, Collection calls) {
        Collection foundMembers= new ArrayList();

        for (Iterator iter= calls.iterator(); iter.hasNext();) {
            MethodWrapper element= (MethodWrapper) iter.next();
            foundMembers.add(element.getMember());
        }

        Assert.assertEquals("Wrong number of calls", expectedMembers.size(), calls.size());
        Assert.assertTrue("One or more members not found", foundMembers.containsAll(expectedMembers));
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    public void assertCalls(Collection expectedMembers, MethodWrapper[] callResults) {
        assertCalls(expectedMembers, Arrays.asList(callResults));
    }

    /**
     * Asserts that all the expected methods were found in the call results.
     */
    public void assertCalls(IMember[] expectedMembers, Object[] callResults) {
        assertCalls(Arrays.asList(expectedMembers), Arrays.asList(callResults));
    }

    public MethodWrapper findMethodWrapper(IMethod method, Object[] methodWrappers) {
        MethodWrapper thirdLevelMethodWrapper= null;
        for (int i= 0; i < methodWrappers.length; i++) {
            if (method.equals(((MethodWrapper) methodWrappers[i]).getMember())) {
                thirdLevelMethodWrapper= (MethodWrapper) methodWrappers[i];
                break;
            }
        }
        return thirdLevelMethodWrapper;
    }

    /**
     * @return
     */
    public IJavaProject getJavaProject2() {
        return fJavaProject2;
    }

    /**
     * @return
     */
    public IPackageFragment getPackage1() {
        return fPack1;
    }

    /**
     * @return
     */
    public IPackageFragment getPackage2() {
        return fPack2;
    }

    /**
     * @return
     */
    public IType getType1() {
        return fType1;
    }

    /**
     * @return
     */
    public IType getType2() {
        return fType2;
    }

    public IMethod getMethod1() {
        if (fMethod1 == null) {
            fMethod1= getType1().getMethod("method1", EMPTY);
        }
        return fMethod1;
    }

    public IMethod getMethod2() {
        if (fMethod2 == null) {
            fMethod2= getType1().getMethod("method2", EMPTY);
        }
        return fMethod2;
    }

    public IMethod getMethod3() {
        if (fMethod3 == null) {
            fMethod3= getType2().getMethod("method3", EMPTY);
        }
        return fMethod3;
    }

    public IMethod getMethod4() {
        if (fMethod4 == null) {
            fMethod4= getType2().getMethod("method4", EMPTY);
        }
        return fMethod4;
    }

    public IMethod getRecursiveMethod1() {
        if (fRecursiveMethod1 == null) {
            fRecursiveMethod1= getType1().getMethod("recursiveMethod1", EMPTY);
        }
        return fRecursiveMethod1;
    }

    public IMethod getRecursiveMethod2() {
        if (fRecursiveMethod2 == null) {
            fRecursiveMethod2= getType1().getMethod("recursiveMethod2", EMPTY);
        }
        return fRecursiveMethod2;
    }
}
