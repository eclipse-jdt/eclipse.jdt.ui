/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;

public interface IStatusChangeListener {
	
	/**
	 * Called to annonce that the given status has changed
	 * Status must not be <code>null</code>
	 */
	void statusChanged(IStatus status);
}