/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;

import org.eclipse.jdt.internal.ui.refactoring.UserInterfaceManager;
import org.eclipse.jdt.internal.ui.refactoring.UserInterfaceStarter;

public class DeleteUserInterfaceManager extends UserInterfaceManager {
	private static final UserInterfaceManager fgInstance= new DeleteUserInterfaceManager();

	public static UserInterfaceManager getDefault() {
		return fgInstance;
	}

	private DeleteUserInterfaceManager() {
		put(JavaDeleteProcessor.class, UserInterfaceStarter.class, DeleteWizard.class);
	}
}
