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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EquivalenceRepresentative {
	
	private TypeConstraintVariable2[] fElements;
	private ITypeSet fTypeEstimate;
	
	
	public EquivalenceRepresentative(TypeConstraintVariable2 leftElement, TypeConstraintVariable2 rightElement) {
		fElements= new TypeConstraintVariable2[] {leftElement, rightElement };
	}

	public EquivalenceRepresentative(TypeConstraintVariable2 element) {
		fElements= new TypeConstraintVariable2[] {element};
	}

	public void add(TypeConstraintVariable2 element) {
		for (int i= 0; i < fElements.length; i++)
			if (fElements[i] == element)
				return;
		
		int len= fElements.length;
		TypeConstraintVariable2[] newElements= new TypeConstraintVariable2[len + 1];
		System.arraycopy(fElements, 0, newElements, 0, len);
		newElements[len]= element;
		fElements= newElements;
	}
	
	public TypeConstraintVariable2[] getElements() {
		return fElements;
	}

	public void addAll(TypeConstraintVariable2[] rightElements) {
		List elements= Arrays.asList(fElements);
		ArrayList result= new ArrayList(fElements.length + rightElements.length);
		result.addAll(elements);
		for (int i= 0; i < rightElements.length; i++) {
			TypeConstraintVariable2 right= rightElements[i];
			if (! result.contains(right))
				result.add(right);
		}
		fElements= (TypeConstraintVariable2[]) result.toArray(new TypeConstraintVariable2[result.size()]);
	}

	public void setTypeEstimate(ITypeSet typeSet) {
		fTypeEstimate= typeSet;
	}
	
	public ITypeSet getTypeEstimate() {
		return fTypeEstimate;
	}
	
	public String toString() {
		StringBuffer result= new StringBuffer();
		if (fElements.length > 0)
			result.append(fElements[0].toString());
		for (int i= 1; i < fElements.length; i++) {
			result.append(" =^= \n"); //$NON-NLS-1$
			result.append(fElements[i].toString());
		}
		return result.toString();
	}
}
