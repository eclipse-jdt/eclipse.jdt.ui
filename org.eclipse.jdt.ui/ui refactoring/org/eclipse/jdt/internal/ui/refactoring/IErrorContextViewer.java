/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.widgets.Control;

/**
 * A special viewer to present a context for a <code>RefactoringStatusEntry</code>.
 */
public interface  IErrorContextViewer {
	
	/**
	 * Sets the error context viewer's input element.
	 * 
	 * @param input the input element
	 */
	public void setInput(Object input);
	
	/**
	 * Returns the error context viewer's SWT control.
	 * 
	 * @return the error context viewer's SWT control
	 */
	public Control getControl();

}

