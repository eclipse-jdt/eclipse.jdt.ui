/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import org.eclipse.jdt.ui.JavaUI;

/**
 * Action to disable a test by adding @Disabled (JUnit 5) or @Ignore (JUnit 4) annotation.
 * Works on:
 * - TestSuiteElement for parameterized tests (disables entire test method)
 * - TestCaseElement for normal (non-parameterized) tests
 * 
 * @since 3.15
 */
public class DisableTestAction extends Action {

	private TestElement fTestElement;
	private boolean fIsCurrentlyDisabled = false;

	public DisableTestAction(TestRunnerViewPart testRunnerPart) {
		super(JUnitMessages.DisableTestAction_label);
	}

	/**
	 * Update the action based on the current test element selection.
	 * 
	 * @param testElement the selected test element
	 */
	public void update(TestElement testElement) {
		fTestElement = testElement;
		fIsCurrentlyDisabled = false;
		
		// Enable for TestSuiteElement (parameterized test method)
		if (testElement instanceof TestSuiteElement) {
			TestSuiteElement testSuite = (TestSuiteElement) testElement;
			String testName = testSuite.getTestName();
			
			// Parameterized test methods have names like "testWithEnum(TestEnum)"
			// Check for valid method signature pattern
			if (testName != null && isParameterizedTestMethod(testName)) {
				// Check if this is actually a test method by looking up source code
				// This works even when the test has been run with @Disabled and has no children
				if (isTestMethodInSource(testSuite)) {
					updateDisabledStatusAndLabel(testElement);
					setEnabled(true);
					return;
				}
			}
		}
		
		// Enable for TestCaseElement (normal test) that is NOT a parameterized test child
		if (testElement instanceof TestCaseElement) {
			TestCaseElement testCase = (TestCaseElement) testElement;
			
			// Ensure metadata is populated
			if (!testCase.isParameterizedTest() && testCase.getParameterSourceType() == null) {
				ParameterizedTestMetadataExtractor.populateMetadata(testCase);
			}
			
			// Only enable if this is NOT a parameterized test
			if (!testCase.isParameterizedTest()) {
				updateDisabledStatusAndLabel(testElement);
				setEnabled(true);
				return;
			}
		}
		
		setEnabled(false);
	}
	
	/**
	 * Update the disabled status by checking both source code and test result,
	 * then update the action label accordingly.
	 * 
	 * @param testElement the test element to check
	 */
	private void updateDisabledStatusAndLabel(TestElement testElement) {
		// Check if method is already disabled in source code
		checkDisabledStatus(testElement);
		// Also check if test result is IGNORED (both checks can set fIsCurrentlyDisabled to true)
		updateDisabledStatusForIgnoredTest(testElement);
		updateLabel();
	}
	
	/**
	 * Update disabled status if test is ignored.
	 * If test is ignored, mark as disabled so label shows "Enable This Test".
	 * This method only sets the flag to true when needed - it does not reset it to false
	 * because fIsCurrentlyDisabled is initialized to false at the start of update().
	 * 
	 * @param testElement the test element to check
	 */
	private void updateDisabledStatusForIgnoredTest(TestElement testElement) {
		Result result = testElement.getTestResult(false);
		if (result == Result.IGNORED) {
			fIsCurrentlyDisabled = true;
		}
	}

	/**
	 * Check if a test name represents a parameterized test method.
	 * Parameterized test methods have method signatures like "testWithEnum(TestEnum)".
	 * 
	 * @param testName the test name to check
	 * @return true if it matches a parameterized test method pattern
	 */
	private boolean isParameterizedTestMethod(String testName) {
		// Match pattern: methodName(paramType) or methodName(paramType1, paramType2, ...)
		// Must have balanced parentheses with content
		int openParen = testName.indexOf('(');
		int closeParen = testName.lastIndexOf(')');
		return openParen > 0 && closeParen > openParen && closeParen == testName.length() - 1;
	}
	
