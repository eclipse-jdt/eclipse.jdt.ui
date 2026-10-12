/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator.ExclusionTarget;

/** Shared fixture creation and contextual assertions for the EnumSource tests. */
final class EnumSourceTestSupport {

	static ICompilationUnit createCompilationUnit(IPackageFragmentRoot sourceFolder, String packageName,
			String unitName, String source, boolean force) throws JavaModelException {
		IPackageFragment pack= sourceFolder.createPackageFragment(packageName, false, null);
		ICompilationUnit cu= pack.createCompilationUnit(unitName, source, force, null);
		assertCompiles(cu);
		return cu;
	}

	static IMethod getMethod(ICompilationUnit cu, String name, String... parameterSignatures) throws JavaModelException {
		IType type= cu.findPrimaryType();
		assertNotNull("Expected a primary type in " + sourceContext(cu), type); //$NON-NLS-1$
		IMethod method= type.getMethod(name, parameterSignatures);
		assertTrue("Expected method " + name + Arrays.toString(parameterSignatures) + " in " + sourceContext(cu), method.exists()); //$NON-NLS-1$ //$NON-NLS-2$
		return method;
	}

	static void assertCompiles(ICompilationUnit cu) throws JavaModelException {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		String errors= Arrays.stream(root.getProblems()).filter(IProblem::isError)
				.map(problem -> "line " + problem.getSourceLineNumber() + ": " + problem.getMessage()) //$NON-NLS-1$ //$NON-NLS-2$
				.collect(Collectors.joining(System.lineSeparator()));
		assertEquals("Expected no compile errors in " + sourceContext(cu), "", errors); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static String sourceContext(ICompilationUnit cu) throws JavaModelException {
		return cu.getPath() + System.lineSeparator() + cu.getSource();
	}

	static String methodContext(IMethod method) throws JavaModelException {
		return method.getDeclaringType().getFullyQualifiedName('$') + '#' + method.getElementName()
				+ Arrays.toString(method.getParameterTypes()) + System.lineSeparator() + sourceContext(method.getCompilationUnit());
	}

	static TestCaseElement invocation(IMethod method, int index) throws JavaModelException {
		String[] signatures= method.getParameterTypes();
		String[] parameterTypes= new String[signatures.length];
		for (int i= 0; i < signatures.length; i++) {
			parameterTypes[i]= JavaModelUtil.getResolvedTypeName(signatures[i], method.getDeclaringType(), '$');
			assertNotNull("Expected resolved parameter type at index " + i + " for " + methodContext(method), parameterTypes[i]); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String className= method.getDeclaringType().getFullyQualifiedName('$');
		String methodName= method.getElementName() + '(' + String.join(",", parameterTypes) + ')'; //$NON-NLS-1$
		String uniqueId= "[engine:junit-jupiter]/[class:%s]/[test-template:%s]/[test-template-invocation:#%d]" //$NON-NLS-1$
				.formatted(className, methodName, index);
		TestRunSession session= new TestRunSession("EnumSource test", method.getJavaProject()); //$NON-NLS-1$
		TestSuiteElement suite= new TestSuiteElement(session.getTestRoot(), "suite", //$NON-NLS-1$
				methodName + '(' + className + ')', 1, null, parameterTypes, null);
		return new TestCaseElement(suite, "case", "custom(" + className + ')', //$NON-NLS-1$ //$NON-NLS-2$
				"custom display", true, null, uniqueId); //$NON-NLS-1$
	}

	/** Checks the action's editable target, not whether Jupiter will execute this value. */
	static void assertExclusionTarget(IMethod method, int index, String expected) throws JavaModelException {
		ExclusionTarget target= EnumSourceValidator.findExclusionTarget(invocation(method, index));
		assertEquals("Expected exclusion target at invocation index " + index + " for " + methodContext(method), //$NON-NLS-1$ //$NON-NLS-2$
				expected, target == null ? null : target.enumConstantName());
	}

	/** A runnable last value has no exclusion target, just like an unsupported source. */
	static void assertNoExclusionTarget(IMethod method, int index) throws JavaModelException {
		assertNull("Expected no exclusion target at invocation index " + index + " for " + methodContext(method), //$NON-NLS-1$ //$NON-NLS-2$
				EnumSourceValidator.findExclusionTarget(invocation(method, index)));
	}

	static void assertExcludedNames(IMethod method, List<String> expected) throws JavaModelException {
		assertEquals("Expected excluded names for " + methodContext(method), expected, EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
	}

	static void assertExcludeMode(IMethod method) throws JavaModelException {
		IMemberValuePair mode= member(method, "mode"); //$NON-NLS-1$
		assertNotNull("Expected an explicit EnumSource mode on " + methodContext(method), mode); //$NON-NLS-1$
		String value= (String) mode.getValue();
		assertTrue("Expected EXCLUDE mode but found " + value + " on " + methodContext(method), //$NON-NLS-1$ //$NON-NLS-2$
				"EXCLUDE".equals(value) || (value != null && value.endsWith(".EXCLUDE"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static void assertFilterRemoved(IMethod method) throws JavaModelException {
		assertNull("Expected the mode attribute to be removed from " + methodContext(method), member(method, "mode")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("Expected the names attribute to be removed from " + methodContext(method), member(method, "names")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static IMemberValuePair member(IMethod method, String name) throws JavaModelException {
		for (IAnnotation annotation : method.getAnnotations()) {
			String type= annotation.getElementName();
			if (type.equals("EnumSource") || type.equals("org.junit.jupiter.params.provider.EnumSource")) { //$NON-NLS-1$ //$NON-NLS-2$
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if (name.equals(pair.getMemberName())) {
						return pair;
					}
				}
				return null;
			}
		}
		throw new AssertionError("Missing @EnumSource on " + methodContext(method)); //$NON-NLS-1$
	}

	private EnumSourceTestSupport() {
	}
}
