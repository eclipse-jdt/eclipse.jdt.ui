/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;

public interface IStatusChangeListener {
	
	/**
	 * Called to annonce that the given status has changed
	 */
	void statusChanged(IStatus status);
}