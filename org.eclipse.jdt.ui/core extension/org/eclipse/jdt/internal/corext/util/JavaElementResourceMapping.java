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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.jdt.core.IJavaElement;

/**
 * An abstract super class to describe mappings from a Java element to a
 * set of resources
 * 
 * @since 3.1
 */
public abstract class JavaElementResourceMapping extends ResourceMapping {
	
	public IJavaElement getJavaElement() {
		Object o= getModelObject();
		if (o instanceof IJavaElement)
			return (IJavaElement)o;
		return null;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof JavaElementResourceMapping))
			return false;
		return getJavaElement().equals(((JavaElementResourceMapping)obj).getJavaElement());
	}
	
	public int hashCode() {
		return getJavaElement().hashCode();
	}
}
