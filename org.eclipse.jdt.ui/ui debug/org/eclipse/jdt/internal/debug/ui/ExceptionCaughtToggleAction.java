/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;

/**
 * Toggles the caught state of an exception breakpoint
 */
public class ExceptionCaughtToggleAction extends ExceptionAction {

	/**
	 * @see ExceptionAction
	 */
	protected boolean getToggleState(IJavaExceptionBreakpoint exception) throws CoreException {
		return exception.isCaught();
	}

	/**
	 * @see ExceptionAction
	 */
	public void doAction(IJavaExceptionBreakpoint exception) throws CoreException {
		exception.setCaught(!exception.isCaught());
	}

}
