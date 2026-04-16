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


import org.eclipse.jdt.internal.junit.Messages;

/**
 * Action that re-includes a single excluded enum value from {@code @EnumSource}.
 *
 * <p>Removes the given value from the {@code names} array. If the array becomes empty,
 * the {@code mode} and {@code names} attributes are removed entirely.
 *
 * @since 3.15
 */
public class ReincludeEnumValueAction extends Action {

	private final IMethod fMethod;
	private final String fEnumValueName;

	/**
	 * Creates a new action for re-including the given enum constant.
	 *
	 * @param method the test method to modify
	 * @param enumValueName the enum constant name to re-include
	 */
	public ReincludeEnumValueAction(IMethod method, String enumValueName) {
		super(Messages.format(JUnitMessages.ReincludeEnumValueAction_label, enumValueName));
		fMethod= method;
		fEnumValueName= enumValueName;
	}

	@Override
	public void run() {
		if (fMethod == null || fEnumValueName == null) {
			return;
		}
		try {
			EnumSourceValidator.removeValueFromExclusion(fMethod, fEnumValueName);
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
