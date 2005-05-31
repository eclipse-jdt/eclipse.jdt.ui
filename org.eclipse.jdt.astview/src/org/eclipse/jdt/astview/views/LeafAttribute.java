/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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


public class LeafAttribute extends ASTAttribute {

	private final Object fParent;
	private final String fLabel;
	
	public LeafAttribute(Object parent, String name, Object value) {
		fParent= parent;
		fLabel= name + ": " + String.valueOf(value); //$NON-NLS-1$
	}
	
	public LeafAttribute(Object parent, String label) {
		fParent= parent;
		fLabel= label;
	}
	
	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		return EMPTY;
	}

	public String getLabel() {
		return fLabel;
	}
	
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
		
		LeafAttribute other= (LeafAttribute) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fLabel == null) {
			if (other.fLabel != null)
				return false;
		} else if (! fLabel.equals(other.fLabel)) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fLabel != null ? fLabel.hashCode() : 0);
	}
}
