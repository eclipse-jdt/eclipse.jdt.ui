/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;

/**
 * Toggles the uncaught state of an exception breakpoint
 */
public class ExceptionUncaughtToggleAction extends ExceptionAction {

	/**
	 * @see ExceptionException
	 */
	public void doAction(IJavaExceptionBreakpoint exception) throws CoreException {
		exception.setUncaught(!exception.isUncaught());
	}

	/**
	 * @see ExceptionException
	 */
	protected boolean getToggleState(IJavaExceptionBreakpoint exception) throws CoreException {
		return exception.isUncaught();
	}

}
