package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

class MovePackageChange extends PackageReorgChange {
	
	MovePackageChange(IPackageFragment pack, IPackageFragmentRoot dest, String newName){
		super(pack, dest, newName);
	}
	
	protected void doPerform(IProgressMonitor pm) throws JavaModelException{
		getPackage().move(getDestination(), null, getNewName(), true, pm);
	}
	
	/**
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return new NullChange();
	}
	
	public boolean isUndoable(){
		return false;
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Move package " + getPackage().getElementName() + " to " + getDestination().getElementName();
	}
}

