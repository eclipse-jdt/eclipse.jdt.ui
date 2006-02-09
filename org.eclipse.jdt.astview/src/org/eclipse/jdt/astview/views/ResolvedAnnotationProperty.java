/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

public class ResolvedAnnotationProperty extends ASTAttribute {

	private final String fName;
	private final Object fParent;
	private final Object[] fValues;

	public ResolvedAnnotationProperty(Object parent, String name, String value) {
		fParent= parent;
		if (value == null) {
			fName= name + ": null"; //$NON-NLS-1$
		} else if (value.length() > 0) {
			fName= name + ": '" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			fName= name + ": (empty string)"; //$NON-NLS-1$
		}
		fValues= null;
	}
	
	public ResolvedAnnotationProperty(Object parent, String name, boolean value) {
		fParent= parent;
		fName= name + ": " + String.valueOf(value); //$NON-NLS-1$
		fValues= null;
	}
	
	public ResolvedAnnotationProperty(Object parent, String name, int value) {
		fParent= parent;
		fName= name + ": " + String.valueOf(value); //$NON-NLS-1$
		fValues= null;
	}
	
	public ResolvedAnnotationProperty(Object parent, String name, Object[] children) {
		fParent= parent;
		if (children == null || children.length == 0) {
			fName= name + " (0)"; //$NON-NLS-1$
			fValues= null;
		} else {
			fValues= createChildren(children);
			fName= name + " (" + String.valueOf(fValues.length) + ')'; //$NON-NLS-1$
		}
	}
	
	public ResolvedAnnotationProperty(Object parent, StringBuffer label) {
		fParent= parent;
		fName= label.toString();
		fValues= null;
	}
	
	private Object[] createChildren(Object[] properties) {
		ASTAttribute[] res= new ASTAttribute[properties.length];
		for (int i= 0; i < res.length; i++) {
			Object property= properties[i];
			String name= String.valueOf(i);
			if (property instanceof ResolvedAnnotationProperty) {
				res[i]= (ResolvedAnnotationProperty) property;
			} else {
				res[i]= ResolvedAnnotationProperty.convertValue(this, name, property);
			}
		}
		return res;
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
		if (fValues != null) {
			return fValues;
		}
		return EMPTY;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getLabel()
	 */
	public String getLabel() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getImage()
	 */
	public Image getImage() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getLabel();
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
		
		ResolvedAnnotationProperty other= (ResolvedAnnotationProperty) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fName == null) {
			if (other.fName != null)
				return false;
		} else if (! fName.equals(other.fName)) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0);
	}

	public static ASTAttribute convertValue(ASTAttribute parent, String name, Object value) {
		ASTAttribute res;
		if (value instanceof ITypeBinding || value instanceof IVariableBinding) {
			res= new Binding(parent, name, (IBinding) value, true);
			
		} else if (value instanceof String) {
			res= new ResolvedAnnotationProperty(parent, name, "\"" + (String) value + "\"");
			
		} else if (value instanceof IAnnotationBinding) {
			res= new ResolvedAnnotation(parent, name, (IAnnotationBinding) value);
			
		} else if (value instanceof IMemberValuePairBinding) {
			res= new ResolvedMemberValuePair(parent, name, (IMemberValuePairBinding) value);
			
		} else if (value instanceof Object[]) {
			res= new ResolvedAnnotationProperty(parent, name, (Object[]) value);
			
		} else {
			res= new ResolvedAnnotationProperty(parent, name, value == null ? "null" : value.toString());
		}
		return res;
	}
}
