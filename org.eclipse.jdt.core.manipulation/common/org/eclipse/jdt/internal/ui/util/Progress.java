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
import org.eclipse.core.runtime.SubMonitor;

public class Progress {
	private Progress() {
		// static Utility
	}

	public static IProgressMonitor subMonitor(IProgressMonitor monitor, int ticks) {
		if (monitor instanceof SubMonitor) {
			return ((SubMonitor) monitor).split(ticks);
		}
		return SubMonitor.convert(monitor, ticks);
	}

	public static IProgressMonitor subMonitorSupressed(IProgressMonitor monitor, int ticks) {
		if (monitor instanceof SubMonitor) {
			return ((SubMonitor) monitor).split(ticks, SubMonitor.SUPPRESS_SUBTASK);
		}
		return SubMonitor.convert(monitor, ticks);
	}

	public static IProgressMonitor subMonitorPrepend(IProgressMonitor monitor, int ticks) {
		// PREPEND_MAIN_LABEL_TO_SUBTASK has no direct replacement in SubMonitor.
		// Simply use split() as clients should use fully-formatted task labels instead.
		if (monitor instanceof SubMonitor) {
			return ((SubMonitor) monitor).split(ticks);
		}
		return SubMonitor.convert(monitor, ticks);
	}
}
