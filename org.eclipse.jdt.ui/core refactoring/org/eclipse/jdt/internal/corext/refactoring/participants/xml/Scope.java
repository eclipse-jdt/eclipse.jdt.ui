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
 * A default implementation of interface <code>IScope</code>.
 * 
 * @since 3.0
 */
public class Scope implements IScope {

	private IScope fParent;
	private Object fDefaultVariable;
	
	/**
	 * Create a new scope with the current parent and the given
	 * default variable.
	 * 
	 * @param parent the parent scope
	 * @param defaultVariable the default variable
	 */
	public Scope(IScope parent, Object defaultVariable) {
		fParent= parent;
		fDefaultVariable= defaultVariable;
	}
	
	/* (non-Javadoc)
	 * @see IScope#getParent()
	 */
	public IScope getParent() {
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see IScope#getDefaultVariable()
	 */
	public Object getDefaultVariable() {
		if (fDefaultVariable != null)
			return fDefaultVariable;
		if (fParent != null)
			return fParent.getDefaultVariable();
		return null;
	}
}
