package org.eclipse.jdt.internal.ui.dialogs;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

public interface IStatusInfoChangeListener {
	
	/**
	 * Called to annonce that the given status has changed
	 */
	void statusInfoChanged(StatusInfo status);
}