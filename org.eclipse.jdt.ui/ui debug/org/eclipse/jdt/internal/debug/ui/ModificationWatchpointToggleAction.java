package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;

public class ModificationWatchpointToggleAction extends WatchpointAction {

	/**
	 * @see WatchpointAction#getToggleState(IMarker)
	 */
	protected boolean getToggleState(IMarker watchpoint) {
		return DebugJavaUtils.isModification(watchpoint);
	}

	/**
	 * @see WatchpointAction#doAction(IMarker)
	 */
	public void doAction(IMarker watchpoint) throws CoreException {
		DebugJavaUtils.setModification(watchpoint, !DebugJavaUtils.isModification(watchpoint));
	}

}

