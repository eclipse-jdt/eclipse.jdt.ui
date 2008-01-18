/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.ISelectionProvider;


/**
 * Implementors can provide a breadcrumb inside an editor.
 * 
 * <p>Clients should not implement this interface. They should
 * subclass {@link EditorBreadcrumb} instead if possible</p>
 * 
 * @since 3.4
 */
public interface IBreadcrumb {

	/**
	 * Create breadcrumb content.
	 * 
	 * @param parent the parent of the content
	 * @return the control containing the created content
	 */
	public Control createContent(Composite parent);
	
	/**
	 * @return the selection provider for this breadcrumb.
	 */
	public ISelectionProvider getSelectionProvider();
	
	/**
	 * Sets the keyboard focus inside this breadcrumb.
	 */
	public void setFocus();
	
	/**
	 * @return true if this breadcrumb has the keyboard focus
	 */
	public boolean hasFocus();

	/**
	 * Set the input of the breadcrumb to the given element
	 * @param element the input element can be <code>null</code>
	 */
	public void setInput(Object element);

	/**
	 * Dispose all resources hold by this
	 */
	public void dispose();

}
