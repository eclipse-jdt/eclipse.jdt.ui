/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.cus;

import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.CompilationUnitChange;import org.eclipse.jdt.internal.core.refactoring.NullChange;

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
			return "(default package)";
		else
			return pack.getElementName();	
	}
	
	public String getName() {
		return "Move Compilation Unit " + getCorrespondingJavaElement().getElementName() + " to:" + getPackageName(getNewPackage());
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
			pm.beginTask("", 1);	
			pm.subTask("moving the compilation unit");
			ICompilationUnit cu= (ICompilationUnit)getCorrespondingJavaElement();
			cu.move(getNewPackage(), null, null, false, new SubProgressMonitor(pm, 1));
			pm.done();
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);
		}
	}
}