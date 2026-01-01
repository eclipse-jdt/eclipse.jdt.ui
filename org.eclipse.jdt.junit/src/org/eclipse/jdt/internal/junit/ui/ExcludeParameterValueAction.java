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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;

import org.eclipse.jdt.ui.JavaUI;

/**
 * Action to exclude a specific parameter value from a parameterized test
 * by modifying the @EnumSource annotation to add the test parameter value
 * to the names exclusion list.
 *
 * Only works on TestCaseElement that is a child of a parameterized test with @EnumSource.
 *
 * @since 3.15
 */
public class ExcludeParameterValueAction extends Action {

	private TestCaseElement fTestCaseElement;
	private TestRunnerViewPart fTestRunnerPart;

	public ExcludeParameterValueAction(TestRunnerViewPart testRunnerPart) {
		super(JUnitMessages.ExcludeParameterValueAction_label);
		fTestRunnerPart = testRunnerPart;
	}

	/**
	 * Update the action based on the current test case element selection.
	 *
	 * @param testElement the selected test element
	 */
	public void update(TestElement testElement) {
		fTestCaseElement = null;

		// Only enable for TestCaseElement
		if (!(testElement instanceof TestCaseElement)) {
			setEnabled(false);
			return;
		}

		TestCaseElement testCase = (TestCaseElement) testElement;

		// Ensure metadata is populated
		if (!testCase.isParameterizedTest() && testCase.getParameterSourceType() == null) {
			ParameterizedTestMetadataExtractor.populateMetadata(testCase);
		}

		// Only enable for parameterized tests with @EnumSource
		if (testCase.isParameterizedTest() && "EnumSource".equals(testCase.getParameterSourceType())) { //$NON-NLS-1$
			fTestCaseElement = testCase;
			setEnabled(true);
			return;
		}

		setEnabled(false);
	}

	@Override
	public void run() {
		if (fTestCaseElement == null) {
			return;
		}

		try {
			// Extract parameter value from test display name
			String displayName = fTestCaseElement.getDisplayName();
			String paramValue = extractParameterValue(displayName);
			if (paramValue == null) {
				return;
			}

			// Find the test method
			String className = fTestCaseElement.getTestClassName();
			String methodName = fTestCaseElement.getTestMethodName();

			IJavaProject javaProject = fTestCaseElement.getTestRunSession().getLaunchedProject();
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

			// Check how many values would remain after exclusion
			try {
				int remainingValues = EnumSourceValidator.calculateRemainingValues(method, paramValue);
				if (remainingValues >= 0 && remainingValues <= 1) {
					// Show warning dialog
					Shell shell = fTestRunnerPart.getViewSite().getShell();
					String message;
					if (remainingValues == 0) {
						message = "After excluding '" + paramValue + "', no values will remain. " //$NON-NLS-1$ //$NON-NLS-2$
								+ "Consider disabling the entire test instead."; //$NON-NLS-1$
					} else {
						message = "After excluding '" + paramValue + "', only 1 value will remain. " //$NON-NLS-1$ //$NON-NLS-2$
								+ "Consider disabling the entire test instead."; //$NON-NLS-1$
					}
					
					boolean proceed = MessageDialog.openQuestion(
						shell,
						"Only one or no values will remain", //$NON-NLS-1$
						message + "\n\nDo you want to continue?" //$NON-NLS-1$
					);
					
					if (!proceed) {
						return; // User cancelled
					}
				}
			} catch (Exception e) {
				// If we can't determine the count, log but continue
				JUnitPlugin.log(e);
			}

			// Modify the @EnumSource annotation
			TestAnnotationModifier.excludeEnumValue(method, paramValue);
			
			// After modifying, validate and remove any invalid enum names
			// This handles cases where enum constants were renamed/removed
			try {
				EnumSourceValidator.removeInvalidEnumNames(method);
			} catch (Exception e) {
				// Log but don't fail the operation if validation cleanup fails
				JUnitPlugin.log(e);
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

	/**
	 * Extract the parameter value from a parameterized test display name.
	 * JUnit 5 display names for parameterized tests can have different formats:
	 * - "testWithEnum[VALUE2]" -> "VALUE2" (enum name directly in brackets)
	 * - "[2] VALUE2" -> "VALUE2" (index in brackets followed by enum name)
	 * - "testWithEnum[2] VALUE2" -> "VALUE2" (method name, index in brackets, then enum name)
	 * - "1 ==> VALUE2" -> "VALUE2" (index followed by arrow and enum name)
	 */
	private String extractParameterValue(String displayName) {
		// Try to find enum name after the brackets (format: "testWithEnum[2] VALUE2" or "[2] VALUE2")
		int closeBracket = displayName.indexOf(']');
		if (closeBracket >= 0 && closeBracket < displayName.length() - 1) {
			String afterBracket = displayName.substring(closeBracket + 1).trim();
			if (!afterBracket.isEmpty()) {
				// Handle formats like "[2] VALUE2" or "testWithEnum[2] VALUE2"
				// Split by comma, space, or arrow to get the first token
				String[] tokens = afterBracket.split("[,\\s]"); //$NON-NLS-1$
				for (String token : tokens) {
					token = token.trim();
					// Skip empty tokens and arrow symbols
					if (!token.isEmpty() && !"==>".equals(token) && !token.equals("==")) { //$NON-NLS-1$ //$NON-NLS-2$
						return token;
					}
				}
			}
		}

		// Fallback: try to get content inside brackets (format: "testWithEnum[VALUE2]")
		int start = displayName.indexOf('[');
		int end = displayName.indexOf(']');
		if (start >= 0 && end > start) {
			String inBracket = displayName.substring(start + 1, end).trim();
			// Check if it's a number (index) or actual enum name
			if (!inBracket.matches("\\d+")) { //$NON-NLS-1$
				// Handle multiple parameters by taking the first one (enum value)
				int commaIndex = inBracket.indexOf(',');
				if (commaIndex > 0) {
					return inBracket.substring(0, commaIndex).trim();
				}
				return inBracket;
			}
		}

		return null;
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
}
