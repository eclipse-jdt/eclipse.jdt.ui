/*******************************************************************************
 * Copyright (c) 2020 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.core.runtime.IProgressMonitor;

public class CompletionTimeoutProgressMonitor implements IProgressMonitor {

	private static final long JAVA_CODE_ASSIST_TIMEOUT= Long.getLong("org.eclipse.jdt.ui.codeAssistTimeout", 5000); // ms //$NON-NLS-1$

	private final long fTimeout;

	private long fEndTime;

	/**
	 * Creates a new progress monitor that gets cancelled after the given timeout.
	 *
	 * @param timeout the timeout in ms
	 * @since 3.21
	 */
	CompletionTimeoutProgressMonitor(long timeout) {
		fTimeout= timeout;
	}

	/**
	 * Creates a new progress monitor that gets cancelled after {@link #JAVA_CODE_ASSIST_TIMEOUT} ms.
	 *
	 * @since 3.21
	 */
	public CompletionTimeoutProgressMonitor() {
		this(JAVA_CODE_ASSIST_TIMEOUT);
	}

	@Override
	public void beginTask(String name, int totalWork) {
		fEndTime= System.currentTimeMillis() + fTimeout;
	}

	@Override
	public boolean isCanceled() {
		return fEndTime <= System.currentTimeMillis();
	}

	@Override
	public void done() {
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public void setCanceled(boolean value) {
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	}
}