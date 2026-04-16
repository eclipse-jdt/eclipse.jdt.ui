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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.model.ITestElement;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

/**
 * Context menu action that adds a specific enum value to the {@code @EnumSource} exclusion list.
 *
 * <p>This action is enabled when:
 * <ul>
 * <li>The selected element is a {@link TestCaseElement}</li>
 * <li>The parent test method uses {@code @ParameterizedTest} with {@code @EnumSource}</li>
 * </ul>
 *
 * <p>A warning dialog is shown if excluding the value would leave 0 or 1 values in the test.
 *
 * @since 3.15
 */
public class ExcludeParameterValueAction extends Action {

	private TestCaseElement fTestCaseElement;
	private String fEnumValueName;

	public ExcludeParameterValueAction() {
		super(JUnitMessages.ExcludeParameterValueAction_label);
	}

	/**
	 * Updates the action based on the currently selected test element.
	 *
	 * @param testCaseElement the selected test case element
	 */
	public void update(TestCaseElement testCaseElement) {
		fTestCaseElement= null;
		fEnumValueName= null;
		setEnabled(false);

		if (testCaseElement == null) {
			return;
		}

		// Populate metadata lazily
		ParameterizedTestMetadataExtractor.populate(testCaseElement);

		if (!"EnumSource".equals(testCaseElement.getParameterSourceType())) { //$NON-NLS-1$
			return;
		}

		// Extract the enum constant name from the display name
		String displayName= testCaseElement.getDisplayName();
		if (displayName == null) {
			displayName= testCaseElement.getTestMethodName();
		}
		String enumValue= EnumSourceValidator.extractEnumConstantFromDisplayName(displayName);
		if (enumValue == null || enumValue.isEmpty()) {
			return;
		}

		fTestCaseElement= testCaseElement;
		fEnumValueName= enumValue;
		setEnabled(true);
	}

	@Override
	public void run() {
		if (fTestCaseElement == null || fEnumValueName == null) {
			return;
		}

		try {
			TestSuiteElement parent= fTestCaseElement.getParent();
			if (parent == null) {
				return;
			}

			IMethod method= TestMethodFinder.findMethodForParameterizedTest(parent);
			if (method == null) {
				return;
			}

			// Warn if only 0 or 1 test invocations would remain
			ITestElement[] siblings= parent.getChildren();
			int remaining= siblings.length - 1;
			if (remaining <= 1) {
				String message= remaining == 0
						? Messages.format(JUnitMessages.ExcludeParameterValueAction_warning_noValues, fEnumValueName)
						: Messages.format(JUnitMessages.ExcludeParameterValueAction_warning_oneValue, fEnumValueName);
				boolean proceed= MessageDialog.openQuestion(
						null,
						JUnitMessages.ExcludeParameterValueAction_label,
						message);
				if (!proceed) {
					return;
				}
			}

			TestAnnotationModifier.excludeEnumValue(method, fEnumValueName);

			// Open the editor at the method
			try {
				JavaUI.openInEditor(method);
			} catch (Exception e) {
				JUnitPlugin.log(e);
			}

		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
	}
}
