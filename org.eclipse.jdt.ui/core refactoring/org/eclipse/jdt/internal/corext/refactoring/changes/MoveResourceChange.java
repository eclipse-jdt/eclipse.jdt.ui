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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveResourceChange extends ResourceReorgChange {
	
	public MoveResourceChange(IResource res, IContainer dest){
		super(res, dest, null);
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, false);
	}
	
	/* non java-doc
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected Change doPerformReorg(IPath path, IProgressMonitor pm) throws CoreException{
		getResource().move(path, getReorgFlags(), pm);
		return null;
	}
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MoveResourceChange.move", //$NON-NLS-1$
			new String[]{getResource().getFullPath().toString(), getDestination().getName()});
	}
}

