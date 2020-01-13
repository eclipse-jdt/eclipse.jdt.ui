/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.astview.views;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 *
 */
public class CommentsProperty extends ASTAttribute {

	private final CompilationUnit fRoot;

	public CommentsProperty(CompilationUnit root) {
		fRoot= root;
	}

	@Override
	public Object getParent() {
		return fRoot;
	}

	@Override
	public Object[] getChildren() {
		List<Comment> commentList= fRoot.getCommentList();
		return (commentList == null ? EMPTY : commentList.toArray());
	}

	@Override
	public String getLabel() {
		List<Comment> commentList= fRoot.getCommentList();
		return "> comments (" +  (commentList == null ? 0 : commentList.size()) + ")";  //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public Image getImage() {
		return null;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 17;
	}
}
