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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.widgets.Control;

/**
 * Presents a preview of a <code>ChangeElement</code>
 */
public interface IPreviewViewer {

	/**
	 * Sets the previewer's input element.
	 * 
	 * @param input the input element
	 */
	public void setInput(Object input);
	
	/**
	 * Refreshes the preview viewer.
	 */
	public void refresh();
	
	/**
	 * Returns the previewer's SWT control.
	 * 
	 * @return the previewer's SWT control
	 */
	public Control getControl();
}

