package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;

/**
 * Toggles the caught state of an exception breakpoint
 */

public class ExceptionUncaughtToggleAction extends ExceptionAction {

	public void doAction(IMarker exception) throws CoreException {
		DebugJavaUtils.setUncaught(exception, !DebugJavaUtils.isUncaught(exception));
	}

	protected boolean getToggleState(IMarker exception) {
		return DebugJavaUtils.isUncaught(exception);
	}

}
