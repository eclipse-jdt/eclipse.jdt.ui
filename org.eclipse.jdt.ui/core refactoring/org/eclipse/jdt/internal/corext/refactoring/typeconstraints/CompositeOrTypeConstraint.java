/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.internal.corext.Assert;

public class CompositeOrTypeConstraint implements ITypeConstraint{
	
	private final ITypeConstraint[] fConstraints;
	
	public CompositeOrTypeConstraint(ITypeConstraint[] constraints){
		Assert.isNotNull(constraints);
		fConstraints= sort(getCopy(constraints));
	}
	
	private static ITypeConstraint[] getCopy(ITypeConstraint[] constraints) {
		List l= Arrays.asList(constraints);
		return (ITypeConstraint[]) l.toArray(new ITypeConstraint[l.size()]);
	}

	private static ITypeConstraint[] sort(ITypeConstraint[] constraints) {
		//TODO bogus to sort by toString - will have to come up with something better
		Arrays.sort(constraints, new Comparator(){
			public int compare(Object o1, Object o2) {
				return o2.toString().compareTo(o1.toString());
			}
		});
		return constraints;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.ITypeConstraint#toResolvedString()
	 */
	public String toResolvedString() {
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toResolvedString());
		}
		return buff.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.ITypeConstraint#isSimpleTypeConstraint()
	 */
	public boolean isSimpleTypeConstraint() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toString());
		}
		return buff.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! (obj instanceof CompositeOrTypeConstraint))
			return false;
			
		//TODO this is too restrictive - the sequence should not matter	
		CompositeOrTypeConstraint other= (CompositeOrTypeConstraint)obj;
		if (fConstraints.length != other.fConstraints.length)
			return false;
		for (int i= 0; i < fConstraints.length; i++) {
			if (! fConstraints[i].equals(other.fConstraints[i]))
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int hashCode= 0;
		for (int i= 0; i < fConstraints.length; i++) {
			hashCode^= fConstraints[i].hashCode();
		}
		return hashCode;
	}

	public ITypeConstraint[] getConstraints() {
		return fConstraints;
	}
}
