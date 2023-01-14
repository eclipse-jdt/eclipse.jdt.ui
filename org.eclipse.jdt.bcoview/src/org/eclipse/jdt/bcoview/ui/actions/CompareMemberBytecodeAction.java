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

import java.util.ArrayList;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.action.IAction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

public class CompareMemberBytecodeAction extends BytecodeAction {

	@Override
	public void run(IAction action) {
		IJavaElement[] resources = getSelectedResources();
		try {
			exec(resources[0], resources[1]);
		} catch (Exception e) {
			BytecodeOutlinePlugin.error("Failed to run Compare: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

	@Override
	protected IJavaElement[] getSelectedResources() {
		ArrayList<Object> resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList<>();
			for (Object next : selection) {
				if (next instanceof IMember) {
					resources.add(next);
					continue;
				} else if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(IMember.class);
					if (adapter instanceof IMember) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}

		if (resources != null && !resources.isEmpty()) {
			return resources.toArray(new IJavaElement[resources.size()]);
		}

		return new IJavaElement[0];
	}
}
