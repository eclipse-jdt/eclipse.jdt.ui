/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Something went wrong with launching
 * We should throw JavaModelException instead, but that will have to be done when
 * the java launch support is adopted by the model layer.
 */
public class LauncherException extends Exception implements IStatus {
	private Throwable fInnerException;
	private String fMessage;

	public LauncherException(String message, Throwable inner, int code) {
		fMessage= message;
		fInnerException= inner;
	}

	public LauncherException(String message, Throwable inner) {
		this(message, inner, ERROR);
	}

	public LauncherException(Throwable inner) {
		this(null, inner, ERROR);
	}

	public LauncherException(String message) {
		this(message, null, ERROR);
	}

	public Throwable getException() {
		return fInnerException;
	}

	public IStatus[] getChildren() {
		return new IStatus[0];
	}
	
	public int getSeverity() {
		return ERROR;
	}
	
	public String getPlugin() {
		return "org.eclipse.jdt.ui";
	}

	public int getCode() {
		return ERROR;
	}

	public boolean isOK() {
		return false;
	}

	public IPath getPath() {
		return null;
	}

	public boolean isMultiStatus() {
		return false;
	}

	public boolean matches(int mask) {
		return mask == 0 || ((mask & getCode()) != 0);
	}

	public String getMessage() {
		if (fMessage != null)
			return fMessage;
		if (fInnerException != null) {
			String msg= fInnerException.getMessage();
			if (msg != null)
				return msg;
			return fInnerException.toString();
		}
		return null;
	}

}
