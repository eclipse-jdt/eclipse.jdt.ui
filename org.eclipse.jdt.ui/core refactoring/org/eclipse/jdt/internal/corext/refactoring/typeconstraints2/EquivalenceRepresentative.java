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
	
	private CollectionElementVariable2[] fElements;
	private TypeSet fTypeEstimate;
	
	
	public EquivalenceRepresentative(CollectionElementVariable2 leftElement, CollectionElementVariable2 rightElement) {
		fElements= new CollectionElementVariable2[] {leftElement, rightElement };
	}

	public void add(CollectionElementVariable2 element) {
		for (int i= 0; i < fElements.length; i++)
			if (fElements[i] == element)
				return;
		
		int len= fElements.length;
		CollectionElementVariable2[] newElements= new CollectionElementVariable2[len + 1];
		System.arraycopy(fElements, 0, newElements, 0, len);
		newElements[len]= element;
		fElements= newElements;
	}
	
	public CollectionElementVariable2[] getElements() {
		return fElements;
	}

	public void addAll(CollectionElementVariable2[] rightElements) {
		List elements= Arrays.asList(fElements);
		ArrayList result= new ArrayList(fElements.length + rightElements.length);
		result.addAll(elements);
		for (int i= 0; i < rightElements.length; i++) {
			CollectionElementVariable2 right= rightElements[i];
			if (! result.contains(right))
				result.add(right);
		}
		fElements= (CollectionElementVariable2[]) result.toArray(new CollectionElementVariable2[result.size()]);
	}

	public void setTypeEstimate(TypeSet typeSet) {
		fTypeEstimate= typeSet;
	}
	
	public TypeSet getTypeEstimate() {
		return fTypeEstimate;
	}
	
	public String toString() {
		StringBuffer result= new StringBuffer();
		if (fElements.length > 0)
			result.append(fElements[0].toString());
		for (int i= 1; i < fElements.length; i++) {
			result.append(" =^= "); //$NON-NLS-1$
			result.append(fElements[i].toString());
		}
		return result.toString();
	}
}
