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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

public class CopyResourceChange extends ResourceReorgChange {
	
	public CopyResourceChange(IResource res, IContainer dest, INewNameQuery newNameQuery){
		super(res, dest, newNameQuery);
	}
	
	/* non java-doc
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected Change doPerformReorg(IPath path, IProgressMonitor pm) throws CoreException{
		getResource().copy(path, getReorgFlags(), pm);
		return null;
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("CopyResourceString.copy", //$NON-NLS-1$
			new String[]{getResource().getFullPath().toString(), getDestination().getName()});
	}
}

