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
package org.eclipse.jdt.internal.ui.javaeditor;

/**
 * Defines action IDs for private JavaEditor actions.
 */
public interface IJavaEditorActionConstants {

	/**
	 * ID of the action to toggle the style of the presentation.
	 */
	public static final String TOGGLE_PRESENTATION= "togglePresentation"; //$NON-NLS-1$

	
	/**
	 * ID of the toolbar action to go to the previous error.
	 */
	public static final String PREVIOUS_ERROR= "gotoPreviousError"; //$NON-NLS-1$
	
	/**
	 * ID of the toolbar action to go to the next error.
	 */
	public static final String NEXT_ERROR= "gotoNextError"; //$NON-NLS-1$
}
