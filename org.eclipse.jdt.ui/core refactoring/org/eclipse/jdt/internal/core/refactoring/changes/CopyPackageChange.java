/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.changes.*;
import org.eclipse.jdt.internal.core.refactoring.*;

public class CopyPackageChange extends PackageReorgChange {
	
	public CopyPackageChange(IPackageFragment pack, IPackageFragmentRoot dest){
		this(pack, dest, null);
	}
	
	public CopyPackageChange(IPackageFragment pack, IPackageFragmentRoot dest, String newName){
		super(pack, dest, newName);
	}
	
	protected void doPerform(IProgressMonitor pm) throws JavaModelException{
		getPackage().copy(getDestination(), null, getNewName(), true, pm);
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return new NullChange();
	}
	
	public boolean isUndoable(){
		return false;
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Copy package " + getPackage().getElementName() + " to " + getDestination().getElementName();
	}
}

