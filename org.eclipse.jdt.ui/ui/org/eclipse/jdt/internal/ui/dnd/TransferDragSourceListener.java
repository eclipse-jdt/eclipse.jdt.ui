/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;

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