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
 * Javadoc comment range attributes.
 * 
 * @since 3.0
 */
public interface IJavaDocAttributes extends ICommentAttributes {

	/** Range has source code attribute */
	public static final int JAVADOC_CODE= 1 << 1;

	/** Range has the immutable region attribute */
	public static final int JAVADOC_IMMUTABLE= 1 << 2;

	/** Range has paragraph attribute */
	public static final int JAVADOC_PARAGRAPH= 1 << 3;

	/** Range has parameter tag attribute */
	public static final int JAVADOC_PARAMETER= 1 << 4;

	/** Range has root tag attribute */
	public static final int JAVADOC_ROOT= 1 << 5;

	/** Range has paragraph separator attribute */
	public static final int JAVADOC_SEPARATOR= 1 << 6;
}
