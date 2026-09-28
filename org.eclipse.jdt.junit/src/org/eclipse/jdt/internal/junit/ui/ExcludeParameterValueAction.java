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

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator.ExclusionTarget;

/**
 * Context-menu action that excludes the selected enum constant from the
 * effective values of {@code @EnumSource}.
 *
 */
public final class ExcludeParameterValueAction extends Action {

	// Retain the selected value, not its index or AST. Execution revalidates the current source.
	private ExclusionTarget fExclusionTarget;

	public ExcludeParameterValueAction() {
		super(JUnitMessages.ExcludeParameterValueAction_label);
	}

	/**
	 * Updates this action for the selected test invocation.
	 *
	 * @param testCaseElement the selected test invocation
	 */
	public void update(TestCaseElement testCaseElement) {
		fExclusionTarget= EnumSourceValidator.findExclusionTarget(testCaseElement);
		setEnabled(fExclusionTarget != null);
	}

	@Override
	public void run() {
		ExclusionTarget target= fExclusionTarget;
		if (target == null) {
			setEnabled(false);
			return;
		}

		try {
			if (EnumSourceValidator.excludeEnumValue(target.method(), target.enumConstantName())) {
				JavaUI.openInEditor(target.method());
			} else {
				setEnabled(false);
			}
		} catch (JavaModelException | PartInitException ex) {
			JUnitPlugin.log(ex);
		}
	}
}
