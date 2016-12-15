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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jdt.core.JavaCore;

/**
 * Handler for the Rebuild Java Index command
 *
 * @since 3.13
 */
public class RebuildIndexHandler extends AbstractHandler {
	private final Job rebuildJob= Job.create(CommandsMessages.RebuildIndexHandler_jobName, monitor -> {
		JavaCore.rebuildIndex(monitor);
	});

	@Override
	public Object execute(ExecutionEvent event) {
		rebuildJob.cancel();
		rebuildJob.schedule();
		return null;
	}
}
