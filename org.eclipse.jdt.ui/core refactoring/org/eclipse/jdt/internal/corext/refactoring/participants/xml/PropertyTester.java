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

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Abstract superclass of all type extenders.
 * 
 * @since 3.0 
 */
public abstract class PropertyTester implements IPropertyTester {
	
	private String fMethods;
	private String fNamespace;
	
	/* package */ void initialize(String namespace, String methods) {
		Assert.isNotNull(methods);
		Assert.isNotNull(namespace);
		fMethods= methods;
		fNamespace= namespace;
	}
	
	/* (non-Javadoc)
	 */
	public final boolean handles(String namespace, String method) {
		return fNamespace.equals(namespace) && fMethods.indexOf("," + method + ",") != -1;  //$NON-NLS-1$//$NON-NLS-2$
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
	
	/* (non-Javadoc)
	 */
	public IPropertyTester load() {
		return this;
	}
}
