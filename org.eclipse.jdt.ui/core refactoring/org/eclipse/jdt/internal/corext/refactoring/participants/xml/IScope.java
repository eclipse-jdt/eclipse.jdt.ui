/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

/**
 * A scope is used to manage a set of objects needed during
 * XML expression evaluation. A scope has a parent scope and
 * a default variable. Default variable are used during XML
 * expression evaluation if no explicit variable is referenced.
 * 
 * @since 3.0
 */
public interface IScope {

	/**
	 * Returns the parent scope or <code>null</code> if no
	 * this is the root of the scope hierarchy.
	 * 
	 * @return the parent scope or <code>null</code>
	 */
	public IScope getParent();
	
	/**
	 * Returns the default variable. If this scope doesn't
	 * manage a default variable the parent scope's default
	 * variable is returned. 
	 * 
	 * @return the default variable or <code>null</code> if
	 *  no default variable is managed.
	 */
	public Object getDefaultVariable();
}
