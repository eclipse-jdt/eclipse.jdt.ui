/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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

