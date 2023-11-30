/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui.actions;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.jface.action.IAction;

import org.eclipse.jdt.core.IJavaElement;

public class CompareBytecodeAction extends BytecodeAction {

	@Override
	public void run(IAction action) {
		IJavaElement[] resources = getSelectedResources();
		try {
			exec(resources[0], resources[1]);
		} catch (Exception e) {
			BytecodeOutlinePlugin.error("Failed to run Compare: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

}
