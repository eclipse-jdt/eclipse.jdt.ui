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
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

public class NullChange extends Change {

	private String fName;
	
	public NullChange(String name){
		fName= name;
	}
	
	public NullChange(){
		this(null);
	}
		
	public void perform(ChangeContext context, IProgressMonitor pm) throws CoreException {
	}

	public IChange getUndoChange() {
		return new NullChange(fName);
	}
	
	public String getName(){
		return "NullChange (" + fName + ")";  //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public Object getModifiedLanguageElement(){
		return null;
	}
}
