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

public interface ITypeConstraint {
	/**
	 * Returns <code>true</code> iff the constraint is satisfied in the program. 
	 * In program that type-check, this should always be <code>true</code>.
	 */
	public abstract boolean isSatisfied();

	/**
	 * Returns the resolved representation of the constraint.
	 * For example, if <code>toString</code> returns "[a] &lt;= [b]" and types of 'a' and 'b' are A and B,
	 * repespectively, then this method returns "A &lt;= B".
	 *
	 * This method is provided for debugging purposes only.
	 */
	public abstract String toResolvedString();

	/**
	 * Returns whether this is a simple constraint. If so, it can be safely downcast to 
	 * <code>SimpleTypeConstraint</code>.
	 */
	public boolean isSimpleTypeConstraint();
}
