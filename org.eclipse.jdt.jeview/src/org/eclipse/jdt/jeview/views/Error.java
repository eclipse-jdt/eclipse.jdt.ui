/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;

import org.eclipse.swt.graphics.Image;


public class Error extends JEAttribute {
	public static final String ERROR= "ERROR";

	private final JEAttribute fParent;
	private final String fName;
	private final Exception fException;

	public Error(JEAttribute parent, String name, Exception exception) {
		fParent= parent;
		fName= name;
		fException= exception;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public JEAttribute[] getChildren() {
		return EMPTY;
	}

	@Override
	public String getLabel() {
		return (fName == null ? "" : fName + ": ") + fException.toString();
	}

	@Override
	public Image getImage() {
		return null;
	}

}
