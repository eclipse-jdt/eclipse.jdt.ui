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

public class JavaElementProperty extends JEAttribute {

	private final JEAttribute fParent;
	private final String fName;
	private Error[] fChildren;
	private String fValue;

	public JavaElementProperty(JEAttribute parent, String name) {
		fParent= parent;
		fName= name;
		fValue= null;
	}

	public JavaElementProperty(JEAttribute parent, String name, Object value) {
		fParent= parent;
		fName= name;
		if (value instanceof String)
			fValue= "\"" + value + "\"";
		else
			fValue= String.valueOf(value);
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public JEAttribute[] getChildren() {
		return fChildren == null ? EMPTY : fChildren;
	}

	@Override
	public String getLabel() {
		internalComputeValue();
		if (fName == null)
			return fValue;
		else
			return fName + ": " + fValue;
	}

	private void internalComputeValue() {
		if (fValue == null) {
			try {
				fValue= String.valueOf(computeValue());
			} catch (Exception e) {
				fChildren= new Error[] { new Error(this, "", e) };
				fValue= Error.ERROR;
			}
		}
	}

	protected Object computeValue() throws Exception {
		return fValue;
	}

	@Override
	public Image getImage() {
		return null;
	}

}
