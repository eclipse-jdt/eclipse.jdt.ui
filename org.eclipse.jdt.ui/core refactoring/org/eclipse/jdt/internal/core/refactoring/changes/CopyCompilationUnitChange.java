package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

class CopyCompilationUnitChange extends Change {
	
	private String fCuHandle;
	private String fPackageHandle;
	private String fNewName;
	private String fNewCu;
	
	CopyCompilationUnitChange(ICompilationUnit cu, IPackageFragment dest, String newName){
		fCuHandle= cu.getHandleIdentifier();
		fPackageHandle= dest.getHandleIdentifier();
		fNewName= newName;
	}
	/**
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm)	throws JavaModelException, ChangeAbortException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			ICompilationUnit cu= getCu();
			String oldName= cu.getElementName();
			cu.copy(getDestinationPackage(), null, fNewName, true, pm);
			if (fNewName == null)
				fNewCu= getDestinationPackage().getCompilationUnit(oldName).getHandleIdentifier();
			else 
				fNewCu= getDestinationPackage().getCompilationUnit(fNewName).getHandleIdentifier();
				
		} finally {
			pm.done();
		}
	}

	/**
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return null;
	}
	
	public boolean isUndoable(){
		return false;
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Copy " + getCu().getElementName() + " to " + getDestinationPackage().getElementName();
	}

	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return getCu();
	}
	
	private ICompilationUnit getCu(){
		return (ICompilationUnit)JavaCore.create(fCuHandle);
	}

	private IPackageFragment getDestinationPackage(){
		return (IPackageFragment)JavaCore.create(fPackageHandle);
	}
	
	public ICompilationUnit getCopiedElement(){
		return (ICompilationUnit)JavaCore.create(fNewCu);
	}
	
}

