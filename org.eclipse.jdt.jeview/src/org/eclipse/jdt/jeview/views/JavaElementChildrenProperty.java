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


public class JavaElementChildrenProperty extends JEAttribute {

	private final JEAttribute fParent;
	private final String fName;
	private JEAttribute[] fChildren;

	public JavaElementChildrenProperty(JEAttribute parent, String name) {
		fParent= parent;
		fName= name;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public JEAttribute[] getChildren() {
		if (fChildren != null)
			return fChildren;
		
		try {
			fChildren= computeChildren();
		} catch (Exception e) {
			fChildren= new JEAttribute[] {new Error(this, "", e)};
		}
		return fChildren;
	}

	protected JEAttribute[] computeChildren() throws Exception {
		return EMPTY;
	}

	@Override
	public String getLabel() {
		Object[] children= getChildren();
		String count;
		if (children.length == 1 && children[0] instanceof Error) {
			count= Error.ERROR;
		} else {
			count= String.valueOf(children.length);
		}
		return fName + " (" + count + ")";
	}

}
