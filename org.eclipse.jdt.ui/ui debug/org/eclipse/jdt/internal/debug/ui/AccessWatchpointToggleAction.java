package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;

public class AccessWatchpointToggleAction extends WatchpointAction {

	/**
	 * @see WatchpointAction#getToggleState(IMarker)
	 */
	protected boolean getToggleState(IMarker watchpoint) {
		return DebugJavaUtils.isAccess(watchpoint);
	}

	/**
	 * @see WatchpointAction#doAction(IMarker)
	 */
	public void doAction(IMarker watchpoint) throws CoreException {
		DebugJavaUtils.setAccess(watchpoint, !DebugJavaUtils.isAccess(watchpoint));
	}

}

