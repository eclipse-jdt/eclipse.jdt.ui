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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.corext.refactoring.base.Context;

/**
 * A special viewer to present a context for a <code>RefactoringStatusEntry</code>.
 */
public interface IStatusContextViewer {
	
	/**
	 * Creates the status viewer's widget hierarchy. This method 
	 * should only be called once. Method <code>getControl()</code>
	 * should be used to retrieve the widget hierarchy.
	 * 
	 * @param parent the parent for the widget hierarchy
	 * 
	 * @see #getControl()
	 */
	public void createControl(Composite parent);
	
	/**
	 * Returns the status context viewer's SWT control.
	 * 
	 * @return the status context viewer's SWT control
	 */
	public Control getControl();	
	
	/**
	 * Sets the status context viewer's input element.
	 * 
	 * @param input the input element
	 */
	public void setInput(Context input);
}