	/**
	 * Check if the test suite represents an actual test method in source code.
	 * This is used to verify that a TestSuiteElement with a method signature pattern
	 * corresponds to an actual test method, even when the suite has no children
	 * (e.g., when a disabled parameterized test has been run).
	 * 
	 * @param testSuite the test suite element to check
	 * @return true if a test method exists in source code
	 */
	private boolean isTestMethodInSource(TestSuiteElement testSuite) {
		try {
			String className = testSuite.getSuiteTypeName();
			String testName = testSuite.getTestName();
			
			// Extract method name from test name (e.g., "testWithEnum(TestEnum)" -> "testWithEnum")
			int index = testName.indexOf('(');
			String methodName = index > 0 ? testName.substring(0, index) : testName;
			
			IJavaProject javaProject = testSuite.getTestRunSession().getLaunchedProject();
			if (javaProject == null) {
				return false;
			}
			
			IType type = javaProject.findType(className);
			if (type == null) {
				return false;
			}
			
			IMethod method = findTestMethod(type, methodName);
			return method != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Check if the test method is already disabled.
	 * 
	 * @param testElement the test element to check
	 */
	private void checkDisabledStatus(TestElement testElement) {
		try {
			String className;
			String methodName;
			
			if (testElement instanceof TestSuiteElement) {
				TestSuiteElement testSuite = (TestSuiteElement) testElement;
				className = testSuite.getSuiteTypeName();
				String testName = testSuite.getTestName();
				int index = testName.indexOf('(');
				methodName = index > 0 ? testName.substring(0, index) : testName;
			} else if (testElement instanceof TestCaseElement) {
				TestCaseElement testCase = (TestCaseElement) testElement;
				className = testCase.getTestClassName();
				methodName = testCase.getTestMethodName();
			} else {
				return;
			}

			IJavaProject javaProject = testElement.getTestRunSession().getLaunchedProject();
			if (javaProject == null) {
				return;
			}

			IType type = javaProject.findType(className);
			if (type == null) {
				return;
			}

			IMethod method = findTestMethod(type, methodName);
			if (method != null) {
				fIsCurrentlyDisabled = TestAnnotationModifier.isDisabled(method);
			}
		} catch (Exception e) {
			// Unable to check disabled status, assume not disabled
			fIsCurrentlyDisabled = false;
		}
	}
	
	/**
	 * Update the action label based on current disabled status.
	 */
	private void updateLabel() {
		if (fIsCurrentlyDisabled) {
			setText(JUnitMessages.DisableTestAction_enable_label);
		} else {
			setText(JUnitMessages.DisableTestAction_label);
		}
	}

	@Override
	public void run() {
		if (fTestElement == null) {
			return;
		}

		try {
			String className;
			String methodName;
			
			if (fTestElement instanceof TestSuiteElement) {
				TestSuiteElement testSuite = (TestSuiteElement) fTestElement;
				className = testSuite.getSuiteTypeName();
				String testName = testSuite.getTestName();
				int index = testName.indexOf('(');
				methodName = index > 0 ? testName.substring(0, index) : testName;
			} else if (fTestElement instanceof TestCaseElement) {
				TestCaseElement testCase = (TestCaseElement) fTestElement;
				className = testCase.getTestClassName();
				methodName = testCase.getTestMethodName();
			} else {
				return;
			}

			IJavaProject javaProject = fTestElement.getTestRunSession().getLaunchedProject();
			if (javaProject == null) {
				return;
			}

			IType type = javaProject.findType(className);
			if (type == null) {
				return;
			}

			IMethod method = findTestMethod(type, methodName);
			if (method == null) {
				return;
			}

			ICompilationUnit cu = method.getCompilationUnit();
			if (cu == null) {
				return;
			}

			// Toggle: remove if already disabled, add if not disabled
			if (fIsCurrentlyDisabled) {
				// Remove @Disabled or @Ignore annotation
				TestAnnotationModifier.removeDisabledAnnotation(method);
			} else {
				// Determine JUnit version and add appropriate annotation
				boolean isJUnit5 = isJUnit5Test(method);
				TestAnnotationModifier.addDisabledAnnotation(method, isJUnit5);
			}

			// Open the editor
			try {
				JavaUI.openInEditor(method);
			} catch (Exception e) {
				// Unable to open editor
				JUnitPlugin.log(e);
			}
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private IMethod findTestMethod(IType type, String methodName) throws JavaModelException {
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods) {
			if (method.getElementName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}

	private boolean isJUnit5Test(IMethod method) throws JavaModelException {
		ICompilationUnit cu = method.getCompilationUnit();
		if (cu == null) {
			return false;
		}

		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		final boolean[] isJUnit5 = new boolean[] { false };
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getName().getIdentifier().equals(method.getElementName())) {
					isJUnit5[0] = TestAnnotationModifier.isJUnit5TestMethod(node) || 
								 !TestAnnotationModifier.hasAnnotation(node, JUnitCorePlugin.JUNIT4_ANNOTATION_NAME);
				}
				return false;
			}
		});

		return isJUnit5[0];
	}
}
