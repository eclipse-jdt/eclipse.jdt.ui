/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;

/**
 * Toggles the caught state of an exception breakpoint
 */
public class ExceptionCaughtToggleAction extends ExceptionAction {

	/**
	 * @see ExceptionException
	 */
	public void doAction(IMarker exception) throws CoreException {
		DebugJavaUtils.setCaught(exception, !DebugJavaUtils.isCaught(exception));
	}

	/**
	 * @see ExceptionException
	 */
	protected boolean getToggleState(IMarker exception) {
		return DebugJavaUtils.isCaught(exception);
	}

}
