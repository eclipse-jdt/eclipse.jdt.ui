/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jdt.internal.core.SourceRange;

public class AccessorClassInfo {
    
    String fName;
    SourceRange fSourceRange;
    
    public AccessorClassInfo(String name, SourceRange sourceRange) {
        super();
        fName = name;
        fSourceRange = sourceRange;
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
