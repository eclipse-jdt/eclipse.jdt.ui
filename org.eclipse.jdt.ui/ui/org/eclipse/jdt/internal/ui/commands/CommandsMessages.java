/*******************************************************************************
 * Copyright (c) 2016 Google, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
