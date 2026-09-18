/*******************************************************************************
 * Copyright (c) 2025, 2026 Carsten Hammer and others.
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

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator.ExclusionTarget;

/**
 * Context-menu action that adds the selected enum constant to the
 * {@code @EnumSource} EXCLUDE filter.
 *
 * @since 3.17
 */
public final class ExcludeParameterValueAction extends Action {

	private TestCaseElement fTestCaseElement;

	public ExcludeParameterValueAction() {
		super(JUnitMessages.ExcludeParameterValueAction_label);
	}

	/**
	 * Updates this action for the selected test invocation.
	 *
	 * @param testCaseElement the selected test invocation
	 */
	public void update(TestCaseElement testCaseElement) {
		fTestCaseElement= testCaseElement;
		setEnabled(EnumSourceValidator.findExclusionTarget(testCaseElement) != null);
	}

	@Override
	public void run() {
		ExclusionTarget target= EnumSourceValidator.findExclusionTarget(fTestCaseElement);
		if (target == null) {
			setEnabled(false);
			return;
		}

		ExclusionTarget currentTarget= target;
		int remaining= target.getRemainingValues();
		if (remaining <= 1) {
			String message= remaining == 0
					? Messages.format(JUnitMessages.ExcludeParameterValueAction_warning_noValues,
							target.getEnumConstantName())
					: Messages.format(JUnitMessages.ExcludeParameterValueAction_warning_oneValue,
							target.getEnumConstantName());
			if (!MessageDialog.openQuestion(JUnitPlugin.getActiveWorkbenchShell(),
					JUnitMessages.ExcludeParameterValueAction_label, message)) {
				return;
			}

			currentTarget= EnumSourceValidator.findExclusionTarget(fTestCaseElement);
			if (!isSameTarget(target, currentTarget)) {
				return;
			}
		}

		try {
			if (EnumSourceValidator.excludeEnumValue(
					currentTarget.getMethod(), currentTarget.getEnumConstantName())) {
				JavaUI.openInEditor(currentTarget.getMethod());
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private static boolean isSameTarget(ExclusionTarget first, ExclusionTarget second) {
		return second != null
				&& first.getMethod().equals(second.getMethod())
				&& first.getEnumConstantName().equals(second.getEnumConstantName())
				&& first.getRemainingValues() == second.getRemainingValues();
	}
}
