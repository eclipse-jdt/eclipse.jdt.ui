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

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.junit.Messages;

/**
 * Re-includes one enum constant by removing it from the {@code @EnumSource}
 * EXCLUDE filter.
 *
 * @since 3.17
 */
public final class ReincludeEnumValueAction extends Action {

	private final IMethod fMethod;
	private final String fEnumConstantName;

	public ReincludeEnumValueAction(IMethod method, String enumConstantName) {
		super(Messages.format(JUnitMessages.ReincludeEnumValueAction_label, enumConstantName));
		fMethod= method;
		fEnumConstantName= enumConstantName;
	}

	@Override
	public void run() {
		try {
			if (EnumSourceValidator.removeValueFromExclusion(fMethod, fEnumConstantName)) {
				JavaUI.openInEditor(fMethod);
			}
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}
}
