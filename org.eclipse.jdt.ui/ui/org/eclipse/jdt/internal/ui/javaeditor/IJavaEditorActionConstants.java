/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;


/**
 * Defines action IDs for private JavaEditor actions.
 * <p>
 * This interface is not intended to be implemented or extended.
 * </p>.
 */
public interface IJavaEditorActionConstants {

	/**
	 * ID of the action to toggle smart typing.
	 * Value: <code>"smartTyping"</code>
	 * @since 3.0
	 */
	String TOGGLE_SMART_TYPING= "smartTyping"; //$NON-NLS-1$

	/**
	 * ID of the smart typing status item
	 * Value: <code>"SmartTyping"</code>
	 * @since 3.0
	 */
	String STATUS_CATEGORY_SMART_TYPING= "SmartTyping"; //$NON-NLS-1$

	/**
	 * ID of the action to toggle the style of the presentation.
	 */
	String TOGGLE_PRESENTATION= "togglePresentation"; //$NON-NLS-1$

	/**
	 * ID of the action to copy the qualified name.
	 * @since 3.3
	 */
	String COPY_QUALIFIED_NAME= "copyQualifiedName"; //$NON-NLS-1$

	/**
	 * ID of the action to paste raw text and ignore smart settings.
	 * @since 4.21
	 */
	String RAW_PASTE= "rawPaste"; //$NON-NLS-1$

	/**
	 * ID of the action to open method in a new tab.
	 * @since 4.37
	 */
	String OPEN_METHOD="openMethod"; //$NON-NLS-1$
}
