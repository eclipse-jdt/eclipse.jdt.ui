/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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

abstract class PackageReorgChange extends Change {

	private String fPackageHandle;
	private String fNewName;
	private String fDestinationHandle;
	
	PackageReorgChange(IPackageFragment pack, IPackageFragmentRoot dest, String newName){
		fPackageHandle= pack.getHandleIdentifier();
		fDestinationHandle= dest.getHandleIdentifier();
		fNewName= newName;
	}
	
	abstract void doPerform(IProgressMonitor pm) throws JavaModelException;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			doPerform(new SubProgressMonitor(pm, 1));
		}catch (Exception e) {
			handleException(context, e);
			setActive(false);	
		} finally{
			pm.done();
		}
	}
	
	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getPackage();
	}
	
	IPackageFragmentRoot getDestination(){
		return (IPackageFragmentRoot)JavaCore.create(fDestinationHandle);
	}
	
	IPackageFragment getPackage(){
		return (IPackageFragment)JavaCore.create(fPackageHandle);
	}

	String getNewName() {
		return fNewName;
	}
}

