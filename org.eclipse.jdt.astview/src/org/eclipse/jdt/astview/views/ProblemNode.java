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

import org.eclipse.jdt.core.compiler.IProblem;

/**
 *
 */
public class ProblemNode extends ASTAttribute {

	private final IProblem fProblem;
	private final Object fParent;

	public ProblemNode(Object parent, IProblem problem) {
		fParent= parent;
		fProblem= problem;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getParent()
	 */
	public Object getParent() {
		return fParent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getChildren()
	 */
	public Object[] getChildren() {
		String[] arguments= fProblem.getArguments();
		Object[] children= new Object[arguments.length];
		for (int i= 0; i < arguments.length; i++) {
			children[i]= new LeafAttribute(this, String.valueOf(i), arguments[i]);
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getLabel()
	 */
	public String getLabel() {
		StringBuffer buf= new StringBuffer();
		int id= fProblem.getID();
		int offset= fProblem.getSourceStart();
		int length= fProblem.getSourceEnd() + 1 - offset;
		
		buf.append('[').append(offset).append(", ").append(length).append(']').append(' '); //$NON-NLS-1$
		buf.append(fProblem.getMessage()).append(' '); //$NON-NLS-1$
		buf.append("(").append(getErrorCode(id)).append(") ");  //$NON-NLS-1$//$NON-NLS-2$
		return buf.toString();
	}
	
	private String getErrorCode(int code) {
		StringBuffer buf= new StringBuffer();
			
		if ((code & IProblem.TypeRelated) != 0) {
			buf.append("TypeRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.FieldRelated) != 0) {
			buf.append("FieldRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.ConstructorRelated) != 0) {
			buf.append("ConstructorRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.MethodRelated) != 0) {
			buf.append("MethodRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.ImportRelated) != 0) {
			buf.append("ImportRelated + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Internal) != 0) {
			buf.append("Internal + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Syntax) != 0) {
			buf.append("Syntax + "); //$NON-NLS-1$
		}
		if ((code & IProblem.Javadoc) != 0) {
			buf.append("Javadoc + "); //$NON-NLS-1$
		}
		buf.append(code & IProblem.IgnoreCategoriesMask);
			
		return buf.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/**
	 * @return Returns the offset of the problem
	 */
	public int getOffset() {
		return fProblem.getSourceStart();
	}
	
	/**
	 * @return Returns the length of the problem
	 */
	public int getLength() {
		return fProblem.getSourceEnd() + 1 - fProblem.getSourceStart();
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
		
		ProblemNode other= (ProblemNode) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fProblem== null) {
			if (other.fProblem != null)
				return false;
		} else if (! fProblem.equals(other.fProblem)) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0) + (fProblem != null ? fProblem.hashCode() : 0);
	}
}
