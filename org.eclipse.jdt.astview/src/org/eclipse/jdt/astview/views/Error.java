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


class Error extends ASTAttribute {
	
	private final Binding fParent;
	private final String fLabel;

	public Error(Binding parent, String label) {
		fParent= parent;
		fLabel= label;
	}

	public Object[] getChildren() {
		return EMPTY;
	}

	public Image getImage() {
		return null;
	}

	public String getLabel() {
		return fLabel;
	}

	public Object getParent() {
		return fParent;
	}
}