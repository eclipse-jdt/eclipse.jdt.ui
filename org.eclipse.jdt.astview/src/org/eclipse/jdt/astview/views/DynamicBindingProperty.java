/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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


public abstract class DynamicBindingProperty {

	protected static final Object[] EMPTY= new Object[0];

	private final Binding fParent;

	public DynamicBindingProperty(Binding parent) {
		fParent= parent;
	}

	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		return EMPTY;
	}

	public abstract String getLabel(Binding viewerElement); 

	public Image getImage() {
		return null;
	}

}
