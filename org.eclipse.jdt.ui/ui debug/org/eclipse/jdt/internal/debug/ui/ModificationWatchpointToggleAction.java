package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;

public class ModificationWatchpointToggleAction extends WatchpointAction {

	/**
	 * @see WatchpointAction#getToggleState(IMarker)
	 */
	protected boolean getToggleState(IJavaWatchpoint watchpoint) {
		return watchpoint.isModification();
	}

	/**
	 * @see WatchpointAction#doAction(IMarker)
	 */
	public void doAction(IJavaWatchpoint watchpoint) throws CoreException {
		watchpoint.toggleModification();
	}

}

