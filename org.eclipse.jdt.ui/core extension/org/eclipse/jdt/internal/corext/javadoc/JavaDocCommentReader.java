/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.javadoc;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreJavaDocCommentReader;


/**
 * Reads a java doc comment from a java doc comment. Skips star-character on begin of line.
 */
public class JavaDocCommentReader extends CoreJavaDocCommentReader {
	public JavaDocCommentReader(IBuffer buf, int start, int end) {
		super(buf, start, end);
	}
}
