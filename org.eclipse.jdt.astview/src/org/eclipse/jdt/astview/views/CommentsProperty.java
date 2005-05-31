/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.astview.views;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 *
 */
public class CommentsProperty extends ASTAttribute {
	
	private final CompilationUnit fRoot;

	public CommentsProperty(CompilationUnit root) {
		fRoot= root;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getParent()
	 */
	public Object getParent() {
		return fRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getChildren()
	 */
	public Object[] getChildren() {
		return fRoot.getCommentList().toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getLabel()
	 */
	public String getLabel() {
		return "> comments (" +  fRoot.getCommentList().size() + ")";  //$NON-NLS-1$//$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
	public int hashCode() {
		return 17;
	}
}
