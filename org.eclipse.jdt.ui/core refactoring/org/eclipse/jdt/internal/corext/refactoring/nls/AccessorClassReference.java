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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.dom.ITypeBinding;


public class AccessorClassReference {
    
    private ITypeBinding fBinding;
    private Region fRegion;
    
    public AccessorClassReference(ITypeBinding typeBinding, Region accessorRegion) {
        super();
        fBinding = typeBinding;
        fRegion = accessorRegion;
    }
    
	public ITypeBinding getBinding() {
		return fBinding;
	}

	public String getName() {
		return fBinding.getName();
	}

	public Region getRegion() {
		return fRegion;
	}
	
    public boolean equals(Object obj) {
        if (obj instanceof AccessorClassReference) {
            AccessorClassReference cmp = (AccessorClassReference) obj;
            return fBinding == cmp.fBinding;          
        }
        return false;        
    }
    
    public int hashCode() {
        return fBinding.hashCode();
    }
}
