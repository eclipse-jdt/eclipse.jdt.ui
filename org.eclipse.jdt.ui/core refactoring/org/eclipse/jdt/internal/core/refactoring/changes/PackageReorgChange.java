package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;

public abstract class PackageReorgChange extends Change {

	private String fPackageHandle;
	private String fNewName;
	private String fDestinationHandle;
	
	private String fNewPackageHandle;
	
	PackageReorgChange(IPackageFragment pack, IPackageFragmentRoot dest, String newName){
		fPackageHandle= pack.getHandleIdentifier();
		fDestinationHandle= dest.getHandleIdentifier();
		fNewName= newName;
	}
	
	protected abstract void doPerform(IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			
			IPackageFragment pkg= getPackage();
			String oldName= pkg.getElementName();
			IPackageFragmentRoot destination= getDestination();
			doPerform(new SubProgressMonitor(pm, 1));
			if (fNewName == null)
				fNewPackageHandle= destination.getPackageFragment(oldName).getHandleIdentifier();
			else 
				fNewPackageHandle= destination.getPackageFragment(fNewName).getHandleIdentifier();
		} finally{
			pm.done();
		}
	}
	
	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return getPackage();
	}
	
	protected IPackageFragmentRoot getDestination(){
		return (IPackageFragmentRoot)JavaCore.create(fDestinationHandle);
	}
	
	protected IPackageFragment getPackage(){
		return (IPackageFragment)JavaCore.create(fPackageHandle);
	}

	public IJavaElement getCopiedElement(){
		return JavaCore.create(fNewPackageHandle);
	}
	
	protected String getNewName() {
		return fNewName;
	}
}

