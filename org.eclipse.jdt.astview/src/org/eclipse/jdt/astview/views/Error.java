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


public class Error extends ExceptionAttribute {
	
	private final Binding fParent;
	private final String fLabel;
	
	public Error(Binding parent, String label, RuntimeException thrownException) {
		fParent= parent;
		fLabel= label;
		fException= thrownException;
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