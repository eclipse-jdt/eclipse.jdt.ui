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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.internal.core.BufferManager;


public class RefactoringWorkignCopyOwner extends WorkingCopyOwner{
	
	BufferManager d= new BufferManager();
	public IBuffer createBuffer(ICompilationUnit workingCopy) {
		//XXX using internal
		return d.createBuffer(workingCopy);
	}
}
