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
public interface IJavaDocAttributes {

	/** Range has line break attribute */
	public static final int JAVADOC_BREAK= 1 << 0;

	/** Range has close tag attribute */
	public static final int JAVADOC_CLOSE= 1 << 1;

	/** Range has source code attribute */
	public static final int JAVADOC_CODE= 1 << 2;

	/** Range has html tag attribute */
	public static final int JAVADOC_HTML= 1 << 3;

	/** Range has the immutable region attribute */
	public static final int JAVADOC_IMMUTABLE= 1 << 4;

	/** Range has new line attribute */
	public static final int JAVADOC_NEWLINE= 1 << 5;

	/** Range has open tag attribute */
	public static final int JAVADOC_OPEN= 1 << 6;

	/** Range has paragraph attribute */
	public static final int JAVADOC_PARAGRAPH= 1 << 7;

	/** Range has parameter tag attribute */
	public static final int JAVADOC_PARAMETER= 1 << 8;

	/** Range has root tag attribute */
	public static final int JAVADOC_ROOT= 1 << 9;

	/** Range has paragraph separator attribute */
	public static final int JAVADOC_SEPARATOR= 1 << 10;

	/** Tag prefix of javadoc tags */
	public static final char JAVADOC_TAG_PREFIX= '@';
}
