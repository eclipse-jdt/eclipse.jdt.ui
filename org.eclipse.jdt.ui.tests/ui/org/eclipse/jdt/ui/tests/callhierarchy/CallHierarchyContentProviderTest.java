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
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *             (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.callhierarchy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyContentProvider;
import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyUI;
import org.eclipse.jdt.internal.ui.callhierarchy.TreeRoot;

public class CallHierarchyContentProviderTest {

    private static final int DEFAULT_MAX_DEPTH = 10;

    private CallHierarchyTestHelper helper;

    private CallHierarchyContentProvider fProvider;

	private int fOriginalMaxCallDepth;

    @Before
	public void setUp() throws Exception {
		if (!welcomeClosed) {
			closeIntro(PlatformUI.getWorkbench());
		}
		helper= new CallHierarchyTestHelper();
		helper.setUp();
		fProvider= new CallHierarchyContentProvider(null);

		CallHierarchyUI callHierarchyUI= CallHierarchyUI.getDefault();
		fOriginalMaxCallDepth= callHierarchyUI.getMaxCallDepth();
		callHierarchyUI.setMaxCallDepth(DEFAULT_MAX_DEPTH);
		DisplayHelper.driveEventQueue(Display.getDefault());
    }

    @After
	public void tearDown() throws Exception {
        helper.tearDown();
        helper= null;
        CallHierarchyUI.getDefault().setMaxCallDepth(fOriginalMaxCallDepth);
    }

    /*
     * Tests getChildren and hasChildren on an "ordinary" callee tree.
     */
    @Test
    public void testGetChildrenOfCalleeRoot() throws Exception {
        helper.createSimpleClasses();

        TreeRoot root= wrapCalleeRoot(helper.getMethod4());
        Object[] children= fProvider.getChildren(root);
        assertEquals("Wrong number of children", 1, children.length);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, children);
        assertEquals("Wrong method", helper.getMethod4(), ((MethodWrapper) children[0]).getMember());
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod3()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        Object[] thirdLevelChildren= fProvider.getChildren(secondLevelChildren[0]);
        helper.assertCalls(new IMember[] { helper.getMethod1(), helper.getMethod2()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);
        assertTrue("third level hasChildren", fProvider.hasChildren(secondLevelChildren[0]));

        MethodWrapper fourthLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod1(), thirdLevelChildren);
        assertNotNull("method1 not found", fourthLevelMethodWrapper);
        assertEquals(
            "Wrong number of fourth level children",
            0,
            fProvider.getChildren(fourthLevelMethodWrapper).length);
        // hasChildren should be true even if the node doesn't have children (for performance reasons)
        assertTrue("fourth level hasChildren", fProvider.hasChildren(fourthLevelMethodWrapper));
    }

    /*
     * Tests getChildren and hasChildren on an "ordinary" callers tree.
     */
    @Test
    public void testGetChildrenOfCallerRoot() throws Exception {
        helper.createSimpleClasses();

        TreeRoot root= wrapCallerRoot(helper.getMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getMethod1()}, children);
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod2(), helper.getMethod3()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod3(), secondLevelChildren);
        assertNotNull("method3() not found", thirdLevelMethodWrapper);
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);
        assertTrue("third level hasChildren", fProvider.hasChildren(thirdLevelMethodWrapper));

        assertEquals("Wrong number of fourth level children", 0, fProvider.getChildren(thirdLevelChildren[0]).length);
        // hasChildren should be true even if the node doesn't have children (for performance reasons)
        assertTrue("fourth level hasChildren", fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /*
     * Tests getChildren and hasChildren on an callers tree which exceeds the max call depth.
     */
    @Test
    public void testGetChildrenOfCallerMaxDepth() throws Exception {
        helper.createSimpleClasses();

        CallHierarchyUI.getDefault().setMaxCallDepth(2);

        TreeRoot root= wrapCallerRoot(helper.getMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getMethod1()}, children);
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod2(), helper.getMethod3()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod3(), secondLevelChildren);
        assertNotNull("method3() not found", thirdLevelMethodWrapper);
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);
        assertTrue("third level hasChildren", fProvider.hasChildren(thirdLevelMethodWrapper));

        assertEquals("Wrong number of fourth level children", 0, fProvider.getChildren(thirdLevelChildren[0]).length);
        // hasChildren should be false since the maximum depth has been reached
        assertFalse("fourth level hasChildren", fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /*
     * Tests getChildren and hasChildren on an callee tree which exceeds the max call depth.
     */
    @Test
    public void testGetChildrenOfCalleeMaxDepth() throws Exception {
        helper.createSimpleClasses();

        CallHierarchyUI.getDefault().setMaxCallDepth(2);

        TreeRoot root= wrapCalleeRoot(helper.getMethod4());
        Object[] children= fProvider.getChildren(root);
        assertEquals("Wrong number of children", 1, children.length);
        helper.assertCalls(new IMember[] { helper.getMethod4()}, children);
        assertEquals("Wrong method", helper.getMethod4(), ((MethodWrapper) children[0]).getMember());
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getMethod3()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        Object[] thirdLevelChildren= fProvider.getChildren(secondLevelChildren[0]);
        helper.assertCalls(new IMember[] { helper.getMethod1(), helper.getMethod2()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);
        assertTrue("third level hasChildren", fProvider.hasChildren(secondLevelChildren[0]));

        MethodWrapper fourthLevelMethodWrapper= helper.findMethodWrapper(helper.getMethod1(), thirdLevelChildren);
        assertNotNull("method1 not found", fourthLevelMethodWrapper);
        assertEquals(
            "Wrong number of fourth level children",
            0,
            fProvider.getChildren(fourthLevelMethodWrapper).length);
        // hasChildren should be false since the maximum depth has been reached
        assertFalse("fourth level hasChildren", fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /*
    * Tests getChildren and hasChildren on an callers tree with recursion.
    */
    @Test
    public void testGetChildrenOfCalleeRecursive() throws Exception {
        helper.createSimpleClasses();

        TreeRoot root= wrapCalleeRoot(helper.getRecursiveMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, children);
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod2()}, secondLevelChildren);
        assertCalleeMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= (MethodWrapper) secondLevelChildren[0];
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, thirdLevelChildren);
        assertCalleeMethodWrapperChildren(thirdLevelChildren);

        // A recursion should have occurred, resulting in hasChildren = false
        assertFalse("third level hasChildren", fProvider.hasChildren(thirdLevelChildren[0]));
    }

    /*
     * Tests getChildren and hasChildren on an callees tree with recursion.
     */
    @Test
    public void testGetChildrenOfCallerRecursive() throws Exception {
        helper.createSimpleClasses();

        TreeRoot root= wrapCallerRoot(helper.getRecursiveMethod1());
        Object[] children= fProvider.getChildren(root);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, children);
        assertTrue("root's hasChildren", fProvider.hasChildren(root));

        Object[] secondLevelChildren= fProvider.getChildren(children[0]);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod2()}, secondLevelChildren);
        assertCallerMethodWrapperChildren(secondLevelChildren);
        assertTrue("second level hasChildren", fProvider.hasChildren(children[0]));

        MethodWrapper thirdLevelMethodWrapper= (MethodWrapper) secondLevelChildren[0];
        Object[] thirdLevelChildren= fProvider.getChildren(thirdLevelMethodWrapper);
        helper.assertCalls(new IMember[] { helper.getRecursiveMethod1()}, thirdLevelChildren);
        assertCallerMethodWrapperChildren(thirdLevelChildren);

        // A recursion should have occurred, resulting in hasChildren = false
        assertFalse("third level hasChildren", fProvider.hasChildren(thirdLevelChildren[0]));
    }

	@Test
	public void testLambdaCallers() throws Exception {
		helper.createClassWithLambdaCalls();
		TreeRoot root= wrapCallerRoot(helper.getMethod1());
		Object[] children= fProvider.getChildren(root);
		helper.assertCalls(new IMember[] { helper.getMethod1() }, children);
		assertTrue("root's hasChildren", fProvider.hasChildren(root));

		Object[] secondLevelChildren= fProvider.getChildren(children[0]);
		helper.assertCalls(new IMember[] { helper.getMethod2() }, secondLevelChildren);

		Object[] thirdLevelChildren= fProvider.getChildren(secondLevelChildren[0]);

		assertEquals(
				"Wrong number of third level children", 6, thirdLevelChildren.length);
	}

    private void assertCalleeMethodWrapperChildren(Object[] children) {
    	for (Object child : children) {
    		assertTrue("Wrong class returned", child.getClass().getName().endsWith(".CalleeMethodWrapper"));
    	}
    }

    private void assertCallerMethodWrapperChildren(Object[] children) {
    	for (Object child : children) {
    		assertTrue("Wrong class returned", child.getClass().getName().endsWith(".CallerMethodWrapper"));
    	}
    }

    private TreeRoot wrapCalleeRoot(IMethod method) {
        return new TreeRoot(CallHierarchy.getDefault().getCalleeRoots(new IMember[] { method }));
    }

    private TreeRoot wrapCallerRoot(IMethod method) {
        return new TreeRoot(CallHierarchy.getDefault().getCallerRoots(new IMember[] { method }));
    }

	private static boolean welcomeClosed;
	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window= wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im= wb.getIntroManager();
			IIntroPart intro= im.getIntro();
			if (intro != null) {
				welcomeClosed= im.closeIntro(intro);
			}
		}
	}

}
