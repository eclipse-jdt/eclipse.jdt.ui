/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jface.text.Region;


public class AccessorClassInfo {
    
    private String fName;
    private Region fRegion;
    
    public AccessorClassInfo(String name, Region accessorRegion) {
        super();
        fName = name;
        fRegion = accessorRegion;
    }

	public String getName() {
		return fName;
	}

	public Region getRegion() {
		return fRegion;
	}
	
    public boolean equals(Object obj) {
        if (obj instanceof AccessorClassInfo) {
            AccessorClassInfo cmp = (AccessorClassInfo) obj;
            return fName.equals(cmp.fName);            
        }
        return false;        
    }
    
    public int hashCode() {
        return fName.hashCode();
    }
}
