/*******************************************************************************
 * Copyright (c) 2023 SSI and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.core.runtime.IProgressMonitor;

@SuppressWarnings("deprecation")
public class Progress {
	private Progress() {
		// static Utility
	}

	public static IProgressMonitor subMonitor(IProgressMonitor monitor, int ticks) {
		return new org.eclipse.core.runtime.SubProgressMonitor(monitor, ticks);
	}
	public static IProgressMonitor subMonitorSupressed(IProgressMonitor monitor, int ticks) {
		return new org.eclipse.core.runtime.SubProgressMonitor(monitor, ticks, org.eclipse.core.runtime.SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
	}
	public static IProgressMonitor subMonitorPrepend(IProgressMonitor monitor, int ticks) {
		return new org.eclipse.core.runtime.SubProgressMonitor(monitor, ticks, org.eclipse.core.runtime.SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
	}
}
