/*****************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

/**
 * Html tag constants.
 * 
 * @since 3.0
 */
public interface IHtmlTagConstants {

	/** Html tag close prefix */
	public static final String HTML_CLOSE_PREFIX= "</"; //$NON-NLS-1$

	/** Html tag postfix */
	public static final char HTML_TAG_POSTFIX= '>';

	/** Html tag prefix */
	public static final char HTML_TAG_PREFIX= '<';
}
