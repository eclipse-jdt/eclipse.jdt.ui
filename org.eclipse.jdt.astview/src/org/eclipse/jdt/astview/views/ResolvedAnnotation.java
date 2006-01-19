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

import org.eclipse.jdt.core.dom.IResolvedAnnotation;

public class ResolvedAnnotation extends ASTAttribute {


	private final Object fParent;
	private final String fName;
	private final IResolvedAnnotation fAnnotation;

	public ResolvedAnnotation(Object parent, String name, IResolvedAnnotation annotation) {
		fParent= parent;
		fName= name + ": " + annotation.toString();
		fAnnotation= annotation;
	}

	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		Object[] res= new Object[3];
		res[0]= new Binding(this, "ANNOTATION TYPE", fAnnotation.getAnnotationType(), true);
		res[1]= new ResolvedAnnotationProperty(this, "DECLARED MEMBER VALUE PAIRS", fAnnotation.getDeclaredMemberValuePairs());
		res[2]= new ResolvedAnnotationProperty(this, "ALL MEMBER VALUE PAIRS", fAnnotation.getAllMemberValuePairs());
		return res;
	}

	public String getLabel() {
		return fName;
	}

	public Image getImage() {
		return null;
	}

	public IResolvedAnnotation getAnnotation() {
		return fAnnotation;
	}

}
