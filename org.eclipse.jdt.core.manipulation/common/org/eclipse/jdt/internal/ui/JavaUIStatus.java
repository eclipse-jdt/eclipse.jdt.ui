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
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

/**
 * Convenience class for error exceptions thrown inside JavaUI plugin.
 */
public class JavaUIStatus extends Status {

	private JavaUIStatus(int severity, int code, String message, Throwable throwable) {
		super(severity, JavaManipulation.getPreferenceNodeId(), code, message, throwable);
	}

	public static IStatus createError(int code, Throwable throwable) {
		String message= throwable.getMessage();
		if (message == null) {
			message= throwable.getClass().getName();
		}
		return new JavaUIStatus(IStatus.ERROR, code, message, throwable);
	}

	public static IStatus createError(int code, String message, Throwable throwable) {
		return new JavaUIStatus(IStatus.ERROR, code, message, throwable);
	}

	public static IStatus createWarning(int code, String message, Throwable throwable) {
		return new JavaUIStatus(IStatus.WARNING, code, message, throwable);
	}

	public static IStatus createInfo(int code, String message, Throwable throwable) {
		return new JavaUIStatus(IStatus.INFO, code, message, throwable);
	}
}

