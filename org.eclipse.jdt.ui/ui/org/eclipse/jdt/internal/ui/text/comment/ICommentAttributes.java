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
 * General comment range attributes.
 * 
 * @since 3.0
 */
public interface ICommentAttributes {
	
	/** Range has blank line attribute */
	public static final int COMMENT_BLANKLINE= 1 << 16;
	
	/** Range has line break attribute */
	public static final int COMMENT_BREAK= 1 << 17;

	/** Range has close tag attribute */
	public static final int COMMENT_CLOSE= 1 << 18;

	/** Range has html tag attribute */
	public static final int COMMENT_HTML= 1 << 19;

	/** Range has new line attribute */
	public static final int COMMENT_NEWLINE= 1 << 20;

	/** Range has open tag attribute */
	public static final int COMMENT_OPEN= 1 << 21;
}
