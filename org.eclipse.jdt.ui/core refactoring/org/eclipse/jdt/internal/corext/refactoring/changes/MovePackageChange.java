/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

public class MovePackageChange extends PackageReorgChange {
	
	public MovePackageChange(IPackageFragment pack, IPackageFragmentRoot dest){
		super(pack, dest, null);
	}
	
	protected void doPerform(IProgressMonitor pm) throws JavaModelException{
		getPackage().move(getDestination(), null, getNewName(), true, pm);
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MovePackageChange.move", //$NON-NLS-1$
			new String[]{getPackage().getElementName(), getDestination().getElementName()});
	}
}

