/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

public class CopyResourceChange extends ResourceReorgChange {
	
	public CopyResourceChange(IResource res, IContainer dest, INewNameQuery newNameQuery){
		super(res, dest, newNameQuery);
	}
	
	/* non java-doc
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected void doPerform(IPath path, IProgressMonitor pm) throws CoreException{
		getResource().copy(path, getReorgFlags(), pm);
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("CopyResourceString.copy", //$NON-NLS-1$
			new String[]{getResource().getFullPath().toString(), getDestination().getName()});
	}
}

