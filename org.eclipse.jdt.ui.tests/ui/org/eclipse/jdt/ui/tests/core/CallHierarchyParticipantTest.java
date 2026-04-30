/*******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Arcadiy Ivanov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyCore;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.ui.tests.callhierarchy.TestCallHierarchyParticipant;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

/**
 * Tests for call hierarchy integration with contributed search participants.
 *
 * <p>Verifies that {@code CallerMethodWrapper} uses all search participants
 * (not just the default) for incoming calls, and that {@code CalleeMethodWrapper}
 * falls back to {@code SearchParticipant.locateCallees()} for outgoing calls
 * when no Java AST is available.
 */
public class CallHierarchyParticipantTest {

	private IJavaProject fSourceProject;
	private IJavaProject fBinaryProject;

	@Before
	public void setUp() throws Exception {
		fSourceProject = JavaProjectHelper.createJavaProject("SourceProject", "bin");
		fBinaryProject = JavaProjectHelper.createJavaProject("BinaryProject", "bin");
		TestCallHierarchyParticipant.reset();
	}

	@After
	public void tearDown() throws Exception {
		TestCallHierarchyParticipant.reset();
		JavaProjectHelper.delete(fBinaryProject);
		JavaProjectHelper.delete(fSourceProject);
	}

	/**
	 * Verifies that {@code SearchEngine.getSearchParticipants()} includes both the
	 * default participant and the contributed test participant.
	 */
	@Test
	public void searchParticipantsIncludesContributed() throws Exception {
		SearchParticipant[] participants = SearchEngine.getSearchParticipants();
		assertTrue("Should have at least 2 participants (default + contributed)",
				participants.length >= 2);

		boolean foundTest = false;
		for (SearchParticipant p : participants) {
			if (p instanceof TestCallHierarchyParticipant) {
				foundTest = true;
			}
		}
		assertTrue("Test search participant should be present", foundTest);
		assertNotNull("First participant should be non-null", participants[0]);
	}

	/**
	 * Verifies that the existing Java AST-based callee analysis path is unchanged
	 * when a Java AST is available.
	 */
	@Test
	public void outgoingCallsJavaASTPathUnchanged() throws Exception {
		JavaProjectHelper.addRTJar9(fSourceProject);
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fSourceProject, "src");
		IPackageFragment pkg = src.createPackageFragment("testpkg", true, null);
		ICompilationUnit cu = pkg.createCompilationUnit("Source.java",
				"""
				package testpkg;
				public class Source {
				    public void targetMethod() {}
				    public void callerMethod() { targetMethod(); }
				}
				""",
				true, null);
		fSourceProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IType type = cu.getType("Source");
		IMethod callerMethod = type.getMethod("callerMethod", new String[0]);
		assertNotNull(callerMethod);
		assertTrue(callerMethod.exists());

		TestCallHierarchyParticipant.reset();

		MethodWrapper[] roots = CallHierarchyCore.getDefault().getCalleeRoots(new IMember[] { callerMethod });
		assertEquals(1, roots.length);
		MethodWrapper[] callees = roots[0].getCalls(new NullProgressMonitor());

