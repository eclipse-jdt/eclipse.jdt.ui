/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

/**
 * Defines action IDs for private JavaEditor actions.
 * 
 * @since 3.1
 */
public interface IPropertiesFileEditorActionConstants {

	/**
	 * ID of the action to toggle smart typing.
	 * Value: <code>"smartTyping"</code>
	 * @since 3.0
	 */
	public static final String TOGGLE_SMART_TYPING= "smartTyping"; //$NON-NLS-1$

	/**
	 * ID of the smart typing status item
	 * Value: <code>"SmartTyping"</code>
	 * @since 3.0
	 */
	public static final String STATUS_CATEGORY_SMART_TYPING= "SmartTyping"; //$NON-NLS-1$

	/**
	 * ID of the action to toggle the style of the presentation.
	 */
	public static final String TOGGLE_PRESENTATION= "togglePresentation"; //$NON-NLS-1$
}
