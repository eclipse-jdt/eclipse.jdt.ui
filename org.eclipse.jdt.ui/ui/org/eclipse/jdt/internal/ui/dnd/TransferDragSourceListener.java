package org.eclipse.jdt.internal.ui.dnd;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;

/**
 * A special drag source listener which is typed with a <code>TransferData</code>.
 */
public interface TransferDragSourceListener extends DragSourceListener {

	/**
	 * Returns the transfer used by this drag source.
	 */
	public Transfer getTransfer();
}