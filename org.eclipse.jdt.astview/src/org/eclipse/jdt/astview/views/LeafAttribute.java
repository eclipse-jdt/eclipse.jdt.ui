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
}
