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
 * Range in a javadoc comment region in comment region coordinates.
 * 
 * <p>
 * Comment ranges are considered reference objects.
 * Its offset and length may change over time.
 * </p>
 * 
 * @since 3.0
 */
public class JavaDocRange extends CommentRange {

	/** Is this range immutable? */
	private boolean fImmutable= false;

	/** Is this range a root tag? */
	private boolean fRootTag= false;

	/** Is this range a separator tag */
	private boolean fSeparatorTag= false;

	/**
	 * Creates a new javadoc range.
	 * 
	 * @param offset Offset of the range
	 * @param length Length of the range
	 */
	public JavaDocRange(int offset, int length) {
		super(offset, length);
	}

	/**
	 * Is this javadoc range immutable?
	 * 
	 * @return <code>true</code> iff this range is immutable, <code>false</code> otherwise
	 */
	public final boolean isImmutable() {
		return fImmutable;
	}

	/**
	 * Is this javadoc range a root tag?
	 * 
	 * @return <code>true</code> iff this range is a root tag, <code>false</code> otherwise
	 */
	public final boolean isRootTag() {
		return fRootTag;
	}

	/**
	 * Is this javadoc range a separator tag?
	 * 
	 * @return <code>true</code> iff this range is a separator tag, <code>false</code> otherwise
	 */
	public final boolean isSeparatorTag() {
		return fSeparatorTag;
	}

	/**
	 * Marks this javadoc range as immutable
	 */
	public final void setImmutable() {
		fImmutable= true;
	}

	/**
	 * Marks this javadoc range as being a root tag.
	 */
	public final void setRootTag() {
		fRootTag= true;
	}

	/**
	 * Marks this javadoc range as being a separator tag.
	 */
	public final void setSeparatorTag() {
		fSeparatorTag= true;
	}
}
