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

public class TypeEquivalenceSet {
	
	private ConstraintVariable2[] fElements;
	private ITypeSet fTypeEstimate;
	
	
	public TypeEquivalenceSet(ConstraintVariable2 leftElement, ConstraintVariable2 rightElement) {
		fElements= new ConstraintVariable2[] {leftElement, rightElement };
	}

	public TypeEquivalenceSet(ConstraintVariable2 element) {
		fElements= new ConstraintVariable2[] {element};
	}

	public void add(ConstraintVariable2 element) {
		for (int i= 0; i < fElements.length; i++)
			if (fElements[i] == element)
				return;
		
		int len= fElements.length;
		ConstraintVariable2[] newElements= new ConstraintVariable2[len + 1];
		System.arraycopy(fElements, 0, newElements, 0, len);
		newElements[len]= element;
		fElements= newElements;
	}
	
	public ConstraintVariable2[] getContributingVariables() {
		return fElements;
	}

	public void addAll(ConstraintVariable2[] rightElements) {
		List elements= Arrays.asList(fElements);
		ArrayList result= new ArrayList(fElements.length + rightElements.length);
		result.addAll(elements);
		for (int i= 0; i < rightElements.length; i++) {
			ConstraintVariable2 right= rightElements[i];
			if (! result.contains(right))
				result.add(right);
		}
		fElements= (ConstraintVariable2[]) result.toArray(new ConstraintVariable2[result.size()]);
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
