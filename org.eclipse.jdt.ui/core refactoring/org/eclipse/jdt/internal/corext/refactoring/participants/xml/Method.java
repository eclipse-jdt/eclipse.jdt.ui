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

import org.eclipse.jdt.internal.corext.Assert;


public class Method {
	
	private Class fType;
	private String fName;
	
	private ITypeExtender fExtender;

	/* package */ Method(Class type, String name) {
		Assert.isNotNull(type);
		Assert.isNotNull(name);
		
		fType= type;
		fName= name;
	}
	
	/* package */ void setExtender(ITypeExtender extender) {
		Assert.isNotNull(extender);
		fExtender= extender;
	}
	
	public boolean isLoaded() {
		return fExtender.isLoaded();
	}
 	
	public Object invoke(Object receiver, Object[] args) throws ExpressionException {
		return fExtender.invoke(receiver, fName, args);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Method))
			return false;
		Method other= (Method)obj;
		return fType.equals(other.fType) && fName.equals(other.fName);
	}
	
	public int hashCode() {
		return (fType.hashCode() << 16) | fName.hashCode();
	}
}
