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
 * Comment tag constants.
 * 
 * @since 3.0
 */
public interface ICommentTagConstants {

	/** Comment root tags */
	public static final String[] COMMENT_ROOT_TAGS= new String[] { "@deprecated", "@see", "@since", "@version" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Tag prefix of comment tags */
	public static final char COMMENT_TAG_PREFIX= '@';
}
