/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.link;

import org.eclipse.jface.text.Position;

/**
 * A listener for highlight change notification and exititing linked mode.
 */
public interface ILinkedPositionListener {
	
	/**
	 * Notifies that the linked mode has been left. On success, all changes
	 * are kept, otherwise all changes made to the linked positions are restored
	 * to the state before entering linked mode.
	 */
	void exit(int flags);
	
	/**
	 * Notifies the changed linked position. The listener is asked
	 * to reposition the caret at the given offset.
	 * 
	 * @param position    the linked position which initiated the change.
	 * @param caretOffset the caret offset relative to the position.
	 */
	void setCurrentPosition(Position position, int caretOffset);

}