		assertEquals("Should find exactly one callee via Java AST", 1, callees.length);
		assertEquals("targetMethod", callees[0].getMember().getElementName());
	}

	/**
	 * Verifies that {@code CalleeMethodWrapper} falls back to
	 * {@code SearchParticipant.locateCallees()} when the member has no Java AST
	 * (binary member without source attachment).
	 */
	@Test
	public void outgoingCallsFallBackToParticipants() throws Exception {
		JavaProjectHelper.addRTJar9(fSourceProject);
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fSourceProject, "src");
		IPackageFragment pkg = src.createPackageFragment("testpkg", true, null);
		ICompilationUnit cu = pkg.createCompilationUnit("Lib.java",
				"""
				package testpkg;
				public class Lib {
				    public void targetMethod() {}
				    public void anotherTarget() {}
				    public void callerMethod() { targetMethod(); anotherTarget(); }
				}
				""",
				true, null);
		fSourceProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IType sourceType = cu.getType("Lib");
		IMethod sourceTargetMethod = sourceType.getMethod("targetMethod", new String[0]);
		IMethod sourceAnotherTarget = sourceType.getMethod("anotherTarget", new String[0]);
		assertTrue("sourceTargetMethod should exist", sourceTargetMethod.exists());
		assertTrue("sourceAnotherTarget should exist", sourceAnotherTarget.exists());

		// Add source project's output as a class folder to binary project (no source attachment)
		JavaProjectHelper.addRTJar9(fBinaryProject);
		IPath outputPath = fSourceProject.getOutputLocation();
		IFolder outputFolder = fSourceProject.getProject()
				.getFolder(outputPath.removeFirstSegments(1));
		JavaProjectHelper.addLibrary(fBinaryProject, outputFolder.getFullPath(), null, null);

		IType binaryType = fBinaryProject.findType("testpkg.Lib");
		assertNotNull("Binary type should be found", binaryType);
		IMethod binaryCallerMethod = binaryType.getMethod("callerMethod", new String[0]);
		assertTrue("Binary callerMethod should exist", binaryCallerMethod.exists());

		// Verify this is truly a binary member with no source
		ITypeRoot typeRoot = binaryCallerMethod.getTypeRoot();
		assertTrue("Should be a class file, not source", typeRoot instanceof IClassFile);
		assertNull("Binary class file without source should have null buffer", typeRoot.getBuffer());

		// Set up participant to return callees
		TestCallHierarchyParticipant.reset();
		TestCallHierarchyParticipant.calleesToReturn = new SearchMatch[] {
				new SearchMatch(sourceTargetMethod, SearchMatch.A_ACCURATE, 0, 14,
						SearchEngine.getDefaultSearchParticipant(),
						binaryCallerMethod.getResource()),
				new SearchMatch(sourceAnotherTarget, SearchMatch.A_ACCURATE, 20, 13,
						SearchEngine.getDefaultSearchParticipant(),
						binaryCallerMethod.getResource()),
		};

		// Invoke callee hierarchy on the binary method
		MethodWrapper[] roots = CallHierarchyCore.getDefault().getCalleeRoots(
				new IMember[] { binaryCallerMethod });
		assertEquals(1, roots.length);
		MethodWrapper[] callees = roots[0].getCalls(new NullProgressMonitor());

		// Verify participant was called
		assertTrue("locateCallees should have been called",
				TestCallHierarchyParticipant.locateCalleesCallCount > 0);

		// Verify callees from participant appeared
		assertEquals("Should find 2 callees from participant", 2, callees.length);

		boolean foundTarget = false;
		boolean foundAnother = false;
		for (MethodWrapper callee : callees) {
			String name = callee.getMember().getElementName();
			if ("targetMethod".equals(name)) foundTarget = true;
			if ("anotherTarget".equals(name)) foundAnother = true;
		}
		assertTrue("Should find targetMethod as callee", foundTarget);
		assertTrue("Should find anotherTarget as callee", foundAnother);
	}

	/**
	 * Verifies that {@code CalleeMethodWrapper.resolveCallee()} resolves
	 * existing callees directly without additional search.
	 */
	@Test
	public void outgoingCallsResolvesExistingCallees() throws Exception {
		JavaProjectHelper.addRTJar9(fSourceProject);
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fSourceProject, "src");
		IPackageFragment pkg = src.createPackageFragment("testpkg", true, null);
		ICompilationUnit cu = pkg.createCompilationUnit("Lib.java",
				"""
				package testpkg;
				public class Lib {
				    public void targetMethod() {}
				    public void anotherTarget() {}
				    public void callerMethod() {}
				}
				""",
				true, null);
		fSourceProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Add source project's output as a class folder to binary project (no source)
		JavaProjectHelper.addRTJar9(fBinaryProject);
		IPath outputPath = fSourceProject.getOutputLocation();
		IFolder outputFolder = fSourceProject.getProject()
				.getFolder(outputPath.removeFirstSegments(1));
		JavaProjectHelper.addLibrary(fBinaryProject, outputFolder.getFullPath(), null, null);

		IType binaryType = fBinaryProject.findType("testpkg.Lib");
		assertNotNull("Binary type should be found", binaryType);
		IMethod binaryCallerMethod = binaryType.getMethod("callerMethod", new String[0]);
		assertTrue("Binary callerMethod should exist", binaryCallerMethod.exists());

		// Return source methods (exists=true) - resolveCallee short-circuits
		IType sourceType = cu.getType("Lib");
		IMethod sourceTargetMethod = sourceType.getMethod("targetMethod", new String[0]);
		IMethod sourceAnotherTarget = sourceType.getMethod("anotherTarget", new String[0]);
		assertTrue("Source targetMethod should exist for resolution", sourceTargetMethod.exists());
		assertTrue("Source anotherTarget should exist for resolution", sourceAnotherTarget.exists());

		TestCallHierarchyParticipant.reset();
		TestCallHierarchyParticipant.calleesToReturn = new SearchMatch[] {
				new SearchMatch(sourceTargetMethod, SearchMatch.A_ACCURATE, 0, 14,
						SearchEngine.getDefaultSearchParticipant(),
						binaryCallerMethod.getResource()),
				new SearchMatch(sourceAnotherTarget, SearchMatch.A_ACCURATE, 20, 13,
						SearchEngine.getDefaultSearchParticipant(),
						binaryCallerMethod.getResource()),
		};

		MethodWrapper[] roots = CallHierarchyCore.getDefault().getCalleeRoots(
				new IMember[] { binaryCallerMethod });
		assertEquals(1, roots.length);
		MethodWrapper[] callees = roots[0].getCalls(new NullProgressMonitor());

		assertTrue("locateCallees should have been called",
				TestCallHierarchyParticipant.locateCalleesCallCount > 0);
		assertEquals("Should find 2 callees", 2, callees.length);
	}

	/**
	 * Verifies that {@code CalleeMethodWrapper} falls back to participants when
	 * the member's type root is a non-standard {@link ICompilationUnit} that does
	 * not implement the internal compiler interface (causing a
	 * {@code ClassCastException} in {@code ASTParser.createAST()}).
	 *
	 * <p>This simulates the scenario where a contributed search participant provides
	 * {@code IMember} instances backed by a custom {@code ICompilationUnit}
	 * (e.g. KotlinCompilationUnit) that implements the public API but not the
	 * internal compiler interface.
	 */
	@Test
	public void outgoingCallsFallBackOnClassCastException() throws Exception {
		JavaProjectHelper.addRTJar9(fSourceProject);
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fSourceProject, "src");
		IPackageFragment pkg = src.createPackageFragment("testpkg", true, null);
		ICompilationUnit cu = pkg.createCompilationUnit("Source.java",
				"""
				package testpkg;
				public class Source {
				    public void targetMethod() {}
				    public void anotherTarget() {}
				    public void callerMethod() { targetMethod(); anotherTarget(); }
				}
				""",
				true, null);
		fSourceProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IType sourceType = cu.getType("Source");
		IMethod realCallerMethod = sourceType.getMethod("callerMethod", new String[0]);
		IMethod sourceTargetMethod = sourceType.getMethod("targetMethod", new String[0]);
		IMethod sourceAnotherTarget = sourceType.getMethod("anotherTarget", new String[0]);
		assertTrue(realCallerMethod.exists());

		// Create a proxy ICompilationUnit that implements only the public API
		// (not org.eclipse.jdt.internal.compiler.env.ICompilationUnit),
		// causing ASTParser.createAST() to throw ClassCastException
		ICompilationUnit fakeCU = (ICompilationUnit) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { ICompilationUnit.class },
				(proxy, method, args) -> method.invoke(cu, args));

		// Create a proxy IMethod that delegates to realCallerMethod but returns
		// the non-standard ICompilationUnit from getTypeRoot()
		IMethod proxyMethod = (IMethod) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { IMethod.class },
				(proxy, method, args) -> {
					if ("getTypeRoot".equals(method.getName())) {
						return fakeCU;
					}
					return method.invoke(realCallerMethod, args);
				});

		assertTrue("Proxy method should delegate exists()", proxyMethod.exists());
		assertNotNull("Proxy method should have a type root", proxyMethod.getTypeRoot());
		assertNotNull("Proxy type root should have a buffer", proxyMethod.getTypeRoot().getBuffer());

		// Set up participant to return callees
		TestCallHierarchyParticipant.reset();
		TestCallHierarchyParticipant.calleesToReturn = new SearchMatch[] {
				new SearchMatch(sourceTargetMethod, SearchMatch.A_ACCURATE, 0, 14,
						SearchEngine.getDefaultSearchParticipant(),
						realCallerMethod.getResource()),
				new SearchMatch(sourceAnotherTarget, SearchMatch.A_ACCURATE, 20, 13,
						SearchEngine.getDefaultSearchParticipant(),
						realCallerMethod.getResource()),
		};

		// Invoke callee hierarchy on the proxy method — should NOT crash with
		// ClassCastException, should fall back to participants
		MethodWrapper[] roots = CallHierarchyCore.getDefault().getCalleeRoots(
				new IMember[] { proxyMethod });
		assertEquals(1, roots.length);
		MethodWrapper[] callees = roots[0].getCalls(new NullProgressMonitor());

		assertTrue("locateCallees should have been called after ClassCastException fallback",
				TestCallHierarchyParticipant.locateCalleesCallCount > 0);
		assertEquals("Should find 2 callees from participant", 2, callees.length);
	}

	/**
	 * Verifies that incoming call search uses all search participants
	 * (not just the default) and still finds Java callers correctly.
	 * Regression test for switching from {@code getDefaultSearchParticipant()}
	 * to {@code SearchEngine.getSearchParticipants()}.
	 */
	@Test
	public void incomingCallsUsesAllParticipants() throws Exception {
		JavaProjectHelper.addRTJar9(fSourceProject);
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fSourceProject, "src");
		IPackageFragment pkg = src.createPackageFragment("testpkg", true, null);
		ICompilationUnit cu = pkg.createCompilationUnit("Caller.java",
				"""
				package testpkg;
				public class Caller {
				    public void targetMethod() {}
				    public void firstCaller() { targetMethod(); }
				    public void secondCaller() { targetMethod(); }
				}
				""",
				true, null);
		fSourceProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		IType type = cu.getType("Caller");
		IMethod targetMethod = type.getMethod("targetMethod", new String[0]);
		assertNotNull(targetMethod);
		assertTrue(targetMethod.exists());

		MethodWrapper[] roots = CallHierarchyCore.getDefault().getCallerRoots(
				new IMember[] { targetMethod });
		assertEquals(1, roots.length);
		MethodWrapper[] callers = roots[0].getCalls(new NullProgressMonitor());

		assertEquals("Should find 2 callers", 2, callers.length);

		boolean foundFirst = false;
		boolean foundSecond = false;
		for (MethodWrapper caller : callers) {
			String name = caller.getMember().getElementName();
			if ("firstCaller".equals(name)) foundFirst = true;
			if ("secondCaller".equals(name)) foundSecond = true;
		}
		assertTrue("Should find firstCaller", foundFirst);
		assertTrue("Should find secondCaller", foundSecond);
	}
}
