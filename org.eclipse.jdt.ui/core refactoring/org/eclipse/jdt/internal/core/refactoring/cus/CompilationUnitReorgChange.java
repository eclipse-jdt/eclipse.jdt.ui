package org.eclipse.jdt.internal.core.refactoring.cus;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;

abstract class CompilationUnitReorgChange extends Change {

	private String fCuHandle;
	private String fOldPackageHandle;
	private String fNewPackageHandle;
	private String fNewName;
	
	CompilationUnitReorgChange(ICompilationUnit cu, IPackageFragment dest, String newName){
		fCuHandle= cu.getHandleIdentifier();
		fNewPackageHandle= dest.getHandleIdentifier();
		fNewName= newName;
		fOldPackageHandle= cu.getParent().getHandleIdentifier();
	}
	
	CompilationUnitReorgChange(ICompilationUnit cu, IPackageFragment dest){
		this(cu, dest, null);
	}
	
	CompilationUnitReorgChange(String oldPackageHandle, String newPackageHandle, String cuHandle){
		fOldPackageHandle= oldPackageHandle;
		fNewPackageHandle= newPackageHandle;
		fCuHandle= cuHandle;
	}
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		pm.beginTask(getName(), 1);
		try{
			if (!isActive())
				return;
			ICompilationUnit cu= getCu();
			String oldName= cu.getElementName();
			doPeform(new SubProgressMonitor(pm, 1));
		}catch (Exception e) {
			handleException(context, e);
			setActive(false);
		} finally {
			pm.done();
		}
	}
	
	abstract void doPeform(IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return getCu();
	}
	
	ICompilationUnit getCu(){
		return (ICompilationUnit)JavaCore.create(fCuHandle);
	}
	
	IPackageFragment getOldPackage(){
		return (IPackageFragment)JavaCore.create(fOldPackageHandle);
	}
	
	IPackageFragment getDestinationPackage(){
		return (IPackageFragment)JavaCore.create(fNewPackageHandle);
	}
	
	String getNewName() {
		return fNewName;
	}
	static String getPackageName(IPackageFragment pack){
		if (pack.isDefaultPackage())
			return RefactoringCoreMessages.getString("MoveCompilationUnitChange.default_package"); //$NON-NLS-1$
		else
			return pack.getElementName();	
	}
}

