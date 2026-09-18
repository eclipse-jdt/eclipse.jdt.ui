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

/**
 * Re-includes all enum constants by removing the {@code mode} and {@code names}
 * members from {@code @EnumSource}.
 *
 * @since 3.17
 */
public final class ReincludeAllEnumValuesAction extends Action {

	private final IMethod fMethod;

	public ReincludeAllEnumValuesAction(IMethod method) {
		super(JUnitMessages.ReincludeAllEnumValuesAction_label);
		fMethod= method;
		setEnabled(method != null);
	}

	@Override
	public void run() {
		if (fMethod == null) {
			return;
		}
		try {
			if (EnumSourceValidator.removeExcludeMode(fMethod)) {
				JavaUI.openInEditor(fMethod);
			}
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}
}
