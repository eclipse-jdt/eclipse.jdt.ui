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


/**
 * Abstract superclass of all type extenders. 
 */
public abstract class TypeExtender implements ITypeExtender {
	
	private String fProperties;
	
	/* package */ void initialize(String properties) {
		fProperties= properties;
	}
	
	/* (non-Javadoc)
	 */
	public final boolean handles(String property) {
		return fProperties.indexOf("," + property + ",") != -1;  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	/* (non-Javadoc)
	 */
	public final boolean isLoaded() {
		return true;
	}
	
	/* (non-Javadoc)
	 */
	public final boolean canLoad() {
		return true;
	}	
}
