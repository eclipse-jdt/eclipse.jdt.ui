/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Convenience class for error exceptions thrown inside JavaUI plugin.
 */
public class JavaUIStatus extends Status {

	public JavaUIStatus(int code, String message, Throwable throwable) {
		super(IStatus.ERROR, JavaPlugin.getPluginId(), code, message, throwable);
	}

	public JavaUIStatus(int code, String message) {
		this(code, message, null);
	}

	public JavaUIStatus(int code) {
		this(code, ""); //$NON-NLS-1$
	}
}

