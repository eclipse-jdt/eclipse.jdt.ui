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

public class TypeConstraintComparer implements IElementComparer/*<ITypeConstraint2>*/ {
	public boolean equals(Object a, Object b) {
		return ((ITypeConstraint2) a).isSameAs((ITypeConstraint2) b);
	}
	public int hashCode(Object element) {
		return ((ITypeConstraint2) element).getHash();
	}
}