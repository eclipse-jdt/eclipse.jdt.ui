/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

public class MoveCompilationUnitChange extends CompilationUnitReorgChange {

	private boolean fUndoable;
	
	public MoveCompilationUnitChange(ICompilationUnit cu, IPackageFragment newPackage){
		super(cu, newPackage);
	}
	
	private MoveCompilationUnitChange(IPackageFragment oldPackage, String cuName, IPackageFragment newPackage){
		super(oldPackage.getHandleIdentifier(), newPackage.getHandleIdentifier(), oldPackage.getCompilationUnit(cuName).getHandleIdentifier());
	}
	
	private static boolean hasCu(IPackageFragment pack, String newName){
		return pack.getCompilationUnit(newName).exists();
	}
	
	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("MoveCompilationUnitChange.name", //$NON-NLS-1$
		new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())}); 
	}

	/* non java-doc
	 * @see IChange#isUndoable()
	 */
	public boolean isUndoable(){
		return fUndoable;
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();
		else	
			return new MoveCompilationUnitChange(getDestinationPackage(), getCu().getElementName(), getOldPackage());
	}

	/* non java-doc
	 * @see CompilationUnitReorgChange#doPeform(IProgressMonitor)
	 */
	void doPeform(IProgressMonitor pm) throws JavaModelException{
		String name;
		String newName= getNewName();
		if (newName == null)
			name= getCu().getElementName();
		else
			name= newName;	
		fUndoable= ! getDestinationPackage().getCompilationUnit(name).exists();
		
		getCu().move(getDestinationPackage(), null, newName, true, pm);
	}
}