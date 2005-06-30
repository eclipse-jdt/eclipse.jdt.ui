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

import org.eclipse.jdt.core.IJavaElement;


public class JERoot extends JEAttribute {

	private final JavaElement fJavaElement;

	public JERoot(IJavaElement javaElement) {
		fJavaElement= new JavaElement(null, javaElement);
	}

	@Override
	public JEAttribute getParent() {
		return null;
	}

	@Override
	public JEAttribute[] getChildren() {
		return new JEAttribute[] { fJavaElement };
	}

	@Override
	public String getLabel() {
		return "root: " + fJavaElement.getLabel();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JERoot other= (JERoot) obj;
		return fJavaElement.equals(other.fJavaElement);
	}
	
	@Override
	public int hashCode() {
		return fJavaElement.hashCode();
	}

}
