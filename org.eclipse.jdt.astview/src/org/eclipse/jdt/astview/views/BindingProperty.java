/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.astview.views;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.IBinding;

/**
 *
 */
public class BindingProperty extends ASTAttribute {

	private final String fName;
	private final Binding fParent;
	private Binding[] fValues;

	public BindingProperty(Binding parent, String name, String value) {
		fParent= parent;
		if (value.length() > 0)
			fName= name + ": '" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		else 
			fName= name + ": (empty string)"; //$NON-NLS-1$
		fValues= null;
	}
	
	public BindingProperty(Binding parent, String name, boolean value) {
		fParent= parent;
		fName= name + ": " + String.valueOf(value); //$NON-NLS-1$
		fValues= null;
	}
	
	public BindingProperty(Binding parent, String name, int value) {
		fParent= parent;
		fName= name + ": " + String.valueOf(value); //$NON-NLS-1$
		fValues= null;
	}
	
	public BindingProperty(Binding parent, String name, IBinding[] bindings) {
		fParent= parent;
		if (bindings == null || bindings.length == 0) {
			fName= name + ": none"; //$NON-NLS-1$
			fValues= null;
		} else {
			fName= name;
			fValues= createBindings(bindings);
		}
	}
	
	/**
	 * @param bindings
	 * @return
	 */
	private Binding[] createBindings(IBinding[] bindings) {
		Binding[] res= new Binding[bindings.length];
		for (int i= 0; i < res.length; i++) {
			res[i]= new Binding(this, String.valueOf(i), bindings[i]);
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

}
