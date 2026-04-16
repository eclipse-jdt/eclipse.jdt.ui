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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;


import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

/**
 * Action that removes all exclusions from {@code @EnumSource} (removes the {@code mode} and
 * {@code names} attributes), effectively re-including all enum values.
 *
 * @since 3.15
 */
public class ReincludeAllEnumValuesAction extends Action {

	private IMethod fMethod;

	public ReincludeAllEnumValuesAction() {
		super(JUnitMessages.ReincludeAllEnumValuesAction_label);
	}

	/**
	 * Updates this action with the method to modify.
	 *
	 * @param method the test method carrying the {@code @EnumSource} annotation
	 * @param testSuiteElement the corresponding test suite element (unused, reserved for future use)
	 */
	public void update(IMethod method, TestSuiteElement testSuiteElement) {
		fMethod= method;
		setEnabled(method != null);
	}

	@Override
	public void run() {
		if (fMethod == null) {
			return;
		}
		try {
			EnumSourceValidator.removeExcludeMode(fMethod);
			try {
				JavaUI.openInEditor(fMethod);
			} catch (Exception e) {
				JUnitPlugin.log(e);
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
	}
}
