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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.ui.tests.callhierarchy.CallHierarchyTestHelper;

public class CallHierarchyTest {
    private static final String[] EMPTY= new String[0];

    private CallHierarchyTestHelper helper;

	@Before
	public void setUp() throws Exception {
        helper= new CallHierarchyTestHelper();
        helper.setUp();
    }

	@After
	public void tearDown() throws Exception {
        helper.tearDown();
        helper= null;
    }

	static MethodWrapper getSingleCallerRoot(IMethod method) {
		MethodWrapper[] methodWrappers= CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
		assertEquals(1, methodWrappers.length);
		return methodWrappers[0];
	}
	static MethodWrapper getSingleCalleeRoot(IMethod method) {
		MethodWrapper[] methodWrappers= CallHierarchy.getDefault().getCalleeRoots(new IMember[] { method });
		assertEquals(1, methodWrappers.length);
		return methodWrappers[0];
	}

	@Test
	public void callers() throws Exception {
        helper.createSimpleClasses();

        IMethod method= helper.getMethod1();
        IMethod secondLevelMethod= helper.getMethod3();

        Collection<IMember> expectedMethods= new ArrayList<>();
        expectedMethods.add(helper.getMethod2());
        expectedMethods.add(secondLevelMethod);

        MethodWrapper wrapper= getSingleCallerRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, cachedCalls);

        MethodWrapper wrapper2= helper.findMethodWrapper(secondLevelMethod, cachedCalls);

