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

import org.eclipse.jdt.internal.corext.Assert;

public abstract class ConstraintOperator {
	
	private final String fOperatorString;

	protected ConstraintOperator(String string){
		Assert.isNotNull(string);
		fOperatorString= string;
	}
		
	public String getOperatorString(){
		return fOperatorString;
	}
	
	public abstract boolean isSatisfied(ConstraintVariable var1, ConstraintVariable var2);

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getOperatorString();
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! (obj instanceof ConstraintOperator))
			return false;
		ConstraintOperator other= (ConstraintOperator)obj;
		return fOperatorString.equals(other.fOperatorString);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fOperatorString.hashCode();
	}

	public final boolean isSubtypeOperator() {
		return this instanceof SubtypeOperator;
	}

	public final boolean isStrictSubtypeOperator() {
		return this instanceof StrictSubtypeOperator;
	}

	public final boolean isEqualsOperator() {
		return this instanceof EqualsOperator;
	}

	public final boolean isDefinesOperator() {
		return this instanceof DefinesOperator;
	}
}