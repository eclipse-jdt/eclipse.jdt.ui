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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyContentProvider;
import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyUI;
import org.eclipse.jdt.internal.ui.callhierarchy.TreeRoot;

public class CallHierarchyContentProviderTest extends TestCase {
    private static final int DEFAULT_MAX_DEPTH= 10;

    private static final Class THIS= CallHierarchyContentProviderTest.class;

    private CallHierarchyTestHelper helper;

    private CallHierarchyContentProvider fProvider;
    public CallHierarchyContentProviderTest(String name) {
        super(name);
    }

    public static Test suite() {
        if (true) {
            return new TestSuite(THIS);
        } else {
            TestSuite suite= new TestSuite();
            suite.addTest(new CallHierarchyContentProviderTest("test1"));

            return suite;
        }
    }

    protected void setUp() throws Exception {
        helper= new CallHierarchyTestHelper();
        helper.setUp();

        fProvider= new CallHierarchyContentProvider(null);

        CallHierarchyUI.getDefault().setMaxCallDepth(DEFAULT_MAX_DEPTH);
    }

    protected void tearDown() throws Exception {
        helper.tearDown();
        helper= null;

        CallHierarchyUI.getDefault().setMaxCallDepth(DEFAULT_MAX_DEPTH);
    }

    /**
     * Tests getChildren and hasChildren on an "ordinary" callee tree.
     *
     * @throws JavaModelException
     * @throws CoreException
     */
    public void testGetChildrenOfCalleeRoot() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        TreeRoot root= wrapCalleeRoot(helper.getMethod4());
        Object[] children= fProvider.getChildren(root);
        assertEquals("Wrong number of children", 1, children.length);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, children);
        assertEquals("Wrong method", helper.getMethod4(), ((MethodWrapper) children[0]).getMember());
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod3()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        Object[] thirdLevelChildren= fProvider.getChildren(secondLevelChildren[0]);
        helper.assertCalls(new IMember[] { helper.getMethod1(), helper.getMethod2()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);
        assertEquals("third level hasChildren", true, fProvider.hasChildren(secondLevelChildren[0]));

        MethodWrapper fourthLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod1(), thirdLevelChildren);
        assertNotNull("method1 not found", fourthLevelMethodWrapper);
        assertEquals(
            "Wrong number of fourth level children",
            0,
            fProvider.getChildren(fourthLevelMethodWrapper).length);
        // hasChildren should be true even if the node doesn't have children (for performance reasons)
        assertEquals("fourth level hasChildren", true, fProvider.hasChildren(fourthLevelMethodWrapper));
    }

    /**
     * Tests getChildren and hasChildren on an "ordinary" callers tree.
     *
     * @throws JavaModelException
     * @throws CoreException
     */
    public void testGetChildrenOfCallerRoot() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        TreeRoot root= wrapCallerRoot(helper.getMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getMethod1()}, children);
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod2(), helper.getMethod3()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod3(), secondLevelChildren);
        assertNotNull("method3() not found", thirdLevelMethodWrapper);
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);
        assertEquals("third level hasChildren", true, fProvider.hasChildren(thirdLevelMethodWrapper));

        assertEquals("Wrong number of fourth level children", 0, fProvider.getChildren(thirdLevelChildren[0]).length);
        // hasChildren should be true even if the node doesn't have children (for performance reasons)
        assertEquals("fourth level hasChildren", true, fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /**
     * Tests getChildren and hasChildren on an callers tree which exceeds the max call depth.
     *
     * @throws JavaModelException
     * @throws CoreException
     */
    public void testGetChildrenOfCallerMaxDepth() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        CallHierarchyUI.getDefault().setMaxCallDepth(2);

        TreeRoot root= wrapCallerRoot(helper.getMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getMethod1()}, children);
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod2(), helper.getMethod3()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod3(), secondLevelChildren);
        assertNotNull("method3() not found", thirdLevelMethodWrapper);
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);
        assertEquals("third level hasChildren", true, fProvider.hasChildren(thirdLevelMethodWrapper));

        assertEquals("Wrong number of fourth level children", 0, fProvider.getChildren(thirdLevelChildren[0]).length);
        // hasChildren should be false since the maximum depth has been reached
        assertEquals("fourth level hasChildren", false, fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /**
     * Tests getChildren and hasChildren on an callee tree which exceeds the max call depth.
     *
     * @throws JavaModelException
     * @throws CoreException
     */
    public void testGetChildrenOfCalleeMaxDepth() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        CallHierarchyUI.getDefault().setMaxCallDepth(2);

        TreeRoot root= wrapCalleeRoot(helper.getMethod4());
        Object[] children= fProvider.getChildren(root);
        assertEquals("Wrong number of children", 1, children.length);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, children);
        assertEquals("Wrong method", helper.getMethod4(), ((MethodWrapper) children[0]).getMember());
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod3()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        Object[] thirdLevelChildren= fProvider.getChildren(secondLevelChildren[0]);
        helper.assertCalls(new IMember[] { helper.getMethod1(), helper.getMethod2()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);
        assertEquals("third level hasChildren", true, fProvider.hasChildren(secondLevelChildren[0]));

        MethodWrapper fourthLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod1(), thirdLevelChildren);
        assertNotNull("method1 not found", fourthLevelMethodWrapper);
        assertEquals(
            "Wrong number of fourth level children",
            0,
            fProvider.getChildren(fourthLevelMethodWrapper).length);
        // hasChildren should be false since the maximum depth has been reached
        assertEquals("fourth level hasChildren", false, fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /**
    * Tests getChildren and hasChildren on an callers tree with recursion.
    *
    * @throws JavaModelException
    * @throws CoreException
    */
    public void testGetChildrenOfCalleeRecursive() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        TreeRoot root= wrapCalleeRoot(helper.getRecursiveMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, children);
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod2()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= (MethodWrapper) secondLevelChildren[0];
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);

        // A recursion should have occurred, resulting in hasChildren = false
        assertEquals("third level hasChildren", false, fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /**
     * Tests getChildren and hasChildren on an callees tree with recursion.
     *
     * @throws JavaModelException
     * @throws CoreException
     */
    public void testGetChildrenOfCallerRecursive() throws JavaModelException, CoreException {
        helper.createSimpleClasses();

        TreeRoot root= wrapCallerRoot(helper.getRecursiveMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, children);
        assertEquals("root's hasChildren", true, fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod2()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertEquals("second level hasChildren", true, fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= (MethodWrapper) secondLevelChildren[0];
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);

        // A recursion should have occurred, resulting in hasChildren = false
        assertEquals("third level hasChildren", false, fProvider.hasChildren(thirdLevelChildren[0]));
    }

    private void assertCalleeMethodWrapperChildren(Object[] children) {
        for (int i= 0; i < children.length; i++) {
            assertTrue("Wrong class returned", children[i].getClass().getName().endsWith(".CalleeMethodWrapper"));
        }
    }

    private void assertCallerMethodWrapperChildren(Object[] children) {
        for (int i= 0; i < children.length; i++) {
            assertTrue("Wrong class returned", children[i].getClass().getName().endsWith(".CallerMethodWrapper"));
        }
    }

    private TreeRoot wrapCalleeRoot(IMethod method) {
        return new TreeRoot(CallHierarchy.getDefault().getCalleeRoots(new IMember[] { method }));
    }

    private TreeRoot wrapCallerRoot(IMethod method) {
        return new TreeRoot(CallHierarchy.getDefault().getCallerRoots(new IMember[] { method }));
    }

}
