/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.cus;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;

import org.eclipse.jdt.internal.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

class MoveCompilationUnitChange extends CompilationUnitChange {

	private String fPackageHandle;
		
	public MoveCompilationUnitChange(ICompilationUnit cu, IPackageFragment newPackage){
		this((IPackageFragment)cu.getParent(), cu.getElementName(), newPackage);
	}
	
	private MoveCompilationUnitChange(IPackageFragment oldPackage, String cuName, IPackageFragment newPackage){
		super(oldPackage, cuName);
		fPackageHandle= newPackage.getHandleIdentifier();
	}
	
	private IPackageFragment getNewPackage(){
		return (IPackageFragment)JavaCore.create(fPackageHandle);
	}
	
	public IJavaElement getCorrespondingJavaElement(){
		return getPackage().getCompilationUnit(getCUName());
	}
	
	private String getPackageName(IPackageFragment pack){
		if (pack.isDefaultPackage())
			return RefactoringCoreMessages.getString("MoveCompilationUnitChange.default_package"); //$NON-NLS-1$
		else
			return pack.getElementName();	
	}
	
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MoveCompilationUnitChange.name", new String[]{getCorrespondingJavaElement().getElementName(), getPackageName(getNewPackage())}); //$NON-NLS-1$
	}

	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();
		else	
			return new MoveCompilationUnitChange(getNewPackage(), getCUName(), getPackage());
	}

	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException {
		try {
			if (!isActive())
				return;
			pm.beginTask("", 1);	 //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("MoveCompilationUnitChange.moving")); //$NON-NLS-1$
			ICompilationUnit cu= (ICompilationUnit)getCorrespondingJavaElement();
			cu.move(getNewPackage(), null, null, false, new SubProgressMonitor(pm, 1));
			pm.done();
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);
		}
	}
}