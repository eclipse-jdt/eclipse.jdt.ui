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
import org.eclipse.jdt.internal.corext.refactoring.reorg.IDeepCopyQuery;

public class MoveResourceChange extends ResourceReorgChange {
	
	public MoveResourceChange(IResource res, IContainer dest, IDeepCopyQuery deepCopyQuery){
		super(res, dest, null, deepCopyQuery);
	}
	
	/* non java-doc
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected void doPerform(IPath path, IProgressMonitor pm) throws CoreException{
		getResource().move(path, getReorgFlags(), pm);
	}
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MoveResourceChange.move", //$NON-NLS-1$
			new String[]{getResource().getFullPath().toString(), getDestination().getName()});
	}
}

