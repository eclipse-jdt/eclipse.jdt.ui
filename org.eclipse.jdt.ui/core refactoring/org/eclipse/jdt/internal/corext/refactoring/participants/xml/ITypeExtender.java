/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;

/**
 * A type extender can be used to add additional methods to an 
 * existing type.
 * <p>
 * This interface is not intended to be implemented by clients. Clients
 * should subclass type <code>PropertyAdapter</code>.
 * </p>
 * @since 3.0
 */
/* package */ interface ITypeExtender {

	/**
	 * Returns whether the type extender can handle the given
	 * method call or not.
	 * 
	 * @param the method to call
	 * @return <code>true</code> if the extender provides an implementation
	 *  for the given method; otherwise <code>false</code> is returned
	 */
	public boolean handles(String method);
	
	public boolean isLoaded();
	
	public boolean canLoad();
	
	/**
	 * Performs the given method on the provided receiver using the
	 * arguments passe in <code>args</code>.
	 * 
	 * @param receiver the method call's receiver
	 * @param method the method to call
	 * @param args the method arguments
	 * @return the method's result value
	 */
	public Object invoke(Object receiver, String method, Object[] args) throws CoreException;
}
