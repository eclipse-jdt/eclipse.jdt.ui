/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;

public class CreateCompilationUnitChange extends CompilationUnitChange {

	private IChange fUndoChange;
	
	public CreateCompilationUnitChange(IPackageFragment parent, String source, String name){
		super(parent, source, name);
	}
	
	public String getName(){
		return "Create Compilation Unit " + getCUName() + " in " + getPackageName(); 
	}

	public IJavaElement getCorrespondingJavaElement(){
		IPackageFragment pack= getPackage();
		return pack.getCompilationUnit(getCUName());
	}
	
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		try {
			pm.beginTask("creating resource:" + getCUName(), 1);
			if (!isActive()){
				fUndoChange= new NullChange();	
			} else{
				IPackageFragment pack= getPackage();
				ICompilationUnit cu= pack.getCompilationUnit(getCUName());
				if (cu.exists()){
					CompositeChange composite= new CompositeChange();
					composite.addChange(new DeleteCompilationUnitChange(cu));
					/* 
					 * once you delete the file it'll not be there, so
					 * there should not be an infinite loop here
					 */
					composite.addChange(new CreateCompilationUnitChange(pack, getSource(), getCUName()));
					composite.perform(context, pm);
					fUndoChange= composite.getUndoChange();
				} else {
					ICompilationUnit newCu= pack.createCompilationUnit(getCUName(), getSource(), true, pm);
					fUndoChange= new DeleteCompilationUnitChange(newCu);
				}
			}	
			pm.done();
		} catch (Exception e) {
			handleException(context, e);
			fUndoChange= new NullChange();
			setActive(false);
		}
	}

	public IChange getUndoChange() {
		return fUndoChange;
	}

}
