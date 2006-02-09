/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

public class ResolvedMemberValuePair extends ASTAttribute {

	private final ASTAttribute fParent;
	private final String fName;
	private final IMemberValuePairBinding fPair;

	public ResolvedMemberValuePair(ASTAttribute parent, String name, IMemberValuePairBinding pair) {
		fParent= parent;
		fName= name + ": " + pair.toString();
		fPair= pair;
	}

	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		Object[] res= new Object[4];
		res[0]= new Binding(this, "METHOD BINDING", fPair.getMethodBinding(), true);
		res[1]= new ResolvedAnnotationProperty(this, "NAME", fPair.getName());
		res[2]= new ResolvedAnnotationProperty(this, "IS DEFAULT", fPair.isDefault());
		res[3]= ResolvedAnnotationProperty.convertValue(this, "VALUE", fPair.getValue());
		return res;
	}

	public IMemberValuePairBinding getPair() {
		return fPair;
	}
	
	public String getLabel() {
		return fName;
	}

	public Image getImage() {
		return null;
	}
	

}
