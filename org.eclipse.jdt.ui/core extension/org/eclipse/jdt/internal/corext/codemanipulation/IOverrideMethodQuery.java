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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeHierarchy;

public interface IOverrideMethodQuery {
	
	/**
	 * Selects methods. Returns <code>null</code> if user pressed cancel.
	 */
	public IMethod[] select(IMethod[] elements, IMethod[] defaultSelected, ITypeHierarchy typeHierarchy);

}

