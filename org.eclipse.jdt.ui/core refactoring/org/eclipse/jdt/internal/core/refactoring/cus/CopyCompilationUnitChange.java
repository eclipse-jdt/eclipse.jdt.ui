package org.eclipse.jdt.internal.core.refactoring.cus;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class CopyCompilationUnitChange extends CompilationUnitReorgChange {
	
	public CopyCompilationUnitChange(ICompilationUnit cu, IPackageFragment dest){
		this(cu, dest, null);
	}
	
	public CopyCompilationUnitChange(ICompilationUnit cu, IPackageFragment dest, String newName){
		super(cu, dest, newName);
	}
		
	/* non java-doc
	 * @see CompilationUnitReorgChange#doPeform(IProgressMonitor)
	 */
	void doPeform(IProgressMonitor pm) throws JavaModelException{
		getCu().copy(getDestinationPackage(), null, getNewName(), true, pm);
	}

	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return null;
	}

	/* non java-doc
	 * @see IChange#isUndoable()
	 */	
	public boolean isUndoable(){
		return false;
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Copy " + getCu().getElementName() + " to " + getPackageName(getDestinationPackage());
	}
}

