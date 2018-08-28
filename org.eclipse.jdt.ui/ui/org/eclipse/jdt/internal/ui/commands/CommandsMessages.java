/*******************************************************************************
 * Copyright (c) 2016 Google, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos <sxenos@gmail.com> (Google) - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.commands;

import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the commands package.
 *
 * @since 3.13
 */
final class CommandsMessages extends NLS {

	public static String RebuildIndexHandler_jobName;

	static {
		// Initialize resource bundle
		NLS.initializeMessages(CommandsMessages.class.getName(), CommandsMessages.class);
	}

	private CommandsMessages() {
	}
}