        Collection<IMember> expectedSecondLevelMethods= new ArrayList<>();
        expectedSecondLevelMethods.add(helper.getMethod4());
        helper.assertCalls(expectedSecondLevelMethods, wrapper2.getCalls(new NullProgressMonitor()));
    }

	@Test
	public void callersNoResults() throws Exception {
        helper.createSimpleClasses();

        IMethod method= helper.getMethod4();

        Collection<IMember> expectedMethods= new ArrayList<>();

        MethodWrapper wrapper= getSingleCallerRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, cachedCalls);
    }

	@Test
	public void callees() throws Exception {
        helper.createSimpleClasses();

        IMethod method= helper.getMethod4();
        IMethod secondLevelMethod= helper.getMethod3();

        Collection<IMember> expectedMethods= new ArrayList<>();
        expectedMethods.add(secondLevelMethod);

        MethodWrapper wrapper= getSingleCalleeRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, cachedCalls);

        MethodWrapper wrapper2= helper.findMethodWrapper(secondLevelMethod, cachedCalls);

        Collection<IMember> expectedMethodsTo3= new ArrayList<>();
        expectedMethodsTo3.add(helper.getMethod1());
        expectedMethodsTo3.add(helper.getMethod2());

        helper.assertCalls(expectedMethodsTo3, wrapper2.getCalls(new NullProgressMonitor()));
    }

	@Test
	public void calleesNoResults() throws Exception {
        helper.createSimpleClasses();

        IMethod method= helper.getMethod1();

        Collection<IMember> expectedMethods= new ArrayList<>();

        MethodWrapper wrapper= getSingleCalleeRoot(method);

        MethodWrapper[] uncachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, uncachedCalls);

        MethodWrapper[] cachedCalls= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethods, cachedCalls);
    }

	@Test
	public void recursiveCallers() throws Exception {
        helper.createSimpleClasses();

        IMethod method1= helper.getRecursiveMethod1();
        IMethod method2= helper.getRecursiveMethod2();

        Collection<IMember> expectedMethodsTo1= new ArrayList<>();
        expectedMethodsTo1.add(method2);

        Collection<IMember> expectedMethodsTo2= new ArrayList<>();
        expectedMethodsTo2.add(method1);

        MethodWrapper wrapper= getSingleCallerRoot(method1);
        MethodWrapper[] callsTo1= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callsTo1, false);

        MethodWrapper wrapper2= helper.findMethodWrapper(method2, callsTo1);
        assertFalse("Should be marked as recursive", wrapper2.isRecursive());

        MethodWrapper[] callsTo2= wrapper2.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethodsTo2, callsTo2);

        assertRecursive(callsTo2, true);

        MethodWrapper method1Wrapper= helper.findMethodWrapper(method1, callsTo2);
        callsTo1= method1Wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethodsTo1, callsTo1);

        assertRecursive(callsTo1, true);
    }

	@Test
	public void recursiveCallees() throws Exception {
        helper.createSimpleClasses();

        IMethod method1= helper.getRecursiveMethod1();
        IMethod method2= helper.getRecursiveMethod2();

        Collection<IMember> expectedMethodsFrom1= new ArrayList<>();
        expectedMethodsFrom1.add(method2);

        Collection<IMember> expectedMethodsFrom2= new ArrayList<>();
        expectedMethodsFrom2.add(method1);

        MethodWrapper wrapper= getSingleCalleeRoot(method1);
        MethodWrapper[] callsFrom1= wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethodsFrom1, callsFrom1);

        MethodWrapper wrapper2= helper.findMethodWrapper(method2, callsFrom1);
        assertRecursive(callsFrom1, false);

        MethodWrapper[] callsFrom2= wrapper2.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethodsFrom2, callsFrom2);

        assertRecursive(callsFrom2, true);

        MethodWrapper method1Wrapper= helper.findMethodWrapper(method1, callsFrom2);
        callsFrom1= method1Wrapper.getCalls(new NullProgressMonitor());
        helper.assertCalls(expectedMethodsFrom1, callsFrom1);

        assertRecursive(callsFrom1, true);
    }

    /**
     * Tests calls that origin from an inner class
     * @throws Exception
     */
	@Test
	public void innerClassCallers() throws Exception {
        helper.createInnerClass();

        IMethod someMethod= helper.getType1().getMethod("outerMethod1", EMPTY);

        IMethod innerMethod1= helper.getType1().getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= helper.getType1().getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(innerMethod1);

        MethodWrapper wrapper= getSingleCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);

        Collection<IMember> expectedCallersSecondLevel= new ArrayList<>();
        expectedCallersSecondLevel.add(innerMethod2);
        MethodWrapper innerMethod1Wrapper= helper.findMethodWrapper(innerMethod1, callers);
        helper.assertCalls(expectedCallersSecondLevel, innerMethod1Wrapper.getCalls(new NullProgressMonitor()));
    }

    /**
     * Tests callees that enter an inner class
     * @throws Exception
     */
	@Test
	public void innerClassCalleesEntering() throws Exception {
        helper.createInnerClass();

        IMethod someMethod= helper.getType1().getMethod("outerMethod2", EMPTY);

        IMethod innerMethod1= helper.getType1().getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= helper.getType1().getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(innerMethod2);

        MethodWrapper wrapper= getSingleCalleeRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);

        Collection<IMember> expectedCallersSecondLevel= new ArrayList<>();
        expectedCallersSecondLevel.add(innerMethod1);
        MethodWrapper innerMethod2Wrapper= helper.findMethodWrapper(innerMethod2, callers);
        helper.assertCalls(expectedCallersSecondLevel, innerMethod2Wrapper.getCalls(new NullProgressMonitor()));
    }

    /**
     * Tests callees that exits an inner class
     * @throws Exception
     */
	@Test
	public void innerClassCalleesExiting() throws Exception {
        helper.createInnerClass();

        IMethod someMethod= helper.getType1().getMethod("outerMethod1", EMPTY);

        IMethod innerMethod1= helper.getType1().getType("Inner").getMethod("innerMethod1", EMPTY);
        IMethod innerMethod2= helper.getType1().getType("Inner").getMethod("innerMethod2", EMPTY);

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(innerMethod1);

        MethodWrapper wrapper= getSingleCalleeRoot(innerMethod2);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);

        Collection<IMember> expectedCallersSecondLevel= new ArrayList<>();
        expectedCallersSecondLevel.add(someMethod);
        MethodWrapper innerMethod1Wrapper= helper.findMethodWrapper(innerMethod1, callers);
        helper.assertCalls(expectedCallersSecondLevel, innerMethod1Wrapper.getCalls(new NullProgressMonitor()));
    }

    /**
     * Tests calls that origin from an inner class
     * @throws Exception
     */
	@Test
	public void anonymousInnerClassCallers() throws Exception {
        helper.createAnonymousInnerClass();

        IMethod someMethod= helper.getType1().getMethod("someMethod", EMPTY);

        IMethod result= helper.getType1().getField("anonClass").getType("", 1).getMethod("anotherMethod", EMPTY);
        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(result);

        MethodWrapper wrapper= getSingleCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);
    }

    /**
     * Tests calls that origin from an inner class
     * @throws Exception
     */
	@Test
	public void anonymousInnerClassOnInterfaceCallees() throws Exception {
    	//regression test for bug 37290 call hierarchy: Searching for callees into anonymous inner classes fails
        helper.createAnonymousInnerClass();

        IMethod method= helper.getType2().getMethod("anonymousOnInterface", EMPTY);

        MethodWrapper wrapper= getSingleCalleeRoot(method);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);

        assertEquals("Wrong number of callees", 1, callers.length);
        IMember member= callers[0].getMember();
        assertTrue("Wrong member type (expected an instanceof IType)", member instanceof IType);
        assertEquals("Wrong member name", "Intf", member.getElementName());
    }

    /**
     * Tests calls that origin from an inner class
     * @throws Exception
     */
	@Test
	public void anonymousInnerClassInsideMethodCallees() throws Exception {
        //regression test for bug 56732 call hierarchy: Call Hierarchy doesn't show callees of method from anonymous type
        helper.createAnonymousInnerClassInsideMethod();

        IMethod methodM= helper.getType1().getMethod("m", EMPTY);

        MethodWrapper wrapper= getSingleCalleeRoot(methodM);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);

        assertEquals("Wrong number of callees", 3, callers.length);

        IMethod methodRun= methodM.getType("", 1).getMethod("run", EMPTY);

        wrapper= getSingleCalleeRoot(methodRun);
        callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);

        assertEquals("Wrong number of callees", 1, callers.length);
        assertEquals("Wrong callee method", "println", callers[0].getMember().getElementName());
        assertEquals("Wrong callee type", "java.io.PrintStream", callers[0].getMember().getDeclaringType().getFullyQualifiedName());
    }

    /**
     * Tests calls that origin from an inner class
     * @throws Exception
     */
	@Test
	public void anonymousInnerClassOnClassCallees() throws Exception {
		//regression test for bug 37290 call hierarchy: Searching for callees into anonymous inner classes fails
        helper.createAnonymousInnerClass();

        IMethod method= helper.getType2().getMethod("anonymousOnClass", EMPTY);

        MethodWrapper wrapper= getSingleCalleeRoot(method);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);

        assertEquals("Wrong number of callees", 1, callers.length);
        IMember member= callers[0].getMember();
        assertTrue("Wrong member type (expected an instanceof IType)", member instanceof IType);
        assertEquals("Wrong member name", "Clazz", member.getElementName());
    }

    /**
     * Tests calls that origin from a static initializer block.
     * @throws Exception
     */
	@Test
	public void initializerCallers() throws Exception {
        helper.createStaticInitializerClass();

        IMethod someMethod= helper.getType1().getMethod("someMethod", EMPTY);

        IInitializer initializer= helper.getType1().getInitializer(1);

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(initializer);

        MethodWrapper wrapper= getSingleCallerRoot(someMethod);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);
    }

	@Test
	public void implicitConstructorCallers() throws Exception {
        helper.createSimpleClasses();

        IMethod constructorA= helper.getType1().getMethod("A", EMPTY);

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(helper.getType2());

        MethodWrapper wrapper= getSingleCallerRoot(constructorA);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);
    }

	@Test
	public void implicitConstructorCallees() throws Exception {
        helper.createImplicitConstructorClasses();

        IMethod constructorB= helper.getType2().getMethods()[0];

        Collection<IMember> expectedCallers= new ArrayList<>();
        expectedCallers.add(helper.getType1());

        MethodWrapper wrapper= getSingleCalleeRoot(constructorB);
        MethodWrapper[] callers= wrapper.getCalls(new NullProgressMonitor());
        assertRecursive(callers, false);
        helper.assertCalls(expectedCallers, callers);
    }

	@Test
	public void lineNumberCallers() throws Exception {
        helper.createSimpleClasses();

        MethodWrapper wrapper= getSingleCallerRoot(helper.getMethod1());
        MethodWrapper[] calls= wrapper.getCalls(new NullProgressMonitor());
        MethodWrapper method2Wrapper= helper.findMethodWrapper(helper.getMethod2(), calls);
        assertEquals("Wrong line number", 9, method2Wrapper.getMethodCall().getFirstCallLocation().getLineNumber());

        wrapper= getSingleCallerRoot(helper.getRecursiveMethod2());
        calls= wrapper.getCalls(new NullProgressMonitor());
        MethodWrapper recursiveMethod1Wrapper= helper.findMethodWrapper(helper.getRecursiveMethod1(), calls);
        assertEquals("Wrong line number", 12, recursiveMethod1Wrapper.getMethodCall().getFirstCallLocation().getLineNumber());
    }

	@Test
	public void lineNumberCallees() throws Exception {
        helper.createSimpleClasses();

        MethodWrapper wrapper= getSingleCalleeRoot(helper.getMethod2());
        MethodWrapper[] calls= wrapper.getCalls(new NullProgressMonitor());
        MethodWrapper method1Wrapper= helper.findMethodWrapper(helper.getMethod1(), calls);
        assertEquals("Wrong line number", 9, method1Wrapper.getMethodCall().getFirstCallLocation().getLineNumber());

        wrapper= getSingleCalleeRoot(helper.getRecursiveMethod1());
        calls= wrapper.getCalls(new NullProgressMonitor());
        MethodWrapper recursiveMethod2Wrapper= helper.findMethodWrapper(helper.getRecursiveMethod2(), calls);
        assertEquals("Wrong line number", 12, recursiveMethod2Wrapper.getMethodCall().getFirstCallLocation().getLineNumber());
    }

    private void assertRecursive(MethodWrapper[] callResults, boolean shouldBeRecursive) {
    	for (MethodWrapper callResult : callResults) {
    		assertEquals("Wrong recursive value: " + callResult.getName(), shouldBeRecursive, callResult.isRecursive());
    	}
    }
}
